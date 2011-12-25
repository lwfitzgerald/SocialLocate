package com.inflatablegoldfish.sociallocate;


import com.facebook.android.Facebook;
import com.inflatablegoldfish.sociallocate.foursquare.Foursquare;
import com.inflatablegoldfish.sociallocate.request.FBAuthRequest;
import com.inflatablegoldfish.sociallocate.request.RequestListener;
import com.inflatablegoldfish.sociallocate.request.RequestManager;
import com.inflatablegoldfish.sociallocate.request.SLAuthRequest;
import com.inflatablegoldfish.sociallocate.request.SLInitialFetchRequest;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class SLActivity extends Activity implements OnItemClickListener {
    private Facebook facebook = new Facebook("162900730478788");
    private SocialLocate socialLocate = new SocialLocate();
    private Foursquare foursquare = new Foursquare();
    
    private RequestManager requestManager;
    
    private TextView ownName;
    private ImageView ownPicture;
    private ListView friendList;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        // Create UI handler and preferences interface
        Util.uiHandler = new Handler();
        Util.prefs = getPreferences(MODE_PRIVATE);
        
        // Initialise custom SSL socket factory for IG
        Util.initIGSSLSocketFactory(getResources().openRawResource(R.raw.igkeystore));
        
        // Initialise request manager
        requestManager = new RequestManager(this);
        
        ownName = (TextView) findViewById(R.id.own_name);
        ownName.setText("Loading...");
        ownPicture = (ImageView) findViewById(R.id.own_picture);
        
        friendList = (ListView) findViewById(R.id.friend_list);
        friendList.setOnItemClickListener(this);
        //friendList.setAdapter(new FriendListAdapter(this));

        requestManager.addRequestWithoutStarting(
            new SLInitialFetchRequest(
                requestManager,
                new RequestListener<User[]>() {
                    public void onError() {
                        Util.showToast("Initial fetch error", getApplicationContext());
                    }
                    
                    public void onComplete(final User[] users) {
                        Util.showToast("Initial fetch OK", getApplicationContext());
                        
                        // Set our name
                        Util.uiHandler.post(new Runnable() {
                            public void run() {
                                ownName.setText(users[0].getName());
                                ownName.invalidate();
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
                        
                        Util.showToast(buffer.toString(), getApplicationContext());
                    }
                    
                    public void onCancel() {
                        Util.showToast("FB auth cancelled so cancelling initial fetch", getApplicationContext());
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
                        Util.showToast("SL auth error", getApplicationContext());
                    }
                    
                    public void onComplete(User[] users) {
                        Util.showToast("SL auth OK", getApplicationContext());
                    }
                    
                    public void onCancel() {
                        Util.showToast("FB auth cancelled so cancelling SL auth", getApplicationContext());
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
                facebook,
                socialLocate,
                foursquare
            )
        );
        
        requestManager.startProcessing();
    }
    
    public void onItemClick(AdapterView<?> arg0, View v, int position, long arg3) {
    }
    
    //private 
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        facebook.authorizeCallback(requestCode, resultCode, data);
    }
}