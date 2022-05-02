package com.example.appchat3n.Adapters;

import android.annotation.SuppressLint;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.appchat3n.Enums.FriendState;
import com.example.appchat3n.Models.Friend;
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
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Objects;

public class FriendsAdapter extends RecyclerView.Adapter<FriendsAdapter.FriendViewHolder> {
    Context context;
    ArrayList<User> friends;

    public FriendsAdapter(Context context, ArrayList<User> friends) {
        this.context = context;
        this.friends = friends;
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
                        if(snapshot.exists())
                        {
                            //Lưu ý: Hàm onComplete bị ghi đè sau khi thực hiện xong event, nên ta đổi ngược lại các thông báo(Toast) trong hàm onComplete
                            String friendState = snapshot.child("friendState").getValue(String.class);
                            if(friendState.equals(FriendState.FRIEND.name())) {
                                holder.binding.addRemove.setText("Remove");
                                holder.binding.addRemove.setBackgroundColor(context.getResources().getColor(R.color.semiRed));
                                holder.binding.addRemove.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                            Friend newFriend= new Friend(friend,FriendState.NOT_FRIEND);
                                            FirebaseDatabase.getInstance().getReference()
                                                    .child("friends").child(FirebaseAuth.getInstance().getUid()).child(friend.getUid()).setValue(newFriend, new DatabaseReference.CompletionListener() {
                                                @Override
                                                public void onComplete(@Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                                                    Toast.makeText(context, "Remove "+friend.getName()+" successfully!", Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                        }
                                    }
                                );
                            }
                            else
                            {
                                holder.binding.addRemove.setText("Add");
                                holder.binding.addRemove.setBackgroundColor(context.getResources().getColor(R.color.green));
                                holder.binding.addRemove.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                            Friend newFriend= new Friend(friend,FriendState.FRIEND);
                                            FirebaseDatabase.getInstance().getReference()
                                                    .child("friends").child(FirebaseAuth.getInstance().getUid()).child(friend.getUid()).setValue(newFriend, new DatabaseReference.CompletionListener() {
                                                @Override
                                                public void onComplete(@Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                                                    Toast.makeText(context, "Add "+friend.getName()+" to friend successfully!", Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                        }
                                    }
                                );

                            }
                        } else {
                            holder.binding.addRemove.setText("Add");
                            holder.binding.addRemove.setBackgroundColor(context.getResources().getColor(R.color.green));
                            holder.binding.addRemove.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        Friend newFriend= new Friend(friend,FriendState.FRIEND);
                                        FirebaseDatabase.getInstance().getReference()
                                                .child("friends").child(FirebaseAuth.getInstance().getUid()).child(friend.getUid()).setValue(newFriend, new DatabaseReference.CompletionListener() {
                                            @Override
                                            public void onComplete(@Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                                                Toast.makeText(context, "Add "+friend.getName()+" to friend successfully!", Toast.LENGTH_SHORT).show();
                                            }
                                        });
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

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public int getItemCount() {
        if(!Objects.isNull(friends))
            return friends.size();
        return 0;
    }

    public class FriendViewHolder extends RecyclerView.ViewHolder
    {
        ItemFriendBinding binding;
        public FriendViewHolder(@NonNull View itemView) {
            super(itemView);
            binding= ItemFriendBinding.bind(itemView);
        }
    }

}
