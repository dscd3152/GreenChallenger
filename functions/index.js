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
    const submissionId = history.submissionId || history.storagePath;

    try {
      const claimed = await db.runTransaction(async (transaction) => {
        const latestSnap = await transaction.get(historyRef);
        if (!isSameSubmission(latestSnap, submissionId)
            || latestSnap.get("verificationStatus") !== "pending") {
          return false;
        }
        transaction.update(historyRef, {
          verificationStatus: "reviewing",
          verificationReason: "AI가 사진과 미션 내용을 확인하고 있습니다.",
          reviewedAt: FieldValue.serverTimestamp(),
        });
        return true;
      });
      if (!claimed) {
        logger.info("Stale verification request ignored before analysis", {
          uid, historyId, submissionId,
        });
        return;
      }

      const imageDataUrl = await loadMissionImageDataUrl(history.storagePath);
      const verification = await verifyWithOpenAI(history.title, imageDataUrl);

      if (!verification.approved) {
        const updated = await db.runTransaction(async (transaction) => {
          const latestSnap = await transaction.get(historyRef);
          if (!isSameSubmission(latestSnap, submissionId)) {
            return false;
          }

          transaction.update(historyRef, {
            status: "rejected",
            verificationStatus: "rejected",
            verificationReason: verification.reason,
            pointsAwarded: history.pointsAwarded === true,
            reviewedAt: FieldValue.serverTimestamp(),
          });
          return true;
        });
        logger.info(updated ? "Mission rejected" : "Stale rejection ignored", {
          uid, historyId, submissionId, reason: verification.reason,
        });
        return;
      }

      const approved = await db.runTransaction(async (transaction) => {
        const userSnap = await transaction.get(userRef);
        const historySnap = await transaction.get(historyRef);

        if (!userSnap.exists || !historySnap.exists) {
          throw new Error("User or mission history not found.");
        }

        if (!isSameSubmission(historySnap, submissionId)) {
          return false;
        }

        const latestHistory = historySnap.data();
        if (latestHistory.pointsAwarded === true) {
          transaction.update(historyRef, {
            status: "completed",
            verificationStatus: "approved",
            verificationReason: `${verification.reason} 오늘 미션 포인트는 이미 지급되었습니다.`,
            pointsEarned: MISSION_POINTS,
            reviewedAt: FieldValue.serverTimestamp(),
          });
          return true;
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
        return true;
      });

      logger.info(approved ? "Mission approved" : "Stale approval ignored", {
        uid, historyId, submissionId,
      });
    } catch (error) {
      logger.error("Mission verification failed", { uid, historyId, error });
      await db.runTransaction(async (transaction) => {
        const latestSnap = await transaction.get(historyRef);
        if (!isSameSubmission(latestSnap, submissionId)) {
          return;
        }
        transaction.update(historyRef, {
          status: "review_failed",
          verificationStatus: "failed",
          verificationReason: "AI 검증 중 오류가 발생했습니다. 다시 촬영해서 시도해주세요.",
          pointsAwarded: history.pointsAwarded === true,
          reviewedAt: FieldValue.serverTimestamp(),
        });
      });
    }
  },
);

function isSameSubmission(snapshot, submissionId) {
  if (!snapshot.exists) {
    return false;
  }
  const latest = snapshot.data();
  return (latest.submissionId || latest.storagePath) === submissionId;
}

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
  const missionRule = getMissionRule(missionTitle);
  const prompt = [
    "You are a strict photo verifier for an eco habit challenge app.",
    "Approve only when the visible evidence clearly proves the exact mission.",
    "Do not approve merely because the photo contains an eco-related object.",
    "When uncertain or when the main object is ambiguous, reject the photo.",
    "The boolean approved, reason, detectedItems, and confidence must never contradict each other.",
    "detectedItems must list only objects actually visible in the photo. Never include comparison objects that are absent, such as saying 'not a PET bottle'.",
    `Approval requirement: ${missionRule.requirement}`,
    `Mandatory rejection cases: ${missionRule.rejections}`,
    "Write the reason in Korean for a mobile app user.",
    "Return only compact JSON with keys approved(boolean), reason(string), confidence(number 0-1), detectedItems(array of short Korean strings).",
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
            { type: "input_image", image_url: imageDataUrl, detail: "high" },
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
  const parsed = parseJsonOutput(outputText);
  const confidence = Number(parsed.confidence || 0);
  const detectedItems = Array.isArray(parsed.detectedItems)
    ? parsed.detectedItems.map((item) => String(item))
    : [];
  const detectedText = detectedItems.join(" ").toLowerCase();
  const hasPositiveEvidence = missionRule.positiveTerms.some(
    (term) => detectedText.includes(term.toLowerCase()),
  );
  const hasForbiddenEvidence = !hasPositiveEvidence && missionRule.forbiddenTerms.some(
    (term) => detectedText.includes(term.toLowerCase()),
  );
  const approved = parsed.approved === true && confidence >= 0.72 && !hasForbiddenEvidence;

  return {
    approved,
    reason: hasForbiddenEvidence
      ? missionRule.forbiddenReason
      : parsed.reason || "검증 결과 사유가 없습니다.",
    confidence,
  };
}

