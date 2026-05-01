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

import java.util.ArrayList;
import java.util.List;

public class MissionActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private MissionAdapter adapter;
    private List<Mission> missionList;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    // 미션 완료 결과 받기
    private final ActivityResultLauncher<Intent> missionLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    String completedMission = result.getData().getStringExtra("completedMission");

                    for (Mission mission : missionList) {
                        if (mission.getTitle().equals(completedMission)) {
                            mission.setCompleted(true);
                            adapter.notifyDataSetChanged();
                            break;
                        }
                    }
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

        missionList = new ArrayList<>();
        missionList.add(new Mission("텀블러 사용하기", "일회용 컵 대신 텀블러를 사용해보세요!"));
        missionList.add(new Mission("대중교통 이용하기", "자가용 대신 버스나 지하철을 타면 탄소를 줄일 수 있어요."));
        missionList.add(new Mission("분리수거 실천하기", "올바르게 분리배출하여 환경을 보호하세요."));
        missionList.add(new Mission("플라스틱 줄이기", "비닐봉투 대신 장바구니를 사용해보세요."));
        missionList.add(new Mission("잔반 남기지 않기", "음식을 남기지 않고 먹으면 음식물 쓰레기를 줄일 수 있습니다."));

        adapter = new MissionAdapter(missionList, mission -> {
            Intent intent = new Intent(MissionActivity.this, MissionDetailActivity.class);
            intent.putExtra("missionTitle", mission.getTitle());
            intent.putExtra("missionDetail", mission.getDetail());
            missionLauncher.launch(intent);
        });

        recyclerView.setAdapter(adapter);

        loadCompletedMissions();
    }

    private void loadCompletedMissions() {
        FirebaseUser currentUser = auth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "로그인된 사용자가 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = currentUser.getUid();

        db.collection("users")
                .document(uid)
                .collection("missionHistory")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                        String title = doc.getString("title");

                        if (title != null) {
                            for (Mission mission : missionList) {
                                if (mission.getTitle().equals(title)) {
                                    mission.setCompleted(true);
                                }
                            }
                        }
                    }

                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "미션 기록 불러오기 실패: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}