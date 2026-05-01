package com.example.greenchallenger;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RewardStoreActivity extends AppCompatActivity {

    private TextView txtMyPoint;
    private RecyclerView recyclerRewards;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private final List<RewardItem> rewardList = new ArrayList<>();
    private RewardAdapter adapter;

    private int currentPoints = 0;
    private boolean isExchanging = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reward_store);

        txtMyPoint = findViewById(R.id.txtMyPoint);
        recyclerRewards = findViewById(R.id.recyclerRewards);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        recyclerRewards.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RewardAdapter(rewardList, this::exchangeReward);
        recyclerRewards.setAdapter(adapter);

        loadMyPoint();
        seedDefaultRewards();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadMyPoint();
        loadRewards();
    }

    private void loadMyPoint() {
        FirebaseUser currentUser = auth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "로그인된 사용자가 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("users")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Long ecoPoints = documentSnapshot.getLong("ecoPoints");
                        currentPoints = ecoPoints != null ? ecoPoints.intValue() : 0;
                        txtMyPoint.setText("내 포인트: " + currentPoints + "P");
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "포인트 불러오기 실패: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    private void loadRewards() {
        db.collection("rewards")
                .whereEqualTo("isActive", true)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    rewardList.clear();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        RewardItem reward = doc.toObject(RewardItem.class);
                        if (reward != null) {
                            rewardList.add(reward);
                        }
                    }

                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "보상 목록 불러오기 실패: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    private void seedDefaultRewards() {
        DocumentReference rewardRef = db.collection("rewards").document("reward_starbucks");

        rewardRef.get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        loadRewards();
                        return;
                    }

                    WriteBatch batch = db.batch();

                    Map<String, Object> reward = new HashMap<>();
                    reward.put("name", "스타벅스 아메리카노");
                    reward.put("cost", 30);
                    reward.put("description", "포인트로 교환하는 테스트용 기프티콘입니다.");
                    reward.put("isActive", true);
                    reward.put("stockCount", 3);
                    reward.put("thumbnailImageName", "gifticon_starbucks_001");
                    batch.set(rewardRef, reward);

                    addStock(batch, "stock_starbucks_001", "gifticon_starbucks_001");
                    addStock(batch, "stock_starbucks_002", "gifticon_starbucks_002");
                    addStock(batch, "stock_starbucks_003", "gifticon_starbucks_003");

                    batch.commit()
                            .addOnSuccessListener(unused -> loadRewards())
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "기본 보상 데이터 생성 실패: " + e.getMessage(), Toast.LENGTH_LONG).show()
                            );
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "보상 데이터 확인 실패: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    private void addStock(WriteBatch batch, String stockId, String imageName) {
        Map<String, Object> stock = new HashMap<>();
        stock.put("rewardId", "reward_starbucks");
        stock.put("imageName", imageName);
        stock.put("status", "available");
        batch.set(db.collection("rewardStock").document(stockId), stock);
    }

    private void exchangeReward(RewardItem reward) {
        FirebaseUser currentUser = auth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "로그인된 사용자가 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isExchanging) {
            Toast.makeText(this, "교환 처리 중입니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentPoints < reward.getCost()) {
            Toast.makeText(this, "포인트가 부족합니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (reward.getStockCount() <= 0) {
            Toast.makeText(this, "재고가 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = currentUser.getUid();
        String redeemedAt = new SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss",
                Locale.KOREA
        ).format(new Date());

        isExchanging = true;

        DocumentReference userRef = db.collection("users").document(uid);
        DocumentReference rewardRef = db.collection("rewards").document("reward_starbucks");
        DocumentReference myRewardRef = userRef.collection("myRewards").document();

        db.runTransaction(transaction -> {
                    Long pointValue = transaction.get(userRef).getLong("ecoPoints");
                    int userPoints = pointValue != null ? pointValue.intValue() : 0;
                    if (userPoints < reward.getCost()) {
                        throw new IllegalStateException("포인트가 부족합니다.");
                    }

                    Long stockValue = transaction.get(rewardRef).getLong("stockCount");
                    int stockCount = stockValue != null ? stockValue.intValue() : 0;
                    if (stockCount <= 0) {
                        throw new IllegalStateException("재고가 없습니다.");
                    }

                    DocumentReference selectedStockRef = null;
                    String selectedStockId = null;
                    String imageName = null;
                    String[] stockIds = {"stock_starbucks_001", "stock_starbucks_002", "stock_starbucks_003"};

                    for (String stockId : stockIds) {
                        DocumentReference stockRef = db.collection("rewardStock").document(stockId);
                        com.google.firebase.firestore.DocumentSnapshot stockSnapshot = transaction.get(stockRef);
                        if ("available".equals(stockSnapshot.getString("status"))) {
                            selectedStockRef = stockRef;
                            selectedStockId = stockId;
                            imageName = stockSnapshot.getString("imageName");
                            break;
                        }
                    }

                    if (selectedStockRef == null) {
                        throw new IllegalStateException("사용 가능한 기프티콘 재고가 없습니다.");
                    }

                    int newPoints = userPoints - reward.getCost();
                    int newStockCount = stockCount - 1;

                    MyRewardItem myReward = new MyRewardItem(
                            "reward_starbucks",
                            selectedStockId,
                            reward.getName(),
                            reward.getCost(),
                            imageName,
                            reward.getDescription(),
                            "unused",
                            redeemedAt
                    );

                    transaction.update(userRef, "ecoPoints", newPoints);
                    transaction.update(rewardRef, "stockCount", newStockCount);
                    transaction.update(selectedStockRef, "status", "sold");
                    transaction.set(myRewardRef, myReward);
                    return newPoints;
                })
                .addOnSuccessListener(newPoints -> {
                    isExchanging = false;
                    currentPoints = newPoints;
                    txtMyPoint.setText("내 포인트: " + currentPoints + "P");
                    Toast.makeText(this, reward.getName() + " 교환 완료!", Toast.LENGTH_SHORT).show();
                    loadRewards();
                })
                .addOnFailureListener(e -> {
                    isExchanging = false;
                    Toast.makeText(this, "교환 실패: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    loadMyPoint();
                    loadRewards();
                });
    }
}
