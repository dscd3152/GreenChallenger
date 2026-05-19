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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private ImageView treeHomeImage;
    private TextView txtWelcome, txtDate, txtPointSummary, txtGrowthSummary, txtEcoTip;
    private Button btnStartMission, btnMyPage, btnAttendance, btnRanking, btnRewardAd, btnRewardStore, btnNavMission;
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
        btnMyPage = findViewById(R.id.btnMyPage);
        btnAttendance = findViewById(R.id.btnAttendance);
        btnRanking = findViewById(R.id.btnRanking);
        btnRewardAd = findViewById(R.id.btnRewardAd);
        btnRewardStore = findViewById(R.id.btnRewardStore);
        btnNavMission = findViewById(R.id.btnNavMission);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        String currentDate = new SimpleDateFormat("yyyy.MM.dd (E)", Locale.KOREA)
                .format(new Date());
        txtDate.setText(currentDate);
        setTodayEcoTip();

        loadMainUserInfo();

        btnStartMission.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, MissionActivity.class)));

        btnNavMission.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, MissionActivity.class)));

        btnMyPage.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, MyPageActivity.class)));

        btnAttendance.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, AttendanceActivity.class)));

        btnRanking.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, RankingActivity.class)));

        btnRewardAd.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, RewardAdActivity.class)));

        btnRewardStore.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, RewardStoreActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadMainUserInfo();
    }

    private void loadMainUserInfo() {
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
                            txtWelcome.setText(user.getNickname() + "님,\n오늘도 지구를 가볍게");
                            txtPointSummary.setText(
                                    user.getEcoPoints() + "P 보유 · 출석 " +
                                            user.getAttendanceCount() + "일 · 미션 " +
                                            user.getMissionCompletedCount() + "개"
                            );
                            updateGrowthView(user.getGrowthStage());
                        }
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "메인 정보 불러오기 실패: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    private void updateGrowthView(int growthStage) {
        switch (growthStage) {
            case 1:
                treeHomeImage.setImageResource(R.drawable.tree_stage1);
                txtGrowthSummary.setText("씨앗이 자라는 중");
                break;
            case 2:
                treeHomeImage.setImageResource(R.drawable.tree_stage2);
                txtGrowthSummary.setText("새싹이 커지는 중");
                break;
            case 3:
            default:
                treeHomeImage.setImageResource(R.drawable.tree_stage3);
                txtGrowthSummary.setText("나무가 건강하게 성장 중");
                break;
        }
    }

    private void setTodayEcoTip() {
        String[] tips = {
                "텀블러를 챙기면 일회용 컵 사용을 줄일 수 있어요.",
                "장바구니를 미리 챙기면 비닐봉투 사용을 쉽게 줄일 수 있어요.",
                "분리배출 전 내용물을 비우고 헹구면 재활용률이 높아져요.",
                "가까운 거리는 걷거나 자전거를 이용하면 탄소 배출을 줄일 수 있어요.",
                "사용하지 않는 충전기는 콘센트에서 빼두면 대기전력을 아낄 수 있어요.",
                "음식은 먹을 만큼만 담으면 음식물 쓰레기를 줄일 수 있어요.",
                "종이 영수증 대신 모바일 영수증을 선택해보세요."
        };
        int dayOfYear = Integer.parseInt(new SimpleDateFormat("D", Locale.KOREA).format(new Date()));
        txtEcoTip.setText(tips[dayOfYear % tips.length]);
    }
}
