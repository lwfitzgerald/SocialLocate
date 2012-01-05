package com.inflatablegoldfish.sociallocate;

import java.util.List;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.inflatablegoldfish.sociallocate.ProfilePicRunner.ProfilePicRunnerListener;
import com.inflatablegoldfish.sociallocate.SLActivity.LocationUpdateListener;
import com.inflatablegoldfish.sociallocate.SLActivity.SLUpdateListener;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.location.Location;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class FriendView extends RelativeLayout implements
        ProfilePicRunnerListener, LocationUpdateListener, SLUpdateListener {
    
    private SLActivity slActivity;
    
    private User ownUser = null;
    private GeoPoint ownUserGeoPoint;
    private volatile Bitmap ownCroppedBitmap = null;
    
    private User user = null;
    private GeoPoint userGeoPoint;
    private volatile Bitmap userCroppedBitmap = null;
    
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
        /*
         * Disable software rendering as android has no
         * fallback when paths get too large to fit into
         * textures
         */
        mapView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        
        mapController = mapView.getController();
        mapController.setZoom(12);
    }
    
    public void setUp(SLActivity slActivity, ProfilePicRunner picRunner) {
        this.slActivity = slActivity;
        this.picRunner = picRunner;
        picRunner.addListener(this);
        
        mapView.getOverlays().add(new MapOverlay());
    }
    
    private class MapOverlay extends Overlay {
        @Override
        public void draw(Canvas canvas, MapView mapView, boolean shadow) {
            super.draw(canvas, mapView, shadow);

            Point ownUserPoint = null;
            if (ownUserGeoPoint != null) {
                ownUserPoint = getAndDrawPoint(ownUser, ownUserGeoPoint, canvas, true);
            }
            
            Point userPoint = null;
            if (userGeoPoint != null) {
                userPoint = getAndDrawPoint(user, userGeoPoint, canvas, false);
            }
            
            if (ownUserPoint != null && userPoint != null) {
                Paint paint = new Paint();
                paint.setDither(true);
                paint.setColor(Color.BLACK);
                paint.setStyle(Paint.Style.FILL_AND_STROKE);
                paint.setStrokeJoin(Paint.Join.ROUND);
                paint.setStrokeCap(Paint.Cap.ROUND);
                paint.setStrokeWidth(4);
                
                Path path = new Path();
                
                path.moveTo(userPoint.x, userPoint.y);
                path.lineTo(ownUserPoint.x, ownUserPoint.y);
                
                canvas.drawPath(path, paint);   
            }
        }
        
        private Point getAndDrawPoint(User user, GeoPoint geoPoint, Canvas canvas, boolean ownUser) {
            Point point = new Point();
            mapView.getProjection().toPixels(geoPoint, point);
            
            Bitmap croppedBitmap;
            
            if (ownUser) {
                croppedBitmap = ownCroppedBitmap;
            } else {
                croppedBitmap = userCroppedBitmap;
            }
            
            if (croppedBitmap == null) {
                Bitmap image = picRunner.getImage(user.getId(), user.getPic());
                
                if (image != null) {
                    croppedBitmap = Util.cropBitmap(image, 100, 100);
                    
                    canvas.drawBitmap(croppedBitmap,
                            point.x, point.y - 100, null);
                    
                    if (ownUser) {
                        ownCroppedBitmap = croppedBitmap;
                    } else {
                        userCroppedBitmap = croppedBitmap;
                    }
                }
            } else {
                canvas.drawBitmap(croppedBitmap,
                        point.x, point.y - 100, null);
            }
            
            return point;
        }
    }
    
    public void setOwnUser(final User ownUser) {
        this.ownUser = ownUser;
        
        if (slActivity.getCurrentLocation() != null) {
            this.ownUserGeoPoint = Util.getGeoPoint(slActivity.getCurrentLocation());
        }
    }
    
    public void updateUser(final User user) {
        this.user = user;
        this.userGeoPoint = Util.getGeoPoint(user.getLocation());
        this.userCroppedBitmap = null;
        
        final Bitmap bitmap = picRunner.getImage(user.getId(), user.getPic());
        if (bitmap != null) {
            userCroppedBitmap = Util.cropBitmap(bitmap, 100, 100);
        }
        
        Util.uiHandler.post(
            new Runnable() {
                public void run() {
                    if (bitmap != null) {
                        pic.setImageBitmap(bitmap);
                    }
                    
                    name.setText(user.getName());
                    lastUpdated.setText(user.getPrettyLastUpdated());
                    
                    if (user.getDistance() != null) {
                        distance.setText(user.getPrettyDistance());
                    }
                }
            }
        );
        
        final GeoPoint centerPoint;
        
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
        
        Util.uiHandler.post(
            new Runnable() {
                public void run() {
                    mapController.animateTo(centerPoint);
                }
            }
        );
    }
    
    public void onLocationUpdate(Location newLocation) {
        // Update own user geopoint
        this.ownUserGeoPoint = Util.getGeoPoint(slActivity.getCurrentLocation());
        
        // Center map
        if (user != null) {
            // Have current location so center on midpoint
            final Location center = Util.getCenter(
                new Location[] {
                    newLocation,
                    user.getLocation()
                }
            );
            
            Log.d("SocialLocate", "Animating to point due to location update");
            
            Util.uiHandler.post(
                new Runnable() {
                    public void run() {
                        mapController.animateTo(Util.getGeoPoint(center));
                    }
                }
            );
        }
    }
    
    public void onSLUpdate(List<User> friends) {
        // Will refresh UI
        if (user != null) {
            updateUser(user);
        }
    }

    public void onProfilePicDownloaded() {
        if (ownUser != null && ownCroppedBitmap == null) {
            // Attempt to set cropped image for own user
            final Bitmap ownBitmap = picRunner.getImage(ownUser.getId(), ownUser.getPic());
            if (ownBitmap != null) {
                ownCroppedBitmap = Util.cropBitmap(ownBitmap, 100, 100);
            }
        }
        
        if (user != null) {
            final Bitmap userBitmap = picRunner.getImage(user.getId(), user.getPic());
            if (userBitmap != null) {
                userCroppedBitmap = Util.cropBitmap(userBitmap, 100, 100);
            }
            
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
        }
    }
}
