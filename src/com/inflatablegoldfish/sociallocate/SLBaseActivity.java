package com.inflatablegoldfish.sociallocate;

import com.facebook.android.Facebook;
import com.google.android.maps.MapActivity;
import com.inflatablegoldfish.sociallocate.foursquare.Foursquare;
import com.inflatablegoldfish.sociallocate.request.FBAuthRequest;
import com.inflatablegoldfish.sociallocate.request.RequestListener;
import com.inflatablegoldfish.sociallocate.request.RequestManager;
import com.inflatablegoldfish.sociallocate.request.SLAuthRequest;
import com.inflatablegoldfish.sociallocate.request.Request.ResultCode;

import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;

public abstract class SLBaseActivity extends MapActivity {
    protected Facebook facebook = new Facebook("162900730478788");
    
    protected SocialLocate socialLocate = new SocialLocate();
    protected Foursquare foursquare = new Foursquare();
    
    protected ActivityLocationHandler activityLocHandler;
    protected RequestManager requestManager;
    protected PicRunner picRunner;
    
    protected volatile Location currentLocation = null;
    
    protected SLBaseMapView mapView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Set up custom IG SSL socket factory
        Util.initIGSSLSocketFactory(getResources().openRawResource(R.raw.igkeystore));
        
        // Set up request manager
        requestManager = new RequestManager(this);
        
        // Set up the pic runner
        picRunner = new PicRunner();
        
        // Set up GPS location updates
        activityLocHandler = new ActivityLocationHandler(this);
        
        // Create a handler for this thread
        Util.uiHandler = new Handler();
        
        // Get preferences reference
        Util.prefs = PreferenceManager.getDefaultSharedPreferences(this);
        
        // Load cookies
        socialLocate.loadCookies();
        
        initialAuth();
    }
    
    protected void initialAuth() {
        // Only FB auth if we haven't already got a token
        if (!Util.prefs.contains("access_token")) {
            requestManager.addRequest(
                new FBAuthRequest(
                    requestManager,
                    new RequestListener<User[]>() {
                        public void onComplete(Object result) {}
    
                        public void onError(ResultCode resultCode) {}
    
                        public void onCancel() {}
                    },
                    facebook
                )
            );
        }
        
        // Only SL auth if we've not got a cookie
        if (!Util.prefs.contains("cookie_name")) {
            requestManager.addRequest(
                new SLAuthRequest(
                    requestManager,
                    new RequestListener<Void>() {
                        public void onComplete(Object users) {
                            Log.d("SocialLocate", "SL auth OK");
                        }
                        
                        public void onError(ResultCode resultCode) {
                            Log.d("SocialLocate", "SL auth error");
                        }
                        
                        public void onCancel() {
                            Log.d("SocialLocate", "FB auth cancelled so cancelling SL auth");
                        }
                    },
                    facebook,
                    socialLocate
                )
            );
        }
    }
    
    public void locationUpdated(final Location newLocation) {
        // Update current location
        currentLocation = newLocation;
        
        // Pass on to map view
        mapView.onLocationUpdate(currentLocation);
    }
    
    /**
     * Updates the own user object
     * @param ownUser New user object
     */
    public void updateOwnUser(User ownUser) {
        mapView.setOwnUser(ownUser);
    }
    
    /**
     * Get the current location
     * @return Current location
     */
    public Location getCurrentLocation() {
        return currentLocation;
    }
    
    /**
     * Get the request manager
     * @return Request manager
     */
    public RequestManager getRequestManager() {
        return requestManager;
    }
    
    /**
     * Get the Facebook interface object
     * @return Facebook interface object
     */
    public Facebook getFacebook() {
        return facebook;
    }
    
    /**
     * Get the SocialLocate interface object
     * @return SocialLocate interface object
     */
    public SocialLocate getSocialLocate() {
        return socialLocate;
    }
    
    /**
     * Get the Map view
     * @return Map view
     */
    public SLBaseMapView getMapView() {
        return mapView;
    }
    
    @Override
    public void onResume() {
        // Start GPS updates
        activityLocHandler.startUpdates();
        
        super.onResume();
    }
    
    @Override
    public void onPause() {
        // Save cookies
        socialLocate.saveCookies();
        
        // Stop GPS updates and resume background updates
        activityLocHandler.stopUpdates();
        BackgroundUpdater.setUpAlarm(this);
        
        super.onPause();
    }
    
    @Override
    public void onDestroy() {
        requestManager.abortAll();
        
        super.onDestroy();
    }
    
    @Override
    protected boolean isRouteDisplayed() {
        return false;
    }
    
    public static interface LocationUpdateListener {
        /**
         * Called when a location update is received
         * @param newLocation New location
         */
        public void onLocationUpdate(Location newLocation);
    }
    
    public static interface BackButtonListener {
        /**
         * Called when the back button is pressed
         */
        public void onBackPressed();
    }
}
