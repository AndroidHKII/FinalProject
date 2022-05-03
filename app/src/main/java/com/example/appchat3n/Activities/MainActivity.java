package com.example.appchat3n.Activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.example.appchat3n.Adapters.TopStatusAdapter;
import com.example.appchat3n.Constant.KeyIntentConstant;
import com.example.appchat3n.Dtos.ChatDto;
import com.example.appchat3n.Dtos.SenderUserDto;
import com.example.appchat3n.Enums.FriendState;
import com.example.appchat3n.Models.Status;
import com.example.appchat3n.Models.UserStatus;
import com.example.appchat3n.R;
import com.example.appchat3n.Models.User;
import com.example.appchat3n.Adapters.UsersAdapter;
import com.example.appchat3n.databinding.ActivityMainBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;

public class MainActivity extends AppCompatActivity {

    CountDownLatch countDownListUser = new CountDownLatch(1);

    ActivityMainBinding binding;
    FirebaseDatabase database;
    UsersAdapter usersAdapter;
    TopStatusAdapter statusAdapter;

    ArrayList<User> users;

    ArrayList<User> listFriends;
    ArrayList<UserStatus> userStatuses;

    ProgressDialog dialog;

    User user;

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        FirebaseRemoteConfig mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(0)
                .build();
        mFirebaseRemoteConfig.setConfigSettingsAsync(configSettings);

        mFirebaseRemoteConfig.fetchAndActivate().addOnSuccessListener(new OnSuccessListener<Boolean>() {
            @Override
            public void onSuccess(Boolean aBoolean) {

                String backgroundImage = mFirebaseRemoteConfig.getString("backgroundImage");
                Glide.with(MainActivity.this)
                        .load(backgroundImage)
                        .into(binding.backgroundImage);

                /* Toolbar Color */
                String toolbarColor = mFirebaseRemoteConfig.getString("toolbarColor");
                String toolBarImage = mFirebaseRemoteConfig.getString("toolbarImage");
                boolean isToolBarImageEnabled = mFirebaseRemoteConfig.getBoolean("toolBarImageEnabled");



                if(isToolBarImageEnabled) {
                    Glide.with(MainActivity.this)
                            .load(toolBarImage)
                            .into(new CustomTarget<Drawable>() {
                                @Override
                                public void onResourceReady(@NonNull @NotNull Drawable resource, @Nullable @org.jetbrains.annotations.Nullable Transition<? super Drawable> transition) {
                                    getSupportActionBar()
                                            .setBackgroundDrawable(resource);
                                }

                                @Override
                                public void onLoadCleared(@Nullable @org.jetbrains.annotations.Nullable Drawable placeholder) {

                                }
                            });
                } else {
                    getSupportActionBar()
                            .setBackgroundDrawable
                                    (new ColorDrawable(Color.parseColor(toolbarColor)));
                }

            }
        });

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
                        //Toast.makeText(MainActivity.this, token, Toast.LENGTH_SHORT).show();
                    }
                });


        dialog = new ProgressDialog(this);
        dialog.setMessage("Uploading Image...");
        dialog.setCancelable(false);


        users = new ArrayList<>();

        userStatuses = new ArrayList<>();
        listFriends=new ArrayList<>();

        database.getReference().child("users").child(FirebaseAuth.getInstance().getUid())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        user = snapshot.getValue(User.class);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });


        usersAdapter = new UsersAdapter(this, listFriends);
        statusAdapter = new TopStatusAdapter(this, userStatuses);
