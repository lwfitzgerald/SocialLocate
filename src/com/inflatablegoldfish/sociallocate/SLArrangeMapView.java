package com.inflatablegoldfish.sociallocate;

import java.util.List;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.inflatablegoldfish.sociallocate.SLArrangeMeet.ActivityStage;
import com.inflatablegoldfish.sociallocate.SLArrangeMeet.SLUpdateListener;
import com.inflatablegoldfish.sociallocate.foursquare.Foursquare;
import com.inflatablegoldfish.sociallocate.foursquare.Venue;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.location.Location;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;

public class SLArrangeMapView extends SLBaseMapView implements SLUpdateListener {
    private volatile Location centerLocation = null;
    private volatile GeoPoint center = null;
    
    private Button findVenuesButton;
    private Button notifyButton;
    
    public SLArrangeMapView(Context context) {
        super(context);
    }
    
    public SLArrangeMapView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    
    public SLArrangeMapView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void onFinishInflate() {
        super.onFinishInflate();
        
        // Set up buttons
        findVenuesButton = (Button) findViewById(R.id.find_venues_button);
        findVenuesButton.setOnClickListener(this);
        
        notifyButton = (Button) findViewById(R.id.notify_button);
        notifyButton.setOnClickListener(this);
        notifyButton.setVisibility(View.GONE);
    }
    
    public void setUp(SLArrangeMeet activity, PicRunner picRunner) {
        this.activity = activity;
        this.picRunner = picRunner;
        picRunner.addListener(this);
        
        userOverlay = new UserOverlay();
        venueOverlay = new VenueOverlay();
        mapView.getOverlays().add(userOverlay);
        mapView.getOverlays().add(venueOverlay);
    }
    
    private class UserOverlay extends SLBaseMapView.UserOverlay {
        @Override
        public void draw(Canvas canvas, MapView mapView, boolean shadow) {
            Point ownUserPoint = null;
            if (userItems[0] != null) {
                ownUserPoint = new Point();
                mapView.getProjection().toPixels(userItems[0].getPoint(), ownUserPoint);
            }
            
            Point friendUserPoint = null;
            if (userItems[1] != null) {
                friendUserPoint = new Point();
                mapView.getProjection().toPixels(userItems[1].getPoint(), friendUserPoint);
            }
            
            if (center != null) {
                drawLinesAndSearchRadius(ownUserPoint, friendUserPoint, canvas, paint);
            }
            
            super.draw(canvas, mapView, shadow);
        }
        
        private void drawLinesAndSearchRadius(Point ownUserPoint, Point friendUserPoint, Canvas canvas, Paint paint) {
            Point centerPoint = new Point();
            mapView.getProjection().toPixels(center, centerPoint);
            
            Path path = new Path();

            path.moveTo(ownUserPoint.x, ownUserPoint.y);
            path.lineTo(centerPoint.x, centerPoint.y);
            path.moveTo(centerPoint.x, centerPoint.y);
            path.lineTo(friendUserPoint.x, friendUserPoint.y);

            canvas.drawPath(path, paint);
            
            paint.setStyle(Paint.Style.STROKE);
            
            float searchRadius = (float) (mapView.getProjection().metersToEquatorPixels(Foursquare.SEARCH_RADIUS)
                    * (1 / Math.cos(Math.toRadians(centerLocation.getLatitude()))));
            
            canvas.drawCircle(centerPoint.x, centerPoint.y, searchRadius, paint);
            
            paint.setStyle(Paint.Style.FILL_AND_STROKE);
        }
    }
    
    @Override
    public void updateUser(final User user, boolean forceCenter) {
        this.friendUser = user;
        
        // We're forcing centering
        if (forceCenter) {
            this.initiallyCentered = false;
            this.centerLocation = null;
            this.center = null;
        }
        
        // Update the friend overlay
        this.userOverlay.updateFriendUser(user);
        
        // Set the values for the UI elements at the top of the view
        if (((SLArrangeMeet) activity).getCurrentStage() == ActivityStage.FRIEND_VIEW) {
            setTopForUser();
        } else if (((SLArrangeMeet) activity).getCurrentStage() == ActivityStage.VENUE_VIEW) {
            setTopForVenue();
        }
        
        if (activity.getCurrentLocation() != null) {
            // Set/update map center
            this.centerLocation = Util.getCenter(
                new Location[] {
                    activity.getCurrentLocation(),
                    user.getLocation()
                }
            );
            
            this.center = Util.getGeoPoint(this.centerLocation);
        }
        
        if (!initiallyCentered) {
            final GeoPoint centerPoint;
            
            if (center != null) {
                // Have actual center from above, use that
                centerPoint = center;
                
                // We'll be centering following this
                this.initiallyCentered = true;
                
                Util.uiHandler.post(
                    new Runnable() {
                        public void run() {
                            mapController.zoomToSpan(userOverlay.getLatSpanE6(), userOverlay.getLonSpanE6());
                        }
                    }
                );
            } else {
                // No location so just center on friend
                // Doesn't count as an initial centering
                centerPoint = Util.getGeoPoint(user.getLocation());
                mapController.setZoom(12);
            }
            
            // Only center if we haven't already
            Util.uiHandler.post(
                new Runnable() {
                    public void run() {
                        mapController.animateTo(centerPoint);
                    }
                }
            );
        }
    }
    
