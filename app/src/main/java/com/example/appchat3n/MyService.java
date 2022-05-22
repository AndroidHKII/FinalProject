package com.example.appchat3n;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.example.appchat3n.Activities.DashBoard;

public class MyService extends Service {
    private static final String TAG = "com.example.appchat3n";
    boolean isRun;
    public MyService() {

    }

    @Override
    public void onCreate() {
        super.onCreate();
        isRun=true;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "OnStart Called");
        Runnable r = new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 50; i++) {
                    long futuretime = System.currentTimeMillis() + 1000;
                    while (System.currentTimeMillis() < futuretime) {
                        synchronized (this) {
                            try {
                                wait(futuretime-System.currentTimeMillis());
                                Log.i(TAG, "Service is running");
                                sendNotification("AppChat3N", "Tương tác đi bạn ơi");
                                if(!isRun){
                                    return;
                                }
                            } catch (Exception e) {
                            }
                        }
                    }
                }
            }
        };
        Thread myThread = new Thread(r);
        myThread.start();
        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "Service is stop");
        isRun= false;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void sendNotification(String title, String messageBody) {
        Intent intent = new Intent(this, DashBoard.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
                PendingIntent.FLAG_ONE_SHOT);

        String channelId = "1";
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, channelId)
                        .setSmallIcon(R.drawable.ic_chat)
                        .setContentTitle(title)
                        .setContentText(messageBody)
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId,
                    "Channel human readable title",
                    NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build());
    }
}