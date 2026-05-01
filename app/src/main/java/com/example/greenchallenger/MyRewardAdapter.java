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

public class MyRewardAdapter extends RecyclerView.Adapter<MyRewardAdapter.ViewHolder> {

    public interface OnUseClickListener {
        void onUseClick(String documentId, MyRewardItem rewardItem);
    }

    private final List<MyRewardItem> rewardList;
    private final List<String> documentIds;
    private final OnUseClickListener listener;

    public MyRewardAdapter(List<MyRewardItem> rewardList, List<String> documentIds, OnUseClickListener listener) {
        this.rewardList = rewardList;
        this.documentIds = documentIds;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_my_reward, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MyRewardItem reward = rewardList.get(position);
        String documentId = documentIds.get(position);

        holder.txtRewardName.setText(reward.getName());
        holder.txtRewardDesc.setText(reward.getDescription());
        holder.txtRewardDate.setText("교환일: " + reward.getRedeemedAt());
        holder.txtRewardStatus.setText("상태: " + reward.getStatus());

        int imageResId = holder.itemView.getContext().getResources()
                .getIdentifier(reward.getImageName(), "drawable",
                        holder.itemView.getContext().getPackageName());

        if (imageResId != 0) {
            holder.imgReward.setImageResource(imageResId);
        }

        boolean canUse = "unused".equals(reward.getStatus());
        holder.btnUseReward.setEnabled(canUse);
        holder.btnUseReward.setText(canUse ? "사용 완료" : "사용됨");

        holder.btnUseReward.setOnClickListener(v -> listener.onUseClick(documentId, reward));
    }

    @Override
    public int getItemCount() {
        return rewardList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgReward;
        TextView txtRewardName, txtRewardDesc, txtRewardDate, txtRewardStatus;
        Button btnUseReward;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgReward = itemView.findViewById(R.id.imgMyReward);
            txtRewardName = itemView.findViewById(R.id.txtMyRewardName);
            txtRewardDesc = itemView.findViewById(R.id.txtMyRewardDesc);
            txtRewardDate = itemView.findViewById(R.id.txtMyRewardDate);
            txtRewardStatus = itemView.findViewById(R.id.txtMyRewardStatus);
            btnUseReward = itemView.findViewById(R.id.btnUseReward);
        }
    }
}