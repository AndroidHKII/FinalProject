package com.example.appchat3n.Adapters;

import android.os.Build;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.appchat3n.Enums.FriendState;
import com.example.appchat3n.Models.User;
import com.example.appchat3n.R;
import com.example.appchat3n.databinding.ItemFriendBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import android.content.Context;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Objects;

public class FriendsAdapter extends RecyclerView.Adapter<FriendsAdapter.FriendViewHolder> implements Filterable {
    Context context;
    ArrayList<User> friends;
    ArrayList<User> listFriendOrigin;
    User myUser;

    public FriendsAdapter(Context context, ArrayList<User> friends) {
        this.context = context;
        this.friends = friends;
        this.listFriendOrigin=friends;
    }

    @NonNull
    @Override
    public FriendViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_friend,parent,false);
        return new FriendViewHolder(view);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onBindViewHolder(@NonNull FriendViewHolder holder, int position) {
        User friend=friends.get(position);

        if(Objects.isNull(friend))
            return;
        //Get FriendState
        FirebaseDatabase.getInstance().getReference()
                .child("friends")
                .child(FirebaseAuth.getInstance().getUid())
                .child(friend.getUid())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        //Disable deny button
                        holder.binding.deny.setVisibility(View.GONE);
                        if(snapshot.exists())
                        {
                            //Lưu ý: Hàm onComplete bị ghi đè sau khi thực hiện xong event, nên ta đổi ngược lại các thông báo(Toast) trong hàm onComplete
                            String friendState = snapshot.getValue(String.class);
                            if(friendState.equals(FriendState.FRIEND.name())) {
                                holder.binding.addRemove.setText("Unfriend");
                                holder.binding.addRemove.setBackgroundColor(context.getResources().getColor(R.color.semiRed));
                                holder.binding.addRemove.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                            removeFriend(friend);
                                        }

                                    }
                                );
                            }
                            else        //friendState='WAIT'
                            {
                                if(friendState.equals(FriendState.WAIT.name())) {
                                    holder.binding.addRemove.setText("CANCEL");
                                    holder.binding.addRemove.setBackgroundColor(context.getResources().getColor(R.color.brown));
                                    holder.binding.addRemove.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View view) {
                                                removeRequestFriend(friend,FriendState.WAIT);
                                            }
                                        }
                                    );
                                }
                                else    //friendState==Request
                                {
                                    holder.binding.addRemove.setText("ACCEPT");
                                    holder.binding.addRemove.setBackgroundColor(context.getResources().getColor(R.color.teal_200));
                                    holder.binding.addRemove.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View view) {
                                                addFriend(friend);
                                            }
                                        }
                                    );
                                    //Set up deny button
                                    holder.binding.deny.setVisibility(View.VISIBLE);
                                    holder.binding.deny.setText("DENY");
                                    holder.binding.deny.setBackgroundColor(context.getResources().getColor(R.color.red));
                                    holder.binding.deny.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View view) {
                                                removeRequestFriend(friend,FriendState.REQUEST);
                                            }
                                        }
                                    );
                                }
                            }
                        }
                        else {
                            holder.binding.addRemove.setText("ADD");
                            holder.binding.addRemove.setBackgroundColor(context.getResources().getColor(R.color.green));
                            holder.binding.addRemove.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        requestFriend(friend);
                                    }
                                }
                            );
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });

        //Set image
        Glide.with(context)                                         //truyền context của chúng ta vào phương thức with().
                .load(friend.getProfileImage())                     //nguồn hình ảnh được chỉ định như là một đường dẫn thư mục, một URI hay một URL.
                .placeholder(R.drawable.avatar)                     //một id tài nguyên ứng dụng cục bộ, thường được gọi là drawable, đó sẽ là một placeholder (hình giữ chỗ) cho đến khi hình ảnh được tải và hiển thị.
                .into(holder.binding.image);                        //ImageVeiw đích mà hình ảnh sẽ được đặt ở đó.
        //Set username and phone
        holder.binding.username.setText(friend.getName());
        holder.binding.phone.setText(friend.getPhoneNumber());
    }
    public void requestFriend(User friend)
    {
        FirebaseDatabase.getInstance().getReference()
                .child("friends").child(FirebaseAuth.getInstance().getUid()).child(friend.getUid()).setValue(FriendState.WAIT.name(), new DatabaseReference.CompletionListener() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onComplete(@Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                if(Objects.isNull(error)) {
                    FirebaseDatabase.getInstance().getReference()
                            .child("friends").child(friend.getUid()).child(FirebaseAuth.getInstance().getUid()).setValue(FriendState.REQUEST.name(), new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(@Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                            if(Objects.isNull(error))
                                Toast.makeText(context, "Send friend request to " + friend.getName() + " successfully!", Toast.LENGTH_LONG).show();
                            else {
                                Log.e("Request friend error: ",error.getMessage());
                                Log.e("Restore: ","Remove request friend from current User");
                                FirebaseDatabase.getInstance().getReference()
                                        .child("friends").child(friend.getUid()).child(FirebaseAuth.getInstance().getUid()).removeValue();
                                Toast.makeText(context, "Send friend request to " + friend.getName() + " unsuccessfully!", Toast.LENGTH_LONG).show();
                            }
                        }
                    });
                }
                else
                {
                    Log.e("Request friend error: ",error.getMessage());
                    Toast.makeText(context, "Send friend request to " + friend.getName() + " unsuccessfully!", Toast.LENGTH_LONG).show();
                }
            }
        });
    }
    public void removeRequestFriend(User friend, FriendState originState)
    {
        FirebaseDatabase.getInstance().getReference()
                .child("friends").child(FirebaseAuth.getInstance().getUid()).child(friend.getUid()).removeValue(new DatabaseReference.CompletionListener() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onComplete(@Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                if(Objects.isNull(error)) {
                    FirebaseDatabase.getInstance().getReference()
                            .child("friends").child(friend.getUid()).child(FirebaseAuth.getInstance().getUid()).removeValue( new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(@Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                            if(Objects.isNull(error)) {
                                if (originState == FriendState.WAIT)
                                    Toast.makeText(context, "Remove friend request from " + friend.getName() + " successfully!", Toast.LENGTH_LONG).show();
                                else
                                    Toast.makeText(context, "Deny friend request of " + friend.getName() + " successfully!", Toast.LENGTH_LONG).show();
                            }
                            else {
                                Log.e("Remove friend req err: ",error.getMessage());
                                Log.e("Restore: ","Add removing friend request to current User");
                                FirebaseDatabase.getInstance().getReference()
                                        .child("friends").child(friend.getUid()).child(FirebaseAuth.getInstance().getUid()).setValue(originState.name());
                                if(originState==FriendState.WAIT)
                                    Toast.makeText(context, "Remove friend request from " + friend.getName() + " unsuccessfully!", Toast.LENGTH_LONG).show();
                                else
                                    Toast.makeText(context, "Deny friend request of " + friend.getName() + " unsuccessfully!", Toast.LENGTH_LONG).show();
                            }
                        }
                    });
                }
                else
                {
                    Log.e("Remove friend req err: ",error.getMessage());
                    if(originState==FriendState.WAIT)
                        Toast.makeText(context, "Remove friend request from " + friend.getName() + " unsuccessfully!", Toast.LENGTH_LONG).show();
                    else
                        Toast.makeText(context, "Deny friend request of " + friend.getName() + " unsuccessfully!", Toast.LENGTH_LONG).show();
                }
            }
        });
    }
    public void addFriend(User friend)
    {
        FirebaseDatabase.getInstance().getReference()
                .child("friends").child(FirebaseAuth.getInstance().getUid()).child(friend.getUid()).setValue(FriendState.FRIEND, new DatabaseReference.CompletionListener() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onComplete(@Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                if(Objects.isNull(error)) {
                    FirebaseDatabase.getInstance().getReference()
                            .child("friends").child(friend.getUid()).child(FirebaseAuth.getInstance().getUid()).setValue(FriendState.FRIEND.name(), new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(@Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                            if(Objects.isNull(error))
                                Toast.makeText(context, "Add " + friend.getName() + " to friend successfully!", Toast.LENGTH_LONG).show();
                            else {
                                Log.e("Add friend error: ",error.getMessage());
                                Log.e("Restore: ","Remove adding friend from current User");
                                FirebaseDatabase.getInstance().getReference()
                                        .child("friends").child(friend.getUid()).child(FirebaseAuth.getInstance().getUid()).removeValue();
                                Toast.makeText(context, "Add " + friend.getName() + " to friend unsuccessfully!", Toast.LENGTH_LONG).show();
                            }
                        }
                    });
                }
                else
                {
                    Log.e("Add friend error: ",error.getMessage());
                    Toast.makeText(context, "Add " + friend.getName() + " to friend unsuccessfully!", Toast.LENGTH_LONG).show();
                }
            }
        });
    }
    public void removeFriend(User friend)
    {
        FirebaseDatabase.getInstance().getReference()
                .child("friends").child(FirebaseAuth.getInstance().getUid()).child(friend.getUid()).removeValue(new DatabaseReference.CompletionListener() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onComplete(@Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                if(Objects.isNull(error)) {
                    FirebaseDatabase.getInstance().getReference()
                            .child("friends").child(friend.getUid()).child(FirebaseAuth.getInstance().getUid()).removeValue( new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(@Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                            if(Objects.isNull(error))
                                Toast.makeText(context, "Unfriend " + friend.getName() + " successfully!", Toast.LENGTH_LONG).show();
                            else {
                                Log.e("Unfriend friend error: ",error.getMessage());
                                Log.e("Restore: ","Add removing friend to current User");
                                FirebaseDatabase.getInstance().getReference()
                                        .child("friends").child(friend.getUid()).child(FirebaseAuth.getInstance().getUid()).setValue(FriendState.FRIEND.name());
                                Toast.makeText(context, "Unfriend " + friend.getName() + " unsuccessfully!", Toast.LENGTH_LONG).show();
                            }
                        }
                    });
                }
                else
                {
                    Log.e("Remove friend error: ",error.getMessage());
                    Toast.makeText(context, "Unfriend " + friend.getName() + " unsuccessfully!", Toast.LENGTH_LONG).show();
                }
            }
        });
    }
    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public int getItemCount() {
        if(!Objects.isNull(friends))
            return friends.size();
        return 0;
    }

    class FriendViewHolder extends RecyclerView.ViewHolder
    {
        ItemFriendBinding binding;
        public FriendViewHolder(@NonNull View itemView) {
            super(itemView);
            binding= ItemFriendBinding.bind(itemView);
        }
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence charSequence) {
                String stringSearch=charSequence.toString();
                if(stringSearch.isEmpty()) {
                    friends = listFriendOrigin;
                }
                else
                {
                    ArrayList<User> searchFriends=new ArrayList<>();
                    for(User user: listFriendOrigin)
                    {
                        if(user.getName().toLowerCase().contains(stringSearch)||user.getPhoneNumber().contains(stringSearch))
                            searchFriends.add(user);
                    }
                    friends=searchFriends;
                }
                FilterResults filterResults=new FilterResults();
                filterResults.values=friends;
                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
                friends= (ArrayList<User>) filterResults.values;
                notifyDataSetChanged();
            }
        };
    }

}
