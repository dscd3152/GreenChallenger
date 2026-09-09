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
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class CommunityDetailActivity extends AppCompatActivity {

    private ImageView imgAuthor;
    private TextView txtCategory, txtTitle, txtAuthor, txtContent, txtMeta, txtParticipants;
    private Button btnJoinPost, btnOpenChat;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private String postId;
    private CommunityPost loadedPost;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_community_detail);

        imgAuthor = findViewById(R.id.imgPostAuthor);
        txtCategory = findViewById(R.id.txtDetailCategory);
        txtTitle = findViewById(R.id.txtDetailTitle);
        txtAuthor = findViewById(R.id.txtDetailAuthor);
        txtContent = findViewById(R.id.txtDetailContent);
        txtMeta = findViewById(R.id.txtDetailMeta);
        txtParticipants = findViewById(R.id.txtDetailParticipants);
        btnJoinPost = findViewById(R.id.btnJoinPost);
        btnOpenChat = findViewById(R.id.btnOpenChat);
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        postId = getIntent().getStringExtra("postId");
        if (postId == null) {
            finish();
            return;
        }

        btnJoinPost.setOnClickListener(v -> joinPost());
        btnOpenChat.setOnClickListener(v -> openChat());
        loadPost();
    }

    private void loadPost() {
        db.collection("communityPosts").document(postId).get()
                .addOnSuccessListener(doc -> {
                    loadedPost = doc.toObject(CommunityPost.class);
                    if (loadedPost == null) {
                        Toast.makeText(this, "게시글을 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }
                    loadedPost.setPostId(doc.getId());
                    render();
                });
    }

    private void render() {
        imgAuthor.setImageResource(ProfileUi.getProfileImageRes(loadedPost.getAuthorProfileImageUrl()));
        txtCategory.setText(loadedPost.getCategory());
        txtTitle.setText(loadedPost.getTitle());
        txtAuthor.setText(loadedPost.getAuthorNickname());
        txtContent.setText(loadedPost.getContent());
        txtMeta.setText(loadedPost.getLocation() + " · " + loadedPost.getMeetingDate());
        txtParticipants.setText("참여 인원: " + loadedPost.getParticipantCount() + "/" + loadedPost.getMaxParticipants() + "명");
    }

    private void joinPost() {
        FirebaseUser firebaseUser = auth.getCurrentUser();
        if (firebaseUser == null || loadedPost == null) {
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (loadedPost.getParticipantCount() >= loadedPost.getMaxParticipants()) {
            Toast.makeText(this, "모집 인원이 가득 찼어요.", Toast.LENGTH_SHORT).show();
            return;
        }

        DocumentReference postRef = db.collection("communityPosts").document(postId);
        DocumentReference participantRef = postRef.collection("participants").document(firebaseUser.getUid());
        participantRef.get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                Toast.makeText(this, "이미 참여 중입니다.", Toast.LENGTH_SHORT).show();
                openChat();
                return;
            }

            db.collection("users").document(firebaseUser.getUid()).get().addOnSuccessListener(userDoc -> {
                User user = userDoc.toObject(User.class);
                Map<String, Object> participant = new HashMap<>();
                participant.put("uid", firebaseUser.getUid());
                participant.put("nickname", user != null ? ProfileUi.nickname(user) : "그린챌린저");
                participant.put("joinedAt", FieldValue.serverTimestamp());

                participantRef.set(participant)
                        .continueWithTask(task -> postRef.update("participantCount", FieldValue.increment(1)))
                        .addOnSuccessListener(unused -> {
                            loadedPost.setParticipantCount(loadedPost.getParticipantCount() + 1);
                            render();
                            ensureChatThenOpen();
                        });
            });
        });
    }

    private void openChat() {
        ensureChatThenOpen();
    }

    private void ensureChatThenOpen() {
        FirebaseUser firebaseUser = auth.getCurrentUser();
        if (firebaseUser == null || loadedPost == null) {
            return;
        }

        String chatId = "post_" + postId;
        Map<String, Object> chat = new HashMap<>();
        chat.put("chatId", chatId);
        chat.put("type", "group");
        chat.put("postId", postId);
        chat.put("title", loadedPost.getTitle());
        chat.put("updatedAtMillis", System.currentTimeMillis());
        chat.put("updatedAt", FieldValue.serverTimestamp());

        db.collection("chats").document(chatId).set(chat, SetOptions.merge())
                .continueWithTask(task -> db.collection("chats").document(chatId).update(
                        "participantUids",
                        FieldValue.arrayUnion(firebaseUser.getUid(), loadedPost.getAuthorUid())
                ))
                .addOnSuccessListener(unused -> {
                    Intent intent = new Intent(this, ChatActivity.class);
                    intent.putExtra("chatId", chatId);
                    intent.putExtra("chatTitle", loadedPost.getTitle());
                    startActivity(intent);
                });
    }
}
