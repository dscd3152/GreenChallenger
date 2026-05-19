package com.example.greenchallenger;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageException;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MissionDetailActivity extends AppCompatActivity {

    private static final String TAG = "MissionDetail";
    private static final String STORAGE_BUCKET_URL = "gs://greeen-challenger.firebasestorage.app";

    private TextView txtMissionTitle, txtMissionDetail;
    private TextView txtVerificationStatus, txtVerificationReason;
    private ProgressBar progressVerification;
    private Button btnCompleteMission, btnTakePhoto;

    private boolean isCompleted = false;
    private String photoUri = "";
    private String photoPath = "";
    private ListenerRegistration verificationListener;
    private final Handler verificationHandler = new Handler(Looper.getMainLooper());
    private Runnable verificationTimeoutRunnable;

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;

    // 카메라 촬영 결과 받기
    private final ActivityResultLauncher<Intent> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    if (result.getData() != null) {
                        photoUri = result.getData().getStringExtra("photoUri");
                        photoPath = result.getData().getStringExtra("photoPath");
                    }

                    if ((photoUri == null || photoUri.isEmpty()) && (photoPath == null || photoPath.isEmpty())) {
                        Toast.makeText(this, "사진 정보를 받지 못했습니다. 다시 촬영해주세요.", Toast.LENGTH_SHORT).show();
                        isCompleted = false;
                        btnCompleteMission.setEnabled(false);
                        return;
                    }

                    Toast.makeText(this, "사진이 인증되었습니다. 미션 완료 준비!", Toast.LENGTH_SHORT).show();
                    isCompleted = true;
                    btnCompleteMission.setEnabled(true);
                    btnCompleteMission.setText("AI 검증 요청하기");
                    updateVerificationUi("ready", "사진 준비 완료", "AI 검증 요청을 누르면 미션 수행 여부를 확인합니다.");
                } else {
                    Toast.makeText(this, "사진 촬영이 취소되었습니다.", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mission_detail);

        txtMissionTitle = findViewById(R.id.txtMissionTitle);
        txtMissionDetail = findViewById(R.id.txtMissionDetail);
        txtVerificationStatus = findViewById(R.id.txtVerificationStatus);
        txtVerificationReason = findViewById(R.id.txtVerificationReason);
        progressVerification = findViewById(R.id.progressVerification);
        btnCompleteMission = findViewById(R.id.btnCompleteMission);
        btnTakePhoto = findViewById(R.id.btnTakePhoto);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance(STORAGE_BUCKET_URL);

        String title = getIntent().getStringExtra("missionTitle");
        String detail = getIntent().getStringExtra("missionDetail");

        txtMissionTitle.setText(title);
        txtMissionDetail.setText(detail);

        btnCompleteMission.setEnabled(false);
        updateVerificationUi("idle", "사진 인증 전", "먼저 미션 수행 사진을 촬영해주세요.");

        btnTakePhoto.setOnClickListener(v -> {
            Intent intent = new Intent(this, CameraActivity.class);
            cameraLauncher.launch(intent);
        });

        btnCompleteMission.setOnClickListener(v -> {
            if (!isCompleted) {
                Toast.makeText(this, "사진 인증 후 완료할 수 있습니다.", Toast.LENGTH_SHORT).show();
                return;
            }

            uploadPhotoAndCompleteMission(title);
        });
    }

    private void uploadPhotoAndCompleteMission(String title) {
        FirebaseUser currentUser = auth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "로그인된 사용자가 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (photoUri == null || photoUri.isEmpty()) {
            Toast.makeText(this, "사진 인증 정보가 없습니다. 다시 촬영해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = currentUser.getUid();

        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(new Date());
        String missionId = title.replace(" ", "_");
        String docId = missionId + "_" + today;
        String safeMissionId = String.valueOf(Math.abs(title.hashCode()));
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.KOREA).format(new Date());
        String storagePath = "mission_photos/" + uid + "/" + safeMissionId + "_" + timeStamp + ".jpg";

        btnCompleteMission.setEnabled(false);
        btnCompleteMission.setText("완료 여부 확인 중...");
        updateVerificationUi("reviewing", "완료 여부 확인 중", "오늘 이미 제출한 미션인지 확인하고 있습니다.");

        db.collection("users")
                .document(uid)
                .collection("missionHistory")
                .document(docId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String status = documentSnapshot.getString("verificationStatus");
                        String reason = documentSnapshot.getString("verificationReason");

                        if ("approved".equals(status) || "pending".equals(status) || "reviewing".equals(status)) {
                            Toast.makeText(this, "오늘 이미 제출한 미션입니다.", Toast.LENGTH_SHORT).show();
                            showExistingMissionStatus(status, reason);
                            return;
                        }
                    }

                    uploadPhotoToStorage(uid, missionId, title, today, docId, storagePath);
                })
                .addOnFailureListener(e -> {
                    btnCompleteMission.setEnabled(true);
                    btnCompleteMission.setText("AI 검증 요청하기");
                    updateVerificationUi("failed", "확인 실패", e.getMessage());
                    Toast.makeText(this, "미션 완료 여부 확인 실패: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void uploadPhotoToStorage(String uid, String missionId, String title, String today,
                                      String docId, String storagePath) {
        btnCompleteMission.setText("사진 업로드 중...");
        btnTakePhoto.setEnabled(false);
        updateVerificationUi("reviewing", "사진 업로드 중", "AI 검증을 위해 사진을 서버에 저장하고 있습니다.");

        StorageReference photoRef = storage.getReference().child(storagePath);
        byte[] photoBytes = readPhotoBytes();

        if (photoBytes == null || photoBytes.length == 0) {
            btnCompleteMission.setEnabled(true);
            btnCompleteMission.setText("AI 검증 요청하기");
            btnTakePhoto.setEnabled(true);
            updateVerificationUi("failed", "사진 파일 확인 실패", "촬영한 사진 파일을 찾을 수 없습니다. 다시 촬영해주세요.");
            return;
        }

        StorageMetadata metadata = new StorageMetadata.Builder()
                .setContentType("image/jpeg")
                .build();

        Log.d(TAG, "Uploading mission photo to " + STORAGE_BUCKET_URL + "/" + storagePath
                + ", bytes=" + photoBytes.length);

        photoRef.putBytes(photoBytes, metadata)
                .addOnSuccessListener(taskSnapshot -> {
                    saveMissionToFirestore(uid, missionId, title, today, docId, "", storagePath);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Photo upload failed. path=" + storagePath, e);
                    btnCompleteMission.setEnabled(true);
                    btnCompleteMission.setText("AI 검증 요청하기");
                    btnTakePhoto.setEnabled(true);
                    String message = buildStorageErrorMessage(e);
                    updateVerificationUi("failed", "사진 업로드 실패", message);
                    Toast.makeText(this, "사진 업로드 실패: " + message, Toast.LENGTH_LONG).show();
                });
    }

    private void saveMissionToFirestore(String uid, String missionId, String title, String today,
                                        String docId, String photoUrl, String storagePath) {
        DocumentReference userRef = db.collection("users").document(uid);
        DocumentReference historyRef = userRef.collection("missionHistory").document(docId);

        MissionHistory history = new MissionHistory(
                missionId,
                title,
                today,
                photoUrl,
                storagePath,
                0,
                "pending",
                "pending",
                "AI 검증 대기 중입니다.",
                false
        );

        btnCompleteMission.setText("미션 저장 중...");
        updateVerificationUi("reviewing", "AI 검증 준비 중", "사진 제출 기록을 저장하고 있습니다.");

        db.runTransaction(transaction -> {
                    com.google.firebase.firestore.DocumentSnapshot userSnapshot = transaction.get(userRef);
                    if (!userSnapshot.exists()) {
                        throw new IllegalStateException("사용자 정보가 없습니다.");
                    }

                    transaction.set(historyRef, history);
                    return null;
                })
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "AI 검증을 시작했습니다.", Toast.LENGTH_SHORT).show();
                    listenVerificationResult(uid, docId, title, photoUrl);
                })
                .addOnFailureListener(e -> {
                    btnCompleteMission.setEnabled(true);
                    btnCompleteMission.setText("AI 검증 요청하기");
                    btnTakePhoto.setEnabled(true);
                    updateVerificationUi("failed", "미션 저장 실패", e.getMessage());
                    Toast.makeText(this, "미션 저장 실패: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void listenVerificationResult(String uid, String docId, String title, String photoUrl) {
        if (verificationListener != null) {
            verificationListener.remove();
        }

        btnCompleteMission.setEnabled(false);
        btnTakePhoto.setEnabled(false);
        updateVerificationUi("reviewing", "AI 검증 중", "사진과 미션 내용을 비교하고 있습니다. 잠시만 기다려주세요.");

        verificationListener = db.collection("users")
                .document(uid)
                .collection("missionHistory")
                .document(docId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        updateVerificationUi("failed", "검증 상태 확인 실패", error.getMessage());
                        btnCompleteMission.setEnabled(true);
                        btnTakePhoto.setEnabled(true);
                        return;
                    }

                    if (snapshot == null || !snapshot.exists()) {
                        return;
                    }

                    String verificationStatus = snapshot.getString("verificationStatus");
                    String reason = snapshot.getString("verificationReason");

                    if ("approved".equals(verificationStatus)) {
                        updateVerificationUi("approved", "인증 완료", reason != null ? reason : "미션 사진이 승인되었습니다.");
                        Toast.makeText(this, "AI 인증 완료! 포인트가 지급되었습니다.", Toast.LENGTH_SHORT).show();

                        Intent resultIntent = new Intent();
                        resultIntent.putExtra("completedMission", title);
                        resultIntent.putExtra("photoUrl", photoUrl);
                        setResult(RESULT_OK, resultIntent);
                        clearVerificationListener();
                        finish();
                    } else if ("rejected".equals(verificationStatus)) {
                        updateVerificationUi("rejected", "인증 실패", reason != null ? reason : "미션 조건이 충분히 보이지 않습니다. 다시 촬영해주세요.");
                        btnCompleteMission.setEnabled(false);
                        btnCompleteMission.setText("AI 검증 요청하기");
                        btnTakePhoto.setEnabled(true);
                        isCompleted = false;
                        photoUri = "";
                        photoPath = "";
                        clearVerificationListener();
                    } else if ("failed".equals(verificationStatus)) {
                        updateVerificationUi("failed", "검증 오류", reason != null ? reason : "검증 중 오류가 발생했습니다. 다시 시도해주세요.");
                        btnCompleteMission.setEnabled(true);
                        btnCompleteMission.setText("AI 검증 요청하기");
                        btnTakePhoto.setEnabled(true);
                        clearVerificationListener();
                    } else {
                        updateVerificationUi("reviewing", "AI 검증 중", reason != null ? reason : "사진과 미션 내용을 비교하고 있습니다.");
                    }
                });
        startVerificationTimeout();
    }

    private void showExistingMissionStatus(String status, String reason) {
        if ("approved".equals(status)) {
            updateVerificationUi("approved", "이미 인증 완료", reason != null ? reason : "오늘 이미 승인된 미션입니다.");
            btnCompleteMission.setEnabled(false);
            btnTakePhoto.setEnabled(false);
        } else if ("rejected".equals(status) || "failed".equals(status)) {
            updateVerificationUi("rejected", "다시 촬영 필요", reason != null ? reason : "이전 제출이 승인되지 않았습니다. 다시 촬영해주세요.");
            btnCompleteMission.setEnabled(false);
            btnCompleteMission.setText("AI 검증 요청하기");
            btnTakePhoto.setEnabled(true);
            isCompleted = false;
            photoUri = "";
            photoPath = "";
        } else {
            updateVerificationUi(
                    "idle",
                    "AI 검증 대기 중",
                    reason != null ? reason : "사진 제출은 완료되었습니다. OpenAI API 결제와 Functions 배포가 활성화되면 자동 검증됩니다."
            );
            btnCompleteMission.setEnabled(false);
            btnTakePhoto.setEnabled(true);
        }
    }

    private void updateVerificationUi(String status, String title, String reason) {
        txtVerificationStatus.setText(title);
        txtVerificationReason.setText(reason != null ? reason : "");
        progressVerification.setVisibility(
                "reviewing".equals(status) || "pending".equals(status)
                        ? android.view.View.VISIBLE
                        : android.view.View.GONE
        );
    }

    private void clearVerificationListener() {
        if (verificationListener != null) {
            verificationListener.remove();
            verificationListener = null;
        }
        clearVerificationTimeout();
    }

    private void startVerificationTimeout() {
        clearVerificationTimeout();
        verificationTimeoutRunnable = () -> {
            updateVerificationUi(
                    "idle",
                    "AI 검증 대기 상태",
                    "사진은 서버에 저장되었습니다. OpenAI API 결제와 Functions 배포가 활성화되면 이 기록을 기준으로 자동 검증됩니다."
            );
            btnTakePhoto.setEnabled(true);
            btnCompleteMission.setEnabled(false);
            btnCompleteMission.setText("AI 검증 요청하기");
        };
        verificationHandler.postDelayed(verificationTimeoutRunnable, 20000L);
    }

    private void clearVerificationTimeout() {
        if (verificationTimeoutRunnable != null) {
            verificationHandler.removeCallbacks(verificationTimeoutRunnable);
            verificationTimeoutRunnable = null;
        }
    }

    private byte[] readPhotoBytes() {
        if (photoPath != null && !photoPath.isEmpty()) {
            File photoFile = new File(photoPath);
            if (photoFile.exists() && photoFile.length() > 0) {
                try {
                    return readFile(photoFile);
                } catch (IOException e) {
                    return null;
                }
            }
        }

        if (photoUri != null && !photoUri.isEmpty()) {
            try {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int read;

                try (java.io.InputStream inputStream = getContentResolver().openInputStream(Uri.parse(photoUri))) {
                    if (inputStream == null) {
                        return null;
                    }

                    while ((read = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, read);
                    }
                }

                return outputStream.toByteArray();
            } catch (IOException e) {
                return null;
            }
        }

        return null;
    }

    private byte[] readFile(File file) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;

        try (FileInputStream inputStream = new FileInputStream(file)) {
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
        }

        return outputStream.toByteArray();
    }

    private String buildStorageErrorMessage(Exception exception) {
        if (exception instanceof StorageException) {
            StorageException storageException = (StorageException) exception;
            int errorCode = storageException.getErrorCode();

            if (errorCode == StorageException.ERROR_OBJECT_NOT_FOUND) {
                return "Storage 버킷 또는 업로드 경로를 찾지 못했습니다. Firebase Console에서 Storage가 생성되어 있는지 확인해주세요.";
            }

            if (errorCode == StorageException.ERROR_NOT_AUTHENTICATED) {
                return "로그인 인증이 필요합니다. 다시 로그인 후 시도해주세요.";
            }

            if (errorCode == StorageException.ERROR_NOT_AUTHORIZED) {
                return "Storage 업로드 권한이 없습니다. Firebase Storage Rules를 확인해주세요.";
            }

            if (errorCode == StorageException.ERROR_QUOTA_EXCEEDED) {
                return "Storage 사용량 한도를 초과했습니다.";
            }

            return "Storage 오류 코드 " + errorCode + ": " + storageException.getMessage();
        }

        return exception.getMessage() != null ? exception.getMessage() : "알 수 없는 오류가 발생했습니다.";
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        clearVerificationListener();
    }

    public static class MissionHistory {
        public String missionId;
        public String title;
        public String completedAt;
        public String photoUrl;
        public String storagePath;
        public int pointsEarned;
        public String status;
        public String verificationStatus;
        public String verificationReason;
        public boolean pointsAwarded;

        public MissionHistory() {
        }

        public MissionHistory(String missionId, String title, String completedAt,
                              String photoUrl, String storagePath, int pointsEarned, String status,
                              String verificationStatus, String verificationReason, boolean pointsAwarded) {
            this.missionId = missionId;
            this.title = title;
            this.completedAt = completedAt;
            this.photoUrl = photoUrl;
            this.storagePath = storagePath;
            this.pointsEarned = pointsEarned;
            this.status = status;
            this.verificationStatus = verificationStatus;
            this.verificationReason = verificationReason;
            this.pointsAwarded = pointsAwarded;
        }
    }
}
