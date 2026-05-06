package com.example.greenchallenger;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MissionDetailActivity extends AppCompatActivity {

    private TextView txtMissionTitle, txtMissionDetail;
    private Button btnCompleteMission, btnTakePhoto;

    private boolean isCompleted = false;
    private String photoUri = "";

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    // 카메라 촬영 결과 받기
    private final ActivityResultLauncher<Intent> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    if (result.getData() != null) {
                        photoUri = result.getData().getStringExtra("photoUri");
                    }

                    Toast.makeText(this, "사진이 인증되었습니다. 미션 완료 준비!", Toast.LENGTH_SHORT).show();
                    isCompleted = true;
                    btnCompleteMission.setEnabled(true);
                    btnCompleteMission.setText("미션 완료하기");
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
        btnCompleteMission = findViewById(R.id.btnCompleteMission);
        btnTakePhoto = findViewById(R.id.btnTakePhoto);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        String title = getIntent().getStringExtra("missionTitle");
        String detail = getIntent().getStringExtra("missionDetail");

        txtMissionTitle.setText(title);
        txtMissionDetail.setText(detail);

        btnCompleteMission.setEnabled(false);

        btnTakePhoto.setOnClickListener(v -> {
            Intent intent = new Intent(this, CameraActivity.class);
            cameraLauncher.launch(intent);
        });

        btnCompleteMission.setOnClickListener(v -> {
            if (!isCompleted) {
                Toast.makeText(this, "사진 인증 후 완료할 수 있습니다.", Toast.LENGTH_SHORT).show();
                return;
            }

            saveMissionToFirestore(title);
        });
    }

    private void saveMissionToFirestore(String title) {
        FirebaseUser currentUser = auth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "로그인된 사용자가 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = currentUser.getUid();

        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(new Date());
        String missionId = title.replace(" ", "_");
        String docId = missionId + "_" + today;

        MissionHistory history = new MissionHistory(
                missionId,
                title,
                today,
                photoUri,
                10,
                "completed"
        );

        db.collection("users")
                .document(uid)
                .collection("missionHistory")
                .document(docId)
                .set(history)
                .addOnSuccessListener(unused -> {
                    db.collection("users")
                            .document(uid)
                            .get()
                            .addOnSuccessListener(documentSnapshot -> {
                                if (documentSnapshot.exists()) {
                                    Long currentPoints = documentSnapshot.getLong("ecoPoints");
                                    Long currentMissionCount = documentSnapshot.getLong("missionCompletedCount");

                                    int newPoints = (currentPoints != null ? currentPoints.intValue() : 0) + 10;
                                    int newMissionCount = (currentMissionCount != null ? currentMissionCount.intValue() : 0) + 1;

                                    int newGrowthStage;
                                    if (newPoints < 3) {
                                        newGrowthStage = 1;
                                    } else if (newPoints < 7) {
                                        newGrowthStage = 2;
                                    } else {
                                        newGrowthStage = 3;
                                    }

                                    db.collection("users")
                                            .document(uid)
                                            .update(
                                                    "ecoPoints", newPoints,
                                                    "missionCompletedCount", newMissionCount,
                                                    "growthStage", newGrowthStage
                                            )
                                            .addOnSuccessListener(unused2 -> {
                                                Toast.makeText(this, "미션이 완료되었습니다!", Toast.LENGTH_SHORT).show();

                                                Intent resultIntent = new Intent();
                                                resultIntent.putExtra("completedMission", title);
                                                resultIntent.putExtra("photoUri", photoUri);
                                                setResult(RESULT_OK, resultIntent);
                                                finish();
                                            })
                                            .addOnFailureListener(e -> {
                                                Toast.makeText(this, "포인트 업데이트 실패: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                            });
                                }
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "사용자 정보 불러오기 실패: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "미션 기록 저장 실패: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    public static class MissionHistory {
        public String missionId;
        public String title;
        public String completedAt;
        public String photoUrl;
        public int pointsEarned;
        public String status;

        public MissionHistory() {
        }

        public MissionHistory(String missionId, String title, String completedAt,
                              String photoUrl, int pointsEarned, String status) {
            this.missionId = missionId;
            this.title = title;
            this.completedAt = completedAt;
            this.photoUrl = photoUrl;
            this.pointsEarned = pointsEarned;
            this.status = status;
        }
    }
}
