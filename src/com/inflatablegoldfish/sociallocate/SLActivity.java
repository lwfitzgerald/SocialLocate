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
import com.inflatablegoldfish.sociallocate.service.SLService;
import com.inflatablegoldfish.sociallocate.service.SLService.SLServiceListener;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.TextView;

public class SLActivity extends MapActivity implements OnItemClickListener, SLServiceListener {
    private SLService service = null;
    private ServiceConnection serviceConnection;
    
    private Facebook facebook = new Facebook("162900730478788");
    private SocialLocate socialLocate = new SocialLocate();
    private Foursquare foursquare = new Foursquare();
    
    private RequestManager requestManager;
    
    private User ownUser = null;
    private Location currentLocation = null;
    
    private TextView ownName;
    private ImageView ownPicture;
    private AmazingListView friendList;
    private FriendListAdapter friendListAdapter;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        // Set up service connection
        setUpService();
    }
    
    /**
     * Called by the ServiceConnection listener
     * when the service is bound
     */
    private void continueCreate() {        
        // Get request manager from service
        requestManager = service.getRequestManager();
        
        // Set the request manager context as this activity
        requestManager.updateContext(this);
        
        ownName = (TextView) findViewById(R.id.own_name);
        ownName.setText("Loading...");
        ownPicture = (ImageView) findViewById(R.id.own_picture);
        
        // Set up the the friend list
        friendList = (AmazingListView) findViewById(R.id.friend_list);
        friendList.setLoadingView(getLayoutInflater().inflate(R.layout.loading_view, null));
        friendList.mayHaveMorePages();
        friendList.setOnItemClickListener(this);
        
        // Set the adapter
        friendListAdapter = new FriendListAdapter(this);
        friendList.setAdapter(friendListAdapter);
        
        // Register as service location update listener
        service.addListener(this);

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
                        
                        // Store own details
                        ownUser = users.get(0);
                        
                        // Get sublist of friends
                        final List<User> friends = users.subList(1, users.size());
                        
                        // Set our name
                        Util.uiHandler.post(new Runnable() {
                            public void run() {
                                ownName.setText(ownUser.getName());
                                ownName.invalidate();
                                
                                // Update list view adapter
                                friendListAdapter.updateFriends(friends);
                                
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
                socialLocate,
                foursquare
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
                socialLocate,
                foursquare
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
                socialLocate,
                foursquare
            )
        );
        
        requestManager.startProcessing();
    }
    
    private void setUpService() {
        serviceConnection = new ServiceConnection() {
            public void onServiceConnected(ComponentName className, IBinder service) {
                SLActivity.this.service = ((SLService.SLServiceBinder) service).getService();
                
                // Start service if not already running
                if (!SLActivity.this.service.isStarted()) {
                    startService(new Intent(SLActivity.this, SLService.class));
                }
                
                // Continue creation
                continueCreate();
            }
        
            public void onServiceDisconnected(ComponentName className) {
                SLActivity.this.service = null;
            }
        };
        
        // Service is vital for application so raise priority when binding
        bindService(new Intent(this, 
                SLService.class), serviceConnection, BIND_AUTO_CREATE | BIND_IMPORTANT);
    }
    
    public void locationUpdated(final Location newLocation) {
        currentLocation = newLocation;
        
        friendListAdapter.updateDistances(currentLocation);
    }
    
    public void onItemClick(AdapterView<?> adapterView, View v, int position, long id) {
        Util.showToast("You clicked " + ((User) adapterView.getItemAtPosition(position)).getName(), this);
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        facebook.authorizeCallback(requestCode, resultCode, data);
    }
    
    @Override
    public void onPause() {
        super.onPause();
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();

        unbindService(serviceConnection);
    }

    @Override
    protected boolean isRouteDisplayed() {
        return false;
    }
}