package com.inflatablegoldfish.sociallocate;

import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import com.facebook.android.Facebook;
import com.google.android.maps.MapActivity;
import com.inflatablegoldfish.sociallocate.foursquare.Foursquare;
import com.inflatablegoldfish.sociallocate.request.FBAuthRequest;
import com.inflatablegoldfish.sociallocate.request.RequestListener;
import com.inflatablegoldfish.sociallocate.request.RequestManager;
import com.inflatablegoldfish.sociallocate.request.SLAuthRequest;
import com.inflatablegoldfish.sociallocate.request.SLFetchRequest;
import com.inflatablegoldfish.sociallocate.request.SLUpdateRequest;
import com.inflatablegoldfish.sociallocate.request.Request.ResultCode;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.ViewFlipper;

public class SLActivity extends MapActivity {
    private Facebook facebook = new Facebook("162900730478788");
    
    private SocialLocate socialLocate = new SocialLocate();
    private Foursquare foursquare = new Foursquare();
    
    private ActivityLocationHandler activityLocHandler;
    private RequestManager requestManager;
    
    private Location currentLocation = null;
    
    private ViewFlipper viewFlipper;
    private FriendList friendList;
    
    private FriendView friendView;
    
    private Timer fetchTimer;
    
    private static final int FETCHES_PER_MINUTE = 2;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        /*
         * Starting UI so we'll ask for GPS updates
         * as well
         * 
         * First, cancel the background updating
         */
        BackgroundUpdater.cancelAlarm(this);
        
        // Set up custom IG SSL socket factory
        Util.initIGSSLSocketFactory(getResources().openRawResource(R.raw.igkeystore));
        
        // Set up request manager
        requestManager = new RequestManager(this);
        
        // Set up GPS location updates
        activityLocHandler = new ActivityLocationHandler(this);
        
        // Create a handler for this thread
        Util.uiHandler = new Handler();
        
        // Get preferences reference
        Util.prefs = PreferenceManager.getDefaultSharedPreferences(this);
        
        // Load cookies
        socialLocate.loadCookies();
        
        // Authenticate
        initialAuth();
        
        // Set up the flipper
        viewFlipper = (ViewFlipper) findViewById(R.id.flipper);
        viewFlipper.setInAnimation(this, android.R.anim.fade_in);
        viewFlipper.setOutAnimation(this, android.R.anim.fade_out);
        
        // Set up the pic runner
        ProfilePicRunner picRunner = new ProfilePicRunner();
        
        // Set up the the friend list
        friendList = (FriendList) findViewById(R.id.friend_list);
        friendList.setUp(this, picRunner);
        
        // Set the friend view
        friendView = (FriendView) findViewById(R.id.friend_view);
        friendView.setUp(this, picRunner);
    }
    
    private void initialAuth() {
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
                    facebook,
                    socialLocate
                )
            );
        }
        
        // Only SL auth if we've not got a cookie
        if (!Util.prefs.contains("cookie_name")) {
            requestManager.addRequest(
                new SLAuthRequest(
                    requestManager,
                    new RequestListener<List<User>>() {
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
    
    /**
     * Starts repeating fetch requests
     */
    public void startFetchRequests() {
        fetchTimer = new Timer();
        
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                // Only fetch if we've done an initial fetch
                if (friendList.initialFetchCompleted()) {
                    requestManager.addRequest(
                        new SLFetchRequest(
                            requestManager,
                            new RequestListener<List<User>>() {
                                @SuppressWarnings("unchecked")
                                public void onComplete(Object result) {
                                    List<User> friends = (List<User>) result;
                                    
                                    // Pass onto friendList and friendView
                                    friendList.onSLUpdate(friends);
                                    friendView.onSLUpdate(friends);
                                }

                                public void onError(ResultCode resultCode) {
                                    stopFetchRequests();
                                }

                                public void onCancel() {
                                    stopFetchRequests();
                                }
                            },
                            facebook,
                            socialLocate
                        )
                    );
                }
            }
        };
        
        fetchTimer.scheduleAtFixedRate(task, new Date(), 60000 / FETCHES_PER_MINUTE);
    }
    
    /**
     * Stops repeating fetch requests
     */
    public void stopFetchRequests() {
        fetchTimer.cancel();
    }
    
    public void locationUpdated(final Location newLocation) {
        // Update current location
        currentLocation = newLocation;
        
        // Pass on to friend list
        friendList.onLocationUpdate(currentLocation);
        
        // Pass on to friend view
        friendView.onLocationUpdate(currentLocation);
        
        // Send location update
        requestManager.addRequest(
            new SLUpdateRequest(
                currentLocation,
                requestManager,
                new RequestListener<List<User>>() {
                    public void onComplete(Object result) {}
                    public void onError(ResultCode resultCode) {}
                    public void onCancel() {}
                },
                facebook,
                socialLocate
            )
        );
    }
    
    /**
     * Switch to friend view
     * @param user Friend to view
     */
    public void showFriendView(User user) {
        friendView.updateUser(user);
        
        viewFlipper.showNext();
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        facebook.authorizeCallback(requestCode, resultCode, data);
    }
    
    @Override
    public void onResume() {
        // Start GPS updates and stop background updates
        BackgroundUpdater.cancelAlarm(this);
        
        if (friendList.initialFetchCompleted()) {
            // Only start if we're authenticated
            startFetchRequests();
            activityLocHandler.startUpdates();
        }
        
        super.onResume();
    }
    
    @Override
    public void onPause() {
        // Save cookies
        socialLocate.saveCookies();
        
        // Stop repetitive fetches
        stopFetchRequests();
        
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
    public void onBackPressed() {
        if (viewFlipper.getDisplayedChild() != 0) {
            viewFlipper.showPrevious();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected boolean isRouteDisplayed() {
        return false;
    }
    
    public RequestManager getRequestManager() {
        return requestManager;
    }
    
    public ActivityLocationHandler getActivityLocationHandler() {
        return activityLocHandler;
    }
    
    public Location getCurrentLocation() {
        return currentLocation;
    }
    
    public Facebook getFacebook() {
        return facebook;
    }
    
    public SocialLocate getSocialLocate() {
        return socialLocate;
    }
    
    public Foursquare getFoursquare() {
        return foursquare;
    }
    
    public static interface LocationUpdateListener {
        /**
         * Called when a location update is received
         * @param newLocation New location
         */
        public void onLocationUpdate(Location newLocation);
    }
    
    public static interface SLUpdateListener {
        /**
         * Called when an SL fetch request completes
         * @param friends Friends retrieved
         */
        public void onSLUpdate(List<User> friends);
    }
}