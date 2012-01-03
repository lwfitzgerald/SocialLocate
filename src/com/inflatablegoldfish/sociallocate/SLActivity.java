package com.inflatablegoldfish.sociallocate;

import java.util.List;

import com.facebook.android.Facebook;
import com.foound.widget.AmazingListView;
import com.google.android.maps.MapActivity;
import com.inflatablegoldfish.sociallocate.foursquare.Foursquare;
import com.inflatablegoldfish.sociallocate.request.FBAuthRequest;
import com.inflatablegoldfish.sociallocate.request.RequestListener;
import com.inflatablegoldfish.sociallocate.request.RequestManager;
import com.inflatablegoldfish.sociallocate.request.SLAuthRequest;
import com.inflatablegoldfish.sociallocate.request.SLInitialFetchRequest;
import com.inflatablegoldfish.sociallocate.request.SLUpdateRequest;
import com.inflatablegoldfish.sociallocate.service.BackgroundUpdater;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
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
        
        // Load cookies
        socialLocate.loadCookies();
        
        // Authenticate
        initialAuth();
    }
    
    private void initialAuth() {
        // Only FB auth if we haven't already got a key
        if (!Util.prefs.contains("access_token")) {
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
        }
        
        // Only SL auth if we've not got a cookie
        if (!Util.prefs.contains("cookie_name")) {
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
        }
            
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
        
        requestManager.startProcessing();
    }
    
    public void locationUpdated(final Location newLocation) {
        currentLocation = newLocation;
        
        friendListAdapter.updateDistances(currentLocation);
        
        requestManager.addRequest(
            new SLUpdateRequest(
                currentLocation,
                requestManager,
                new RequestListener<List<User>>() {
                    public void onComplete(Object result) {}
                    public void onError() {}
                    public void onCancel() {}
                },
                facebook,
                socialLocate
            )
        );
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
        // Start GPS updates and stop background updates
        //BackgroundUpdater.cancelAlarm(this);
        activityLocHandler.startUpdates();
        
        super.onResume();
    }
    
    @Override
    public void onPause() {
        // Save cookies
        socialLocate.saveCookies();
        
        // Stop GPS updates and resume background updates
        activityLocHandler.stopUpdates();
        //BackgroundUpdater.setUpAlarm(this);
        
        super.onPause();
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