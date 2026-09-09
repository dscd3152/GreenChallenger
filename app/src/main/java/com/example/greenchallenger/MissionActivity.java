package com.example.greenchallenger;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MissionActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private MissionAdapter adapter;
    private List<Mission> missionList;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private final ActivityResultLauncher<Intent> missionLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Toast.makeText(this, "AI 인증 완료! 포인트가 지급되었습니다.", Toast.LENGTH_SHORT).show();
                    loadCompletedMissions();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mission);

        recyclerView = findViewById(R.id.missionRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        NavHelper.setup(this, NavHelper.MISSION);

        missionList = new ArrayList<>();
        missionList.add(new Mission("텀블러 사용하기", "일회용 컵 대신 텀블러를 사용해보세요."));
        missionList.add(new Mission("대중교통 이용하기", "자가용 대신 버스나 지하철을 이용해 탄소 배출을 줄여보세요."));
        missionList.add(new Mission("분리수거 실천하기", "올바르게 분리배출하여 환경을 보호하세요."));
        missionList.add(new Mission("플라스틱 줄이기", "비닐봉투 대신 장바구니를 사용해보세요."));
        missionList.add(new Mission("잔반 남기지 않기", "음식을 남기지 않고 먹으면 음식물 쓰레기를 줄일 수 있어요."));

        adapter = new MissionAdapter(missionList, mission -> {
            Intent intent = new Intent(MissionActivity.this, MissionDetailActivity.class);
            intent.putExtra("missionTitle", mission.getTitle());
            intent.putExtra("missionDetail", mission.getDetail());
            missionLauncher.launch(intent);
        });

        recyclerView.setAdapter(adapter);
        loadCompletedMissions();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (adapter != null) {
            loadCompletedMissions();
        }
    }

    private void loadCompletedMissions() {
        FirebaseUser currentUser = auth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = currentUser.getUid();

        db.collection("users")
                .document(uid)
                .collection("missionHistory")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (Mission mission : missionList) {
                        mission.setCompleted(false);
                    }

                    String today = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(new Date());

                    for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                        String title = doc.getString("title");
                        String completedAt = doc.getString("completedAt");
                        String verificationStatus = doc.getString("verificationStatus");

                        if (title != null && today.equals(completedAt) && "approved".equals(verificationStatus)) {
                            for (Mission mission : missionList) {
                                if (mission.getTitle().equals(title)) {
                                    mission.setCompleted(true);
                                }
                            }
                        }
                    }

                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "미션 기록 불러오기 실패: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }
}
