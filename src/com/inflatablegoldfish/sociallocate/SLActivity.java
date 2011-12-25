package com.inflatablegoldfish.sociallocate;


import com.facebook.android.Facebook;
import com.inflatablegoldfish.sociallocate.SocialLocate.SLRequestListener;
import com.inflatablegoldfish.sociallocate.foursquare.Foursquare;
import com.inflatablegoldfish.sociallocate.request.FBAuthRequest;
import com.inflatablegoldfish.sociallocate.request.RequestManager;
import com.inflatablegoldfish.sociallocate.request.SLAuthRequest;
import com.inflatablegoldfish.sociallocate.request.SLInitialFetchRequest;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
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
    
    private Handler mHandler;
    private SharedPreferences mPrefs;
    
    private RequestManager requestManager;
    
    private TextView ownName;
    private ImageView ownPicture;
    private ListView friendList;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        mHandler = new Handler();
        mPrefs = getPreferences(MODE_PRIVATE);
        
        // Initialise custom SSL socket factory for IG
        Util.initIGSSLSocketFactory(getResources().openRawResource(R.raw.igkeystore));
        
        requestManager = new RequestManager(this);
        
        ownName = (TextView) findViewById(R.id.own_name);
        ownName.setText("Luke Fitzgerald");
        ownPicture = (ImageView) findViewById(R.id.own_picture);
        
        friendList = (ListView) findViewById(R.id.friend_list);
        friendList.setOnItemClickListener(this);
        //friendList.setAdapter(new FriendListAdapter(this));

        requestManager.addRequestWithoutStarting(
            new SLInitialFetchRequest(
                requestManager,
                new SLRequestListener() {
                    
                    @Override
                    public void onError() {
                        Util.showToast("Initial fetch error", mHandler, getApplicationContext());
                    }
                    
                    @Override
                    public void onComplete(User[] users) {
                        Util.showToast("Initial fetch ok", mHandler, getApplicationContext());
                    }
                },
                facebook,
                socialLocate,
                foursquare,
                mPrefs
            )
        );
        
        requestManager.addRequestWithoutStarting(
            new SLAuthRequest(
                requestManager,
                new SLRequestListener() {
                    
                    @Override
                    public void onError() {
                        Util.showToast("SL auth error", mHandler, getApplicationContext());
                    }
                    
                    @Override
                    public void onComplete(User[] users) {
                        Util.showToast("SL auth ok", mHandler, getApplicationContext());
                    }
                },
                facebook,
                socialLocate,
                foursquare,
                mPrefs
            )
        );
        
        requestManager.addRequestWithoutStarting(
            new FBAuthRequest(
                requestManager,
                facebook,
                socialLocate,
                foursquare,
                mPrefs
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