package com.example.greenchallenger;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FriendsActivity extends AppCompatActivity {

    private EditText edtFriendSearch;
    private Button btnSearchFriend, btnShowMyFriends, btnShowRequests;
    private TextView txtFriendTitle, txtFriendEmpty;
    private RecyclerView recyclerFriends;

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private FriendAdapter adapter;
    private final List<User> users = new ArrayList<>();
    private final Set<String> friendIds = new HashSet<>();
    private final Set<String> outgoingRequestIds = new HashSet<>();
    private int currentMode = FriendAdapter.MODE_FRIENDS;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friends);

        edtFriendSearch = findViewById(R.id.edtFriendSearch);
        btnSearchFriend = findViewById(R.id.btnSearchFriend);
        btnShowMyFriends = findViewById(R.id.btnShowMyFriends);
        btnShowRequests = findViewById(R.id.btnShowRequests);
        txtFriendTitle = findViewById(R.id.txtFriendTitle);
        txtFriendEmpty = findViewById(R.id.txtFriendEmpty);
        recyclerFriends = findViewById(R.id.recyclerFriends);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = auth.getCurrentUser();
        NavHelper.setup(this, NavHelper.FRIENDS);

        recyclerFriends.setLayoutManager(new LinearLayoutManager(this));
        setAdapter(FriendAdapter.MODE_FRIENDS);

        btnSearchFriend.setOnClickListener(v -> searchFriends());
        btnShowMyFriends.setOnClickListener(v -> loadFriendIdsThenList());
        btnShowRequests.setOnClickListener(v -> loadIncomingRequests());

        if (currentUser == null) {
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadRelationshipState(this::loadFriendIdsThenList);
    }

    private void setAdapter(int mode) {
        currentMode = mode;
        adapter = new FriendAdapter(users, friendIds, outgoingRequestIds,
                currentUser != null ? currentUser.getUid() : "",
                currentMode,
                new FriendAdapter.Listener() {
                    @Override
                    public void onOpenProfile(User user) {
                        openFriendProfile(user);
                    }

                    @Override
                    public void onPrimaryAction(User user) {
                        if (currentMode == FriendAdapter.MODE_REQUESTS) {
                            acceptFriendRequest(user);
                        } else if (currentMode == FriendAdapter.MODE_SEARCH) {
                            sendFriendRequest(user);
                        } else {
                            openPrivateChat(user);
                        }
                    }
                });
        recyclerFriends.setAdapter(adapter);
    }

    private void loadRelationshipState(Runnable afterLoad) {
        if (currentUser == null) {
            return;
        }

        friendIds.clear();
        outgoingRequestIds.clear();

        db.collection("users")
                .document(currentUser.getUid())
                .collection("friends")
                .get()
                .addOnSuccessListener(friendSnapshot -> {
                    for (DocumentSnapshot doc : friendSnapshot.getDocuments()) {
                        friendIds.add(doc.getId());
                    }

                    db.collection("users")
                            .document(currentUser.getUid())
                            .collection("sentFriendRequests")
                            .get()
                            .addOnSuccessListener(requestSnapshot -> {
                                for (DocumentSnapshot doc : requestSnapshot.getDocuments()) {
                                    outgoingRequestIds.add(doc.getId());
                                }
                                afterLoad.run();
                            })
                            .addOnFailureListener(e -> afterLoad.run());
                })
                .addOnFailureListener(e -> afterLoad.run());
    }

    private void loadFriendIdsThenList() {
        if (currentUser == null) {
            return;
        }

        txtFriendTitle.setText("내 친구");
        setAdapter(FriendAdapter.MODE_FRIENDS);
        friendIds.clear();

        db.collection("users")
                .document(currentUser.getUid())
                .collection("friends")
                .get()
                .addOnSuccessListener(snapshot -> {
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        friendIds.add(doc.getId());
                    }
                    loadProfilesByIds(friendIds, "아직 추가한 친구가 없어요. 닉네임으로 친구를 찾아보세요.");
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "친구 목록 불러오기 실패: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    private void loadIncomingRequests() {
        if (currentUser == null) {
            return;
        }

        txtFriendTitle.setText("받은 친구 요청");
        setAdapter(FriendAdapter.MODE_REQUESTS);
        users.clear();
        adapter.notifyDataSetChanged();
        updateEmptyText("");

        db.collection("users")
                .document(currentUser.getUid())
                .collection("friendRequests")
                .get()
                .addOnSuccessListener(snapshot -> {
                    Set<String> requesterIds = new HashSet<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        requesterIds.add(doc.getId());
                    }
                    loadProfilesByIds(requesterIds, "받은 친구 요청이 없어요.");
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "친구 요청 불러오기 실패: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    private void loadProfilesByIds(Set<String> ids, String emptyMessage) {
        users.clear();
        adapter.notifyDataSetChanged();

        if (ids.isEmpty()) {
            updateEmptyText(emptyMessage);
            return;
        }

        updateEmptyText("");
        final int[] remaining = {ids.size()};
        for (String uid : ids) {
            db.collection("users")
                    .document(uid)
                    .get()
                    .addOnSuccessListener(doc -> {
                        User user = toUser(doc);
                        if (user != null) {
                            users.add(user);
                        }
                        remaining[0]--;
                        if (remaining[0] == 0) {
                            adapter.notifyDataSetChanged();
                            updateEmptyText(users.isEmpty() ? emptyMessage : "");
                        }
                    })
                    .addOnFailureListener(e -> {
                        remaining[0]--;
                        if (remaining[0] == 0) {
                            adapter.notifyDataSetChanged();
                            updateEmptyText(users.isEmpty() ? emptyMessage : "");
                        }
                    });
        }
    }

    private void searchFriends() {
        String keyword = edtFriendSearch.getText().toString().trim();
        if (keyword.isEmpty()) {
            Toast.makeText(this, "검색할 닉네임을 입력해 주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        txtFriendTitle.setText("검색 결과");
        setAdapter(FriendAdapter.MODE_SEARCH);
        users.clear();
        adapter.notifyDataSetChanged();
        updateEmptyText("");

        loadRelationshipState(() ->
                db.collection("users")
                        .orderBy("nickname")
                        .startAt(keyword)
                        .endAt(keyword + "\uf8ff")
                        .limit(20)
                        .get()
                        .addOnSuccessListener(snapshot -> {
                            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                                User user = toUser(doc);
                                if (user != null) {
                                    users.add(user);
                                }
                            }
                            adapter.notifyDataSetChanged();
                            updateEmptyText(users.isEmpty() ? "검색 결과가 없어요." : "");
                        })
                        .addOnFailureListener(e ->
                                Toast.makeText(this, "친구 검색 실패: " + e.getMessage(), Toast.LENGTH_LONG).show()
                        )
        );
    }

    private void sendFriendRequest(User friend) {
        if (currentUser == null || friend == null || friend.getUid() == null) {
            return;
        }

        if (currentUser.getUid().equals(friend.getUid())) {
            Toast.makeText(this, "내 계정에는 친구 요청을 보낼 수 없어요.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (friendIds.contains(friend.getUid())) {
            Toast.makeText(this, "이미 친구로 추가되어 있어요.", Toast.LENGTH_SHORT).show();
            return;
        }

        DocumentReference requestRef = db.collection("users")
                .document(friend.getUid())
                .collection("friendRequests")
                .document(currentUser.getUid());
        DocumentReference sentRef = db.collection("users")
                .document(currentUser.getUid())
                .collection("sentFriendRequests")
                .document(friend.getUid());

        requestRef.get().addOnSuccessListener(doc -> {
            if (doc.exists() || outgoingRequestIds.contains(friend.getUid())) {
                Toast.makeText(this, "이미 친구 요청을 보냈어요.", Toast.LENGTH_SHORT).show();
                return;
            }

            WriteBatch batch = db.batch();
            batch.set(requestRef, newRequestData(currentUser.getUid()));
            batch.set(sentRef, newRequestData(friend.getUid()));
            batch.commit()
                    .addOnSuccessListener(unused -> {
                        outgoingRequestIds.add(friend.getUid());
                        adapter.notifyDataSetChanged();
                        Toast.makeText(this, "친구 요청을 보냈어요.", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "친구 요청 실패: " + e.getMessage(), Toast.LENGTH_LONG).show()
                    );
        });
    }

    private void acceptFriendRequest(User requester) {
        if (currentUser == null || requester == null || requester.getUid() == null) {
            return;
        }

        String myUid = currentUser.getUid();
        String requesterUid = requester.getUid();

        DocumentReference myFriendRef = db.collection("users").document(myUid)
                .collection("friends").document(requesterUid);
        DocumentReference requesterFriendRef = db.collection("users").document(requesterUid)
                .collection("friends").document(myUid);
        DocumentReference incomingRequestRef = db.collection("users").document(myUid)
                .collection("friendRequests").document(requesterUid);
        DocumentReference requesterSentRef = db.collection("users").document(requesterUid)
                .collection("sentFriendRequests").document(myUid);
        DocumentReference myUserRef = db.collection("users").document(myUid);
        DocumentReference requesterUserRef = db.collection("users").document(requesterUid);

        WriteBatch batch = db.batch();
        batch.set(myFriendRef, newFriendData(requesterUid));
        batch.set(requesterFriendRef, newFriendData(myUid));
        batch.delete(incomingRequestRef);
        batch.delete(requesterSentRef);
        batch.update(myUserRef, "friendCount", FieldValue.increment(1));
        batch.update(requesterUserRef, "friendCount", FieldValue.increment(1));
        batch.commit()
                .addOnSuccessListener(unused -> {
                    friendIds.add(requesterUid);
                    users.remove(requester);
                    adapter.notifyDataSetChanged();
                    updateEmptyText(users.isEmpty() ? "받은 친구 요청이 없어요." : "");
                    Toast.makeText(this, "친구 요청을 수락했어요.", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "친구 요청 수락 실패: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    private java.util.Map<String, Object> newRequestData(String uid) {
        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("uid", uid);
        data.put("createdAt", FieldValue.serverTimestamp());
        return data;
    }

    private java.util.Map<String, Object> newFriendData(String uid) {
        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("uid", uid);
        data.put("addedAt", FieldValue.serverTimestamp());
        return data;
    }

    private void openFriendProfile(User user) {
        if (user == null || user.getUid() == null) {
            return;
        }
        Intent intent = new Intent(this, FriendProfileActivity.class);
        intent.putExtra("friendUid", user.getUid());
        startActivity(intent);
    }

    private void openPrivateChat(User friend) {
        if (currentUser == null || friend == null || friend.getUid() == null) {
            return;
        }

        String myUid = currentUser.getUid();
        String friendUid = friend.getUid();
        if (myUid.equals(friendUid)) {
            return;
        }

        String chatId = myUid.compareTo(friendUid) < 0
                ? "private_" + myUid + "_" + friendUid
                : "private_" + friendUid + "_" + myUid;

        db.collection("users").document(myUid).get()
                .addOnSuccessListener(myDoc -> {
                    User me = toUser(myDoc);
                    String myNickname = me != null ? ProfileUi.nickname(me) : "그린챌린저";
                    String friendNickname = ProfileUi.nickname(friend);

                    Map<String, Object> titleByUid = new java.util.HashMap<>();
                    titleByUid.put(myUid, friendNickname);
                    titleByUid.put(friendUid, myNickname);

                    Map<String, Object> chat = new java.util.HashMap<>();
                    chat.put("chatId", chatId);
                    chat.put("type", "private");
                    chat.put("title", friendNickname);
                    chat.put("titleByUid", titleByUid);
                    chat.put("participantUids", Arrays.asList(myUid, friendUid));
                    chat.put("updatedAtMillis", System.currentTimeMillis());
                    chat.put("updatedAt", FieldValue.serverTimestamp());

                    db.collection("chats").document(chatId).set(chat, SetOptions.merge())
                            .addOnSuccessListener(unused -> {
                                Intent intent = new Intent(this, ChatActivity.class);
                                intent.putExtra("chatId", chatId);
                                intent.putExtra("chatTitle", friendNickname);
                                startActivity(intent);
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "채팅방 생성 실패: " + e.getMessage(), Toast.LENGTH_LONG).show()
                            );
                });
    }

    private User toUser(DocumentSnapshot doc) {
        User user = doc.toObject(User.class);
        if (user != null && (user.getUid() == null || user.getUid().trim().isEmpty())) {
            user.setUid(doc.getId());
        }
        return user;
    }

    private void updateEmptyText(String message) {
        txtFriendEmpty.setText(message);
        txtFriendEmpty.setVisibility(message == null || message.isEmpty()
                ? android.view.View.GONE
                : android.view.View.VISIBLE);
    }
}
