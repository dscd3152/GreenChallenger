package com.example.greenchallenger;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class MissionAdapter extends RecyclerView.Adapter<MissionAdapter.ViewHolder> {

    public interface OnMissionClickListener {
        void onMissionClick(Mission mission);
    }

    private final List<Mission> missionList;
    private final OnMissionClickListener listener;

    public MissionAdapter(List<Mission> missionList, OnMissionClickListener listener) {
        this.missionList = missionList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_mission, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Mission mission = missionList.get(position);
        holder.missionTitle.setText(mission.getTitle());

        if (mission.isCompleted()) {
            holder.missionStatus.setText("완료");
        } else {
            holder.missionStatus.setText("");
        }

        holder.itemView.setOnClickListener(v -> listener.onMissionClick(mission));
    }

    @Override
    public int getItemCount() {
        return missionList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView missionTitle, missionStatus;

        public ViewHolder(View itemView) {
            super(itemView);
            missionTitle = itemView.findViewById(R.id.missionTitle);
            missionStatus = itemView.findViewById(R.id.missionStatus);
        }
    }
}
