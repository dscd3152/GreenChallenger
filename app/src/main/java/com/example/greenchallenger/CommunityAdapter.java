package com.example.greenchallenger;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class CommunityAdapter extends RecyclerView.Adapter<CommunityAdapter.ViewHolder> {

    public interface Listener {
        void onOpenPost(CommunityPost post);
    }

    private final List<CommunityPost> posts;
    private final Listener listener;

    public CommunityAdapter(List<CommunityPost> posts, Listener listener) {
        this.posts = posts;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_community_post, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CommunityPost post = posts.get(position);
        holder.txtCategory.setText(post.getCategory());
        holder.txtTitle.setText(post.getTitle());
        holder.txtContent.setText(post.getContent());
        holder.txtAuthor.setText(post.getAuthorNickname());
        holder.txtMeta.setText(post.getLocation() + " · " + post.getMeetingDate() + " · " +
                post.getParticipantCount() + "/" + post.getMaxParticipants() + "명");
        holder.itemView.setOnClickListener(v -> listener.onOpenPost(post));
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtCategory, txtTitle, txtContent, txtAuthor, txtMeta;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtCategory = itemView.findViewById(R.id.txtPostCategory);
            txtTitle = itemView.findViewById(R.id.txtPostTitle);
            txtContent = itemView.findViewById(R.id.txtPostContent);
            txtAuthor = itemView.findViewById(R.id.txtPostAuthor);
            txtMeta = itemView.findViewById(R.id.txtPostMeta);
        }
    }
}
