const { initializeApp } = require("firebase-admin/app");
const { getAuth } = require("firebase-admin/auth");
const { getFirestore, FieldValue } = require("firebase-admin/firestore");
const { getStorage } = require("firebase-admin/storage");
const { onDocumentWritten } = require("firebase-functions/v2/firestore");
const { HttpsError, onCall } = require("firebase-functions/v2/https");
const { defineSecret } = require("firebase-functions/params");
const logger = require("firebase-functions/logger");

initializeApp();

const db = getFirestore();
const storage = getStorage();
const auth = getAuth();
const openaiApiKey = defineSecret("OPENAI_API_KEY");
const MISSION_POINTS = 10;

exports.createNaverCustomToken = onCall(
  {
    region: "asia-northeast3",
  },
  async (request) => {
    const accessToken = request.data?.accessToken;
    if (!accessToken || typeof accessToken !== "string") {
      throw new HttpsError("invalid-argument", "Naver access token is required.");
    }

    const profile = await fetchNaverProfile(accessToken);
    if (!profile.id) {
      throw new HttpsError("unauthenticated", "Naver profile id is missing.");
    }

    const uid = `naver_${profile.id}`.slice(0, 128);
    const customToken = await auth.createCustomToken(uid, {
      provider: "naver",
      naverId: profile.id,
    });

    return {
      customToken,
      profile: {
        uid,
        email: profile.email || "",
        nickname: profile.nickname || profile.name || "그린챌린저",
        profileImageUrl: profile.profile_image || "",
      },
    };
  },
);

exports.verifyMissionPhoto = onDocumentWritten(
  {
    document: "users/{uid}/missionHistory/{historyId}",
    region: "asia-northeast3",
    secrets: [openaiApiKey],
  },
  async (event) => {
    const afterSnap = event.data?.after;
    if (!afterSnap?.exists) {
      return;
    }

    const history = afterSnap.data();
    if (history.verificationStatus !== "pending") {
      return;
    }

    const { uid, historyId } = event.params;
    const userRef = db.collection("users").doc(uid);
    const historyRef = afterSnap.ref;

    try {
      await historyRef.update({
        verificationStatus: "reviewing",
        verificationReason: "AI가 사진과 미션 내용을 확인하고 있습니다.",
        reviewedAt: FieldValue.serverTimestamp(),
      });

      const imageDataUrl = await loadMissionImageDataUrl(history.storagePath);
      const verification = await verifyWithOpenAI(history.title, imageDataUrl);

      if (!verification.approved) {
        await historyRef.update({
          status: "rejected",
          verificationStatus: "rejected",
          verificationReason: verification.reason,
          pointsAwarded: false,
          reviewedAt: FieldValue.serverTimestamp(),
        });
        logger.info("Mission rejected", { uid, historyId, reason: verification.reason });
        return;
      }

      await db.runTransaction(async (transaction) => {
        const userSnap = await transaction.get(userRef);
        const historySnap = await transaction.get(historyRef);

        if (!userSnap.exists || !historySnap.exists) {
          throw new Error("User or mission history not found.");
        }

        const latestHistory = historySnap.data();
        if (latestHistory.pointsAwarded === true) {
          return;
        }

        const currentPoints = userSnap.get("ecoPoints") || 0;
        const currentMissionCount = userSnap.get("missionCompletedCount") || 0;
        const newPoints = currentPoints + MISSION_POINTS;

        transaction.update(userRef, {
          ecoPoints: newPoints,
          missionCompletedCount: currentMissionCount + 1,
          growthStage: calculateGrowthStage(newPoints),
        });

        transaction.update(historyRef, {
          status: "completed",
          verificationStatus: "approved",
          verificationReason: verification.reason,
          pointsEarned: MISSION_POINTS,
          pointsAwarded: true,
          reviewedAt: FieldValue.serverTimestamp(),
        });
      });

      logger.info("Mission approved", { uid, historyId });
    } catch (error) {
      logger.error("Mission verification failed", { uid, historyId, error });
      await historyRef.update({
        status: "review_failed",
        verificationStatus: "failed",
        verificationReason: "AI 검증 중 오류가 발생했습니다. 다시 시도해주세요.",
        pointsAwarded: false,
        reviewedAt: FieldValue.serverTimestamp(),
      });
    }
  },
);

async function loadMissionImageDataUrl(storagePath) {
  if (!storagePath) {
    throw new Error("Mission storagePath is missing.");
  }

  const file = storage.bucket().file(storagePath);
  const [bytes] = await file.download();
  return `data:image/jpeg;base64,${bytes.toString("base64")}`;
}

async function fetchNaverProfile(accessToken) {
  const response = await fetch("https://openapi.naver.com/v1/nid/me", {
    method: "GET",
    headers: {
      Authorization: `Bearer ${accessToken}`,
    },
  });

  if (!response.ok) {
    const errorText = await response.text();
    logger.warn("Naver profile request failed", { status: response.status, errorText });
    throw new HttpsError("unauthenticated", "Naver access token is invalid.");
  }

  const body = await response.json();
  if (body.resultcode !== "00" || !body.response) {
    logger.warn("Naver profile response invalid", body);
    throw new HttpsError("unauthenticated", "Naver profile response is invalid.");
  }

  return body.response;
}

async function verifyWithOpenAI(missionTitle, imageDataUrl) {
  const prompt = [
    "You are validating an eco habit challenge photo.",
    "Decide whether the photo clearly satisfies the mission.",
    "Be strict: reject unrelated, unclear, fake, blank, or generic photos.",
    "Return only compact JSON with keys approved(boolean), reason(string), confidence(number 0-1).",
    `Mission: ${missionTitle}`,
  ].join("\n");

  const response = await fetch("https://api.openai.com/v1/responses", {
    method: "POST",
    headers: {
      "Authorization": `Bearer ${openaiApiKey.value()}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      model: "gpt-4.1-mini",
      input: [
        {
          role: "user",
          content: [
            { type: "input_text", text: prompt },
            { type: "input_image", image_url: imageDataUrl, detail: "low" },
          ],
        },
      ],
    }),
  });

  if (!response.ok) {
    const errorText = await response.text();
    throw new Error(`OpenAI API error ${response.status}: ${errorText}`);
  }

  const body = await response.json();
  const outputText = extractOutputText(body);
  const parsed = JSON.parse(outputText);

  return {
    approved: parsed.approved === true && Number(parsed.confidence || 0) >= 0.55,
    reason: parsed.reason || "검증 결과 사유가 없습니다.",
    confidence: Number(parsed.confidence || 0),
  };
}

function extractOutputText(body) {
  if (typeof body.output_text === "string") {
    return body.output_text;
  }

  const message = (body.output || []).find((item) => item.type === "message");
  const textPart = message?.content?.find((part) => part.type === "output_text");
  if (textPart?.text) {
    return textPart.text;
  }

  throw new Error("No text output returned from OpenAI.");
}

function calculateGrowthStage(points) {
  if (points < 3) {
    return 1;
  }
  if (points < 7) {
    return 2;
  }
  return 3;
}
