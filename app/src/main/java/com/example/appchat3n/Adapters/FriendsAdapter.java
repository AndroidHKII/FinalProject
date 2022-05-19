package com.example.appchat3n.Adapters;

import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.AuthFailureError;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.example.appchat3n.Enums.FriendState;
import com.example.appchat3n.Models.User;
import com.example.appchat3n.R;
import com.example.appchat3n.databinding.ItemFriendBinding;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class FriendsAdapter extends RecyclerView.Adapter<FriendsAdapter.FriendViewHolder> implements Filterable {
    Context context;
    ArrayList<User> friends;
    ArrayList<User> listFriendOrigin;
    User currentUser;
    DatabaseReference databaseReference;

    public FriendsAdapter(Context context, ArrayList<User> friends, User currentUser) {
        this.context = context;
        this.friends = friends;
        this.listFriendOrigin = friends;
        this.currentUser = currentUser;
        databaseReference = FirebaseDatabase.getInstance().getReference();
    }

    @NonNull
    @Override
    public FriendViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_friend, parent, false);
        return new FriendViewHolder(view);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onBindViewHolder(@NonNull FriendViewHolder holder, int position) {
        User friend = friends.get(position);

        if (Objects.isNull(friend))
            return;
        //Get FriendState
        databaseReference
                .child("friends")
                .child(currentUser.getUid())
                .child(friend.getUid())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        //Disable deny button
                        holder.binding.deny.setVisibility(View.GONE);
                        if (snapshot.exists()) {
                            String friendState = snapshot.getValue(String.class);
                            if (friendState.equals(FriendState.FRIEND.name())) {
                                holder.binding.addRemove.setText("UNFRIEND");
                                holder.binding.addRemove.setBackgroundColor(context.getResources().getColor(R.color.semiRed));
                                holder.binding.addRemove.setOnClickListener(new View.OnClickListener() {
                                                                                @Override
                                                                                public void onClick(View view) {
                                                                                    removeFriend(friend);
                                                                                }
                                                                            }
                                );
                            } else if (friendState.equals(FriendState.WAIT.name())) {
                                holder.binding.addRemove.setText("CANCEL");
                                holder.binding.addRemove.setBackgroundColor(context.getResources().getColor(R.color.brown));
                                holder.binding.addRemove.setOnClickListener(new View.OnClickListener() {
                                                                                @Override
                                                                                public void onClick(View view) {
                                                                                    removeRequestFriend(friend,FriendState.WAIT);
                                                                                }
                                                                            }
                                );
                            } else {
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
                        } else {
                            holder.binding.addRemove.setText("ADD");
                            holder.binding.addRemove.setBackgroundColor(context.getResources().getColor(R.color.colorPrimary));
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
                    public void onCancelled(@NonNull DatabaseError error) {}
                });


        Glide.with(context)
                .load(friend.getProfileImage())
                .placeholder(R.drawable.avatar)
                .into(holder.binding.image);

        holder.binding.username.setText(friend.getName());
        holder.binding.phone.setText(friend.getPhoneNumber());
    }

    public void requestFriend(User friend)
    {
        databaseReference
                .child("friends").child(currentUser.getUid()).child(friend.getUid()).setValue(FriendState.WAIT.name(), new DatabaseReference.CompletionListener() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onComplete(@Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                if (Objects.isNull(error)) {
                    databaseReference
                            .child("friends").child(friend.getUid()).child(currentUser.getUid()).setValue(FriendState.REQUEST.name(), new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(@Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                            if (Objects.isNull(error)) {
                                sendNotification(currentUser.getName(), "send you a friend request!", friend.getToken());
                                Toast.makeText(context, "Send friend request to " + friend.getName() + " successfully!", Toast.LENGTH_LONG).show();
                            } else {
                                Log.e("Request friend error: ", error.getMessage());
                                Log.e("Restore: ","Remove request friend from current User");
                                databaseReference
                                        .child("friends").child(friend.getUid()).child(currentUser.getUid()).removeValue();
                                Toast.makeText(context, "Send friend request to " + friend.getName() + " unsuccessfully!", Toast.LENGTH_LONG).show();
                            }
                        }
                    });
                } else {
                    Log.e("Request friend error: ", error.getMessage());
                    Toast.makeText(context, "Send friend request to " + friend.getName() + " unsuccessfully!", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    public void removeRequestFriend(User friend, FriendState originState) {
        databaseReference
                .child("friends").child(currentUser.getUid()).child(friend.getUid()).removeValue(new DatabaseReference.CompletionListener() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onComplete(@Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                if (Objects.isNull(error)) {
                    databaseReference
                            .child("friends").child(friend.getUid()).child(currentUser.getUid()).removeValue(new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(@Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                            if(Objects.isNull(error)) {
                                if (originState == FriendState.WAIT)
                                    Toast.makeText(context, "Remove friend request from " + friend.getName() + " successfully!", Toast.LENGTH_LONG).show();
                                else {
                                    sendNotification(currentUser.getName(), "deny your friend request!",friend.getToken());
                                    Toast.makeText(context, "Deny friend request of " + friend.getName() + " successfully!", Toast.LENGTH_LONG).show();
                                }
                            } else {
                                Log.e("Remove friend req err: ",error.getMessage());
                                Log.e("Restore: ","Add removing friend request to current User");
                                databaseReference
                                        .child("friends").child(friend.getUid()).child(currentUser.getUid()).setValue(originState.name());
                                if (originState == FriendState.WAIT)
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
                    if (originState == FriendState.WAIT)
                        Toast.makeText(context, "Remove friend request from " + friend.getName() + " unsuccessfully!", Toast.LENGTH_LONG).show();
                    else
                        Toast.makeText(context, "Deny friend request of " + friend.getName() + " unsuccessfully!", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    public void addFriend(User friend) {
        databaseReference
                .child("friends").child(currentUser.getUid()).child(friend.getUid()).setValue(FriendState.FRIEND.name(), new DatabaseReference.CompletionListener() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onComplete(@Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                if (Objects.isNull(error)) {
                    databaseReference
                            .child("friends").child(friend.getUid()).child(currentUser.getUid()).setValue(FriendState.FRIEND.name(), new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(@Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                            if (Objects.isNull(error)) {
                                HashMap<String, Object> lastMsgObj = new HashMap<>();
                                lastMsgObj.put("lastMsg", "Tap to chat");
                                lastMsgObj.put("lastMsgTime", new Date().getTime());

                                databaseReference.child("chatLists").child(currentUser.getUid()).child(friend.getUid()).updateChildren(lastMsgObj);
                                databaseReference.child("chatLists").child(friend.getUid()).child(currentUser.getUid()).updateChildren(lastMsgObj);

                                sendNotification(currentUser.getName(),"accept your friend request", friend.getToken());
                                Toast.makeText(context, "Add " + friend.getName() + " to friend successfully!", Toast.LENGTH_LONG).show();
                            } else {
                                Log.e("Add friend error: ", error.getMessage());
                                Log.e("Restore: ", "Remove adding friend from current User");
                                databaseReference
                                        .child("friends").child(friend.getUid()).child(currentUser.getUid()).removeValue();
                                Toast.makeText(context, "Add " + friend.getName() + " to friend unsuccessfully!", Toast.LENGTH_LONG).show();
                            }
                        }
                    });
                } else {
                    Log.e("Add friend error: ", error.getMessage());
                    Toast.makeText(context, "Add " + friend.getName() + " to friend unsuccessfully!", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    public void removeFriend(User friend) {
        databaseReference.child("friends").child(currentUser.getUid()).child(friend.getUid()).removeValue();
        databaseReference.child("friends").child(friend.getUid()).child(currentUser.getUid()).removeValue();
        databaseReference.child("chatMessages").child(currentUser.getUid()).child(friend.getUid()).removeValue();
        databaseReference.child("chatMessages").child(friend.getUid()).child(currentUser.getUid()).removeValue();
        databaseReference.child("chatLists").child(currentUser.getUid()).child(friend.getUid()).removeValue();
        databaseReference.child("chatLists").child(friend.getUid()).child(currentUser.getUid()).removeValue();

        Toast.makeText(context, "Unfriend " + friend.getName() + " successfully!", Toast.LENGTH_LONG).show();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public int getItemCount() {
        if (!Objects.isNull(friends))
            return friends.size();
        return 0;
    }

    class FriendViewHolder extends RecyclerView.ViewHolder {

        ItemFriendBinding binding;
        public FriendViewHolder(@NonNull View itemView) {
            super(itemView);
            binding = ItemFriendBinding.bind(itemView);
        }
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence charSequence) {
                String stringSearch = charSequence.toString();
                if (stringSearch.isEmpty()) {
                    friends = listFriendOrigin;
                } else {
                    ArrayList<User> searchFriends = new ArrayList<>();
                    for (User user : listFriendOrigin) {
                        if (user.getName().toLowerCase().contains(stringSearch) || user.getPhoneNumber().contains(stringSearch))
                            searchFriends.add(user);
                    }
                    friends = searchFriends;
                }
                FilterResults filterResults = new FilterResults();
                filterResults.values = friends;
                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
                friends = (ArrayList<User>) filterResults.values;
                notifyDataSetChanged();
            }
        };
    }
    void sendNotification(String name, String action, String token) {
        try {
            RequestQueue queue = Volley.newRequestQueue(context);

            String url = "https://fcm.googleapis.com/fcm/send";

            JSONObject data = new JSONObject();
            data.put("title", name+" "+action);
            JSONObject notificationData = new JSONObject();
            notificationData.put("notification", data);
            notificationData.put("to",token);

            JsonObjectRequest request = new JsonObjectRequest(url, notificationData
                    , new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    // Toast.makeText(ChatActivity.this, "success", Toast.LENGTH_SHORT).show();
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Toast.makeText(context, error.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                }
            }) {
                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    HashMap<String, String> map = new HashMap<>();
                    String key = "Key=AAAAqcARJAE:APA91bHHYtVDUwlqcOrGOTJhvm3Oa8j0_uUr5yYkzo74EFJEut6cG0zGkuBi39udSbaWSkhhm0w2EqP9xYgqJL9xgUaerhm8uc_QqoJIfgNO5__Ad7HDczvbeoA7E3-0wkaOccUXKkzV";
                    map.put("Content-Type", "application/json");
                    map.put("Authorization", key);
                    return map;
                }
            };
            queue.add(request);
        } catch (Exception ex) {

        }
    }

}