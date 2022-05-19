package com.example.appchat3n.Activities;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.appchat3n.Fragments.GroupFragment;
import com.example.appchat3n.Fragments.MainFragment;
import com.example.appchat3n.R;
import com.example.appchat3n.Utils.Util;
import com.ismaeldivita.chipnavigation.ChipNavigationBar;


public class DashBoard extends AppCompatActivity {

    private ChipNavigationBar navigationBar;
    Fragment mainFragment = new MainFragment();
    Fragment groupFragment = new GroupFragment();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dash_board);
        navigationBar = findViewById(R.id.navigationChip);

        if (savedInstanceState == null) {
            navigationBar.setItemSelected(R.id.chat, true);
            replaceFragment(mainFragment);
        }

        navigationBar.setOnItemSelectedListener(new ChipNavigationBar.OnItemSelectedListener() {
            @Override
            public void onItemSelected(int i) {
                switch (i) {

                    case R.id.chat:
                        replaceFragment(mainFragment);
                        break;
                    case R.id.group:
                        replaceFragment(groupFragment);
                        break;
                    case R.id.profile:
//                        fragment = new ProfileFragment();
                }
            }
        });
    }

    @Override
    protected void onResume() {
        Util.updateOnlineStatus("Online");
        super.onResume();
    }

    @Override
    protected void onPause() {
        Util.updateOnlineStatus("Offline");
        super.onPause();
    }

    public void replaceFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out);
        fragmentTransaction.replace(R.id.dashboardContainer, fragment);
        fragmentTransaction.addToBackStack(fragment.toString());
        fragmentTransaction.commit();
    }
}