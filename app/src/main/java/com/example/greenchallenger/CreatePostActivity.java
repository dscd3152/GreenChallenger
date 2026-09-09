package com.example.greenchallenger;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class CreatePostActivity extends AppCompatActivity {

    private Spinner spinnerCategory;
    private EditText edtTitle, edtContent, edtLocation, edtMeetingDate, edtMaxParticipants;
    private Button btnSubmitPost;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_post);

        spinnerCategory = findViewById(R.id.spinnerCategory);
        edtTitle = findViewById(R.id.edtPostTitle);
        edtContent = findViewById(R.id.edtPostContent);
        edtLocation = findViewById(R.id.edtPostLocation);
        edtMeetingDate = findViewById(R.id.edtMeetingDate);
        edtMaxParticipants = findViewById(R.id.edtMaxParticipants);
        btnSubmitPost = findViewById(R.id.btnSubmitPost);
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        spinnerCategory.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
                new String[]{"봉사 모집", "생활 꿀팁", "미션 후기", "나눔/교환"}));
        btnSubmitPost.setOnClickListener(v -> submitPost());
    }

    private void submitPost() {
        FirebaseUser firebaseUser = auth.getCurrentUser();
        if (firebaseUser == null) {
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        String title = edtTitle.getText().toString().trim();
        String content = edtContent.getText().toString().trim();
        if (title.isEmpty() || content.isEmpty()) {
            Toast.makeText(this, "제목과 내용을 입력해 주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("users").document(firebaseUser.getUid()).get()
                .addOnSuccessListener(userDoc -> {
                    User user = userDoc.toObject(User.class);
                    String nickname = user != null ? ProfileUi.nickname(user) : "그린챌린저";
                    String profileKey = user != null ? user.getProfileImageUrl() : "default";
                    int maxParticipants = parseInt(edtMaxParticipants.getText().toString().trim(), 4);
                    DocumentReference postRef = db.collection("communityPosts").document();

                    Map<String, Object> post = new HashMap<>();
                    post.put("postId", postRef.getId());
                    post.put("authorUid", firebaseUser.getUid());
                    post.put("authorNickname", nickname);
                    post.put("authorProfileImageUrl", profileKey);
                    post.put("category", spinnerCategory.getSelectedItem().toString());
                    post.put("title", title);
                    post.put("content", content);
                    post.put("location", emptyToDefault(edtLocation.getText().toString().trim(), "장소 미정"));
                    post.put("meetingDate", emptyToDefault(edtMeetingDate.getText().toString().trim(), "일정 미정"));
                    post.put("maxParticipants", maxParticipants);
                    post.put("participantCount", 1);
                    post.put("status", "open");
                    post.put("createdAtMillis", System.currentTimeMillis());
                    post.put("createdAt", FieldValue.serverTimestamp());

                    Map<String, Object> participant = new HashMap<>();
                    participant.put("uid", firebaseUser.getUid());
                    participant.put("nickname", nickname);
                    participant.put("joinedAt", FieldValue.serverTimestamp());

                    postRef.set(post)
                            .continueWithTask(task -> postRef.collection("participants").document(firebaseUser.getUid()).set(participant))
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(this, "게시글을 등록했어요.", Toast.LENGTH_SHORT).show();
                                finish();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "게시글 등록 실패: " + e.getMessage(), Toast.LENGTH_LONG).show()
                            );
                });
    }

    private int parseInt(String value, int fallback) {
        try {
            return Math.max(1, Integer.parseInt(value));
        } catch (Exception e) {
            return fallback;
        }
    }

    private String emptyToDefault(String value, String fallback) {
        return value.isEmpty() ? fallback : value;
    }
}
