package com.example.greenchallenger;

public class MissionItem {
    private String title;
    private String detail;
    private boolean isCompleted;

    public MissionItem(String title, String detail) {
        this.title = title;
        this.detail = detail;
        this.isCompleted = false;
    }

    public String getTitle() { return title; }
    public String getDetail() { return detail; }

    public boolean isCompleted() { return isCompleted; }
    public void setCompleted(boolean completed) { isCompleted = completed; }
}
