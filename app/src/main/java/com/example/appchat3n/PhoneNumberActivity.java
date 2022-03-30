package com.example.appchat3n;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.example.appchat3n.databinding.ActivityPhoneNumberBinding;

public class PhoneNumberActivity extends AppCompatActivity {

    ActivityPhoneNumberBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone_number);

        binding = ActivityPhoneNumberBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.continueBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(PhoneNumberActivity.this, OTPActivity.class);
                intent.putExtra("phoneNumber", binding.phoneBox.getText().toString());
                startActivity(intent);
            }
        });
    }
}