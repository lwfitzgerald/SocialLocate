package com.inflatablegoldfish.sociallocate.service;

import java.util.List;

import com.commonsware.cwac.locpoll.LocationPoller;
import com.inflatablegoldfish.sociallocate.SocialLocate;
import com.inflatablegoldfish.sociallocate.User;
import com.inflatablegoldfish.sociallocate.Util;
import com.inflatablegoldfish.sociallocate.request.RequestListener;
import com.inflatablegoldfish.sociallocate.request.SLUpdateRequest;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.preference.PreferenceManager;

public class LocationReceiver extends BroadcastReceiver {
    private static Object lock = new Object();
    
    @Override
    public void onReceive(final Context context, final Intent intent) {
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
                            /*
                             * Perform request in new thread
                             * 
                             * Don't use request manager as we
                             * don't care if the request fails
                             */
                            new Thread(
                                new Runnable() {
                                    public void run() {
                                        SocialLocate socialLocate = new SocialLocate();
                                        
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
                                        ).execute();
                                    }
                                }
                            ).start();
                        }
                    }
                }
            }
        ).start();
    }
}
