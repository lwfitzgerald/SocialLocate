package com.inflatablegoldfish.sociallocate;

import java.util.List;

import com.commonsware.cwac.locpoll.LocationPoller;
import com.facebook.android.Facebook;
import com.foound.widget.AmazingListView;
import com.google.android.maps.MapActivity;
import com.inflatablegoldfish.sociallocate.foursquare.Foursquare;
import com.inflatablegoldfish.sociallocate.request.FBAuthRequest;
import com.inflatablegoldfish.sociallocate.request.RequestListener;
import com.inflatablegoldfish.sociallocate.request.RequestManager;
import com.inflatablegoldfish.sociallocate.request.SLAuthRequest;
import com.inflatablegoldfish.sociallocate.request.SLInitialFetchRequest;
import com.inflatablegoldfish.sociallocate.service.LocationReceiver;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

public class SLActivity extends MapActivity implements OnItemClickListener {
    private Facebook facebook = new Facebook("162900730478788");
    
    private SocialLocate socialLocate = new SocialLocate();
    private Foursquare foursquare = new Foursquare();
    
    private ActivityLocationHandler activityLocHandler;
    private RequestManager requestManager;
    
    private User ownUser = null;
    private Location currentLocation = null;
    
    private AmazingListView friendList;
    private FriendListAdapter friendListAdapter;
    
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
        cancelAlarm();
        
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
        
        // Set up the the friend list
        friendList = (AmazingListView) findViewById(R.id.friend_list);
        friendList.setLoadingView(getLayoutInflater().inflate(R.layout.loading_view, null));
        friendList.setPinnedHeaderView(getLayoutInflater().inflate(R.layout.list_header, friendList, false));
        friendList.mayHaveMorePages();
        friendList.setEmptyView(findViewById(R.id.empty_view));
        friendList.setOnItemClickListener(this);
        
        // Set the adapter
        friendListAdapter = new FriendListAdapter(this);
        friendList.setAdapter(friendListAdapter);

        requestManager.addRequestWithoutStarting(
            new SLInitialFetchRequest(
                requestManager,
                new RequestListener<List<User>>() {
                    public void onError() {
                        Util.showToast("Initial fetch error", SLActivity.this);
                        
                        Util.uiHandler.post(new Runnable() {
                            public void run() {
                                // Hide loading spinner
                                friendList.noMorePages();
                            }
                        });
                    }
                    
                    public void onComplete(final Object userList) {
                        @SuppressWarnings("unchecked")
                        final List<User> users = (List<User>) userList;
                        
                        Util.showToast("Initial fetch OK", SLActivity.this);
                        
                        // Store own details and get photo
                        ownUser = users.get(0);
                        
                        Util.uiHandler.post(new Runnable() {
                            public void run() {
                                // Update list view adapter
                                friendListAdapter.updateUsers(users);
                                
                                // If we have a location fix, calculate distances
                                if (currentLocation != null) {
                                    friendListAdapter.updateDistances(currentLocation);
                                }
                                
                                // Hide loading spinner
                                friendList.noMorePages();
                            }
                        });
                    }
                    
                    public void onCancel() {
                        Util.showToast("FB auth cancelled so cancelling initial fetch", SLActivity.this);
                        
                        Util.uiHandler.post(new Runnable() {
                            public void run() {
                                // Hide loading spinner
                                friendList.noMorePages();
                            }
                        });
                    }
                },
                facebook,
                socialLocate
            )
        );
        
        requestManager.addRequestWithoutStarting(
            new SLAuthRequest(
                requestManager,
                new RequestListener<List<User>>() {
                    public void onError() {
                        Util.showToast("SL auth error", SLActivity.this);
                    }
                    
                    public void onComplete(Object users) {
                        Util.showToast("SL auth OK", SLActivity.this);
                    }
                    
                    public void onCancel() {
                        Util.showToast("FB auth cancelled so cancelling SL auth", SLActivity.this);
                    }
                },
                facebook,
                socialLocate
            )
        );
        
        requestManager.addRequestWithoutStarting(
            new FBAuthRequest(
                requestManager,
                new RequestListener<User[]>() {
                    public void onComplete(Object result) {}

                    public void onError() {}

                    public void onCancel() {}
                },
                facebook,
                socialLocate
            )
        );
        
        requestManager.startProcessing();
    }
    
    /**
     * Cancels the background updating alarm
     */
    private void cancelAlarm() {
        AlarmManager mgr = (AlarmManager) getSystemService(ALARM_SERVICE);
        
        mgr.cancel(PendingIntent.getBroadcast(this, 0, getAlarmIntent(), 0));
    }
    
    /**
     * Schedule the background updating alarm
     */
    private void setUpAlarm() {
        AlarmManager mgr = (AlarmManager) getSystemService(ALARM_SERVICE);
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0,
                getAlarmIntent(), 0);
        
        long interval = 60000 / (SocialLocate.UPDATES_PER_HOUR / 60);
        
        mgr.setRepeating(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            SystemClock.elapsedRealtime() + interval,
            interval,
            pendingIntent
        );
    }
    
    /**
     * Get the intent for the background updating
     * alarm
     * @return Intent for cancelling and scheduling
     */
    public Intent getAlarmIntent() {
        Intent intent = new Intent(this, LocationPoller.class);
        intent.setAction("com.inflatablegoldfish.com.sociallocate.LOCATION_CHANGED");
        
        intent.putExtra(
            LocationPoller.EXTRA_INTENT,
            new Intent(this, LocationReceiver.class)
        );
        
        intent.putExtra(
            LocationPoller.EXTRA_PROVIDER,
            LocationManager.NETWORK_PROVIDER
        );
        
        return intent;
    }
    
    public void locationUpdated(final Location newLocation) {
        currentLocation = newLocation;
        
        friendListAdapter.updateDistances(currentLocation);
        
//        requestManager.addRequest(
//            new SLUpdateRequest(
//                location,
//                manager,
//                listener,
//                facebook,
//                socialLocate,
//                foursquare
//            )
//        );
    }
    
    public void onItemClick(AdapterView<?> adapterView, View v, int position, long id) {
        if (position != 0) {
            Util.showToast("You clicked " + ((User) adapterView.getItemAtPosition(position)).getName(), this);
        }
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        facebook.authorizeCallback(requestCode, resultCode, data);
    }
    
    @Override
    public void onResume() {
        super.onResume();
        
        // Start GPS updates and stop background updates
        cancelAlarm();
        activityLocHandler.startUpdates();
    }
    
    @Override
    public void onPause() {
        super.onPause();
        
        // Stop GPS updates and resume background updates
        activityLocHandler.stopUpdates();
        setUpAlarm();
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected boolean isRouteDisplayed() {
        return false;
    }
}