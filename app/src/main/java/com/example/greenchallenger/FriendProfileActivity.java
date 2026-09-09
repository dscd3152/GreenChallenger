package com.example.greenchallenger;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.firestore.FirebaseFirestore;

public class FriendProfileActivity extends AppCompatActivity {

    private ImageView imgFriendProfile, imgFriendTree;
    private TextView txtFriendName, txtFriendBio, txtFriendInterests, txtFriendStats, txtFriendGrowth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friend_profile);

        imgFriendProfile = findViewById(R.id.imgFriendProfileDetail);
        imgFriendTree = findViewById(R.id.imgFriendTree);
        txtFriendName = findViewById(R.id.txtFriendNameDetail);
        txtFriendBio = findViewById(R.id.txtFriendBioDetail);
        txtFriendInterests = findViewById(R.id.txtFriendInterestsDetail);
        txtFriendStats = findViewById(R.id.txtFriendStatsDetail);
        txtFriendGrowth = findViewById(R.id.txtFriendGrowthDetail);
        db = FirebaseFirestore.getInstance();

        String friendUid = getIntent().getStringExtra("friendUid");
        if (friendUid == null || friendUid.trim().isEmpty()) {
            Toast.makeText(this, "친구 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadFriend(friendUid);
    }

    private void loadFriend(String uid) {
        db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    User user = doc.toObject(User.class);
                    if (user == null) {
                        Toast.makeText(this, "친구 정보를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }
                    user.setUid(doc.getId());
                    render(user);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "친구 정보 불러오기 실패: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    private void render(User user) {
        int stage = GrowthPolicy.getGrowthStage(user.getEcoPoints());
        imgFriendProfile.setImageResource(ProfileUi.getProfileImageRes(user.getProfileImageUrl()));
        txtFriendName.setText(ProfileUi.nickname(user));
        txtFriendBio.setText(ProfileUi.bio(user));
        txtFriendInterests.setText(ProfileUi.interestsText(user));
        txtFriendStats.setText(
                user.getEcoPoints() + "P · 출석 " + user.getAttendanceCount() +
                        "일 · 미션 " + user.getMissionCompletedCount() + "개"
        );
        txtFriendGrowth.setText(GrowthPolicy.getGrowthStatusText(user.getEcoPoints()));

        if (stage == 1) {
            imgFriendTree.setImageResource(R.drawable.tree_stage1);
        } else if (stage == 2) {
            imgFriendTree.setImageResource(R.drawable.tree_stage2);
        } else {
            imgFriendTree.setImageResource(R.drawable.tree_stage3);
        }
    }
}
