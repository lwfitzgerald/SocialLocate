package com.inflatablegoldfish.sociallocate;

import java.util.List;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.inflatablegoldfish.sociallocate.ProfilePicRunner.ProfilePicRunnerListener;
import com.inflatablegoldfish.sociallocate.SLActivity.LocationUpdateListener;
import com.inflatablegoldfish.sociallocate.SLActivity.SLUpdateListener;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Point;
import android.location.Location;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class FriendView extends RelativeLayout implements
        ProfilePicRunnerListener, LocationUpdateListener, SLUpdateListener {
    
    private SLActivity slActivity;
    private User user = null;
    private GeoPoint userGeoPoint;
    
    private ProfilePicRunner picRunner;
    
    private ImageView pic = null;
    private TextView name;
    private TextView lastUpdated;
    private TextView distance;
    
    private MapView mapView;
    private MapController mapController;
    
    public FriendView(Context context) {
        super(context);
    }
    
    public FriendView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    
    public FriendView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
    
    @Override
    public void onFinishInflate() {
        super.onFinishInflate();
        
        pic = (ImageView) findViewById(R.id.profile_pic);
        name = (TextView) findViewById(R.id.name);
        lastUpdated = (TextView) findViewById(R.id.last_updated);
        distance = (TextView) findViewById(R.id.distance);
        
        // Set up the map controller
        mapView = (MapView) findViewById(R.id.mapview);
        mapController = mapView.getController();
        mapController.setZoom(12);
    }
    
    public void setUp(SLActivity slActivity, ProfilePicRunner picRunner) {
        this.slActivity = slActivity;
        this.picRunner = picRunner;
        picRunner.addListener(this);
        
        //mapView.getOverlays().add(new MapOverlay());
    }
    
    private class MapOverlay extends com.google.android.maps.Overlay {
        @Override
        public boolean draw(Canvas canvas, MapView mapView, boolean shadow,
                long when) {
            super.draw(canvas, mapView, shadow);

            // ---translate the GeoPoint to screen pixels---
            Point screenPts = new Point();
            mapView.getProjection().toPixels(userGeoPoint, screenPts);

            // ---add the marker---
            Bitmap image = picRunner.getImage(user.getId(), user.getPic());
            
            if (image != null) {
                canvas.drawBitmap(image,
                        screenPts.x, screenPts.y - 100, null);
            }
            
            return true;
        }
    }
    
    public void updateUser(final User user) {
        this.user = user;
        this.userGeoPoint = Util.getGeoPoint(user.getLocation());
        
        Util.uiHandler.post(
            new Runnable() {
                public void run() {
                    pic.setImageBitmap(picRunner.getImage(user.getId(), user.getPic()));
                    
                    name.setText(user.getName());
                    lastUpdated.setText(user.getPrettyLastUpdated());
                    
                    if (user.getDistance() != null) {
                        distance.setText(user.getPrettyDistance());
                    }
                }
            }
        );
        
        GeoPoint centerPoint;
        
        if (slActivity.getCurrentLocation() != null) {
            // Have current location so center on midpoint
            Location center = Util.getCenter(
                new Location[] {
                    slActivity.getCurrentLocation(),
                    user.getLocation()
                }
            );
            
            // Set map center
            centerPoint = Util.getGeoPoint(center);
        } else {
            // No location so just center on friend
            centerPoint = Util.getGeoPoint(user.getLocation());
        }
        
        Log.d("SocialLocate", "Animating to point due to fetch update");
        
        try {
            mapController.animateTo(centerPoint);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void onLocationUpdate(Location newLocation) {
        // Center map
        if (user != null) {
            // Have current location so center on midpoint
            Location center = Util.getCenter(
                new Location[] {
                    newLocation,
                    user.getLocation()
                }
            );
            
            Log.d("SocialLocate", "Animating to point due to location update");
            
            mapController.animateTo(Util.getGeoPoint(center));
        }
    }
    
    public void onSLUpdate(List<User> friends) {
        // Will refresh UI
        if (user != null) {
            updateUser(user);
        }
    }

    public void onProfilePicDownloaded() {
        Util.uiHandler.post(
            new Runnable() {
                public void run() {
                    if (user != null) {
                        Bitmap bitmap = picRunner.getImage(user.getId(), user.getPic());
                        pic.setImageBitmap(bitmap);
                        pic.invalidate();
                    }
                }
            }
        );
    }
}
