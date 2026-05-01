package com.example.greenchallenger;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class MyRewardsActivity extends AppCompatActivity {

    private RecyclerView recyclerMyRewards;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private final List<MyRewardItem> myRewardList = new ArrayList<>();
    private final List<String> documentIds = new ArrayList<>();
    private MyRewardAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_rewards);

        recyclerMyRewards = findViewById(R.id.recyclerMyRewards);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        recyclerMyRewards.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MyRewardAdapter(myRewardList, documentIds, this::markRewardAsUsed);
        recyclerMyRewards.setAdapter(adapter);

        loadMyRewards();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadMyRewards();
    }

    private void loadMyRewards() {
        FirebaseUser currentUser = auth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "로그인된 사용자가 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = currentUser.getUid();

        db.collection("users")
                .document(uid)
                .collection("myRewards")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    myRewardList.clear();
                    documentIds.clear();

                    for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                        MyRewardItem reward = doc.toObject(MyRewardItem.class);
                        if (reward != null) {
                            myRewardList.add(reward);
                            documentIds.add(doc.getId());
                        }
                    }

                    adapter.notifyDataSetChanged();

                    if (myRewardList.isEmpty()) {
                        Toast.makeText(this, "보관함에 기프티콘이 없습니다.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "보관함 불러오기 실패: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void markRewardAsUsed(String documentId, MyRewardItem rewardItem) {
        FirebaseUser currentUser = auth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "로그인된 사용자가 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!"unused".equals(rewardItem.getStatus())) {
            Toast.makeText(this, "이미 사용된 기프티콘입니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = currentUser.getUid();

        db.collection("users")
                .document(uid)
                .collection("myRewards")
                .document(documentId)
                .update("status", "used")
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, rewardItem.getName() + " 사용 완료 처리됨", Toast.LENGTH_SHORT).show();
                    loadMyRewards();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "상태 변경 실패: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}