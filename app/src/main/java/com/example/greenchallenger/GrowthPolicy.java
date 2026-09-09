package com.example.greenchallenger;

public final class GrowthPolicy {
    public static final int STAGE_2_POINTS = 50;
    public static final int STAGE_3_POINTS = 150;

    private GrowthPolicy() {
    }

    public static int getGrowthStage(int points) {
        if (points < STAGE_2_POINTS) {
            return 1;
        }
        if (points < STAGE_3_POINTS) {
            return 2;
        }
        return 3;
    }

    public static String getGrowthStageName(int stage) {
        switch (stage) {
            case 1:
                return "새싹";
            case 2:
                return "묘목";
            case 3:
            default:
                return "나무";
        }
    }

    public static String getGrowthStatusText(int points) {
        int stage = getGrowthStage(points);
        if (stage == 1) {
            return "새싹이 자라는 중이에요. 묘목까지 " + (STAGE_2_POINTS - points) + "P 남았어요.";
        }
        if (stage == 2) {
            return "묘목이 단단해지는 중이에요. 나무까지 " + (STAGE_3_POINTS - points) + "P 남았어요.";
        }
        return "나무가 건강하게 자랐어요. 계속 포인트를 모아 랭킹을 올려보세요.";
    }
}
