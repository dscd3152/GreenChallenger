package com.example.greenchallenger;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class MyPageActivity extends AppCompatActivity {

    private ImageView profileImage;
    private TextView userName, userEmail, missionCount;
    private Button btnEditProfile, btnLogout;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_page);

        profileImage = findViewById(R.id.profileImage);
        userName = findViewById(R.id.userName);
        userEmail = findViewById(R.id.userEmail);
        missionCount = findViewById(R.id.missionCount);
        btnEditProfile = findViewById(R.id.btnEditProfile);
        btnLogout = findViewById(R.id.btnLogout);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        loadUserProfile();

        btnEditProfile.setOnClickListener(v ->
                Toast.makeText(this, "프로필 수정 기능은 곧 업데이트됩니다!", Toast.LENGTH_SHORT).show()
        );

        btnLogout.setOnClickListener(v -> {
            auth.signOut();

            Toast.makeText(this, "로그아웃되었습니다.", Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(MyPageActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);

            finish();
        });
    }

    private void loadUserProfile() {
        FirebaseUser currentUser = auth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "로그인된 사용자가 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = currentUser.getUid();

        db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);

                        if (user != null) {
                            userName.setText(user.getNickname());
                            userEmail.setText(user.getEmail());

                            String growthText;
                            switch (user.getGrowthStage()) {
                                case 1:
                                    growthText = "씨앗 단계 🌱";
                                    break;
                                case 2:
                                    growthText = "새싹 단계 🍃";
                                    break;
                                case 3:
                                default:
                                    growthText = "나무 단계 🌳";
                                    break;
                            }

                            missionCount.setText(
                                    "포인트: " + user.getEcoPoints() + "P\n" +
                                            "출석: " + user.getAttendanceCount() + "일\n" +
                                            "완료한 미션: " + user.getMissionCompletedCount() + "개\n" +
                                            "성장 단계: " + growthText
                            );
                        }

                    } else {
                        Toast.makeText(this, "사용자 정보가 없습니다.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "데이터 불러오기 실패: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}