package com.example.greenchallenger;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {

    private TextView txtChatTitle;
    private RecyclerView recyclerMessages;
    private EditText edtMessage;
    private Button btnSendMessage;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private MessageAdapter adapter;
    private final List<Map<String, Object>> messages = new ArrayList<>();
    private String chatId;
    private String senderNickname = "그린챌린저";
    private String senderProfileImageUrl = "default";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        txtChatTitle = findViewById(R.id.txtChatTitle);
        recyclerMessages = findViewById(R.id.recyclerMessages);
        edtMessage = findViewById(R.id.edtMessage);
        btnSendMessage = findViewById(R.id.btnSendMessage);
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        chatId = getIntent().getStringExtra("chatId");
        String chatTitle = getIntent().getStringExtra("chatTitle");
        txtChatTitle.setText(chatTitle == null || chatTitle.trim().isEmpty() ? "채팅" : chatTitle);

        FirebaseUser currentUser = auth.getCurrentUser();
        String currentUid = currentUser != null ? currentUser.getUid() : "";
        adapter = new MessageAdapter(messages, currentUid);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerMessages.setLayoutManager(layoutManager);
        recyclerMessages.setAdapter(adapter);

        loadMe();
        listenMessages();
        btnSendMessage.setOnClickListener(v -> sendMessage());
    }

    private void loadMe() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            return;
        }
        db.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(doc -> {
                    User me = doc.toObject(User.class);
                    if (me != null) {
                        senderNickname = ProfileUi.nickname(me);
                        senderProfileImageUrl = ProfileUi.normalizeProfileKey(me.getProfileImageUrl());
                    }
                });
    }

    private void listenMessages() {
        if (chatId == null) {
            finish();
            return;
        }
        db.collection("chats").document(chatId).collection("messages")
                .orderBy("createdAtMillis", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null || snapshot == null) {
                        return;
                    }
                    messages.clear();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : snapshot.getDocuments()) {
                        Map<String, Object> data = doc.getData();
                        if (data != null) {
                            messages.add(data);
                        }
                    }
                    adapter.notifyDataSetChanged();
                    if (!messages.isEmpty()) {
                        recyclerMessages.scrollToPosition(messages.size() - 1);
                    }
                });
    }

    private void sendMessage() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            return;
        }
        String text = edtMessage.getText().toString().trim();
        if (text.isEmpty()) {
            return;
        }

        Map<String, Object> message = new HashMap<>();
        message.put("senderUid", user.getUid());
        message.put("senderNickname", senderNickname);
        message.put("senderProfileImageUrl", senderProfileImageUrl);
        message.put("text", text);
        message.put("createdAtMillis", System.currentTimeMillis());
        message.put("createdAt", FieldValue.serverTimestamp());

        btnSendMessage.setEnabled(false);
        db.collection("chats").document(chatId).collection("messages").add(message)
                .addOnSuccessListener(ref -> {
                    edtMessage.setText("");
                    db.collection("chats").document(chatId).update(
                            "lastMessage", text,
                            "updatedAtMillis", System.currentTimeMillis(),
                            "updatedAt", FieldValue.serverTimestamp()
                    );
                })
                .addOnFailureListener(e -> Toast.makeText(this, "메시지 전송에 실패했습니다.", Toast.LENGTH_SHORT).show())
                .addOnCompleteListener(task -> btnSendMessage.setEnabled(true));
    }
}
