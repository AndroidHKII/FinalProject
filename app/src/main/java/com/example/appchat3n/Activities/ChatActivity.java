package com.example.appchat3n.Activities;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.devlomi.record_view.OnRecordListener;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipDrawable;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.example.appchat3n.Adapters.MessagesAdapter;
import com.example.appchat3n.Constants.AllConstants;
import com.example.appchat3n.Models.Message;
import com.example.appchat3n.Models.User;
import com.example.appchat3n.R;
import com.example.appchat3n.databinding.ActivityChatBinding;
import com.google.mlkit.nl.smartreply.SmartReply;
import com.google.mlkit.nl.smartreply.SmartReplyGenerator;
import com.google.mlkit.nl.smartreply.SmartReplySuggestion;
import com.google.mlkit.nl.smartreply.SmartReplySuggestionResult;
import com.google.mlkit.nl.smartreply.TextMessage;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;

import com.example.appchat3n.Permissions;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {

    ActivityChatBinding binding;
    MessagesAdapter adapter;
    ArrayList<Message> messages;
    private Permissions permissions;
//    String senderRoom, receiverRoom;
    private MediaRecorder mediaRecorder;
    String name, profile, token,receiverPhoneNumber;
    FirebaseDatabase database;
    FirebaseStorage storage;

    ProgressDialog dialog;
    String senderUid, receiverUid, audioPath;
    List<TextMessage> conversation;
    EditText messageBox;
    ChipGroup cgSmartReplies;
    String messageTxt;
    Message lastMsg;

    User currentUser = new User();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());


        cgSmartReplies = findViewById(R.id.cgSmartReplies);
        messageBox = findViewById(R.id.messageBox);

        setSupportActionBar(binding.toolbar);

        database = FirebaseDatabase.getInstance();
        storage = FirebaseStorage.getInstance();

        dialog = new ProgressDialog(this);
        dialog.setMessage("Uploading image...");
        dialog.setCancelable(false);

        messages = new ArrayList<>();
        conversation = new ArrayList<>();

        permissions = new Permissions();
        name = getIntent().getStringExtra("name");
        profile = getIntent().getStringExtra("image");
        token = getIntent().getStringExtra("token");
        receiverPhoneNumber=getIntent().getStringExtra("phone");
