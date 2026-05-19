package com.example.greenchallenger;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PointHistoryActivity extends AppCompatActivity {

    private TextView txtEmptyHistory;
    private RecyclerView recyclerPointHistory;
    private PointHistoryAdapter adapter;
    private final List<PointHistoryItem> historyItems = new ArrayList<>();

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private int pendingLoads = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_point_history);

        txtEmptyHistory = findViewById(R.id.txtEmptyHistory);
        recyclerPointHistory = findViewById(R.id.recyclerPointHistory);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        recyclerPointHistory.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PointHistoryAdapter(historyItems);
        recyclerPointHistory.setAdapter(adapter);

        loadPointHistory();
    }

    private void loadPointHistory() {
        FirebaseUser currentUser = auth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "로그인된 사용자가 없습니다.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String uid = currentUser.getUid();
        historyItems.clear();
        pendingLoads = 4;

        loadAttendanceHistory(uid);
        loadMissionHistory(uid);
        loadAdHistory(uid);
        loadRewardHistory(uid);
    }

    private void loadAttendanceHistory(String uid) {
        db.collection("users").document(uid).collection("attendance")
                .get()
                .addOnSuccessListener(query -> {
                    for (com.google.firebase.firestore.DocumentSnapshot doc : query) {
                        String date = doc.getString("date");
                        Long points = doc.getLong("pointsEarned");
                        historyItems.add(new PointHistoryItem(
                                "출석 체크",
                                "일일 출석 보상",
                                safeDate(date),
                                points != null ? points.intValue() : 1
                        ));
                    }
                    finishOneLoad();
                })
                .addOnFailureListener(e -> {
                    showLoadError("출석 내역");
                    finishOneLoad();
                });
    }

    private void loadMissionHistory(String uid) {
        db.collection("users").document(uid).collection("missionHistory")
                .get()
                .addOnSuccessListener(query -> {
                    for (com.google.firebase.firestore.DocumentSnapshot doc : query) {
                        if (!"approved".equals(doc.getString("verificationStatus"))) {
                            continue;
                        }

                        String title = doc.getString("title");
                        String completedAt = doc.getString("completedAt");
                        Long points = doc.getLong("pointsEarned");
                        historyItems.add(new PointHistoryItem(
                                title != null ? title : "미션 인증",
                                "친환경 미션 완료 보상",
                                safeDate(completedAt),
                                points != null ? points.intValue() : 10
                        ));
                    }
                    finishOneLoad();
                })
                .addOnFailureListener(e -> {
                    showLoadError("미션 내역");
                    finishOneLoad();
                });
    }

    private void loadAdHistory(String uid) {
        db.collection("users").document(uid).collection("adRewards")
                .get()
                .addOnSuccessListener(query -> {
                    for (com.google.firebase.firestore.DocumentSnapshot doc : query) {
                        String date = doc.getString("date");
                        String lastWatchedAt = doc.getString("lastWatchedAt");
                        Long count = doc.getLong("count");
                        Long pointsPerAd = doc.getLong("pointsPerAd");
                        int watchedCount = count != null ? count.intValue() : 0;
                        int points = pointsPerAd != null ? pointsPerAd.intValue() : 5;

                        if (watchedCount > 0) {
                            historyItems.add(new PointHistoryItem(
                                    "광고 보상",
                                    watchedCount + "회 시청 보상",
                                    safeDate(lastWatchedAt != null ? lastWatchedAt : date),
                                    watchedCount * points
                            ));
                        }
                    }
                    finishOneLoad();
                })
                .addOnFailureListener(e -> {
                    showLoadError("광고 내역");
                    finishOneLoad();
                });
    }

    private void loadRewardHistory(String uid) {
        db.collection("users").document(uid).collection("myRewards")
                .get()
                .addOnSuccessListener(query -> {
                    for (com.google.firebase.firestore.DocumentSnapshot doc : query) {
                        String rewardName = doc.getString("name");
                        String redeemedAt = doc.getString("redeemedAt");
                        Long cost = doc.getLong("cost");
                        historyItems.add(new PointHistoryItem(
                                rewardName != null ? rewardName : "기프티콘 교환",
                                "상점 포인트 교환",
                                safeDate(redeemedAt),
                                -(cost != null ? cost.intValue() : 0)
                        ));
                    }
                    finishOneLoad();
                })
                .addOnFailureListener(e -> {
                    showLoadError("교환 내역");
                    finishOneLoad();
                });
    }

    private void finishOneLoad() {
        pendingLoads--;
        if (pendingLoads > 0) {
            return;
        }

        Collections.sort(historyItems, (a, b) -> b.getDateText().compareTo(a.getDateText()));
        adapter.notifyDataSetChanged();

        boolean isEmpty = historyItems.isEmpty();
        txtEmptyHistory.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        recyclerPointHistory.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    private void showLoadError(String sectionName) {
        Toast.makeText(this, sectionName + "을 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
    }

    private String safeDate(String dateText) {
        return dateText != null && !dateText.isEmpty() ? dateText : "날짜 정보 없음";
    }
}
