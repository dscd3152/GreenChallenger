package com.example.greenchallenger;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.util.Log;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class RankingActivity extends AppCompatActivity {

    private RecyclerView recyclerRanking;
    private RankingAdapter adapter;
    private List<User> users = new ArrayList<>();

    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ranking);

        recyclerRanking = findViewById(R.id.recyclerRanking);
        recyclerRanking.setLayoutManager(new LinearLayoutManager(this));

        adapter = new RankingAdapter(users);
        recyclerRanking.setAdapter(adapter);

        loadRanking();
    }

    private void loadRanking() {

        Log.d("DEBUG", "🔥 Firestore users 컬렉션에서 랭킹 로딩 중...");

        db.collection("users")
                .orderBy("ecoPoints", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(result -> {

                    users.clear();

                    for (DocumentSnapshot doc : result) {
                        Log.d("DEBUG", "📌 Firestore 문서: " + doc.getData());

                        User user = doc.toObject(User.class);
                        if (user != null) {
                            users.add(user);
                        }
                    }

                    // Firestore에 유저가 하나도 없으면 테스트용 더미 데이터 표시
                    if (users.isEmpty()) {
                        Log.w("DEBUG", "⚠ Firestore 비어있음 → 더미 데이터 표시");

                        users.add(new User("test01", "테스트 유저", 55, 3, ""));
                        users.add(new User("test02", "나무 키우는 사람", 120, 2, ""));
                        users.add(new User("test03", "환경 지키미", 15, 1, ""));
                    }

                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Log.e("DEBUG", "❌ Firestore 로딩 실패: " + e.getMessage());
                });
    }
}