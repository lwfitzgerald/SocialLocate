package com.inflatablegoldfish.sociallocate;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

public class ActivityLocationHandler {
    private final SLBaseActivity activity;
    
    private LocationManager manager;
    private LocationListener listener;
    private Location currentBest = null;
    private long lastUpdate = Long.MIN_VALUE;

    public ActivityLocationHandler(SLBaseActivity slBaseActivity) {
        this.activity = slBaseActivity;
        
        // Acquire a reference to the system Location Manager
        manager = (LocationManager) slBaseActivity.getSystemService(Context.LOCATION_SERVICE);
        
        // Define a listener that responds to location updates
        listener = new LocationListener() {
            public void onLocationChanged(Location location) {
                if (Util.isBetterLocation(location, currentBest)) {
                    currentBest = location;
                    
                    if (lastUpdate + (60000 / (SocialLocate.UPDATES_PER_HOUR / 60))
                            < System.currentTimeMillis()) {
                        lastUpdate = System.currentTimeMillis();
                        
                        ActivityLocationHandler.this.activity.locationUpdated(location);
                    }
                }
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {}

            public void onProviderEnabled(String provider) {}

            public void onProviderDisabled(String provider) {}
        };
    }
    
    public Location getBestLastKnownLocation() {
        Location networkLocation = manager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        Location gpsLocation = manager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        
        if (gpsLocation != null) {
            return Util.isBetterLocation(gpsLocation, networkLocation) ? networkLocation : gpsLocation;
        } else {
            return networkLocation;
        }
    }
    
    public void startUpdates() {
        Location lastLocation = getBestLastKnownLocation();
        
        if (lastLocation != null) {
            ActivityLocationHandler.this.activity.locationUpdated(lastLocation);
        }
        
        manager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, listener);
        
        if (manager.getProvider(LocationManager.GPS_PROVIDER) != null) {
            manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, listener);
        }
    }
    
    public void stopUpdates() {
        manager.removeUpdates(listener);
    }
}
