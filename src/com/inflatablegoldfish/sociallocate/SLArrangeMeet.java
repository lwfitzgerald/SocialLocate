package com.inflatablegoldfish.sociallocate;

import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import com.facebook.android.Facebook;
import com.google.android.c2dm.C2DMessaging;
import com.google.android.maps.GeoPoint;
import com.inflatablegoldfish.sociallocate.foursquare.Foursquare;
import com.inflatablegoldfish.sociallocate.request.RequestListener;
import com.inflatablegoldfish.sociallocate.request.SLFetchRequest;
import com.inflatablegoldfish.sociallocate.request.SLUpdateLocRequest;
import com.inflatablegoldfish.sociallocate.request.Request.ResultCode;

import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.widget.ViewFlipper;

public class SLArrangeMeet extends SLBaseActivity {
    private ViewFlipper viewFlipper;
    private FriendList friendList;
    private VenueList venueList;
    
    private Timer fetchTimer;
    
    public enum ActivityStage {
        FRIEND_LIST,
        FRIEND_VIEW,
        VENUE_LIST,
        VENUE_VIEW
    };
    
    private volatile ActivityStage currentStage = ActivityStage.FRIEND_LIST;
    
    private static final int FETCHES_PER_MINUTE = 2;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        /*
         * Starting UI so we'll ask for GPS updates
         * as well
         * 
         * First, cancel the background updating
         */
        BackgroundUpdater.cancelAlarm(this);
        
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        // Register for C2DM pushs
        doC2DMRegister();
        
        setTitle(R.string.friends_title);
        
        // Set up the flipper
        viewFlipper = (ViewFlipper) findViewById(R.id.flipper);
        viewFlipper.setInAnimation(this, android.R.anim.fade_in);
        viewFlipper.setOutAnimation(this, android.R.anim.fade_out);
        
        // Set up the pic runner
        PicRunner picRunner = new PicRunner();
        
        // Set up the friend list
        friendList = (FriendList) findViewById(R.id.friend_list);
        friendList.setUp(this, picRunner);
        
        // Set up the friend view
        mapView = (SLArrangeMapView) findViewById(R.id.friend_view);
        ((SLArrangeMapView) mapView).setUp(this, picRunner);
        
        // Set up the venue list
        venueList = (VenueList) findViewById(R.id.venue_list);
        venueList.setUp(this, picRunner);
    }
    
    /**
     * Register for C2DM pushes
     */
    private void doC2DMRegister() {
        SharedPreferences prefs = getSharedPreferences(C2DMessaging.PREFERENCE, MODE_PRIVATE);
        String deviceRegistrationID = prefs.getString("dm_registration", null);

        if (deviceRegistrationID == null) {
            C2DMessaging.register(this, C2DMReceiver.USERNAME);
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
                                    final List<User> friends = (List<User>) result;
                                    
                                    // Start a new thread to handle callbacks
                                    new Thread(
                                        new Runnable() {
                                            public void run() {
                                                // Pass onto friendList and friendView
                                                friendList.onSLUpdate(friends);
                                                ((SLArrangeMapView) mapView).onSLUpdate(friends);
                                                venueList.onSLUpdate(friends);
                                            }
                                        }
                                    ).start();
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
        if (fetchTimer != null) {
            fetchTimer.cancel();
        }
    }
    
    @Override
    public void locationUpdated(final Location newLocation) {
        // Update current location
        currentLocation = newLocation;
        
        // Pass on to friend list
        friendList.onLocationUpdate(currentLocation);
        
        // Pass on to map view
        mapView.onLocationUpdate(currentLocation);
        
        // Send location update
        requestManager.addRequest(
            new SLUpdateLocRequest(
                currentLocation,
                requestManager,
                new RequestListener<Void>() {
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
        currentStage = ActivityStage.FRIEND_VIEW;
        
        setTitle("SocialLocate - " + user.getName());
        
        mapView.updateUser(user, true);
        
        viewFlipper.showNext();
    }
    
    @Override
    public void onBackPressed() {
        switch (currentStage) {
        case FRIEND_LIST:
            friendList.onBackPressed();
            break;
        case FRIEND_VIEW:
            mapView.onBackPressed();
            break;
        case VENUE_LIST:
            venueList.onBackPressed();
            break;
        case VENUE_VIEW:
            mapView.onBackPressed();
        }
    }
    
    /**
     * Switch to the venue list view
     * @param center Center to show venues around
     */
    public void showVenueList(GeoPoint center) {
        currentStage = ActivityStage.VENUE_LIST;
        
        setTitle(R.string.venues_title);
        
        venueList.switchingTo();
        
        viewFlipper.showNext();
    }
    
    /**
     * Get the View flipper
     * @return The view flipper
     */
    public ViewFlipper getViewFlipper() {
        return viewFlipper;
    }
    
    /**
     * Get the map view
     * @return Map view
     */
    public SLArrangeMapView getMapView() {
        return (SLArrangeMapView) mapView;
    }
    
    /**
     * Get the current stage of the activity
     * @return Current stage
     */
    public ActivityStage getCurrentStage() {
        return currentStage;
    }
    
    /**
     * Set the current stage of the activity
     * @param newStage Stage to set
     */
    public void setCurrentStage(ActivityStage newStage) {
        currentStage = newStage;
    }
    
    /**
     * Get the activity location handler
     * @return Activity location handler
     */
    public ActivityLocationHandler getActivityLocationHandler() {
        return activityLocHandler;
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
     * Get the Foursquare interface object
     * @return Foursquare interface object
     */
    public Foursquare getFoursquare() {
        return foursquare;
    }
    
    @Override
    public void onResume() {
        // Stop background updates
        BackgroundUpdater.cancelAlarm(this);
        
        super.onResume();
    }
    
    @Override
    public void onPause() {
        // Stop repetitive fetches
        stopFetchRequests();
        
        super.onPause();
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        facebook.authorizeCallback(requestCode, resultCode, data);
    }
    
    public static interface SLUpdateListener {
        /**
         * Called when an SL fetch request completes
         * @param friends Friends retrieved
         */
        public void onSLUpdate(List<User> friends);
    }
}
