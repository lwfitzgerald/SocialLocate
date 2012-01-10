package com.inflatablegoldfish.sociallocate;

import java.util.List;

import android.content.Context;
import android.location.Location;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

import com.foound.widget.AmazingListView;
import com.inflatablegoldfish.sociallocate.SLArrangeMeet.ActivityStage;
import com.inflatablegoldfish.sociallocate.SLArrangeMeet.SLUpdateListener;
import com.inflatablegoldfish.sociallocate.SLBaseActivity.BackButtonListener;
import com.inflatablegoldfish.sociallocate.SLBaseActivity.LocationUpdateListener;
import com.inflatablegoldfish.sociallocate.foursquare.Venue;
import com.inflatablegoldfish.sociallocate.request.FSVenuesRequest;
import com.inflatablegoldfish.sociallocate.request.Request.ResultCode;
import com.inflatablegoldfish.sociallocate.request.RequestListener;
import com.inflatablegoldfish.sociallocate.request.RequestManager;

public class VenueList extends AmazingListView implements OnItemClickListener,
        LocationUpdateListener, SLUpdateListener, BackButtonListener {
    
    private SLArrangeMeet slArrangeMeet;
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
    
    public void setUp(final SLArrangeMeet slArrangeMeet, PicRunner picRunner) {
        this.slArrangeMeet = slArrangeMeet;
        this.errorView = slArrangeMeet.findViewById(R.id.venue_fail);
        
        // Set the adapter
        adapter = new VenueListAdapter(slArrangeMeet, picRunner);
        setAdapter(adapter);
        
        // Set up loading/header/empty views
        setLoadingView(slArrangeMeet.getLayoutInflater().inflate(R.layout.loading_view, null));
        setEmptyView(slArrangeMeet.findViewById(R.id.venue_empty_view));
        mayHaveMorePages();
        setOnItemClickListener(this);
    }
    
    /**
     * Called when switching to display this view
     */
    public void switchingTo() {
        doUpdate();
    }

    public void onLocationUpdate(Location ourLocation) {
        // If the venue list is currently being shown...
        if (slArrangeMeet.getCurrentStage() == ActivityStage.VENUE_LIST) {
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
        if (slArrangeMeet.getCurrentStage() == ActivityStage.VENUE_LIST) {
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
        RequestManager requestManager = slArrangeMeet.getRequestManager();
        
        Location center = slArrangeMeet.getMapView().getCenter();
        
        if (center != null) {
            requestManager.addRequest(
                new FSVenuesRequest(
                    center,
                    slArrangeMeet.getFoursquare(),
                    requestManager,
                    new RequestListener<List<Venue>>() {
                        @SuppressWarnings("unchecked")
                        public void onComplete(Object result) {
                            List<Venue> venues = (List<Venue>) result;
                            
                            if (slArrangeMeet.getCurrentLocation() != null) {
                                Venue.calculateDistances(venues, slArrangeMeet.getCurrentLocation());
                            }
                            
                            adapter.updateVenues(venues);
                            
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
    
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        if (view != getLoadingView() && !slArrangeMeet.getViewFlipper().isFlipping()) {
            Venue venue = (Venue) getItemAtPosition(position);
            
            // Set the venue in the friend view
            slArrangeMeet.getMapView().setVenue(venue);
            
            slArrangeMeet.setCurrentStage(ActivityStage.VENUE_VIEW);
            
            slArrangeMeet.setTitle("SocialLocate - " + venue.getName());
            
            slArrangeMeet.getViewFlipper().setDisplayedChild(SLArrangeMeet.VENUE_VIEW);
        }
    }

    public void onBackPressed() {
        if (!slArrangeMeet.getViewFlipper().isFlipping()) {
            slArrangeMeet.setCurrentStage(ActivityStage.FRIEND_VIEW);
            
            // Clear venues
            adapter.clear();
            
            // Show loading spinner
            mayHaveMorePages();
            
            // Hide error view as might currently be shown
            errorView.setVisibility(View.GONE);
            
            slArrangeMeet.setTitle("SocialLocate - "
                    + slArrangeMeet.getMapView().getFriend().getName());
            
            slArrangeMeet.getViewFlipper().setDisplayedChild(SLArrangeMeet.FRIEND_VIEW);
        }
    }
}
