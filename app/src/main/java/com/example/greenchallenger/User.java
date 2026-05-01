package com.example.greenchallenger;

public class User {
    private String uid;
    private String nickname;
    private int ecoPoints;
    private int growthStage;
    private String profileImageUrl;
    private String email;
    private int attendanceCount;
    private int missionCompletedCount;
    private int friendCount;

    // Firestore용 기본 생성자 (필수)
    public User() {
    }

    // 기존 랭킹 코드 호환용 생성자
    public User(String uid, String nickname, int ecoPoints, int growthStage, String profileImageUrl) {
        this.uid = uid;
        this.nickname = nickname;
        this.ecoPoints = ecoPoints;
        this.growthStage = growthStage;
        this.profileImageUrl = profileImageUrl;
    }

    // 회원가입 저장용 전체 생성자
    public User(String uid, String email, String nickname, int ecoPoints, int growthStage,
                int attendanceCount, int missionCompletedCount, int friendCount, String profileImageUrl) {
        this.uid = uid;
        this.email = email;
        this.nickname = nickname;
        this.ecoPoints = ecoPoints;
        this.growthStage = growthStage;
        this.attendanceCount = attendanceCount;
        this.missionCompletedCount = missionCompletedCount;
        this.friendCount = friendCount;
        this.profileImageUrl = profileImageUrl;
    }

    public String getUid() {
        return uid;
    }

    public String getNickname() {
        return nickname;
    }

    public int getEcoPoints() {
        return ecoPoints;
    }

    public int getGrowthStage() {
        return growthStage;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public String getEmail() {
        return email;
    }

    public int getAttendanceCount() {
        return attendanceCount;
    }

    public int getMissionCompletedCount() {
        return missionCompletedCount;
    }

    public int getFriendCount() {
        return friendCount;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public void setEcoPoints(int ecoPoints) {
        this.ecoPoints = ecoPoints;
    }

    public void setGrowthStage(int growthStage) {
        this.growthStage = growthStage;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setAttendanceCount(int attendanceCount) {
        this.attendanceCount = attendanceCount;
    }

    public void setMissionCompletedCount(int missionCompletedCount) {
        this.missionCompletedCount = missionCompletedCount;
    }

    public void setFriendCount(int friendCount) {
        this.friendCount = friendCount;
    }
}