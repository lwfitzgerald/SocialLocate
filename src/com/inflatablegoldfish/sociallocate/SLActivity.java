package com.inflatablegoldfish.sociallocate;

import com.facebook.android.Facebook;
import com.google.android.maps.MapActivity;
import com.inflatablegoldfish.sociallocate.foursquare.Foursquare;
import com.inflatablegoldfish.sociallocate.request.FBAuthRequest;
import com.inflatablegoldfish.sociallocate.request.RequestListener;
import com.inflatablegoldfish.sociallocate.request.RequestManager;
import com.inflatablegoldfish.sociallocate.request.SLAuthRequest;
import com.inflatablegoldfish.sociallocate.request.SLInitialFetchRequest;
import com.inflatablegoldfish.sociallocate.service.SLService;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class SLActivity extends MapActivity implements OnItemClickListener {
    private SLService service = null;
    private ServiceConnection serviceConnection;
    
    private Facebook facebook = new Facebook("162900730478788");
    private SocialLocate socialLocate = new SocialLocate();
    private Foursquare foursquare = new Foursquare();
    
    private RequestManager requestManager;
    
    private User ownUser = null;
    
    private TextView ownName;
    private ImageView ownPicture;
    private ListView friendList;
    
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
        
        requestManager.updateContext(this);
        
        Util.uiHandler = new Handler();
        
        ownName = (TextView) findViewById(R.id.own_name);
        ownName.setText("Loading...");
        ownPicture = (ImageView) findViewById(R.id.own_picture);
        
        friendList = (ListView) findViewById(R.id.friend_list);
        friendList.setOnItemClickListener(this);
        friendList.setAdapter(new FriendListAdapter(this));

        requestManager.addRequestWithoutStarting(
            new SLInitialFetchRequest(
                requestManager,
                new RequestListener<User[]>() {
                    public void onError() {
                        Util.showToast("Initial fetch error", SLActivity.this);
                    }
                    
                    public void onComplete(final Object userArray) {
                        final User[] users = (User[]) userArray;
                        
                        Util.showToast("Initial fetch OK", SLActivity.this);
                        
                        // Store own details
                        ownUser = users[0];
                        
                        // Set our name
                        Util.uiHandler.post(new Runnable() {
                            public void run() {
                                ownName.setText(ownUser.getName());
                                ownName.invalidate();
                                
                                // Update list view adapter
                                ((FriendListAdapter) friendList.getAdapter()).updateFriends(users);
                            }
                        });
                        
                        StringBuffer buffer = new StringBuffer("Friends:\n");
                        
                        int i;
                        for (i=1 ; i < users.length-1; i++) {
                            buffer.append(users[i].getName() + "\n");
                        }
                        
                        if (i < users.length) {
                            buffer.append(users[i].getName());
                        }
                        
                        Util.showToast(buffer.toString(), SLActivity.this);
                    }
                    
                    public void onCancel() {
                        Util.showToast("FB auth cancelled so cancelling initial fetch", SLActivity.this);
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
                new RequestListener<User[]>() {
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
    
    public void onItemClick(AdapterView<?> arg0, View v, int position, long arg3) {
        
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        facebook.authorizeCallback(requestCode, resultCode, data);
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