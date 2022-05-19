package com.example.appchat3n.Activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.widget.Toast;

public class WifiReceivers extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        NetworkInfo networkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
        if(networkInfo !=null ) {
            if (networkInfo.isConnected()) {

                Toast.makeText(context, "Wifi is connected", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "Wifi is disconnected", Toast.LENGTH_SHORT).show();
            }
        }
    }
}