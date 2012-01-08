package com.inflatablegoldfish.sociallocate;

import java.util.List;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.OverlayItem;
import com.inflatablegoldfish.sociallocate.PicRunner.PicRunnerListener;
import com.inflatablegoldfish.sociallocate.SLActivity.ActivityStage;
import com.inflatablegoldfish.sociallocate.SLActivity.BackButtonListener;
import com.inflatablegoldfish.sociallocate.SLActivity.LocationUpdateListener;
import com.inflatablegoldfish.sociallocate.SLActivity.SLUpdateListener;
import com.inflatablegoldfish.sociallocate.foursquare.Foursquare;
import com.inflatablegoldfish.sociallocate.foursquare.Venue;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class SLMapView extends RelativeLayout implements
        PicRunnerListener, LocationUpdateListener, SLUpdateListener, OnClickListener, BackButtonListener {
    
    private SLActivity slActivity;
    
    private volatile User ownUser = null;
    private volatile User friendUser = null;
    
    private volatile Location centerLocation = null;
    private volatile GeoPoint center = null;
    
    private volatile Venue venue;
    
    private PicRunner picRunner;
    
    private ImageView pic = null;
    private TextView name;
    private TextView lastUpdated;
    private TextView distance;
    
    private MapView mapView;
    private MapController mapController;
    private UserOverlay userOverlay;
    private VenueOverlay venueOverlay;
    
    private volatile boolean initiallyCentered;
    
    public SLMapView(Context context) {
        super(context);
    }
    
    public SLMapView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    
    public SLMapView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
    
    @Override
    public void onFinishInflate() {
        super.onFinishInflate();
        
        pic = (ImageView) findViewById(R.id.profile_pic);
        name = (TextView) findViewById(R.id.name);
        lastUpdated = (TextView) findViewById(R.id.last_updated);
        distance = (TextView) findViewById(R.id.distance);
        
        // Set this as button listener
        ((Button) findViewById(R.id.lets_meet_button)).setOnClickListener(this);
        
        // Set up the map controller
        mapView = (MapView) findViewById(R.id.mapview);
        
        /*
         * Disable software rendering as android has no
         * fallback when paths get too large to fit into
         * textures
         */
        mapView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        
        mapController = mapView.getController();
        mapController.setZoom(12);
    }
    
    public void setUp(SLActivity slActivity, PicRunner picRunner) {
        this.slActivity = slActivity;
        this.picRunner = picRunner;
        picRunner.addListener(this);
        
        userOverlay = new UserOverlay();
        venueOverlay = new VenueOverlay();
        mapView.getOverlays().add(userOverlay);
        mapView.getOverlays().add(venueOverlay);
    }
    
    private class UserOverlay extends ItemizedOverlay<UserItem> {
        /*
         * userItems[0] = Own User
         * userItems[1] = Friend User
         */
        private UserItem[] userItems = new UserItem[] {null, null};
        
        public UserOverlay() {
            super(null);
        }
        
        public void setOwnUser(Location currentLocation) {
            userItems[0] = new UserItem(ownUser, Util.getGeoPoint(currentLocation), true);
            refresh();
        }
        
        public void updateFriendUser(User friendUser) {
            userItems[1] = new UserItem(friendUser, Util.getGeoPoint(friendUser.getLocation()), false);
            refresh();
        }
        
        @Override
        public void draw(Canvas canvas, MapView mapView, boolean shadow) {
            Paint paint = new Paint();
            paint.setDither(true);
            paint.setColor(Color.BLACK);
            paint.setStyle(Paint.Style.FILL_AND_STROKE);
            paint.setStrokeJoin(Paint.Join.ROUND);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeWidth(4);
            
            Point ownUserPoint = null;
            if (userItems[0] != null) {
                ownUserPoint = getAndDrawItemPoint(userItems[0], canvas, paint);
            }
            
            Point friendUserPoint = null;
            if (userItems[1] != null) {
                friendUserPoint = getAndDrawItemPoint(userItems[1], canvas, paint);
            }
            
            if (center != null) {
                drawLinesAndSearchRadius(ownUserPoint, friendUserPoint, canvas, paint);
            }
            
            super.draw(canvas, mapView, false);
        }
        
        private Point getAndDrawItemPoint(UserItem item, Canvas canvas, Paint paint) {
            Point point = new Point();
            mapView.getProjection().toPixels(item.getPoint(), point);
            
            canvas.drawCircle(point.x, point.y, 3, paint);
            
            return point;
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
        }
        
        public void refresh() {
            populate();
            
            Util.uiHandler.post(
                new Runnable() {
                    public void run() {
                        mapView.invalidate();
                    }
                }
            );
        }
        
        @Override
        protected UserItem createItem(int i) {
            if (size() == 1) {
                return userItems[0] != null ? userItems[0] : userItems[1];
            } else {
                // Both present
                return userItems[i];
            }
        }

        @Override
        public int size() {
            return (userItems[0] != null ? 1 : 0)
                    + (userItems[1] != null ? 1 : 0);
        }
    }
    
    private class UserItem extends OverlayItem {
        private BitmapDrawable drawableImage = null;
        private boolean isOwnUser;
        
        public UserItem(User user, GeoPoint point, boolean isOwnUser) {
            super(point, user.getName(), "");
            this.isOwnUser = isOwnUser;
        }
        
        @Override
        public Drawable getMarker(int stateBitset) {
            if (drawableImage == null) {
                Bitmap image;
                if (isOwnUser) {
                    image = picRunner.getImage(ownUser.getPic(), true);
                } else {
                    image = picRunner.getImage(friendUser.getPic(), true);
                }
                
                if (image != null) {
                    drawableImage = new BitmapDrawable(slActivity.getResources(), image);
                    drawableImage.setBounds(0, 0, image.getWidth(), image.getHeight());
                    return drawableImage;
                } else {
                    Bitmap emptyBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
                    return new BitmapDrawable(slActivity.getResources(), emptyBitmap);
                }
            }
            
            return drawableImage;
        }
    }
    
    /**
     * Set the own user
     * @param ownUser Own user to set
     */
    public void setOwnUser(final User ownUser) {
        this.ownUser = ownUser;
        
        if (slActivity.getCurrentLocation() != null) {
            // Create overlay item
            userOverlay.setOwnUser(slActivity.getCurrentLocation());
        }
    }
    
    /**
     * Update the friend user
     * @param user New friend user
     * @param forceCenter If true, force the map to re-center
     */
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
        if (slActivity.getCurrentStage() == ActivityStage.FRIEND_VIEW) {
            setTopForUser();
        } else if (slActivity.getCurrentStage() == ActivityStage.VENUE_VIEW) {
            setTopForVenue();
        }
        
        if (slActivity.getCurrentLocation() != null) {
            // Set/update map center
            this.centerLocation = Util.getCenter(
                new Location[] {
                    slActivity.getCurrentLocation(),
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
    
    /**
     * Set the values for the top view elements
     * to those for the Friend view
     */
    private void setTopForUser() {
        final Bitmap bitmap = picRunner.getImage(friendUser.getPic(), true);
        
        Util.uiHandler.post(
            new Runnable() {
                public void run() {
                    if (bitmap != null) {
                        pic.setImageBitmap(bitmap);
                    }
                    
                    name.setText(friendUser.getName());
                    lastUpdated.setText(friendUser.getPrettyLastUpdated());
                    
                    if (friendUser.getDistance() != null) {
                        distance.setText(friendUser.getPrettyDistance());
                    }
                    
                    pic.invalidate();
                    name.invalidate();
                    lastUpdated.invalidate();
                    distance.invalidate();
                }
            }
        );
    }

    /**
     * Set the values for the top view elements
     * to those for the Venue view
     */
    private void setTopForVenue() {
        final Bitmap bitmap = picRunner.getImage(venue.getIcon(), true);
        
        Util.uiHandler.post(
            new Runnable() {
                public void run() {
                    if (bitmap != null) {
                        pic.setImageBitmap(bitmap);
                    }
                    
                    name.setText(venue.getName());
                    lastUpdated.setText("");
                    
                    if (friendUser.getDistance() != null) {
                        distance.setText(venue.getPrettyDistance());
                    }
                    
                    pic.invalidate();
                    name.invalidate();
                    lastUpdated.invalidate();
                    distance.invalidate();
                }
            }
        );
    }
    
    public void onLocationUpdate(Location newLocation) {
        if (ownUser != null) {
            // Update own user geopoint
            userOverlay.setOwnUser(newLocation);
        }
        
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
    
    public Location getCenter() {
        return centerLocation;
    }
    
    private class VenueOverlay extends ItemizedOverlay<VenueItem> {
        private volatile VenueItem venueItem = null;
        
        public VenueOverlay() {
            super(null);
        }
        
        public void setVenue(Venue venue) {
            venueItem = new VenueItem(venue);
            refresh();
        }
        
        public void clearVenue() {
            venueItem = null;
            refresh();
        }
        
        @Override
        public void draw(Canvas canvas, MapView mapView, boolean shadow) {
            Paint paint = new Paint();
            paint.setDither(true);
            paint.setColor(Color.BLACK);
            paint.setStyle(Paint.Style.FILL_AND_STROKE);
            paint.setStrokeJoin(Paint.Join.ROUND);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeWidth(4);
            
            if (venueItem != null) {
                Point point = new Point();
                mapView.getProjection().toPixels(venueItem.getPoint(), point);
                
                canvas.drawCircle(point.x, point.y, 3, paint);
            }
                
            super.draw(canvas, mapView, false);
        }

        @Override
        protected VenueItem createItem(int i) {
            return venueItem;
        }

        @Override
        public int size() {
            return venueItem != null ? 1 : 0;
        }
        
        public void refresh() {
            populate();
            
            Util.uiHandler.post(
                new Runnable() {
                    public void run() {
                        mapView.invalidate();
                    }
                }
            );
        }
    }
    
    private class VenueItem extends OverlayItem {
        private BitmapDrawable drawableImage = null;
        
        public VenueItem(Venue venue) {
            super(Util.getGeoPoint(venue.getLocation()), venue.getName(), "");
        }
        
        @Override
        public Drawable getMarker(int stateBitset) {
            if (drawableImage == null) {
                Bitmap image = picRunner.getImage(venue.getIcon(), false);
                
                if (image != null) {
                    drawableImage = new BitmapDrawable(slActivity.getResources(), image);
                    drawableImage.setBounds(0, 0, Math.min(image.getWidth(), 100), Math.min(image.getHeight(), 100));
                    return drawableImage;
                } else {
                    Bitmap emptyBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
                    return new BitmapDrawable(slActivity.getResources(), emptyBitmap);
                }
            }
            
            return drawableImage;
        }
    }
    
    public void setVenue(Venue venue) {
        this.venue = venue;
        
        // Animate to location
        Util.uiHandler.post(
            new Runnable() {
                public void run() {
                    mapController.animateTo(Util.getGeoPoint(SLMapView.this.venue.getLocation()));
                }
            }
        );
        
        // Set venue in overlay
        venueOverlay.setVenue(venue);
        
        // Update top UI elements
        setTopForVenue();
    }

    public void onProfilePicDownloaded() {
        // Refresh data in user overlays
        this.userOverlay.refresh();
        
        // Refresh venue overlay
        this.venueOverlay.refresh();
        
        if (slActivity.getCurrentStage() == ActivityStage.FRIEND_VIEW) {
            // Set image for user
            final Bitmap userBitmap = picRunner.getImage(friendUser.getPic(), true);
            
            // Update top user picture
            Util.uiHandler.post(
                new Runnable() {
                    public void run() {
                        if (userBitmap != null) {
                            pic.setImageBitmap(userBitmap);
                        }
                        pic.invalidate();
                    }
                }
            );
        } else if (slActivity.getCurrentStage() == ActivityStage.VENUE_VIEW) {
            // Set image for venue
            final Bitmap venueBitmap = picRunner.getImage(venue.getIcon(), true);
            
            // Update top user picture
            Util.uiHandler.post(
                new Runnable() {
                    public void run() {
                        if (venueBitmap != null) {
                            pic.setImageBitmap(venueBitmap);
                        }
                        pic.invalidate();
                    }
                }
            );
        }
    }

    public void onClick(View v) {
        slActivity.showVenueList(center);
    }

    public void onBackPressed() {
        if (slActivity.getCurrentStage() == ActivityStage.FRIEND_VIEW) {
            slActivity.setCurrentStage(ActivityStage.FRIEND_LIST);
            
            slActivity.getViewFlipper().showPrevious();
        } else {
            // Venue set so go back to venue list
            slActivity.setCurrentStage(ActivityStage.VENUE_LIST);
            
            slActivity.getViewFlipper().showNext();
            
            venue = null;
            venueOverlay.clearVenue();
            
            // Set the values for the UI elements at the top of the view
            setTopForUser();
        }
    }
}
