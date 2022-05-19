package com.example.appchat3n.Fragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.appchat3n.Activities.FriendActivity;
import com.example.appchat3n.Adapters.ChatListAdapter;
import com.example.appchat3n.Adapters.TopStatusAdapter;
import com.example.appchat3n.Constants.AllConstants;
import com.example.appchat3n.Models.Status;
import com.example.appchat3n.Models.User;
import com.example.appchat3n.Models.UserStatus;
import com.example.appchat3n.R;
import com.example.appchat3n.Utils.Util;
import com.example.appchat3n.databinding.FragmentMainBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Objects;

public class MainFragment extends Fragment {

    FragmentMainBinding binding;
    FirebaseDatabase database;
    User currentUser = new User();
    ArrayList<User> users;
    ArrayList<User> listFriends;
    ArrayList<String> listFriendIds;
    ChatListAdapter chatListAdapter;
    TopStatusAdapter statusAdapter;
    ArrayList<UserStatus> userStatuses;
    ProgressDialog dialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentMainBinding.bind(inflater.inflate(R.layout.fragment_main, container, false));

        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle("ChatsApp");
        setHasOptionsMenu(true);

        binding.statusArea.setVisibility(View.GONE);

        database = FirebaseDatabase.getInstance();

        FirebaseMessaging.getInstance()
                .getToken()
                .addOnSuccessListener(new OnSuccessListener<String>() {
                    @Override
                    public void onSuccess(String token) {
                        HashMap<String, Object> map = new HashMap<>();
                        map.put("token", token);
                        database.getReference()
                                .child("users")
                                .child(FirebaseAuth.getInstance().getUid())
                                .updateChildren(map);
                    }
                });

        users = new ArrayList<>();
        listFriends = new ArrayList<>();
        listFriendIds = new ArrayList<>();
        userStatuses = new ArrayList<>();

        database.getReference().child("users").child(FirebaseAuth.getInstance().getUid())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        currentUser = snapshot.getValue(User.class);

                        Glide.with(getActivity()).load(currentUser.getProfileImage())
                                .placeholder(R.drawable.avatar)
                                .into(binding.imageAvatar);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });

        chatListAdapter = new ChatListAdapter(getActivity(), listFriends);
        statusAdapter = new TopStatusAdapter(getActivity(), userStatuses);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        layoutManager.setOrientation(RecyclerView.HORIZONTAL);
        binding.statusList.setLayoutManager(layoutManager);
        binding.statusList.setAdapter(statusAdapter);

        binding.recyclerView.setAdapter(chatListAdapter);

        binding.statusList.showShimmerAdapter();
        binding.recyclerView.showShimmerAdapter();

        ArrayList<String> listFriendIdsTemp = new ArrayList<>();
        database.getReference().child("chatLists").child(FirebaseAuth.getInstance().getUid()).orderByChild("lastMsgTime")
                .addValueEventListener(new ValueEventListener() {
                    @SuppressLint("NotifyDataSetChanged")
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        listFriendIdsTemp.clear();
                        for (DataSnapshot snapshotTemp : snapshot.getChildren()) {
                            listFriendIdsTemp.add(snapshotTemp.getKey());
                        }

                        Collections.reverse(listFriendIdsTemp);
                        if (!listFriendIds.equals(listFriendIdsTemp)) {
                            listFriendIds = new ArrayList<>(listFriendIdsTemp);

                            listFriends.clear();
                            if (listFriendIds.isEmpty())
                                Toast.makeText(getActivity(), "No Friends", Toast.LENGTH_SHORT).show();

                            database.getReference().child("users").addListenerForSingleValueEvent(new ValueEventListener() {
                                @RequiresApi(api = Build.VERSION_CODES.N)
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    users.clear();
                                    for (DataSnapshot snapshot1 : snapshot.getChildren()) {
                                        User user = snapshot1.getValue(User.class);
                                        if (!Objects.isNull(user.getUid()) && listFriendIds.contains(user.getUid())) {
                                            users.add(user);
                                        }
                                    }

                                    if (users.size() > 0) {
                                        for (String friendId : listFriendIds) {
                                            for (User user : users) {
                                                if (friendId.equals(user.getUid())) {
                                                    listFriends.add(user);
                                                }
                                            }
                                        }

                                        showStoriesOfFriend(listFriendIds);

                                        chatListAdapter.notifyDataSetChanged();

                                        binding.statusArea.setVisibility(View.VISIBLE);
                                        binding.statusArea.scheduleLayoutAnimation();
                                        binding.recyclerView.scheduleLayoutAnimation();
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {

                                }
                            });
                        }

                        chatListAdapter.notifyDataSetChanged();

                        binding.statusList.hideShimmerAdapter();
                        binding.recyclerView.hideShimmerAdapter();

                        binding.statusArea.scheduleLayoutAnimation();
                        binding.recyclerView.scheduleLayoutAnimation();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });

        binding.imageLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(intent, AllConstants.REQUEST_GET_CONTENT);
            }
        });

        return binding.getRoot();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case AllConstants.REQUEST_GET_CONTENT:
                    if (data != null) {
                        if (data.getData() != null) {
                            dialog = new ProgressDialog(getActivity());
                            dialog.setMessage("Uploading Image...");
                            dialog.setCancelable(false);
                            dialog.show();
                            FirebaseStorage storage = FirebaseStorage.getInstance();
                            Date date = new Date();
                            StorageReference reference =
                                    storage.getReference().child("stories").child(Util.getUID()).child(date.getTime() + "");

                            reference.putFile(data.getData()).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                                    if (task.isSuccessful()) {
                                        reference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                            @Override
                                            public void onSuccess(Uri uri) {
                                                UserStatus userStatus = new UserStatus();
                                                userStatus.setName(currentUser.getName());
                                                userStatus.setProfileImage(currentUser.getProfileImage());
                                                userStatus.setLastUpdated(date.getTime());

                                                HashMap<String, Object> obj = new HashMap<>();
                                                obj.put("name", userStatus.getName());
                                                obj.put("profileImage", userStatus.getProfileImage());
                                                obj.put("lastUpdated", userStatus.getLastUpdated());

                                                String imageUrl = uri.toString();
                                                Status status = new Status(imageUrl, userStatus.getLastUpdated());

                                                database.getReference()
                                                        .child("stories")
                                                        .child(FirebaseAuth.getInstance().getUid())
                                                        .updateChildren(obj);

                                                database.getReference()
                                                        .child("stories")
                                                        .child(FirebaseAuth.getInstance().getUid())
                                                        .child("statuses")
                                                        .push()
                                                        .setValue(status);

                                                dialog.dismiss();
                                            }
                                        });
                                    }
                                }
                            });
                        }
                    }
                    break;
            }
        }
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.topmenu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.addFriend:
                Intent intent = new Intent(getActivity(), FriendActivity.class);
                intent.putExtra("UserList", users);
                intent.putExtra("CurrentUser", currentUser);
                startActivity(intent);
                break;
            case R.id.logout:
//                FirebaseAuth.getInstance().signOut();
//                finish();
//                startActivity(new Intent(this, PhoneNumberActivity.class));
                break;
            case R.id.settings:
        }

        return super.onOptionsItemSelected(item);
    }

//    void showChatList() {
//        listFriends.clear();
//        if (listFriendIds.isEmpty()) {
//            Toast.makeText(getActivity(), "No Friends", Toast.LENGTH_SHORT).show();
//        } else {
//            database.getReference().child("users").addValueEventListener(new ValueEventListener() {
//                @RequiresApi(api = Build.VERSION_CODES.N)
//                @Override
//                public void onDataChange(@NonNull DataSnapshot snapshot) {
//                    users.clear();
//                    for (DataSnapshot snapshot1 : snapshot.getChildren()) {
//                        User user = snapshot1.getValue(User.class);
//                        if (!Objects.isNull(user.getUid()) && listFriendIds.contains(user.getUid())) {
//                            users.add(user);
//                        }
//                    }
//
//                    if (users.size() > 0) {
//                        for (String friendId : listFriendIds) {
//                            for (User user : users) {
//                                if (friendId.equals(user.getUid())) {
//                                    listFriends.add(user);
//                                }
//                            }
//                        }
////                        for (User user : users) {
////                            if (listFriendIds.contains(user.getUid()))
////                                listFriends.add(user);
////                        }
//                        showStoriesOfFriend(listFriendIds);
////                        sortUsers();
//                        usersAdapter.notifyDataSetChanged();
//                    }
//                }
//
//                @Override
//                public void onCancelled(@NonNull DatabaseError error) {
//
//                }
//            });
//        }
//    }

    void showStoriesOfFriend(ArrayList<String> listIDFriend) {
        database.getReference().child("stories").addValueEventListener(new ValueEventListener() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    userStatuses.clear();

                    for (DataSnapshot storySnapshot : snapshot.getChildren()) {
                        if (listIDFriend.contains(storySnapshot.getKey())) {
                            UserStatus status = new UserStatus();
                            status.setName(storySnapshot.child("name").getValue(String.class));
                            status.setProfileImage(storySnapshot.child("profileImage").getValue(String.class));
                            status.setLastUpdated(storySnapshot.child("lastUpdated").getValue(Long.class));

                            ArrayList<Status> statuses = new ArrayList<>();

                            for (DataSnapshot statusSnapshot : storySnapshot.child("statuses").getChildren()) {
                                Status sampleStatus = statusSnapshot.getValue(Status.class);
                                statuses.add(sampleStatus);
                            }

                            status.setStatuses(statuses);
                            userStatuses.add(status);
                        }
                    }

                    statusAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}