    @Override
    public void setVenue(Venue venue) {
        super.setVenue(venue);
        
        // Configure button visibility
        Util.uiHandler.post(
            new Runnable() {
                public void run() {
                    findVenuesButton.setVisibility(View.GONE);
                    notifyButton.setVisibility(View.VISIBLE);
                }
            }
        );
    }
    
    public Location getCenter() {
        return centerLocation;
    }
    
    @Override
    public void onLocationUpdate(Location newLocation) {
        super.onLocationUpdate(newLocation);
        
        if (friendUser != null) {
            // Have current location so center
            this.centerLocation = Util.getCenter(
                new Location[] {
                    newLocation,
                    friendUser.getLocation()
                }
            );
            
            this.center = Util.getGeoPoint(this.centerLocation);
            
            if (!initiallyCentered) {
                initiallyCentered = true;
                Util.uiHandler.post(
                    new Runnable() {
                        public void run() {
                            mapController.zoomToSpan(userOverlay.getLatSpanE6(), userOverlay.getLonSpanE6());
                            mapController.animateTo(center);
                        }
                    }
                );
            }
        }
    }
    
    public void onSLUpdate(List<User> friends) {
        // Will refresh UI
        if (friendUser != null) {
            updateUser(friendUser, false);
        }
    }

    @Override
    public void onProfilePicDownloaded() {
        super.onProfilePicDownloaded();
        
        if (((SLArrangeMeet) activity).getCurrentStage() == ActivityStage.FRIEND_VIEW) {
            // Set image for user
            final Bitmap userBitmap = picRunner.getImage(friendUser.getPic(), true);
            
            // Update top user picture
            Util.uiHandler.post(
                new Runnable() {
                    public void run() {
                        if (userBitmap != null) {
                            friendPic.setImageBitmap(userBitmap);
                        }
                        friendPic.invalidate();
                    }
                }
            );
        } else if (((SLArrangeMeet) activity).getCurrentStage() == ActivityStage.VENUE_VIEW) {
            // Set image for venue
            final Bitmap venueBitmap = picRunner.getImage(venue.getIcon(), true);
            
            // Update venue picture
            Util.uiHandler.post(
                new Runnable() {
                    public void run() {
                        if (venueBitmap != null) {
                            venueIcon.setImageBitmap(venueBitmap);
                        }
                        venueIcon.invalidate();
                    }
                }
            );
        }
    }

    @Override
    public void onClick(View view) {
        if (!((SLArrangeMeet) activity).getViewFlipper().isFlipping()) {
            if (view == findVenuesButton) {
                ((SLArrangeMeet) activity).showVenueList(center);
            } else if (view == notifyButton) {
                
            } else { // Cancel button
                // Same as pressing back
                onBackPressed();
            }
        }
    }
    
    @Override
    public void onBackPressed() {
        SLArrangeMeet slArrangeMeet = (SLArrangeMeet) activity;
        
        if (slArrangeMeet.getCurrentStage() == ActivityStage.FRIEND_VIEW) {
            slArrangeMeet.setCurrentStage(ActivityStage.FRIEND_LIST);
            
            slArrangeMeet.setTitle(R.string.friends_title);
            
            slArrangeMeet.getViewFlipper().showPrevious();
        } else {
            // Venue set so go back to venue list
            slArrangeMeet.setCurrentStage(ActivityStage.VENUE_LIST);
            
            slArrangeMeet.setTitle(R.string.venues_title);
            
            slArrangeMeet.getViewFlipper().showNext();
            
            venue = null;
            venueOverlay.clearVenue();
            
            // Hide venue bar
            venueBar.setVisibility(View.GONE);
            
            // Configure button visibility
            Util.uiHandler.post(
                new Runnable() {
                    public void run() {
                        findVenuesButton.setVisibility(View.VISIBLE);
                        notifyButton.setVisibility(View.GONE);
                    }
                }
            );
        }
    }
}
