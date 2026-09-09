package com.example.greenchallenger;

import android.app.Activity;
import android.content.Intent;
import android.widget.ImageButton;

public final class NavHelper {
    public static final String HOME = "home";
    public static final String MISSION = "mission";
    public static final String FRIENDS = "friends";
    public static final String COMMUNITY = "community";
    public static final String MY = "my";

    private NavHelper() {
    }

    public static void setup(Activity activity, String current) {
        setupItem(activity, R.id.navHome, current.equals(HOME), MainActivity.class);
        setupItem(activity, R.id.navMission, current.equals(MISSION), MissionActivity.class);
        setupItem(activity, R.id.navFriends, current.equals(FRIENDS), FriendsActivity.class);
        setupItem(activity, R.id.navCommunity, current.equals(COMMUNITY), CommunityActivity.class);
        setupItem(activity, R.id.navMy, current.equals(MY), MyPageActivity.class);
    }

    private static void setupItem(Activity activity, int id, boolean selected, Class<?> target) {
        ImageButton button = activity.findViewById(id);
        if (button == null) {
            return;
        }
        button.setAlpha(selected ? 1f : 0.58f);
        button.setEnabled(!selected);
        button.setOnClickListener(v -> {
            Intent intent = new Intent(activity, target);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            activity.startActivity(intent);
        });
    }
}
