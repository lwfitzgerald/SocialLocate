package com.inflatablegoldfish.sociallocate;

import java.io.IOException;
import java.util.List;

import com.google.android.c2dm.C2DMBaseReceiver;
import com.inflatablegoldfish.sociallocate.request.RequestListener;
import com.inflatablegoldfish.sociallocate.request.RequestManager;
import com.inflatablegoldfish.sociallocate.request.Request.ResultCode;
import com.inflatablegoldfish.sociallocate.request.SLUpdateRegRequest;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
                new RequestListener<List<User>>() {
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
        Intent launchIntent = new Intent(context, SLArrangeMeet.class);
        
//        Bundle extras = intent.getExtras();
//        if (extras != null) {
//            String payload = (String) extras.get("payload");
//            String[] split = payload.split(";");
//            
//            launchIntent.putExtra("friend_id", Integer.valueOf(split[0]));
//            launchIntent.putExtra("venue_id", Integer.valueOf(split[1]));
//            
//            startActivity(launchIntent);
//        }
    }

    @Override
    public void onError(Context context, String errorId) {
        Log.d("SocialLocate", "C2DM registration failed");
    }
}
