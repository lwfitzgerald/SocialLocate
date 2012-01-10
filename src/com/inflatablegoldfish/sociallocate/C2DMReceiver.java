package com.inflatablegoldfish.sociallocate;

import java.io.IOException;

import com.google.android.c2dm.C2DMBaseReceiver;
import com.inflatablegoldfish.sociallocate.request.RequestListener;
import com.inflatablegoldfish.sociallocate.request.RequestManager;
import com.inflatablegoldfish.sociallocate.request.Request.ResultCode;
import com.inflatablegoldfish.sociallocate.request.SLUpdateRegRequest;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

public class C2DMReceiver extends C2DMBaseReceiver {
    public static final String USERNAME = "sociallocateapp@gmail.com";
    
    public C2DMReceiver() {
        super(USERNAME);
    }

    @Override
    public void onRegistered(Context context, String registrationId) throws IOException {
        Util.prefs = PreferenceManager.getDefaultSharedPreferences(context);
        
        // Set up IG SSL socket factory override
        Util.initIGSSLSocketFactory(context.getResources().openRawResource(R.raw.igkeystore));
        
        // Set up request manager and cookies
        RequestManager requestManager = new RequestManager(null);
        final SocialLocate socialLocate = new SocialLocate();
        socialLocate.loadCookies();
        
        requestManager.addRequest(
            new SLUpdateRegRequest(
                registrationId,
                requestManager,
                new RequestListener<Void>() {
                    public void onComplete(Object result) {
                        SharedPreferences.Editor editor = Util.prefs.edit();
                        editor.putBoolean("registration_sent", true);
                        editor.commit();
                    }
                    public void onError(ResultCode resultCode) {}
                    public void onCancel() {}
                },
                null,
                socialLocate
            )
        );
    }
    
    @Override
    protected void onMessage(Context context, Intent intent) {
        Log.d("SocialLocate", "C2DM message received");
        Log.d("SocialLocate", intent.getExtras().getString("payload"));
        
        Bundle extras = intent.getExtras();
        if (extras != null) {
            String payload = (String) extras.get("payload");
            String[] split = payload.split(";");
            String action = split[0];
            int friendID = Integer.valueOf(split[1]);
            String venueID = split[2];
            
            if (action.equals("meet")) {
                Intent launchIntent = new Intent(context, SLRespond.class);
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                launchIntent.putExtra("friend_id", friendID);
                launchIntent.putExtra("venue_id", venueID);
            
                startActivity(launchIntent);
            } else { // action.equals("respond")
                
            }
        }
    }

    @Override
    public void onError(Context context, String errorId) {
        Log.d("SocialLocate", "C2DM registration failed");
    }
}
