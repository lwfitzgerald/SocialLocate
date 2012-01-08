package com.inflatablegoldfish.sociallocate;

import java.util.List;

import android.content.Context;
import android.location.Location;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

import com.foound.widget.AmazingListView;
import com.inflatablegoldfish.sociallocate.SLActivity.ActivityStage;
import com.inflatablegoldfish.sociallocate.SLActivity.BackButtonListener;
import com.inflatablegoldfish.sociallocate.SLActivity.LocationUpdateListener;
import com.inflatablegoldfish.sociallocate.SLActivity.SLUpdateListener;
import com.inflatablegoldfish.sociallocate.foursquare.Venue;
import com.inflatablegoldfish.sociallocate.request.FSRequest;
import com.inflatablegoldfish.sociallocate.request.Request.ResultCode;
import com.inflatablegoldfish.sociallocate.request.RequestListener;
import com.inflatablegoldfish.sociallocate.request.RequestManager;

public class VenueList extends AmazingListView implements OnItemClickListener,
        LocationUpdateListener, SLUpdateListener, BackButtonListener {
    
    private SLActivity slActivity;
    private VenueListAdapter adapter;
    
    private View errorView;
    
    public VenueList(Context context) {
        super(context);
    }
    
    public VenueList(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    
    public VenueList(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
    
    public void setUp(final SLActivity slActivity, PicRunner picRunner) {
        this.slActivity = slActivity;
        this.errorView = slActivity.findViewById(R.id.venue_fail);
        
        // Set the adapter
        adapter = new VenueListAdapter(slActivity, picRunner);
        setAdapter(adapter);
        
        // Set up loading/header/empty views
        setLoadingView(slActivity.getLayoutInflater().inflate(R.layout.loading_view, null));
        setEmptyView(slActivity.findViewById(R.id.venue_empty_view));
        mayHaveMorePages();
        setOnItemClickListener(this);
    }
    
    /**
     * Called when switching to display this view
     */
    public void switchingTo() {
        Util.uiHandler.post(
            new Runnable() {
                public void run() {
                    // Clear existing venues
                    adapter.clear();
                    
                    // Show loading spinner
                    mayHaveMorePages();
                    
                    // Hide error view as might currently be shown
                    errorView.setVisibility(View.GONE);
                }
            }
        );
        
        doUpdate();
    }

    public void onLocationUpdate(Location ourLocation) {
        // If the venue list is currently being shown...
        if (slActivity.getCurrentStage() == ActivityStage.VENUE_LIST) {
            Util.uiHandler.post(
                new Runnable() {
                    public void run() {
                        // Hide error view as might currently be shown
                        if (errorView != null) {
                            errorView.setVisibility(View.GONE);
                        }
                    }
                }
            );
            
            doUpdate();
        }
    }
    
    public void onSLUpdate(List<User> friends) {
        // If the venue list is currently being shown...
        if (slActivity.getCurrentStage() == ActivityStage.VENUE_LIST) {
            Util.uiHandler.post(
                new Runnable() {
                    public void run() {
                        // Hide error view as might currently be shown
                        if (errorView != null) {
                            errorView.setVisibility(View.GONE);
                        }
                    }
                }
            );
            
            doUpdate();
        }
    }
    
    private void doUpdate() {
        RequestManager requestManager = slActivity.getRequestManager();
        
        Location center = slActivity.getFriendView().getCenter();
        
        if (center != null) {
            requestManager.addRequest(
                new FSRequest(
                    center,
                    slActivity.getCurrentLocation(),
                    slActivity.getFoursquare(),
                    requestManager,
                    new RequestListener<List<Venue>>() {
                        @SuppressWarnings("unchecked")
                        public void onComplete(Object result) {
                            adapter.updateVenues((List<Venue>) result);
                            
                            Util.uiHandler.post(
                                new Runnable() {
                                    public void run() {
                                        // Hide loading spinner
                                        noMorePages();
                                    }
                                }
                            );
                        }

                        public void onError(ResultCode resultCode) {
                            Util.uiHandler.post(
                                new Runnable() {
                                    public void run() {
                                        // Hide loading spinner
                                        noMorePages();
                                        
                                        if (errorView != null) {
                                            // Show error view
                                            errorView.setVisibility(View.VISIBLE);
                                        }
                                    }
                                }
                            );
                        }

                        public void onCancel() {}
                    }
                )
            );
        }
    }
    
    public void onItemClick(AdapterView<?> adapterView, View v, int position, long id) {
        // Set the venue in the friend view
        slActivity.getFriendView().setVenue((Venue) getItemAtPosition(position));
        
        slActivity.setCurrentStage(ActivityStage.FRIEND_VENUE_VIEW);
        
        slActivity.getViewFlipper().showPrevious();
    }

    public void onBackPressed() {
        slActivity.setCurrentStage(ActivityStage.FRIEND_VIEW);
        
        slActivity.getViewFlipper().showPrevious();
    }
}