function getMissionRule(missionTitle) {
  const rules = {
    "텀블러 사용하기": {
      requirement: "A clearly reusable tumbler, reusable cup, or reusable shaker bottle must be the main visible object. Transparent reusable plastic shakers with a wide screw lid, flip cap, handle, mixing insert, or reusable design count as valid tumblers.",
      rejections: "Reject factory-packaged PET beverage bottles, disposable plastic bottles, disposable cups, cans, ordinary packaged drinks, or photos where a reusable tumbler cannot be clearly identified. Do not reject an item only because it is transparent or plastic when its reusable lid and shaker or tumbler structure are visible.",
      positiveTerms: ["텀블러", "재사용", "다회용", "쉐이커", "보온컵", "머그", "스탠리", "stanley", "travel mug", "reusable"],
      forbiddenTerms: ["페트병", "pet병", "pet bottle", "플라스틱 병", "일회용 병", "일회용 컵", "생수병", "음료수병"],
      forbiddenReason: "사진에서 텀블러가 아닌 페트병 또는 일회용 용기가 확인되어 인증할 수 없습니다. 재사용 가능한 텀블러가 잘 보이게 다시 촬영해주세요.",
    },
    "대중교통 이용하기": {
      requirement: "The photo must clearly show actual use of public transit, such as being inside a bus/subway/train, a transit vehicle with boarding evidence, or a readable transit stop/platform context.",
      rejections: "Reject private cars, roads alone, generic outdoor photos, maps, screenshots, or transit objects without evidence of use.",
      positiveTerms: ["버스", "지하철", "전철", "기차", "대중교통", "승강장", "정류장"],
      forbiddenTerms: ["자가용", "승용차", "private car"],
      forbiddenReason: "대중교통 이용 장면이 명확하지 않습니다. 버스나 지하철 이용 상황이 잘 보이게 다시 촬영해주세요.",
    },
    "분리수거 실천하기": {
      requirement: "The photo must clearly show recyclables being sorted or placed in an appropriate recycling bin or separated collection area.",
      rejections: "Reject mixed trash, a single random object, general trash bins, or recyclables not visibly being separated.",
      positiveTerms: ["분리수거", "재활용", "재활용품", "분리배출"],
      forbiddenTerms: ["일반 쓰레기", "혼합 쓰레기", "무단 투기"],
      forbiddenReason: "분리배출을 실천하는 장면이 명확하지 않습니다. 분리수거함과 분리된 재활용품이 함께 보이게 촬영해주세요.",
    },
    "플라스틱 줄이기": {
      requirement: "The photo must clearly show a reusable alternative replacing disposable plastic, such as a reusable shopping bag, reusable container, or refill product.",
      rejections: "Reject disposable plastic bags, PET bottles, plastic packaging alone, or generic objects without a visible reusable alternative.",
      positiveTerms: ["장바구니", "다회용", "재사용", "리필"],
      forbiddenTerms: ["비닐봉투", "페트병", "일회용 플라스틱", "plastic bag"],
      forbiddenReason: "일회용 플라스틱을 줄이는 행동이 명확하지 않습니다. 장바구니나 다회용 용품이 잘 보이게 다시 촬영해주세요.",
    },
    "잔반 남기지 않기": {
      requirement: "The photo must clearly show a finished meal plate or bowl with no meaningful food remaining.",
      rejections: "Reject plates with noticeable leftovers, untouched food, empty tables, or photos where the meal plate cannot be identified.",
      positiveTerms: ["빈 접시", "빈 그릇", "깨끗한 접시", "식사 완료"],
      forbiddenTerms: ["잔반", "남은 음식", "음식이 남아", "leftover"],
      forbiddenReason: "접시에 남은 음식이 확인되어 인증할 수 없습니다. 식사를 마친 빈 접시가 잘 보이게 촬영해주세요.",
    },
  };

  return rules[missionTitle] || {
    requirement: "The photo must clearly and directly prove the exact mission action.",
    rejections: "Reject unrelated, unclear, fake, blank, generic, or ambiguous photos.",
    positiveTerms: [],
    forbiddenTerms: [],
    forbiddenReason: "미션 수행 장면이 명확하지 않습니다. 행동이 잘 보이게 다시 촬영해주세요.",
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

function parseJsonOutput(text) {
  try {
    return JSON.parse(text);
  } catch (error) {
    const start = text.indexOf("{");
    const end = text.lastIndexOf("}");
    if (start !== -1 && end !== -1 && end > start) {
      return JSON.parse(text.slice(start, end + 1));
    }
    throw error;
  }
}

function calculateGrowthStage(points) {
  if (points < 50) {
    return 1;
  }
  if (points < 150) {
    return 2;
  }
  return 3;
}
