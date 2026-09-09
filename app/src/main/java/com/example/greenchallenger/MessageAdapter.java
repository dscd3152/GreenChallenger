package com.example.greenchallenger;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Map;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.ViewHolder> {
    private final List<Map<String, Object>> messages;
    private final String currentUid;

    public MessageAdapter(List<Map<String, Object>> messages, String currentUid) {
        this.messages = messages;
        this.currentUid = currentUid;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_message, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String, Object> message = messages.get(position);
        String senderUid = value(message.get("senderUid"));
        String senderNickname = value(message.get("senderNickname"));
        String text = value(message.get("text"));
        boolean mine = currentUid != null && currentUid.equals(senderUid);

        holder.layoutMyMessage.setVisibility(mine ? View.VISIBLE : View.GONE);
        holder.layoutOtherMessage.setVisibility(mine ? View.GONE : View.VISIBLE);

        if (mine) {
            holder.txtMyMessage.setText(text);
        } else {
            holder.txtOtherSender.setText(senderNickname.isEmpty() ? "Green Challenger" : senderNickname);
            holder.txtOtherMessage.setText(text);
            holder.imgOtherProfile.setImageResource(ProfileUi.getProfileImageRes(value(message.get("senderProfileImageUrl"))));
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    private static String value(Object raw) {
        return raw == null ? "" : String.valueOf(raw);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        LinearLayout layoutOtherMessage, layoutMyMessage;
        ImageView imgOtherProfile;
        TextView txtOtherSender, txtOtherMessage, txtMyMessage;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            layoutOtherMessage = itemView.findViewById(R.id.layoutOtherMessage);
            layoutMyMessage = itemView.findViewById(R.id.layoutMyMessage);
            imgOtherProfile = itemView.findViewById(R.id.imgOtherProfile);
            txtOtherSender = itemView.findViewById(R.id.txtOtherSender);
            txtOtherMessage = itemView.findViewById(R.id.txtOtherMessage);
            txtMyMessage = itemView.findViewById(R.id.txtMyMessage);
        }
    }
}
