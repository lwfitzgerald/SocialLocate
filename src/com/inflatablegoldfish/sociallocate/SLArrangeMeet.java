package com.inflatablegoldfish.sociallocate;

import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import com.google.android.maps.GeoPoint;
import com.inflatablegoldfish.sociallocate.foursquare.Foursquare;
import com.inflatablegoldfish.sociallocate.request.RequestListener;
import com.inflatablegoldfish.sociallocate.request.SLFetchRequest;
import com.inflatablegoldfish.sociallocate.request.SLUpdateLocRequest;
import com.inflatablegoldfish.sociallocate.request.Request.ResultCode;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.location.Location;
import android.os.Bundle;
import android.widget.ViewFlipper;

public class SLArrangeMeet extends SLBaseActivity {
    private ViewFlipper viewFlipper;
    private FriendList friendList;
    private VenueList venueList;
    
    private Timer fetchTimer;
    
    private volatile ActivityStage currentStage = ActivityStage.FRIEND_LIST;
    
    public enum ActivityStage {
        FRIEND_LIST,
        FRIEND_VIEW,
        VENUE_LIST,
        VENUE_VIEW
    };

    public static final int FRIEND_LIST = 0;
    public static final int FRIEND_VIEW = 1;
    public static final int VENUE_LIST = 2;
    public static final int VENUE_VIEW = 1;
    
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
        
        setTitle(R.string.friends_title);
        
        // Check there is a Google account present
        checkForGoogleAccount();
        
        // Set up the flipper
        viewFlipper = (ViewFlipper) findViewById(R.id.flipper);
        
        // Set up the friend list
        friendList = (FriendList) findViewById(R.id.friend_list);
        friendList.setUp(this, picRunner);
        
        // Set up the map view
        mapView = (SLArrangeMapView) findViewById(R.id.map_view);
        ((SLArrangeMapView) mapView).setUp(this, picRunner);
        
        // Set up the venue list
        venueList = (VenueList) findViewById(R.id.venue_list);
        venueList.setUp(this, picRunner);
    }
    
    /**
     * Check for a google account and show
     * an error and quit if there is not one
     */
    private void checkForGoogleAccount() {
        Account[] accounts = AccountManager.get(this).getAccountsByType("com.google");
        if (accounts.length == 0) {
            Util.showToast(getText(R.string.no_google_accounts), this);
            finish();
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
        
        // Pass on to the venue list
        venueList.onLocationUpdate(currentLocation);
        
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
        
        viewFlipper.setDisplayedChild(FRIEND_VIEW);
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
        
        viewFlipper.setDisplayedChild(VENUE_LIST);
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
    
    public static interface SLUpdateListener {
        /**
         * Called when an SL fetch request completes
         * @param friends Friends retrieved
         */
        public void onSLUpdate(List<User> friends);
    }
}