//        Toast.makeText(this, token, Toast.LENGTH_SHORT).show();

        binding.name.setText(name);
        Glide.with(ChatActivity.this)
                .load(profile)
                .placeholder(R.drawable.avatar)
                .into(binding.profile);

        binding.imageView2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        receiverUid = getIntent().getStringExtra("uid");
        senderUid = FirebaseAuth.getInstance().getUid();

        database.getReference().child("presence").child(receiverUid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String status = snapshot.getValue(String.class);
                    if (!status.isEmpty()) {
                        if (status.equals("Offline")) {
                            binding.status.setVisibility(View.GONE);
                        } else {
                            binding.status.setText(status);
                            binding.status.setVisibility(View.VISIBLE);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        adapter = new MessagesAdapter(this, messages, senderUid, receiverUid);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        binding.recyclerView.setLayoutManager(layoutManager);
        binding.recyclerView.setAdapter(adapter);


        database.getReference().child("chatLists").child(receiverUid).child(senderUid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                HashMap<String,Object> map = new HashMap<>();
                map = (HashMap<String, Object>) snapshot.getValue();
                lastMsg = new Message();
                lastMsg.setMessage((String) map.get("lastMsg"));
                lastMsg.setSenderId((String) map.get("senderUid"));
                showSmartReply(lastMsg);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        database.getReference().child("chatMessages")
                .child(senderUid)
                .child(receiverUid)
                .addValueEventListener(new ValueEventListener() {
                    @SuppressLint("NotifyDataSetChanged")
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        messages.clear();
                        for (DataSnapshot snapshot1 : snapshot.getChildren()) {
                            Message message = snapshot1.getValue(Message.class);
                            message.setMessageId(snapshot1.getKey());
                            messages.add(message);
                        }

                        adapter = new MessagesAdapter(ChatActivity.this, messages, senderUid, receiverUid);
                        LinearLayoutManager layoutManager = new LinearLayoutManager(ChatActivity.this);
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

        database.getReference().child("users").child(senderUid)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        currentUser = snapshot.getValue(User.class);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });

        binding.sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String messageTxt = binding.messageBox.getText().toString();

                if (messageTxt.trim().equals(""))
                    return;

                Date date = new Date();
                Message message = new Message(messageTxt, senderUid, date.getTime(), "text");
                lastMsg = message;
                binding.messageBox.setText("");

                String randomKey = database.getReference().push().getKey();

                HashMap<String, Object> lastMsgObj = new HashMap<>();
                lastMsgObj.put("lastMsg", message.getMessage());
                lastMsgObj.put("lastMsgTime", date.getTime());
                lastMsgObj.put("senderUid", senderUid);

                conversation.add(TextMessage.createForLocalUser(message.getMessage(),System.currentTimeMillis()));

                database.getReference().child("chatLists").child(senderUid).child(receiverUid).updateChildren(lastMsgObj);
                database.getReference().child("chatLists").child(receiverUid).child(senderUid).updateChildren(lastMsgObj);

                database.getReference().child("chatMessages")
                        .child(senderUid)
                        .child(receiverUid)
                        .child(randomKey)
                        .setValue(message).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        database.getReference().child("chatMessages")
                                .child(receiverUid)
                                .child(senderUid)
                                .child(randomKey)
                                .setValue(message).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void unused) {
                                sendNotification(name, message.getMessage(), token);
                               // showSmartReply(message);
                            }
                        });
                    }
                });
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

        final Handler handler = new Handler();
        binding.messageBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                binding.sendBtn.setVisibility(View.VISIBLE);
                binding.recordButton.setVisibility(View.GONE);

                if(binding.messageBox.getText().toString().trim().equals("")) {
                   binding.sendBtn.setVisibility(View.GONE);
                    binding.recordButton.setVisibility(View.VISIBLE);
                }
                database.getReference().child("presence").child(senderUid).setValue("Typing...");
                handler.removeCallbacks(null);
                handler.postDelayed(userStopTyping, 1000);
            }

            Runnable userStopTyping = new Runnable() {
                @Override
                public void run() {
                    database.getReference().child("presence").child(senderUid).setValue("Online");
                }
            };
        });

        binding.info.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ChatActivity.this, PersonalChatDetail.class);
                intent.putExtra("name", name);
                intent.putExtra("image", profile);
                startActivity(intent);
            }
        });

        getSupportActionBar().setDisplayShowTitleEnabled(false);

        initView();
    }

    void sendNotification(String name, String message, String token) {
        try {
            RequestQueue queue = Volley.newRequestQueue(this);

            JSONObject data = new JSONObject();
            data.put("title", currentUser.getName());
            data.put("body", message);
            JSONObject notificationData = new JSONObject();
            notificationData.put("notification", data);
            notificationData.put("to", token);

            JsonObjectRequest request = new JsonObjectRequest(AllConstants.NOTIFICATION_URL, notificationData,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
//                            Toast.makeText(ChatActivity.this, "Success", Toast.LENGTH_SHORT).show();
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Toast.makeText(ChatActivity.this, error.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }) {
                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    HashMap<String, String> map = new HashMap<>();
                    map.put("Authorization", "key=" + AllConstants.SERVER_KEY);
                    map.put("Content-Type", "application/json");

                    return map;
                }
            };

            queue.add(request);
        } catch (Exception ex) {

        }
    }

    private void showSmartReply(Message messages) {
        conversation.clear();
        cgSmartReplies.removeAllViews();
        binding.llSmartReplies.setVisibility(View.GONE);
        conversation.add(TextMessage.createForRemoteUser(messages.getMessage(),System.currentTimeMillis(), senderUid));
       if(messages.getSenderId().equals(receiverUid)) {
           if (!conversation.isEmpty()) {
               binding.llSmartReplies.setVisibility(View.VISIBLE);
               SmartReplyGenerator smartReply = SmartReply.getClient();
               smartReply.suggestReplies(conversation).addOnSuccessListener(new OnSuccessListener<SmartReplySuggestionResult>() {
                   @Override
                   public void onSuccess(SmartReplySuggestionResult result) {
                       if (result.getStatus() == SmartReplySuggestionResult.STATUS_NOT_SUPPORTED_LANGUAGE) {
                           //Toast.makeText(ChatActivity.this, "Language not support", Toast.LENGTH_SHORT).show();
                       } else if (result.getStatus() == SmartReplySuggestionResult.STATUS_SUCCESS) {
                           for (SmartReplySuggestion suggestion : result.getSuggestions()) {
                               String replyText = suggestion.getText();
                               Chip chip = new Chip(ChatActivity.this);
                               ChipDrawable drawable = ChipDrawable.createFromAttributes(ChatActivity.this,
                                       null, 0, com.google.android.material.R.style.Widget_MaterialComponents_Chip_Action);
                               chip.setChipDrawable(drawable);
                               LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                       LinearLayout.LayoutParams.WRAP_CONTENT,
                                       LinearLayout.LayoutParams.WRAP_CONTENT
                               );
                               params.setMargins(16, 16, 16, 16);
                               chip.setLayoutParams(params);
                               chip.setText(replyText);
                               chip.setTag(replyText);


                               chip.setOnClickListener(new View.OnClickListener() {
                                   @Override
                                   public void onClick(View view) {
                                       messageBox.setText(view.getTag().toString());
                                       Toast.makeText(ChatActivity.this, view.getTag().toString(), Toast.LENGTH_SHORT).show();
                                   }
                               });

                               cgSmartReplies.addView(chip);
                           }
                       }
                   }
               });
           }
       }
    }


        private void initView() {

            binding.recordButton.setRecordView(binding.recordView);
            binding.recordButton.setListenForRecord(false);

            binding.recordButton.setOnClickListener(view -> {

                if (permissions.isRecordingOk(ChatActivity.this))
                    if (permissions.isStorageOk(ChatActivity.this))
                        binding.recordButton.setListenForRecord(true);
                    else permissions.requestStorage(ChatActivity.this);
                else permissions.requestRecording(ChatActivity.this);


            });

            binding.recordView.setOnRecordListener(new OnRecordListener() {
                @Override
                public void onStart() {
                    //Start Recording..
                    Log.d("RecordView", "onStart");

                    setUpRecording();

                    try {
                        mediaRecorder.prepare();
                        mediaRecorder.start();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    binding.messageBox.setVisibility(View.GONE);
                    binding.recordView.setVisibility(View.VISIBLE);

                }

                @Override
                public void onCancel() {
                    //On Swipe To Cancel
                    Log.d("RecordView", "onCancel");

                    mediaRecorder.reset();
                    mediaRecorder.release();
                    File file = new File(audioPath);
                    if (file.exists())
                        file.delete();

                    binding.recordView.setVisibility(View.GONE);
                    binding.messageBox.setVisibility(View.VISIBLE);


                }

                @Override
                public void onFinish(long recordTime) {
                    //Stop Recording..
                    Log.d("RecordView", "onFinish");

                    try {
                        mediaRecorder.stop();
                        mediaRecorder.release();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }


                    binding.recordView.setVisibility(View.GONE);
                    binding.messageBox.setVisibility(View.VISIBLE);

                    sendRecodingMessage(audioPath);


                }

                @Override
                public void onLessThanSecond() {
                    //When the record time is less than One Second
                    Log.d("RecordView", "onLessThanSecond");

                    mediaRecorder.reset();
                    mediaRecorder.release();

                    File file = new File(audioPath);
                    if (file.exists())
                        file.delete();


                    binding.recordView.setVisibility(View.GONE);
                    //binding.dataLayout.setVisibility(View.VISIBLE);
                }
            });
        }

        private void setUpRecording() {

            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

            File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), "ChatsApp/Media/Recording");

            if (!file.exists())
                file.mkdirs();
            audioPath = file.getAbsolutePath() + File.separator + System.currentTimeMillis() + ".3gp";

            mediaRecorder.setOutputFile(audioPath);
        }

        private void sendRecodingMessage(String audioPath) {


                StorageReference storageReference = FirebaseStorage.getInstance().getReference(  "Media/Recording/" + senderUid + "/" + System.currentTimeMillis());
                Uri audioFile = Uri.fromFile(new File(audioPath));
                storageReference.putFile(audioFile).addOnSuccessListener(success -> {
                    Task<Uri> audioUrl = success.getStorage().getDownloadUrl();

                    audioUrl.addOnCompleteListener(path -> {
                        if (path.isSuccessful()) {

                            String url = path.getResult().toString();


                            Date date = new Date();
                            Message message = new Message(url, senderUid, date.getTime(), "recording");

                            String randomKey = database.getReference().push().getKey();

                            HashMap<String, Object> lastMsgObj = new HashMap<>();
                            lastMsgObj.put("lastMsg", "audio");
                            lastMsgObj.put("senderUid", senderUid);

                            lastMsgObj.put("lastMsgTime", date.getTime());

                            database.getReference().child("chatLists").child(senderUid).child(receiverUid).updateChildren(lastMsgObj);
                            database.getReference().child("chatLists").child(receiverUid).child(senderUid).updateChildren(lastMsgObj);

                            database.getReference().child("chatMessages")
                                    .child(senderUid)
                                    .child(receiverUid)
                                    .child(randomKey)
                                    .setValue(message).addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void unused) {
                                    database.getReference().child("chatMessages")
                                            .child(receiverUid)
                                            .child(senderUid)
                                            .child(randomKey)
                                            .setValue(message).addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void unused) {
                                            sendNotification(name, "audio", token);
                                        }
                                    });
                                }
                            });

                        }
                    });
                });
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

                                                HashMap<String, Object> lastMsgObj = new HashMap<>();
                                                lastMsgObj.put("lastMsg", message.getMessage());
                                                lastMsgObj.put("lastMsgTime", date.getTime());
                                                lastMsgObj.put("senderUid", senderUid);


                                                database.getReference().child("chatLists").child(senderUid).child(receiverUid).updateChildren(lastMsgObj);
                                                database.getReference().child("chatLists").child(receiverUid).child(senderUid).updateChildren(lastMsgObj);

                                                database.getReference().child("chatMessages")
                                                        .child(senderUid)
                                                        .child(receiverUid)
                                                        .child(randomKey)
                                                        .setValue(message).addOnSuccessListener(new OnSuccessListener<Void>() {
                                                    @Override
                                                    public void onSuccess(Void unused) {
                                                        database.getReference().child("chatMessages")
                                                                .child(receiverUid)
                                                                .child(senderUid)
                                                                .child(randomKey)
                                                                .setValue(message).addOnSuccessListener(new OnSuccessListener<Void>() {
                                                            @Override
                                                            public void onSuccess(Void unused) {

                                                            }
                                                        });
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

                                            HashMap<String, Object> lastMsgObj = new HashMap<>();
                                            lastMsgObj.put("lastMsg", message.getMessage());
                                            lastMsgObj.put("lastMsgTime", date.getTime());
                                            lastMsgObj.put("senderUid", senderUid);
                                            database.getReference().child("chatLists").child(senderUid).child(receiverUid).updateChildren(lastMsgObj);
                                            database.getReference().child("chatLists").child(receiverUid).child(senderUid).updateChildren(lastMsgObj);

                                            database.getReference().child("chatMessages")
                                                    .child(senderUid)
                                                    .child(receiverUid)
                                                    .child(randomKey)
                                                    .setValue(message).addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void unused) {
                                                    database.getReference().child("chatMessages")
                                                            .child(receiverUid)
                                                            .child(senderUid)
                                                            .child(randomKey)
                                                            .setValue(message).addOnSuccessListener(new OnSuccessListener<Void>() {
                                                        @Override
                                                        public void onSuccess(Void unused) {

                                                        }
                                                    });
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
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.chat_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return super.onSupportNavigateUp();
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
    protected int sizeOf(Bitmap data) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB_MR1) {
            return data.getRowBytes() * data.getHeight();
        } else {
            return data.getByteCount();
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.callPhone:
                if (checkSelfPermission(Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED)
                {
                    requestPermissions(new String[]{Manifest.permission.CALL_PHONE}, AllConstants.CALL_PERMISSION_CODE);
                }
                else
                {
                    callCameraIntent();
                }

                break;
            default:
                break;
        }

        return super.onOptionsItemSelected(item);
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
        else
        if(requestCode==AllConstants.CALL_PERMISSION_CODE)
        {
            if (grantResults.length>0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                Toast.makeText(this, "call permission granted", Toast.LENGTH_LONG).show();
                callPhoneIntentWithPhoneNumber();
            }
            else
            {
                Toast.makeText(this, "call permission denied", Toast.LENGTH_LONG).show();
            }
        }
    }
    private void callPhoneIntentWithPhoneNumber()
    {
        Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + receiverPhoneNumber));
        startActivity(intent);
    }
    private void callCameraIntent()
    {
        Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(cameraIntent, AllConstants.REQUEST_IMAGE_CAPTURE);
    }
}