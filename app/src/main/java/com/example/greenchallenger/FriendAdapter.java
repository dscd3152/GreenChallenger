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
import java.util.Set;

public class FriendAdapter extends RecyclerView.Adapter<FriendAdapter.ViewHolder> {

    public static final int MODE_FRIENDS = 0;
    public static final int MODE_SEARCH = 1;
    public static final int MODE_REQUESTS = 2;

    public interface Listener {
        void onOpenProfile(User user);

        void onPrimaryAction(User user);
    }

    private final List<User> users;
    private final Set<String> friendIds;
    private final Set<String> outgoingRequestIds;
    private final String currentUid;
    private final int mode;
    private final Listener listener;

    public FriendAdapter(List<User> users, Set<String> friendIds, Set<String> outgoingRequestIds,
                         String currentUid, int mode, Listener listener) {
        this.users = users;
        this.friendIds = friendIds;
        this.outgoingRequestIds = outgoingRequestIds;
        this.currentUid = currentUid;
        this.mode = mode;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_friend, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = users.get(position);
        int stage = GrowthPolicy.getGrowthStage(user.getEcoPoints());
        boolean isSelf = currentUid != null && currentUid.equals(user.getUid());
        boolean alreadyFriend = user.getUid() != null && friendIds.contains(user.getUid());
        boolean requestSent = user.getUid() != null && outgoingRequestIds.contains(user.getUid());

        holder.imgProfile.setImageResource(ProfileUi.getProfileImageRes(user.getProfileImageUrl()));
        holder.txtName.setText(ProfileUi.nickname(user));
        holder.txtBio.setText(ProfileUi.bio(user));
        holder.txtMeta.setText(GrowthPolicy.getGrowthStageName(stage) + " 단계 · " + user.getEcoPoints() + "P");
        holder.txtInterests.setText(ProfileUi.interestsText(user));
        holder.itemView.setOnClickListener(v -> listener.onOpenProfile(user));

        holder.btnAction.setVisibility(View.VISIBLE);
        if (mode == MODE_REQUESTS) {
            holder.btnAction.setText("수락");
            holder.btnAction.setEnabled(true);
            holder.btnAction.setOnClickListener(v -> listener.onPrimaryAction(user));
        } else if (mode == MODE_SEARCH) {
            if (isSelf) {
                holder.btnAction.setText("나");
                holder.btnAction.setEnabled(false);
            } else if (alreadyFriend) {
                holder.btnAction.setText("친구");
                holder.btnAction.setEnabled(false);
            } else if (requestSent) {
                holder.btnAction.setText("대기");
                holder.btnAction.setEnabled(false);
            } else {
                holder.btnAction.setText("요청");
                holder.btnAction.setEnabled(true);
                holder.btnAction.setOnClickListener(v -> listener.onPrimaryAction(user));
            }
        } else {
            holder.btnAction.setText("채팅");
            holder.btnAction.setEnabled(true);
            holder.btnAction.setOnClickListener(v -> listener.onPrimaryAction(user));
        }
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgProfile;
        TextView txtName, txtBio, txtMeta, txtInterests;
        Button btnAction;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgProfile = itemView.findViewById(R.id.imgFriendProfile);
            txtName = itemView.findViewById(R.id.txtFriendName);
            txtBio = itemView.findViewById(R.id.txtFriendBio);
            txtMeta = itemView.findViewById(R.id.txtFriendMeta);
            txtInterests = itemView.findViewById(R.id.txtFriendInterests);
            btnAction = itemView.findViewById(R.id.btnFriendAction);
        }
    }
}
