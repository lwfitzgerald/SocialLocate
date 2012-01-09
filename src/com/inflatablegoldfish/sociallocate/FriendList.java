package com.inflatablegoldfish.sociallocate;

import java.util.List;

import android.content.Context;
import android.location.Location;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

import com.foound.widget.AmazingListView;
import com.inflatablegoldfish.sociallocate.SLActivity.BackButtonListener;
import com.inflatablegoldfish.sociallocate.SLActivity.LocationUpdateListener;
import com.inflatablegoldfish.sociallocate.SLActivity.SLUpdateListener;
import com.inflatablegoldfish.sociallocate.request.RequestListener;
import com.inflatablegoldfish.sociallocate.request.RequestManager;
import com.inflatablegoldfish.sociallocate.request.SLInitialFetchRequest;
import com.inflatablegoldfish.sociallocate.request.Request.ResultCode;

public class FriendList extends AmazingListView implements OnItemClickListener,
        LocationUpdateListener, SLUpdateListener, BackButtonListener {
    
    private SLActivity slActivity;
    private volatile boolean initialFetchCompleted = false;
    private FriendListAdapter adapter;
    
    public FriendList(Context context) {
        super(context);
    }
    
    public FriendList(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    
    public FriendList(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
    
    public void setUp(final SLActivity slActivity, PicRunner picRunner) {
        this.slActivity = slActivity;
        
        // Set the adapter
        adapter = new FriendListAdapter(slActivity, picRunner);
        setAdapter(adapter);
        
        // Set up loading/header/empty views and listener
        setLoadingView(slActivity.getLayoutInflater().inflate(R.layout.loading_view, null));
        setPinnedHeaderView(slActivity.getLayoutInflater().inflate(R.layout.list_header, this, false));
        mayHaveMorePages();
        setEmptyView(slActivity.findViewById(R.id.friend_empty_view));
        setOnItemClickListener(this);
        
        RequestManager requestManager = slActivity.getRequestManager();
        
        // Add initial fetch request
        requestManager.addRequest(
            new SLInitialFetchRequest(
                requestManager,
                new RequestListener<List<User>>() {
                    public void onComplete(final Object userList) {
                        @SuppressWarnings("unchecked")
                        final List<User> users = (List<User>) userList;
                        
                        Util.uiHandler.post(new Runnable() {
                            public void run() {
                                // Mark as complete and start location updates
                                initialFetchCompleted = true;
                                slActivity.getActivityLocationHandler().startUpdates();
                                
                                // Send on own user to friend view
                                slActivity.updateOwnUser(users.get(0));
                                
                                // Update adapter
                                adapter.updateUsers(users);
                                
                                // If we have a location fix, calculate distances
                                if (slActivity.getCurrentLocation() != null) {
                                    adapter.updateDistances(slActivity.getCurrentLocation());
                                }
                                
                                // Hide loading spinner
                                noMorePages();
                                
                                // Start fetch requests
                                slActivity.startFetchRequests();
                            }
                        });
                    }

                    public void onError(ResultCode resultCode) {
                        Util.uiHandler.post(new Runnable() {
                            public void run() {
                                // Hide loading spinner
                                noMorePages();
                                
                                // Show fail message
                                slActivity.findViewById(R.id.initial_fail).setVisibility(View.VISIBLE);
                                
                                // Stop location updates
                                slActivity.getActivityLocationHandler().stopUpdates();
                            }
                        });
                    }
                    
                    public void onCancel() {
                        Log.d("SocialLocate", "FB auth cancelled so cancelling initial fetch");
                        
                        Util.uiHandler.post(new Runnable() {
                            public void run() {
                                // Hide loading spinner
                                noMorePages();
                                
                                // Show fail message
                                slActivity.findViewById(R.id.initial_fail).setVisibility(View.VISIBLE);
                                
                                // Stop location updates
                                slActivity.getActivityLocationHandler().stopUpdates();
                            }
                        });
                    }
                },
                slActivity.getFacebook(),
                slActivity.getSocialLocate()
            )
        );
    }
    
    public void onLocationUpdate(Location newLocation) {
        // Update distances to friends
        adapter.updateDistances(newLocation);
    }
    
    public void onSLUpdate(List<User> friends) {
        // Update UI
        adapter.updateUsers(friends);
        
        if (slActivity.getCurrentLocation() != null) {
            adapter.updateDistances(slActivity.getCurrentLocation());
        }
    }
    
    /**
     * Returns whether the initial fetch has completed
     * @return True if completed
     */
    public boolean initialFetchCompleted() {
        return initialFetchCompleted;
    }

    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        if (view != getLoadingView() && !slActivity.getViewFlipper().isFlipping()) {
            // Set user to view
            slActivity.showFriendView((User) getItemAtPosition(position));
        }
    }

    public void onBackPressed() {
        // Close the app
        slActivity.finish();
    }
}
