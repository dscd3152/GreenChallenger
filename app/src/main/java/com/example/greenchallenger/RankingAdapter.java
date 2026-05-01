package com.example.greenchallenger;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class RankingAdapter extends RecyclerView.Adapter<RankingAdapter.ViewHolder> {

    private final List<User> users;

    public RankingAdapter(List<User> users) {
        this.users = users;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ranking, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = users.get(position);

        // 포지션 (0기준) → 실제 랭킹 번호 + 1
        holder.txtRank.setText((position + 1) + "위");
        holder.txtName.setText(user.getNickname());

        // 🔥 ecoPoints 표시 (출석 대신 포인트 표시)
        holder.txtPoints.setText(user.getEcoPoints() + "P");

        // 🏆 메달 + 성장 시스템
        if (position == 0) {
            holder.iconMedal.setImageResource(R.drawable.ic_gold_medal);
        } else if (position == 1) {
            holder.iconMedal.setImageResource(R.drawable.ic_silver_medal);
        } else if (position == 2) {
            holder.iconMedal.setImageResource(R.drawable.ic_bronze_medal);
        } else {
            switch (user.getGrowthStage()) {
                case 1:
                    holder.iconMedal.setImageResource(R.drawable.tree_stage1);
                    break;
                case 2:
                    holder.iconMedal.setImageResource(R.drawable.tree_stage2);
                    break;
                case 3:
                default:
                    holder.iconMedal.setImageResource(R.drawable.tree_stage3);
                    break;
            }
        }
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        TextView txtRank, txtName, txtPoints;
        ImageView iconMedal;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            txtRank = itemView.findViewById(R.id.txtRank);
            txtName = itemView.findViewById(R.id.txtName);
            txtPoints = itemView.findViewById(R.id.txtPoints);
            iconMedal = itemView.findViewById(R.id.iconMedal);
        }
    }
}
