package com.example.appchat3n.Activities;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.appchat3n.Adapters.PublicMessagesAdapter;
import com.example.appchat3n.Constants.AllConstants;
import com.example.appchat3n.Models.Message;
import com.example.appchat3n.databinding.ActivityPublicChatBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

public class PublicChatActivity extends AppCompatActivity {

    ActivityPublicChatBinding binding;

    PublicMessagesAdapter adapter;
    ArrayList<Message> messages;

    FirebaseDatabase database;
    FirebaseStorage storage;

    ProgressDialog dialog;

    String senderUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPublicChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        getSupportActionBar().setTitle("Public Chat");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        senderUid = FirebaseAuth.getInstance().getUid();
        database = FirebaseDatabase.getInstance();
        storage = FirebaseStorage.getInstance();

        dialog = new ProgressDialog(this);
        dialog.setMessage("Uploading image...");
        dialog.setCancelable(false);

        messages = new ArrayList<>();
        adapter = new PublicMessagesAdapter(this, messages);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.setAdapter(adapter);

        database.getReference().child("public")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        messages.clear();
                        for (DataSnapshot snapshot1 : snapshot.getChildren()) {
                            Message message = snapshot1.getValue(Message.class);
                            message.setMessageId(snapshot1.getKey());
                            messages.add(message);
                        }

                        adapter = new PublicMessagesAdapter(PublicChatActivity.this, messages);
                        LinearLayoutManager layoutManager = new LinearLayoutManager(PublicChatActivity.this);
                        layoutManager.setStackFromEnd(true);
                        binding.recyclerView.setLayoutManager(layoutManager);
                        binding.recyclerView.setAdapter(adapter);
//                        adapter.notifyDataSetChanged();
                        binding.recyclerView.scrollToPosition(adapter.getItemCount() - 1);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });

        binding.sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String messageTxt = binding.messageBox.getText().toString();

                Date date = new Date();
                Message message = new Message(messageTxt, senderUid, date.getTime());
                binding.messageBox.setText("");

                database.getReference().child("public")
                        .push()
                        .setValue(message);
            }
        });

        binding.attachment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                startActivityForResult(intent, AllConstants.REQUEST_GET_CONTENT);
            }
        });
        binding.camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
                {
                    requestPermissions(new String[]{Manifest.permission.CAMERA}, AllConstants.CAMERA_PERMISSION_CODE);
                }
                else
                {
                    callCameraIntent();
                }
            }
        });
    }


    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return super.onSupportNavigateUp();
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == AllConstants.CAMERA_PERMISSION_CODE)
        {
            if (grantResults.length>0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                Toast.makeText(this, "camera permission granted", Toast.LENGTH_LONG).show();
                callCameraIntent();
            }
            else
            {
                Toast.makeText(this, "camera permission denied", Toast.LENGTH_LONG).show();
            }
        }
    }
    private void callCameraIntent()
    {
        Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(cameraIntent, AllConstants.REQUEST_IMAGE_CAPTURE);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case AllConstants.REQUEST_GET_CONTENT:
                    if (data != null) {
                        if (data.getData() != null) {
                            Uri selectedImage = data.getData();
                            Calendar calendar = Calendar.getInstance();
                            StorageReference reference = storage.getReference().child("chatMedias").child(calendar.getTimeInMillis() + "");
                            dialog.show();
                            reference.putFile(selectedImage).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                                    dialog.dismiss();
                                    if (task.isSuccessful()) {
                                        reference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                            @Override
                                            public void onSuccess(Uri uri) {
                                                String filePath = uri.toString();

                                                String messageTxt = binding.messageBox.getText().toString();

                                                Date date = new Date();
                                                Message message = new Message(messageTxt, senderUid, date.getTime());
                                                message.setMessage("photo");
                                                message.setType("photo");
                                                message.setImageUrl(filePath);
                                                binding.messageBox.setText("");

                                                String randomKey = database.getReference().push().getKey();

                                                database.getReference().child("public")
                                                        .child(randomKey)
                                                        .setValue(message).addOnSuccessListener(new OnSuccessListener<Void>() {
                                                    @Override
                                                    public void onSuccess(Void unused) {

                                                    }
                                                });
                                            }
                                        });
                                    }
                                }
                            });
                        }
                    }
                    break;

                case AllConstants.REQUEST_IMAGE_CAPTURE:
                    if (data != null) {
                        Bitmap bmp = (Bitmap) data.getExtras().get("data");
                        ByteArrayOutputStream stream = new ByteArrayOutputStream();
                        bmp.compress(Bitmap.CompressFormat.PNG, 100, stream);
                        byte[] byteArray = stream.toByteArray();
                        bmp.recycle();

                        Calendar calendar = Calendar.getInstance();
                        StorageReference reference = storage.getReference().child("chatMedias").child(calendar.getTimeInMillis() + "");
                        dialog.show();
                        reference.putBytes(byteArray).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                                dialog.dismiss();
                                if (task.isSuccessful()) {
                                    reference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                        @Override
                                        public void onSuccess(Uri uri) {
                                            String filePath = uri.toString();

                                            String messageTxt = binding.messageBox.getText().toString();

                                            Date date = new Date();
                                            Message message = new Message(messageTxt, senderUid, date.getTime());
                                            message.setMessage("photo");
                                            message.setType("photo");
                                            message.setImageUrl(filePath);
                                            binding.messageBox.setText("");

                                            String randomKey = database.getReference().push().getKey();


                                            database.getReference().child("public")
                                                    .child(randomKey)
                                                    .setValue(message).addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void unused) {
                                                }
                                            });
                                        }
                                    });
                                }
                            }
                        });
                    }
                    break;
            }
        }
    }
}