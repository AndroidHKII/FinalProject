package com.example.appchat3n.Activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;

import com.example.appchat3n.Adapters.FriendsAdapter;
import com.example.appchat3n.Constant.KeyIntentConstant;
import com.example.appchat3n.Models.User;
import com.example.appchat3n.databinding.ActivityFriendBinding;

import java.util.ArrayList;

public class FriendActivity extends AppCompatActivity {

    private ActivityFriendBinding binding;
    private FriendsAdapter friendsAdapter;
    private ArrayList<User> users;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding=ActivityFriendBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        //Get list users from intent
        Intent intent=getIntent();
        users=(ArrayList<User>) intent.getSerializableExtra(KeyIntentConstant.keyListUser);

        //Initial adapter and set adapter
        friendsAdapter=new FriendsAdapter(this,users);
        binding.recyclerViewFriends.setAdapter(friendsAdapter);

        //Add bottom decider
        RecyclerView.ItemDecoration itemDecoration=new DividerItemDecoration(this,DividerItemDecoration.VERTICAL);
        binding.recyclerViewFriends.addItemDecoration(itemDecoration);
    }
}