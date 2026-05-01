package com.example.greenchallenger;

public class MyRewardItem {
    public String rewardId;
    public String stockId;
    public String name;
    public int cost;
    public String imageName;
    public String description;
    public String status;
    public String redeemedAt;

    public MyRewardItem() {
    }

    public MyRewardItem(String rewardId, String stockId, String name, int cost,
                        String imageName, String description, String status, String redeemedAt) {
        this.rewardId = rewardId;
        this.stockId = stockId;
        this.name = name;
        this.cost = cost;
        this.imageName = imageName;
        this.description = description;
        this.status = status;
        this.redeemedAt = redeemedAt;
    }

    public String getRewardId() {
        return rewardId;
    }

    public String getStockId() {
        return stockId;
    }

    public String getName() {
        return name;
    }

    public int getCost() {
        return cost;
    }

    public String getImageName() {
        return imageName;
    }

    public String getDescription() {
        return description;
    }

    public String getStatus() {
        return status;
    }

    public String getRedeemedAt() {
        return redeemedAt;
    }
}