package com.example.greenchallenger;

public class UserManager {
    private static int userPoints = 0;

    public static void addPoints(int points) {
        userPoints += points;
    }

    public static int getPoints() {
        return userPoints;
    }
}
