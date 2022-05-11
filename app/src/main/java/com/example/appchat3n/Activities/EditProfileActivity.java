package com.example.appchat3n.Activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

import com.example.appchat3n.R;
import com.google.android.gms.common.internal.service.Common;
import com.google.firebase.auth.FirebaseAuth;

public class EditProfileActivity extends AppCompatActivity {

    private static final String SHARED_PREFERENCE_USER_STATE = "USER";
    FirebaseAuth auth;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);
    }

    public void btnLogoutClick(View view) {

        FirebaseAuth.getInstance().signOut();
        finish();
        startActivity(new Intent(EditProfileActivity.this, PhoneNumberActivity.class));

    }
}