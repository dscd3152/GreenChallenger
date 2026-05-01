package com.example.greenchallenger;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class RewardAdapter extends RecyclerView.Adapter<RewardAdapter.ViewHolder> {

    public interface OnRewardClickListener {
        void onRewardClick(RewardItem reward);
    }

    private final List<RewardItem> rewardList;
    private final OnRewardClickListener listener;

    public RewardAdapter(List<RewardItem> rewardList, OnRewardClickListener listener) {
        this.rewardList = rewardList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_reward, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RewardItem reward = rewardList.get(position);

        holder.txtRewardName.setText(reward.getName());
        holder.txtRewardCost.setText(reward.getCost() + "P");
        holder.txtRewardDesc.setText(reward.getDescription());
        holder.txtRewardStock.setText("남은 수량: " + reward.getStockCount() + "개");

        int imageResId = holder.itemView.getContext().getResources()
                .getIdentifier(reward.getThumbnailImageName(), "drawable",
                        holder.itemView.getContext().getPackageName());

        if (imageResId != 0) {
            holder.imgReward.setImageResource(imageResId);
        }

        holder.btnExchange.setEnabled(reward.getStockCount() > 0);
        holder.btnExchange.setText(reward.getStockCount() > 0 ? "교환" : "품절");

        holder.btnExchange.setOnClickListener(v -> listener.onRewardClick(reward));
    }

    @Override
    public int getItemCount() {
        return rewardList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgReward;
        TextView txtRewardName, txtRewardCost, txtRewardDesc, txtRewardStock;
        Button btnExchange;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgReward = itemView.findViewById(R.id.imgReward);
            txtRewardName = itemView.findViewById(R.id.txtRewardName);
            txtRewardCost = itemView.findViewById(R.id.txtRewardCost);
            txtRewardDesc = itemView.findViewById(R.id.txtRewardDesc);
            txtRewardStock = itemView.findViewById(R.id.txtRewardStock);
            btnExchange = itemView.findViewById(R.id.btnExchange);
        }
    }
}