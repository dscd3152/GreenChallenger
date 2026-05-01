package com.example.greenchallenger;

public class Mission {
    private String title;
    private String detail;
    private boolean completed;

    public Mission(String title, String detail) {
        this.title = title;
        this.detail = detail;
        this.completed = false;
    }

    public String getTitle() {
        return title;
    }

    public String getDetail() {
        return detail;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }
}
