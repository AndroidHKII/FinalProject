package com.example.appchat3n.Adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.appchat3n.Activities.ChatActivity;
import com.example.appchat3n.R;
import com.example.appchat3n.Models.User;
import com.example.appchat3n.databinding.RowConversationBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class UsersAdapter extends RecyclerView.Adapter<UsersAdapter.UsersViewHolder> implements Filterable {

    Context context;
    ArrayList<User> originListUser;
    ArrayList<User> listUserSearch;

    public UsersAdapter(Context context, ArrayList<User> users) {
        this.context = context;
        this.originListUser = users;
        this.listUserSearch=users;
    }

    @NonNull
    @Override
    public UsersViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.row_conversation, parent, false);

        return new UsersViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UsersViewHolder holder, int position) {
        User user = listUserSearch.get(position);
        String senderId = FirebaseAuth.getInstance().getUid();

        String senderRoom = senderId + user.getUid();

        FirebaseDatabase.getInstance().getReference()
                .child("chats")
                .child(senderRoom)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if(snapshot.exists()) {
                            String lastMsg = snapshot.child("lastMsg").getValue(String.class);
                            long time = snapshot.child("lastMsgTime").getValue(Long.class);
                            SimpleDateFormat dateFormat = new SimpleDateFormat("hh:mm a");
                            holder.binding.msgTime.setText(dateFormat.format(new Date(time)));
                            holder.binding.lastMsg.setText(lastMsg);
                        } else {
                            holder.binding.lastMsg.setText("Tap to chat");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });


        holder.binding.username.setText(user.getName());

        Glide.with(context).load(user.getProfileImage())
                .placeholder(R.drawable.avatar)
                .into(holder.binding.profile);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, ChatActivity.class);
                intent.putExtra("name", user.getName());
                intent.putExtra("image", user.getProfileImage());
                intent.putExtra("uid", user.getUid());
                intent.putExtra("token", user.getToken());
                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return listUserSearch.size();
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence charSequence) {
                String stringSearch=charSequence.toString();
                if(stringSearch.isEmpty()) {
                    listUserSearch = originListUser;
                }
                else
                {
                    ArrayList<User> searchFriends=new ArrayList<>();
                    for(User user: originListUser)
                    {
                        if(user.getName().toLowerCase().contains(stringSearch)||user.getPhoneNumber().contains(stringSearch))
                            searchFriends.add(user);
                    }
                    listUserSearch=searchFriends;
                }
                FilterResults filterResults=new FilterResults();
                filterResults.values=listUserSearch;
                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
                listUserSearch= (ArrayList<User>) filterResults.values;
                notifyDataSetChanged();
            }
        };
    }

    public class UsersViewHolder extends RecyclerView.ViewHolder {

        RowConversationBinding binding;

        public UsersViewHolder(@NonNull View itemView) {
            super(itemView);
            binding = RowConversationBinding.bind(itemView);
        }
    }

}
