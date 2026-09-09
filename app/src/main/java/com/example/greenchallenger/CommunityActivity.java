package com.example.greenchallenger;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class CommunityActivity extends AppCompatActivity {

    private RecyclerView recyclerCommunity;
    private Button btnWritePost;
    private ImageButton btnOpenChats;
    private FirebaseFirestore db;
    private final List<CommunityPost> posts = new ArrayList<>();
    private CommunityAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_community);

        recyclerCommunity = findViewById(R.id.recyclerCommunity);
        btnWritePost = findViewById(R.id.btnWritePost);
        btnOpenChats = findViewById(R.id.btnOpenChats);
        db = FirebaseFirestore.getInstance();
        NavHelper.setup(this, NavHelper.COMMUNITY);

        adapter = new CommunityAdapter(posts, post -> {
            Intent intent = new Intent(this, CommunityDetailActivity.class);
            intent.putExtra("postId", post.getPostId());
            startActivity(intent);
        });
        recyclerCommunity.setLayoutManager(new LinearLayoutManager(this));
        recyclerCommunity.setAdapter(adapter);
        btnWritePost.setOnClickListener(v -> startActivity(new Intent(this, CreatePostActivity.class)));
        btnOpenChats.setOnClickListener(v -> startActivity(new Intent(this, ChatListActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPosts();
    }

    private void loadPosts() {
        db.collection("communityPosts")
                .orderBy("createdAtMillis", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snapshot -> {
                    posts.clear();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        CommunityPost post = doc.toObject(CommunityPost.class);
                        if (post != null) {
                            post.setPostId(doc.getId());
                            posts.add(post);
                        }
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "커뮤니티 글 불러오기 실패: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }
}
