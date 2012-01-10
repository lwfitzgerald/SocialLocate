package com.inflatablegoldfish.sociallocate;

import java.io.IOException;

import com.google.android.c2dm.C2DMBaseReceiver;
import com.inflatablegoldfish.sociallocate.request.RequestListener;
import com.inflatablegoldfish.sociallocate.request.RequestManager;
import com.inflatablegoldfish.sociallocate.request.Request.ResultCode;
import com.inflatablegoldfish.sociallocate.request.SLUpdateRegRequest;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

public class C2DMReceiver extends C2DMBaseReceiver {
    public static final String USERNAME = "sociallocateapp@gmail.com";
    private static final int MEET_NOTIFICATION_ID = R.string.meet_notification_title;
    private static final int RESPOND_NOTIFICATION_ID = R.string.respond_title;
    
    private NotificationManager notificationManager; 
    private Notification.Builder notificationBuilder;
    
    public C2DMReceiver() {
        super(USERNAME);
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        // Set up notification manager
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        
        // Set up notification builder
        notificationBuilder = new Notification.Builder(this);
        notificationBuilder.setSmallIcon(R.drawable.ic_launcher);
        notificationBuilder.setAutoCancel(true);
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
            
            if (action.equals("meet")) {
                int friendID = Integer.valueOf(split[1]);
                String venueID = split[2];
                
                Intent launchIntent = new Intent(context, SLRespond.class);
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                launchIntent.putExtra("friend_id", friendID);
                launchIntent.putExtra("venue_id", venueID);
                
                PendingIntent pendingIntent = PendingIntent.getActivity(
                        this, 0, launchIntent, 0);
                
                Notification notification = buildNotification(
                    getText(R.string.meet_notification_title),
                    getText(R.string.meet_notification_text),
                    pendingIntent
                );
                
                notificationManager.notify(MEET_NOTIFICATION_ID, notification);
            } else { // action.equals("respond")
                boolean response = split[1].equals("1");
                
                Notification notification = buildNotification(
                    getText(R.string.respond_title),
                    getText(response ? R.string.accept_notification_text : R.string.reject_notification_text),
                    null
                );
                
                notificationManager.notify(RESPOND_NOTIFICATION_ID, notification);
            }
        }
    }
    
    private Notification buildNotification(CharSequence title, CharSequence text, PendingIntent contentIntent) {
        notificationBuilder.setTicker(title);
        notificationBuilder.setContentTitle(title);
        notificationBuilder.setContentText(text);
        notificationBuilder.setContentIntent(contentIntent);
        
        return notificationBuilder.getNotification();
    }

    @Override
    public void onError(Context context, String errorId) {
        Log.d("SocialLocate", "C2DM registration failed");
    }
}
