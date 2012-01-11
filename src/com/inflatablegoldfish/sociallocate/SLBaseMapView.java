package com.inflatablegoldfish.sociallocate;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.OverlayItem;
import com.inflatablegoldfish.sociallocate.PicRunner.PicRunnerListener;
import com.inflatablegoldfish.sociallocate.SLBaseActivity.BackButtonListener;
import com.inflatablegoldfish.sociallocate.SLBaseActivity.LocationUpdateListener;
import com.inflatablegoldfish.sociallocate.foursquare.Venue;

public abstract class SLBaseMapView extends RelativeLayout implements
        PicRunnerListener, LocationUpdateListener, OnClickListener,
        BackButtonListener {

    protected SLBaseActivity activity;
    
    protected volatile User ownUser = null;
    protected volatile User friendUser = null;
    
    protected volatile Venue venue;
    
    protected PicRunner picRunner;
    
    protected ImageView friendPic = null;
    protected TextView friendName;
    protected TextView lastUpdated;
    protected TextView friendDistance;
    
    protected View venueBar;
    protected ImageView venueIcon = null;
    protected TextView venueName;
    protected TextView venueDistance;
    
    protected MapView mapView;
    protected MapController mapController;
    protected MapOverlay mapOverlay;
    
    protected volatile boolean initiallyCentered;
    
    protected SLBaseMapView(Context context) {
        super(context);
    }
    
    protected SLBaseMapView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    
    protected SLBaseMapView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
    
    @Override
    public void onFinishInflate() {
        super.onFinishInflate();
        
        friendPic = (ImageView) findViewById(R.id.profile_pic);
        friendName = (TextView) findViewById(R.id.name);
        lastUpdated = (TextView) findViewById(R.id.last_updated);
        friendDistance = (TextView) findViewById(R.id.distance);
        
        venueBar = findViewById(R.id.venue_bar);
        venueIcon = (ImageView) findViewById(R.id.venue_pic);
        venueName = (TextView) findViewById(R.id.venue_name);
        venueDistance = (TextView) findViewById(R.id.venue_distance);
        
        // Set up the map controller
        mapView = (MapView) findViewById(R.id.google_map_view);
        
        /*
         * Disable software rendering as android has no
         * fallback when paths get too large to fit into
         * textures
         */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            mapView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }
        
        mapController = mapView.getController();
        mapController.setZoom(12);
    }
    
    public void setUp(SLBaseActivity activity, PicRunner picRunner) {
        this.activity = activity;
        this.picRunner = picRunner;
        picRunner.addListener(this);
        
        mapOverlay = new MapOverlay();
        mapView.getOverlays().add(mapOverlay);
    }
    
    protected class MapOverlay extends ItemizedOverlay<OverlayItem> {
        /*
         * userItems[0] = Own User
         * userItems[1] = Friend User
         */
        protected UserItem[] userItems = new UserItem[] {null, null};
        protected Paint paint = null;
        
        protected volatile VenueItem venueItem = null;
        
        public MapOverlay() {
            super(null);
            
            if (paint == null) {
                paint = new Paint();
                paint.setDither(true);
                paint.setColor(Color.BLACK);
                paint.setStyle(Paint.Style.FILL_AND_STROKE);
                paint.setStrokeJoin(Paint.Join.ROUND);
                paint.setStrokeCap(Paint.Cap.ROUND);
                paint.setStrokeWidth(4);
            }
        }
        
        public void setVenue(Venue venue) {
            venueItem = new VenueItem(venue);
            refresh();
        }
        
        public void clearVenue() {
            venueItem = null;
            refresh();
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
            if (venueItem != null) {
                drawItemPoint(venueItem, canvas);
            }
            
            if (userItems[0] != null) {
                drawItemPoint(userItems[0], canvas);
            }
            
            if (userItems[1] != null) {
                drawItemPoint(userItems[1], canvas);
            }
            
            super.draw(canvas, mapView, false);
        }
        
        private void drawItemPoint(OverlayItem item, Canvas canvas) {
            Point point = new Point();
            mapView.getProjection().toPixels(item.getPoint(), point);
            
            canvas.drawCircle(point.x, point.y, 3, paint);
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
        protected OverlayItem createItem(int i) {
            switch (size()) {
            case 1:
                if (userItems[0] != null) {
                    return userItems[0];
                } else if (userItems[1] != null) {
                    return userItems[1];
                } else { // venueItem != null
                    return venueItem;
                }
            case 2:
                if (i == 0) {
                    if (userItems[0] != null) {
                        return userItems[0];
                    } else if (userItems[1] != null) {
                        return userItems[1];
                    } else {
                        return venueItem;
                    }
                } else if (i == 1) {
                    if (userItems[1] != null) {
                        return userItems[1];
                    } else {
                        return venueItem;
                    }
                }
            case 3:
                if (i <= 1) {
                    return userItems[i];
                } else {
                    return venueItem;
                }
            }
            
            return null;
        }

        @Override
        public int size() {
            return (userItems[0] != null ? 1 : 0)
                    + (userItems[1] != null ? 1 : 0)
                    + (venueItem != null ? 1 : 0);
        }
    }
    
    protected class UserItem extends OverlayItem {
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
                    drawableImage = new BitmapDrawable(activity.getResources(), image);
                    drawableImage.setBounds(0, 0, image.getWidth(), image.getHeight());
                    return drawableImage;
                } else {
                    Bitmap emptyBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ALPHA_8);
                    return new BitmapDrawable(activity.getResources(), emptyBitmap);
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
        
        if (activity.getCurrentLocation() != null) {
            // Create overlay item
            mapOverlay.setOwnUser(activity.getCurrentLocation());
        }
    }
    
    /**
     * Update the friend user
     * 
     * Called from UI thread
     * @param user New friend user
     * @param forceCenter If true, force the map to re-center
     */
    public abstract void updateUser(final User user, boolean forceCenter);
    
    /**
     * Set the values for the top view elements
     * to those for the Friend view
     * 
     * Called from UI thread
     */
    protected void setTopForUser() {
        final Bitmap bitmap = picRunner.getImage(friendUser.getPic(), true);
        
        if (bitmap != null) {
            friendPic.setImageBitmap(bitmap);
        }
        
        friendName.setText(friendUser.getName());
        lastUpdated.setText(friendUser.getPrettyLastUpdated());
        
        if (friendUser.getDistance() != null) {
            friendDistance.setText(friendUser.getPrettyDistance());
        }
        
        friendPic.invalidate();
        friendName.invalidate();
        lastUpdated.invalidate();
        friendDistance.invalidate();
    }

    /**
     * Set the values for the top view elements
     * to those for the Venue view
     * 
     * Called from UI thread
     */
    protected void setTopForVenue() {
        final Bitmap bitmap = picRunner.getImage(venue.getIcon(), false);
        
        if (bitmap != null) {
            venueIcon.setImageBitmap(bitmap);
        }
        
        venueName.setText(venue.getName());
        
        if (venue.getDistance() != null) {
            venueDistance.setText(venue.getPrettyDistance());
        }
        
        venueIcon.invalidate();
        venueName.invalidate();
        venueDistance.invalidate();
    }
    
    protected class VenueItem extends OverlayItem {
        protected BitmapDrawable drawableImage = null;
        
        public VenueItem(Venue venue) {
            super(Util.getGeoPoint(venue.getLocation()), venue.getName(), "");
        }
        
        @Override
        public Drawable getMarker(int stateBitset) {
            if (drawableImage == null) {
                Bitmap image = picRunner.getImage(venue.getIcon(), false);
                
                if (image != null) {
                    drawableImage = new BitmapDrawable(activity.getResources(), image);
                    drawableImage.setBounds(0, 0, Math.min(image.getWidth(), 100), Math.min(image.getHeight(), 100));
                    return drawableImage;
                } else {
                    Bitmap emptyBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ALPHA_8);
                    return new BitmapDrawable(activity.getResources(), emptyBitmap);
                }
            }
            
            return drawableImage;
        }
    }
    
    /**
     * Called to set the venue chosen from the
     * venue list when switching from the venue list
     * 
     * Called from UI thread
     * @param venue Venue chosen
     */
    public void setVenue(Venue venue) {
        this.venue = venue;
        
        // Set venue in overlay
        mapOverlay.setVenue(venue);
        
        // Animate to it
        mapController.animateTo(Util.getGeoPoint(SLBaseMapView.this.venue.getLocation()));
        mapController.zoomToSpan(mapOverlay.getLatSpanE6(), mapOverlay.getLonSpanE6());
        
        // Show venue bar
        venueBar.setVisibility(View.VISIBLE);
        
        // Update top UI elements
        setTopForVenue();
    }

    public void onLocationUpdate(Location newLocation) {
        if (ownUser != null) {
            // Update own user geopoint
            mapOverlay.setOwnUser(newLocation);
        }
    }

    public void onProfilePicDownloaded() {
        // Refresh data in user overlays
        this.mapOverlay.refresh();
    }
    
    public abstract void onClick(View view);

    public abstract void onBackPressed();
}
