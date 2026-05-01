package com.example.greenchallenger;

public class RewardItem {
    private String name;
    private int cost;
    private String description;
    private boolean isActive;
    private int stockCount;
    private String thumbnailImageName;

    public RewardItem() {
    }

    public RewardItem(String name, int cost, String description, boolean isActive, int stockCount, String thumbnailImageName) {
        this.name = name;
        this.cost = cost;
        this.description = description;
        this.isActive = isActive;
        this.stockCount = stockCount;
        this.thumbnailImageName = thumbnailImageName;
    }

    public String getName() {
        return name;
    }

    public int getCost() {
        return cost;
    }

    public String getDescription() {
        return description;
    }

    public boolean isActive() {
        return isActive;
    }

    public int getStockCount() {
        return stockCount;
    }

    public String getThumbnailImageName() {
        return thumbnailImageName;
    }
}