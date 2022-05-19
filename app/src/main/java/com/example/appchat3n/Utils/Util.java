package com.example.appchat3n.Utils;


import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

public class Util {

    public static String getUID() {
        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        return firebaseAuth.getUid();
    }

    public static void updateOnlineStatus(String status) {
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            String currentId = FirebaseAuth.getInstance().getUid();
            FirebaseDatabase.getInstance().getReference().child("presence").child(currentId).setValue(status);
        }
    }

    public static String mySubString(String myString, int start, int length) {
        return myString.substring(start, Math.min(start + length, myString.length()));
    }
}
