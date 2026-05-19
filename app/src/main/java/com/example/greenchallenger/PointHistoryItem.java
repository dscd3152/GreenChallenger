package com.example.greenchallenger;

public class PointHistoryItem {
    private final String title;
    private final String detail;
    private final String dateText;
    private final int points;

    public PointHistoryItem(String title, String detail, String dateText, int points) {
        this.title = title;
        this.detail = detail;
        this.dateText = dateText;
        this.points = points;
    }

    public String getTitle() {
        return title;
    }

    public String getDetail() {
        return detail;
    }

    public String getDateText() {
        return dateText;
    }

    public int getPoints() {
        return points;
    }
}
