package com.example.appchat3n.Adapters;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appchat3n.Models.Group;
import com.example.appchat3n.R;
import com.example.appchat3n.databinding.ChatItemLayoutBinding;

import java.util.ArrayList;

public class GroupChatListAdapter extends RecyclerView.Adapter<GroupChatListAdapter.GroupChatListViewHolder> {

    Context context;
    ArrayList<Group> groupList;

    public GroupChatListAdapter(Context context, ArrayList<Group> groupList) {
        this.context = context;
        this.groupList = groupList;
    }

    @NonNull
    @Override
    public GroupChatListAdapter.GroupChatListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.chat_item_layout, parent, false);

        return new GroupChatListAdapter.GroupChatListViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GroupChatListAdapter.GroupChatListViewHolder holder, int position) {

    }

    @Override
    public int getItemCount() {
        return groupList.size();
    }

    public class GroupChatListViewHolder extends RecyclerView.ViewHolder {

        ChatItemLayoutBinding binding;
        public GroupChatListViewHolder(@NonNull View itemView) {
            super(itemView);
            binding = ChatItemLayoutBinding.bind(itemView);
        }
    }
}

//public class GroupChatAdapter extends RecyclerView.Adapter<GroupChatAdapter.UsersViewHolder> {
//
//    Context context;
//    ArrayList<User> originListUser;
//
//    public ChatListAdapter(Context context, ArrayList<User> users) {
//        this.context = context;
//        this.originListUser = users;
//    }
//
//    @NonNull
//    @Override
//    public UsersViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
//        View view = LayoutInflater.from(context).inflate(R.layout.chat_item_layout, parent, false);
//
//        return new UsersViewHolder(view);
//    }
//
//    @Override
//    public void onBindViewHolder(@NonNull UsersViewHolder holder, int position) {
//        User user = originListUser.get(position);
//        String senderId = FirebaseAuth.getInstance().getUid();
//
//        FirebaseDatabase.getInstance().getReference()
//                .child("chatLists")
//                .child(senderId)
//                .child(user.getUid())
//                .addListenerForSingleValueEvent(new ValueEventListener() {
//                    @Override
//                    public void onDataChange(@NonNull DataSnapshot snapshot) {
//                        if(snapshot.exists()) {
//                            String lastMsg = snapshot.child("lastMsg").getValue(String.class);
//                            long time = snapshot.child("lastMsgTime").getValue(Long.class);
//                            SimpleDateFormat dateFormat = new SimpleDateFormat("hh:mm a");
//                            holder.binding.msgTime.setText(dateFormat.format(new Date(time)));
//                            holder.binding.lastMsg.setText(Util.mySubString(lastMsg, 0, 30));
//                        } else {
//                            holder.binding.lastMsg.setText("Tap to chat");
//                        }
//                    }
//
//                    @Override
//                    public void onCancelled(@NonNull DatabaseError error) {}
//                });
//
//        holder.binding.username.setText(user.getName());
//
//        Glide.with(context).load(user.getProfileImage())
//                .placeholder(R.drawable.avatar)
//                .into(holder.binding.profile);
//
//        holder.itemView.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Intent intent = new Intent(context, ChatActivity.class);
//                intent.putExtra("name", user.getName());
//                intent.putExtra("image", user.getProfileImage());
//                intent.putExtra("uid", user.getUid());
//                intent.putExtra("token", user.getToken());
//                context.startActivity(intent);
//            }
//        });
//    }
//
//    @Override
//    public int getItemCount() {
//        return originListUser.size();
//    }
//
//    public class UsersViewHolder extends RecyclerView.ViewHolder {
//
//        ChatItemLayoutBinding binding;
//        public UsersViewHolder(@NonNull View itemView) {
//            super(itemView);
//            binding = ChatItemLayoutBinding.bind(itemView);
//        }
//    }
//}