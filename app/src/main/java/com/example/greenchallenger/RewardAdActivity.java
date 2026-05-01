package com.example.greenchallenger;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class RewardAdActivity extends AppCompatActivity {

    private static final int DAILY_AD_LIMIT = 3;
    private static final int AD_REWARD_POINTS = 5;

    private TextView txtAdStatus;
    private ProgressBar progressBar;
    private Button btnWatchAd;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private boolean isWatching = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reward_ad);

        txtAdStatus = findViewById(R.id.txtAdStatus);
        progressBar = findViewById(R.id.progressBar);
        btnWatchAd = findViewById(R.id.btnWatchAd);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        progressBar.setMax(5);
        progressBar.setProgress(0);

        loadTodayAdStatus();

        btnWatchAd.setOnClickListener(v -> {
            if (isWatching) {
                Toast.makeText(this, "광고 시청 중입니다.", Toast.LENGTH_SHORT).show();
                return;
            }

            startFakeAd();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTodayAdStatus();
    }

    private void loadTodayAdStatus() {
        FirebaseUser currentUser = auth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "로그인된 사용자가 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(new Date());

        db.collection("users")
                .document(currentUser.getUid())
                .collection("adRewards")
                .document(today)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Long countValue = documentSnapshot.getLong("count");
                    int watchedCount = countValue != null ? countValue.intValue() : 0;
                    updateAdStatusText(watchedCount);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "광고 기록 불러오기 실패: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    private void startFakeAd() {
        FirebaseUser currentUser = auth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "로그인된 사용자가 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(new Date());

        db.collection("users")
                .document(currentUser.getUid())
                .collection("adRewards")
                .document(today)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Long countValue = documentSnapshot.getLong("count");
                    int watchedCount = countValue != null ? countValue.intValue() : 0;

                    if (watchedCount >= DAILY_AD_LIMIT) {
                        updateAdStatusText(watchedCount);
                        Toast.makeText(this, "오늘 광고 보상을 모두 받았습니다.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    playFakeAd(currentUser.getUid(), today);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "광고 기록 확인 실패: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    private void playFakeAd(String uid, String today) {
        isWatching = true;
        btnWatchAd.setEnabled(false);
        txtAdStatus.setText("광고 시청 중... 잠시만 기다려주세요");
        progressBar.setProgress(0);

        Handler handler = new Handler(Looper.getMainLooper());

        for (int i = 1; i <= 5; i++) {
            int progress = i;
            handler.postDelayed(() -> progressBar.setProgress(progress), i * 1000L);
        }

        handler.postDelayed(() -> rewardUser(uid, today), 5000L);
    }

    private void rewardUser(String uid, String today) {
        DocumentReference userRef = db.collection("users").document(uid);
        DocumentReference adRef = userRef.collection("adRewards").document(today);

        db.runTransaction(transaction -> {
                    com.google.firebase.firestore.DocumentSnapshot userSnapshot = transaction.get(userRef);
                    if (!userSnapshot.exists()) {
                        throw new IllegalStateException("사용자 정보가 없습니다.");
                    }

                    com.google.firebase.firestore.DocumentSnapshot adSnapshot = transaction.get(adRef);

                    Long countValue = adSnapshot.getLong("count");
                    int watchedCount = countValue != null ? countValue.intValue() : 0;
                    if (watchedCount >= DAILY_AD_LIMIT) {
                        throw new IllegalStateException("오늘 광고 보상을 모두 받았습니다.");
                    }

                    Long currentPoints = userSnapshot.getLong("ecoPoints");
                    int newPoints = (currentPoints != null ? currentPoints.intValue() : 0) + AD_REWARD_POINTS;
                    int newWatchedCount = watchedCount + 1;

                    int newGrowthStage;
                    if (newPoints < 3) {
                        newGrowthStage = 1;
                    } else if (newPoints < 7) {
                        newGrowthStage = 2;
                    } else {
                        newGrowthStage = 3;
                    }

                    transaction.update(userRef, "ecoPoints", newPoints, "growthStage", newGrowthStage);
                    transaction.set(adRef, new AdRewardRecord(today, newWatchedCount, AD_REWARD_POINTS, getNowText()));

                    return newWatchedCount;
                })
                .addOnSuccessListener(watchedCount -> {
                    txtAdStatus.setText("광고 시청 완료! +" + AD_REWARD_POINTS + "P 지급됨");
                    Toast.makeText(this, "보상 지급 완료! +" + AD_REWARD_POINTS + "P", Toast.LENGTH_SHORT).show();
                    resetUI();
                    updateAdStatusText(watchedCount);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "포인트 지급 실패: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    resetUI();
                    loadTodayAdStatus();
                });
    }

    private void resetUI() {
        isWatching = false;
        btnWatchAd.setEnabled(true);
    }

    private void updateAdStatusText(int watchedCount) {
        int remainCount = DAILY_AD_LIMIT - watchedCount;

        if (remainCount <= 0) {
            txtAdStatus.setText("오늘 광고 보상을 모두 받았습니다. 내일 다시 참여해주세요.");
            btnWatchAd.setEnabled(false);
        } else {
            txtAdStatus.setText("광고 시청 시 +" + AD_REWARD_POINTS + "P / 오늘 남은 횟수: " + remainCount + "회");
            btnWatchAd.setEnabled(!isWatching);
        }
    }

    private String getNowText() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.KOREA).format(new Date());
    }

    public static class AdRewardRecord {
        public String date;
        public int count;
        public int pointsPerAd;
        public String lastWatchedAt;

        public AdRewardRecord() {
        }

        public AdRewardRecord(String date, int count, int pointsPerAd, String lastWatchedAt) {
            this.date = date;
            this.count = count;
            this.pointsPerAd = pointsPerAd;
            this.lastWatchedAt = lastWatchedAt;
        }
    }
}
