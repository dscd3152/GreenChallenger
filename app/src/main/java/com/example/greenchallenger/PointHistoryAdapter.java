package com.example.greenchallenger;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;

public class PointHistoryAdapter extends RecyclerView.Adapter<PointHistoryAdapter.PointHistoryViewHolder> {

    private final List<PointHistoryItem> items;

    public PointHistoryAdapter(List<PointHistoryItem> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public PointHistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_point_history, parent, false);
        return new PointHistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PointHistoryViewHolder holder, int position) {
        PointHistoryItem item = items.get(position);
        holder.txtHistoryTitle.setText(item.getTitle());
        holder.txtHistoryDetail.setText(item.getDetail());
        holder.txtHistoryDate.setText(item.getDateText());

        int points = item.getPoints();
        String sign = points >= 0 ? "+" : "";
        holder.txtHistoryPoints.setText(String.format(Locale.KOREA, "%s%dP", sign, points));
        holder.txtHistoryPoints.setTextColor(ContextCompat.getColor(
                holder.itemView.getContext(),
                points >= 0 ? R.color.green_primary : R.color.warning
        ));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class PointHistoryViewHolder extends RecyclerView.ViewHolder {
        TextView txtHistoryTitle;
        TextView txtHistoryDetail;
        TextView txtHistoryDate;
        TextView txtHistoryPoints;

        PointHistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            txtHistoryTitle = itemView.findViewById(R.id.txtHistoryTitle);
            txtHistoryDetail = itemView.findViewById(R.id.txtHistoryDetail);
            txtHistoryDate = itemView.findViewById(R.id.txtHistoryDate);
            txtHistoryPoints = itemView.findViewById(R.id.txtHistoryPoints);
        }
    }
}
