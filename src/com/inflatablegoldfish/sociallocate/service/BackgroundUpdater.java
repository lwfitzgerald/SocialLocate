package com.inflatablegoldfish.sociallocate.service;

import java.util.List;

import com.commonsware.cwac.locpoll.LocationPoller;
import com.inflatablegoldfish.sociallocate.SocialLocate;
import com.inflatablegoldfish.sociallocate.User;
import com.inflatablegoldfish.sociallocate.Util;
import com.inflatablegoldfish.sociallocate.request.RequestListener;
import com.inflatablegoldfish.sociallocate.request.RequestManager;
import com.inflatablegoldfish.sociallocate.request.SLUpdateRequest;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;

public class BackgroundUpdater extends BroadcastReceiver {
    private static Object lock = new Object();
    
    @Override
    public void onReceive(final Context context, final Intent intent) {
        Log.i("SOCIALLOCATE", intent.getAction());
        
        if (intent.getAction() != null && intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            // Boot notification recieved so set up recurring alarm
            setUpAlarm(context);
        } else {
            new Thread(
                new Runnable() {
                    public void run() {
                        synchronized(lock) {
                            final Location location = (Location) intent.getExtras().get(LocationPoller.EXTRA_LOCATION);
                            
                            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                            
                            Location lastLocation = null;
                            
                            if (prefs.contains("last_lat")) {
                                float lat = prefs.getFloat("last_lat", Float.MIN_VALUE);
                                float lng = prefs.getFloat("last_lng", Float.MIN_VALUE);
                                float acc = prefs.getFloat("last_acc", Float.MIN_VALUE);
                                long time = prefs.getLong("last_time", Long.MIN_VALUE);
                                String provider = prefs.getString("last_provider", "");
                                
                                lastLocation = new Location(provider);
                                lastLocation.setLatitude(lat);
                                lastLocation.setLongitude(lng);
                                lastLocation.setAccuracy(acc);
                                lastLocation.setTime(time);
                            }
                            
                            if (Util.isBetterLocation(location, lastLocation)) {
                                RequestManager requestManager = new RequestManager(null);
                                SocialLocate socialLocate = new SocialLocate();
                                
                                requestManager.addRequest(
                                    new SLUpdateRequest(
                                        location,
                                        null,
                                        new RequestListener<List<User>>() {
                                            public void onComplete(Object result) {
                                                // Store better location
                                                SharedPreferences.Editor editor = prefs.edit();
                                                
                                                editor.putFloat("last_lat", (float) location.getLatitude());
                                                editor.putFloat("last_lng", (float) location.getLongitude());
                                                editor.putFloat("last_acc", location.getAccuracy());
                                                editor.putLong("last_time", location.getTime());
                                                editor.putString("last_provider", location.getProvider());
                                                
                                                editor.commit();
                                            }
                                            public void onError() {}
                                            public void onCancel() {}
                                        },
                                        null,
                                        socialLocate
                                    )
                                );
                            }
                        }
                    }
                }
            ).start();
        }
    }
    
    /**
     * Schedule the background updating alarm
     * @param context Context
     */
    public static void setUpAlarm(Context context) {
        AlarmManager mgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0,
                getAlarmIntent(context), 0);
        
        long interval = 60000 / (SocialLocate.UPDATES_PER_HOUR / 60);
        
        mgr.setRepeating(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            SystemClock.elapsedRealtime() + interval,
            interval,
            pendingIntent
        );
    }
    
    /**
     * Cancels the background updating alarm
     * @param context Context
     */
    public static void cancelAlarm(Context context) {
        AlarmManager mgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        
        mgr.cancel(PendingIntent.getBroadcast(context, 0, getAlarmIntent(context), 0));
    }
    
    /**
     * Get the intent for the background updating
     * alarm
     * @param context Context
     * @return Intent for cancelling and scheduling
     */
    private static Intent getAlarmIntent(Context context) {
        Intent intent = new Intent(context, LocationPoller.class);
        
        intent.putExtra(
            LocationPoller.EXTRA_INTENT,
            new Intent(context, BackgroundUpdater.class).setAction("com.inflatablegoldfish.sociallocate.LOCATION_UPDATED")
        );
        
        intent.putExtra(
            LocationPoller.EXTRA_PROVIDER,
            LocationManager.NETWORK_PROVIDER
        );
        
        return intent;
    }
}
