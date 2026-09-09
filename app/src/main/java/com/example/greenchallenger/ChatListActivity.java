package com.example.greenchallenger;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ChatListActivity extends AppCompatActivity {

    private TextView txtChatEmpty;
    private RecyclerView recyclerChats;
    private final List<Map<String, Object>> chats = new ArrayList<>();
    private ChatRoomAdapter adapter;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_list);

        txtChatEmpty = findViewById(R.id.txtChatEmpty);
        recyclerChats = findViewById(R.id.recyclerChats);
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        NavHelper.setup(this, NavHelper.COMMUNITY);

        adapter = new ChatRoomAdapter(chats, this::openChat);
        recyclerChats.setLayoutManager(new LinearLayoutManager(this));
        recyclerChats.setAdapter(adapter);
        loadChats();
    }

    private void loadChats() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db.collection("chats")
                .whereArrayContains("participantUids", user.getUid())
                .get()
                .addOnSuccessListener(snapshot -> {
                    chats.clear();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : snapshot.getDocuments()) {
                        Map<String, Object> chat = doc.getData();
                        if (chat != null) {
                            chat.put("chatId", doc.getId());
                            applyPrivateChatTitle(chat, user.getUid());
                            chats.add(chat);
                        }
                    }
                    Collections.sort(chats, (a, b) -> Long.compare(getTime(b), getTime(a)));
                    adapter.notifyDataSetChanged();
                    txtChatEmpty.setVisibility(chats.isEmpty() ? View.VISIBLE : View.GONE);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "채팅 목록 불러오기 실패: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    private long getTime(Map<String, Object> chat) {
        Object value = chat.get("updatedAtMillis");
        return value instanceof Number ? ((Number) value).longValue() : 0L;
    }

    private void openChat(Map<String, Object> chat) {
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("chatId", String.valueOf(chat.get("chatId")));
        intent.putExtra("chatTitle", String.valueOf(chat.get("title")));
        startActivity(intent);
    }

    private void applyPrivateChatTitle(Map<String, Object> chat, String uid) {
        if (!"private".equals(String.valueOf(chat.get("type")))) {
            return;
        }
        Object titleByUid = chat.get("titleByUid");
        if (titleByUid instanceof Map) {
            Object myTitle = ((Map<?, ?>) titleByUid).get(uid);
            if (myTitle != null) {
                chat.put("title", String.valueOf(myTitle));
            }
        }
    }
}
