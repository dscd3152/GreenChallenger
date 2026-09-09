package com.example.greenchallenger;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MyPageActivity extends AppCompatActivity {

    private static final String[] INTEREST_OPTIONS = {
            "분리수거", "텀블러", "대중교통", "제로웨이스트", "플로깅"
    };

    private ImageView profileImage;
    private TextView userName, userEmail, txtBio, txtInterests, missionCount;
    private Button btnEditProfile, btnLogout, btnMyRewards, btnPointHistory;

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private User loadedUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_page);

        profileImage = findViewById(R.id.profileImage);
        userName = findViewById(R.id.userName);
        userEmail = findViewById(R.id.userEmail);
        txtBio = findViewById(R.id.txtBio);
        txtInterests = findViewById(R.id.txtInterests);
        missionCount = findViewById(R.id.missionCount);
        btnEditProfile = findViewById(R.id.btnEditProfile);
        btnLogout = findViewById(R.id.btnLogout);
        btnMyRewards = findViewById(R.id.btnMyRewards);
        btnPointHistory = findViewById(R.id.btnPointHistory);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        NavHelper.setup(this, NavHelper.MY);

        loadUserProfile();

        btnEditProfile.setOnClickListener(v -> showEditProfileDialog());
        btnMyRewards.setOnClickListener(v ->
                startActivity(new Intent(MyPageActivity.this, MyRewardsActivity.class)));
        btnPointHistory.setOnClickListener(v ->
                startActivity(new Intent(MyPageActivity.this, PointHistoryActivity.class)));
        btnLogout.setOnClickListener(v -> {
            auth.signOut();
            Toast.makeText(this, "로그아웃되었습니다.", Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(MyPageActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUserProfile();
    }

    private void loadUserProfile() {
        FirebaseUser currentUser = auth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("users")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        Toast.makeText(this, "사용자 정보가 없습니다.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    User user = documentSnapshot.toObject(User.class);
                    if (user == null) {
                        Toast.makeText(this, "사용자 정보를 읽지 못했습니다.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    loadedUser = user;
                    renderUserProfile(user);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "데이터 불러오기 실패: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    private void renderUserProfile(User user) {
        String nickname = hasText(user.getNickname()) ? user.getNickname() : "그린챌린저";
        String email = hasText(user.getEmail()) ? user.getEmail() : "이메일 로그인 사용자";
        String bio = hasText(user.getBio()) ? user.getBio() : "자기소개를 입력해보세요.";
        List<String> interests = user.getInterests();

        int points = user.getEcoPoints();
        int growthStage = GrowthPolicy.getGrowthStage(points);

        profileImage.setImageResource(getProfileImageRes(user.getProfileImageUrl()));
        userName.setText(nickname);
        userEmail.setText(email);
        txtBio.setText(bio);
        txtInterests.setText(interests.isEmpty()
                ? "관심 분야: 아직 선택하지 않음"
                : "관심 분야: " + joinList(interests));
        missionCount.setText(
                "포인트: " + points + "P\n" +
                        "출석: " + user.getAttendanceCount() + "일\n" +
                        "완료한 미션: " + user.getMissionCompletedCount() + "개\n" +
                        "성장 단계: " + GrowthPolicy.getGrowthStageName(growthStage) + " 단계\n" +
                        GrowthPolicy.getGrowthStatusText(points)
        );
    }

    private void showEditProfileDialog() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_profile, null);
        ImageView previewImage = dialogView.findViewById(R.id.dialogProfilePreview);
        TextView previewName = dialogView.findViewById(R.id.dialogPreviewName);
        TextView previewBio = dialogView.findViewById(R.id.dialogPreviewBio);
        EditText nicknameInput = dialogView.findViewById(R.id.dialogNicknameInput);
        EditText bioInput = dialogView.findViewById(R.id.dialogBioInput);
        RadioGroup profileGroup = dialogView.findViewById(R.id.dialogProfileGroup);
        LinearLayout interestContainer = dialogView.findViewById(R.id.dialogInterestContainer);
        Button cancelButton = dialogView.findViewById(R.id.dialogCancelButton);
        Button saveButton = dialogView.findViewById(R.id.dialogSaveButton);

        String currentNickname = loadedUser != null ? safeText(loadedUser.getNickname()) : "";
        String currentBio = loadedUser != null ? safeText(loadedUser.getBio()) : "";
        String currentProfileKey = loadedUser != null ? normalizeProfileKey(loadedUser.getProfileImageUrl()) : "default";
        List<String> currentInterests = loadedUser != null ? loadedUser.getInterests() : new ArrayList<>();

        nicknameInput.setText(currentNickname);
        bioInput.setText(currentBio);
        previewName.setText(currentNickname.isEmpty() ? "그린챌린저" : currentNickname);
        previewBio.setText(currentBio.isEmpty() ? "자기소개를 입력해보세요." : currentBio);
        previewImage.setImageResource(getProfileImageRes(currentProfileKey));

        Map<Integer, String> profileOptionIds = new HashMap<>();
        for (String[] option : getProfileOptions()) {
            RadioButton button = new RadioButton(this);
            int id = View.generateViewId();
            button.setId(id);
            button.setText(option[1]);
            button.setTextColor(getResources().getColor(R.color.text_secondary));
            button.setTextSize(14);
            button.setPadding(0, dpToPx(4), 0, dpToPx(4));
            profileOptionIds.put(id, option[0]);
            profileGroup.addView(button);
            if (option[0].equals(currentProfileKey)) {
                profileGroup.check(id);
            }
        }
        if (profileGroup.getCheckedRadioButtonId() == -1 && profileGroup.getChildCount() > 0) {
            profileGroup.check(profileGroup.getChildAt(0).getId());
        }

        List<CheckBox> interestChecks = new ArrayList<>();
        for (String option : INTEREST_OPTIONS) {
            CheckBox checkBox = new CheckBox(this);
            checkBox.setText(option);
            checkBox.setTextColor(getResources().getColor(R.color.text_secondary));
            checkBox.setTextSize(14);
            checkBox.setPadding(0, dpToPx(3), 0, dpToPx(3));
            checkBox.setChecked(currentInterests.contains(option));
            interestChecks.add(checkBox);
            interestContainer.addView(checkBox);
        }

        profileGroup.setOnCheckedChangeListener((group, checkedId) -> {
            String profileKey = profileOptionIds.get(checkedId);
            previewImage.setImageResource(getProfileImageRes(profileKey));
        });

        TextWatcher previewWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String nickname = nicknameInput.getText().toString().trim();
                String bio = bioInput.getText().toString().trim();
                previewName.setText(nickname.isEmpty() ? "그린챌린저" : nickname);
                previewBio.setText(bio.isEmpty() ? "자기소개를 입력해보세요." : bio);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        };
        nicknameInput.addTextChangedListener(previewWatcher);
        bioInput.addTextChangedListener(previewWatcher);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        cancelButton.setOnClickListener(v -> dialog.dismiss());
        saveButton.setOnClickListener(v -> {
            String newNickname = nicknameInput.getText().toString().trim();
            String newBio = bioInput.getText().toString().trim();
            if (newNickname.isEmpty()) {
                nicknameInput.setError("닉네임을 입력해 주세요.");
                return;
            }

            List<String> selectedInterests = new ArrayList<>();
            for (CheckBox checkBox : interestChecks) {
                if (checkBox.isChecked()) {
                    selectedInterests.add(checkBox.getText().toString());
                }
            }

            String profileKey = profileOptionIds.get(profileGroup.getCheckedRadioButtonId());
            saveButton.setEnabled(false);
            saveButton.setText("저장 중");
            updateProfile(currentUser.getUid(), newNickname, newBio, profileKey, selectedInterests, dialog, saveButton);
        });

        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.94f),
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
    }

    private void updateProfile(String uid, String nickname, String bio, String profileKey,
                               List<String> interests, AlertDialog dialog, Button saveButton) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("nickname", nickname);
        updates.put("bio", bio);
        updates.put("profileImageUrl", profileKey != null ? profileKey : "default");
        updates.put("interests", interests);

        db.collection("users")
                .document(uid)
                .update(updates)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "내 정보가 수정되었습니다.", Toast.LENGTH_SHORT).show();
                    if (loadedUser != null) {
                        loadedUser.setNickname(nickname);
                        loadedUser.setBio(bio);
                        loadedUser.setProfileImageUrl(profileKey);
                        loadedUser.setInterests(interests);
                        renderUserProfile(loadedUser);
                    } else {
                        loadUserProfile();
                    }
                    dialog.dismiss();
                })
                .addOnFailureListener(e -> {
                    saveButton.setEnabled(true);
                    saveButton.setText("저장");
                    Toast.makeText(this, "수정 실패: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private String[][] getProfileOptions() {
        return new String[][]{
                {"default", "기본 프로필"},
                {"brand", "그린챌린지 아이콘"},
                {"stage1", "새싹"},
                {"stage2", "묘목"},
                {"stage3", "나무"}
        };
    }

    private int getProfileImageRes(String profileKey) {
        switch (normalizeProfileKey(profileKey)) {
            case "brand":
                return R.drawable.brand_icon;
            case "stage1":
                return R.drawable.tree_stage1;
            case "stage2":
                return R.drawable.tree_stage2;
            case "stage3":
                return R.drawable.tree_stage3;
            case "default":
            default:
                return R.drawable.ic_profile_placeholder;
        }
    }

    private String normalizeProfileKey(String profileKey) {
        if (profileKey == null || profileKey.trim().isEmpty() || profileKey.startsWith("http")) {
            return "default";
        }
        return profileKey;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String safeText(String value) {
        return value != null ? value : "";
    }

    private String joinList(List<String> values) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(values.get(i));
        }
        return builder.toString();
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
