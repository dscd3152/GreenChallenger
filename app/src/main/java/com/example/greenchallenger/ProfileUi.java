package com.example.greenchallenger;

import java.util.List;

public final class ProfileUi {

    private ProfileUi() {
    }

    public static int getProfileImageRes(String profileKey) {
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

    public static String normalizeProfileKey(String profileKey) {
        if (profileKey == null || profileKey.trim().isEmpty() || profileKey.startsWith("http")) {
            return "default";
        }
        return profileKey;
    }

    public static String nickname(User user) {
        if (user == null || user.getNickname() == null || user.getNickname().trim().isEmpty()) {
            return "그린챌린저";
        }
        return user.getNickname();
    }

    public static String bio(User user) {
        if (user == null || user.getBio() == null || user.getBio().trim().isEmpty()) {
            return "자기소개가 아직 없어요.";
        }
        return user.getBio();
    }

    public static String interestsText(User user) {
        if (user == null || user.getInterests().isEmpty()) {
            return "관심 분야: 아직 선택하지 않음";
        }
        return "관심 분야: " + joinList(user.getInterests());
    }

    public static String joinList(List<String> values) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(values.get(i));
        }
        return builder.toString();
    }
}