//        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(RecyclerView.HORIZONTAL);
        binding.statusList.setLayoutManager(layoutManager);
        binding.statusList.setAdapter(statusAdapter);

        binding.recyclerView.setAdapter(usersAdapter);

        binding.recyclerView.showShimmerAdapter();
        binding.statusList.showShimmerAdapter();

        database.getReference().child("users").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                users.clear();
                for(DataSnapshot snapshot1 : snapshot.getChildren()) {
                    User user = snapshot1.getValue(User.class);
                    if(!user.getUid().equals(FirebaseAuth.getInstance().getUid()))
                        users.add(user);
                }
                binding.recyclerView.hideShimmerAdapter();
                filterFriend();
                usersAdapter.notifyDataSetChanged();
                statusAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });



        binding.bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.status:
                        Intent intent = new Intent();
                        intent.setType("image/*");
                        intent.setAction(Intent.ACTION_GET_CONTENT);
                        startActivityForResult(intent, 75);
                        break;
                }
                return false;
            }
        });
        //users.get(users.size()-1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(data != null) {
            if(data.getData() != null) {
                dialog.show();
                FirebaseStorage storage = FirebaseStorage.getInstance();
                Date date = new Date();
                StorageReference reference = storage.getReference().child("status").child(date.getTime() + "");

                reference.putFile(data.getData()).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                        if(task.isSuccessful()) {
                            reference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {
                                    UserStatus userStatus = new UserStatus();
                                    userStatus.setName(user.getName());
                                    userStatus.setProfileImage(user.getProfileImage());
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

                                    database.getReference().child("stories")
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
    }

    @Override
    protected void onResume() {
        super.onResume();
        String currentId = FirebaseAuth.getInstance().getUid();
        database.getReference().child("presence").child(currentId).setValue("Online");
    }

    @Override
    protected void onPause() {
        super.onPause();
        String currentId = FirebaseAuth.getInstance().getUid();
        database.getReference().child("presence").child(currentId).setValue("Offline");
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.group:
                startActivity(new Intent(MainActivity.this, GroupChatActivity.class));
                break;
            case R.id.addfriend:
                Intent intent=new Intent(MainActivity.this,FriendActivity.class);
                intent.putExtra(KeyIntentConstant.keyListUser,users);
                startActivity(intent);
                Toast.makeText(this, "Add friend clicked.", Toast.LENGTH_SHORT).show();
                break;
            case R.id.settings:
                Toast.makeText(this, "Settings Clicked.", Toast.LENGTH_SHORT).show();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.topmenu, menu);
        return super.onCreateOptionsMenu(menu);
    }
    private void filterFriend()
    {
        database.getReference().child("friends").child(FirebaseAuth.getInstance().getUid()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if(users.size()>0)
                {
                    listFriends.clear();
                    ArrayList<String> listIdFriend=new ArrayList<>();
                    for (DataSnapshot snapshotTemp : snapshot.getChildren()) {
                        if (snapshotTemp.getValue().equals(FriendState.FRIEND.name())) {
                            listIdFriend.add(snapshotTemp.getKey());
                        }
                    }
                    for(int i=0,end=users.size();i<end;i++)
                    {
                        if(listIdFriend.contains(users.get(i).getUid()))
                            listFriends.add(users.get(i));
                    }
                    showStoriesOfFriend(listIdFriend);
                    sortUsers();
                }
                else;       //no action
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
    private void showStoriesOfFriend(ArrayList<String> listIDFriend)
    {
        database.getReference().child("stories").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()) {
                    userStatuses.clear();

                    for(DataSnapshot storySnapshot : snapshot.getChildren()) {
                        if(listIDFriend.contains(storySnapshot.getKey())) {
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
                    binding.statusList.hideShimmerAdapter();
                    statusAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
    private void sortUsers()
    {
        database.getReference().child("chats").addValueEventListener(new ValueEventListener() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(listFriends.size()>0) {
                    List<SenderUserDto> senderUsers = new ArrayList<>();
                    for(int i=0,end=listFriends.size();i<end;i++)
                    {
                        senderUsers.add(new SenderUserDto(listFriends.get(i),Long.MAX_VALUE));
                    }
                    for (DataSnapshot snapshotTemp : snapshot.getChildren()) {
                        if (snapshotTemp.getKey().startsWith(FirebaseAuth.getInstance().getUid())) {
                            ChatDto temp = snapshotTemp.getValue(ChatDto.class);
                            Optional<User> tempUser = listFriends.stream().filter(e -> snapshotTemp.getKey().endsWith(e.getUid())).findFirst();
                            if (tempUser.isPresent()) {
                                senderUsers.remove(senderUsers.stream().filter(e->e.getUser().getUid().equals(tempUser.get().getUid())).findFirst().get());
                                senderUsers.add(new SenderUserDto(tempUser.get(), temp.getLastMsgTime()));
                            }
                            else ;       //no action

                        }
                    }
                    senderUsers.sort(Comparator.comparingLong(SenderUserDto::getLastMessageTime).reversed());
                    listFriends.clear();
                    for (int i=0,end=senderUsers.size();i<end;i++)
                    {
                        listFriends.add(senderUsers.get(i).getUser());
                    }
                    binding.recyclerView.hideShimmerAdapter();
                    usersAdapter.notifyDataSetChanged();

                }
                else; //no action
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }
}