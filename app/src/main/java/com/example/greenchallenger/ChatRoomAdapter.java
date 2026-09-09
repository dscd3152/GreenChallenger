package com.example.greenchallenger;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Map;

public class ChatRoomAdapter extends RecyclerView.Adapter<ChatRoomAdapter.ViewHolder> {

    public interface Listener {
        void onOpenChat(Map<String, Object> chat);
    }

    private final List<Map<String, Object>> chats;
    private final Listener listener;

    public ChatRoomAdapter(List<Map<String, Object>> chats, Listener listener) {
        this.chats = chats;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_room, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String, Object> chat = chats.get(position);
        String type = String.valueOf(chat.get("type"));
        String title = String.valueOf(chat.get("title"));
        Object lastMessage = chat.get("lastMessage");

        holder.txtType.setText("private".equals(type) ? "개인" : "오픈");
        holder.txtTitle.setText(title == null || "null".equals(title) ? "채팅방" : title);
        holder.txtLastMessage.setText(lastMessage != null ? String.valueOf(lastMessage) : "아직 메시지가 없습니다.");
        holder.itemView.setOnClickListener(v -> listener.onOpenChat(chat));
    }

    @Override
    public int getItemCount() {
        return chats.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtType, txtTitle, txtLastMessage;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtType = itemView.findViewById(R.id.txtChatType);
            txtTitle = itemView.findViewById(R.id.txtChatRoomTitle);
            txtLastMessage = itemView.findViewById(R.id.txtChatLastMessage);
        }
    }
}
