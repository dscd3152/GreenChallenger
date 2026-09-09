package com.example.greenchallenger;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private ImageView treeHomeImage;
    private TextView txtWelcome, txtDate, txtPointSummary, txtGrowthSummary, txtEcoTip;
    private Button btnStartMission, btnAttendance, btnRanking, btnFriends, btnCommunity, btnRewardAd, btnRewardStore;
    private ImageButton btnOpenChats;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtWelcome = findViewById(R.id.txtWelcome);
        txtDate = findViewById(R.id.txtDate);
        txtPointSummary = findViewById(R.id.txtPointSummary);
        txtGrowthSummary = findViewById(R.id.txtGrowthSummary);
        txtEcoTip = findViewById(R.id.txtEcoTip);
        treeHomeImage = findViewById(R.id.treeHomeImage);
        btnStartMission = findViewById(R.id.btnStartMission);
        btnAttendance = findViewById(R.id.btnAttendance);
        btnRanking = findViewById(R.id.btnRanking);
        btnFriends = findViewById(R.id.btnFriends);
        btnCommunity = findViewById(R.id.btnCommunity);
        btnRewardAd = findViewById(R.id.btnRewardAd);
        btnRewardStore = findViewById(R.id.btnRewardStore);
        btnOpenChats = findViewById(R.id.btnOpenChats);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        NavHelper.setup(this, NavHelper.HOME);

        txtDate.setText(new SimpleDateFormat("yyyy.MM.dd (E)", Locale.KOREA).format(new Date()));
        setTodayEcoTip();
        loadMainUserInfo();

        btnStartMission.setOnClickListener(v -> startActivity(new Intent(this, MissionActivity.class)));
        btnAttendance.setOnClickListener(v -> startActivity(new Intent(this, AttendanceActivity.class)));
        btnRanking.setOnClickListener(v -> startActivity(new Intent(this, RankingActivity.class)));
        btnFriends.setOnClickListener(v -> startActivity(new Intent(this, FriendsActivity.class)));
        btnCommunity.setOnClickListener(v -> startActivity(new Intent(this, CommunityActivity.class)));
        btnOpenChats.setOnClickListener(v -> startActivity(new Intent(this, ChatListActivity.class)));
        btnRewardAd.setOnClickListener(v -> startActivity(new Intent(this, RewardAdActivity.class)));
        btnRewardStore.setOnClickListener(v -> startActivity(new Intent(this, RewardStoreActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadMainUserInfo();
    }

    private void loadMainUserInfo() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("users").document(currentUser.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    User user = documentSnapshot.toObject(User.class);
                    if (user == null) {
                        return;
                    }
                    int points = user.getEcoPoints();
                    int recalculatedStage = GrowthPolicy.getGrowthStage(points);
                    txtWelcome.setText(ProfileUi.nickname(user) + "님,\n오늘도 지구를 가볍게");
                    txtPointSummary.setText(points + "P 보유 · 출석 " +
                            user.getAttendanceCount() + "일 · 미션 " +
                            user.getMissionCompletedCount() + "개");
                    updateGrowthView(points);

                    if (user.getGrowthStage() != recalculatedStage) {
                        db.collection("users").document(currentUser.getUid()).update("growthStage", recalculatedStage);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "메인 정보 불러오기 실패: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    private void updateGrowthView(int points) {
        int growthStage = GrowthPolicy.getGrowthStage(points);
        if (growthStage == 1) {
            treeHomeImage.setImageResource(R.drawable.tree_stage1);
        } else if (growthStage == 2) {
            treeHomeImage.setImageResource(R.drawable.tree_stage2);
        } else {
            treeHomeImage.setImageResource(R.drawable.tree_stage3);
        }
        txtGrowthSummary.setText(GrowthPolicy.getGrowthStageName(growthStage) + " 단계");
    }

    private void setTodayEcoTip() {
        String[] tips = {
                "텀블러를 챙기면 일회용 컵 사용을 줄일 수 있어요.",
                "장바구니를 미리 챙기면 비닐봉투 사용을 줄일 수 있어요.",
                "분리배출 전 내용물을 비우고 헹구면 재활용률이 높아져요.",
                "가까운 거리는 걷거나 자전거를 이용해보세요.",
                "사용하지 않는 충전기는 콘센트에서 빼두면 대기전력을 줄일 수 있어요.",
                "음식은 먹을 만큼만 담으면 음식물 쓰레기를 줄일 수 있어요.",
                "종이 영수증 대신 모바일 영수증을 선택해보세요."
        };
        int dayOfYear = Integer.parseInt(new SimpleDateFormat("D", Locale.KOREA).format(new Date()));
        txtEcoTip.setText(tips[dayOfYear % tips.length]);
    }
}
