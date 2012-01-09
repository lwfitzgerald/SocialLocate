package com.inflatablegoldfish.sociallocate;

import com.google.android.c2dm.C2DMBaseReceiver;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class C2DMReceiver extends C2DMBaseReceiver {
    private static final String username = "sociallocateapp@gmail.com";
    
    public C2DMReceiver() {
        super(username);
    }

    @Override
    protected void onMessage(Context context, Intent intent) {
        Intent launchIntent = new Intent(context, ApproveMeet.class);
        
        Bundle extras = intent.getExtras();
        if (extras != null) {
            String payload = (String) extras.get("payload");
            String[] split = payload.split(";");
            
            launchIntent.putExtra("friend_id", Integer.valueOf(split[0]));
            launchIntent.putExtra("venue_id", Integer.valueOf(split[1]));
            
            startActivity(launchIntent);
        }
    }

    @Override
    public void onError(Context context, String errorId) {
        Log.d("SocialLocate", "C2DM registration failed");
    }
}
