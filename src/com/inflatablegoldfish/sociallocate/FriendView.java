package com.inflatablegoldfish.sociallocate;

import java.util.List;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.OverlayItem;
import com.inflatablegoldfish.sociallocate.ProfilePicRunner.ProfilePicRunnerListener;
import com.inflatablegoldfish.sociallocate.SLActivity.LocationUpdateListener;
import com.inflatablegoldfish.sociallocate.SLActivity.SLUpdateListener;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
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
    
    private volatile User ownUser = null;
    private volatile User friendUser = null;
    
    private ProfilePicRunner picRunner;
    
    private ImageView pic = null;
    private TextView name;
    private TextView lastUpdated;
    private TextView distance;
    
    private MapView mapView;
    private MapOverlay mapOverlay;
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
        
        mapOverlay = new MapOverlay();
        mapView.getOverlays().add(mapOverlay);
    }
    
//    private class MapOverlay extends Overlay {
//        @Override
//        public void draw(Canvas canvas, MapView mapView, boolean shadow) {
//            super.draw(canvas, mapView, shadow);
//
//            Point ownUserPoint = null;
//            if (ownUserGeoPoint != null) {
//                ownUserPoint = getAndDrawPoint(ownUser, ownUserGeoPoint, canvas, true);
//            }
//            
//            Point userPoint = null;
//            if (userGeoPoint != null) {
//                userPoint = getAndDrawPoint(user, userGeoPoint, canvas, false);
//            }
//            
//            if (ownUserPoint != null && userPoint != null) {
//                Paint paint = new Paint();
//                paint.setDither(true);
//                paint.setColor(Color.BLACK);
//                paint.setStyle(Paint.Style.FILL_AND_STROKE);
//                paint.setStrokeJoin(Paint.Join.ROUND);
//                paint.setStrokeCap(Paint.Cap.ROUND);
//                paint.setStrokeWidth(4);
//                
//                Path path = new Path();
//                
//                path.moveTo(userPoint.x, userPoint.y);
//                path.lineTo(ownUserPoint.x, ownUserPoint.y);
//                
//                canvas.drawPath(path, paint);   
//            }
//        }
//        
//        private Point getAndDrawPoint(User user, GeoPoint geoPoint, Canvas canvas, boolean ownUser) {
//            Point point = new Point();
//            mapView.getProjection().toPixels(geoPoint, point);
//            
//            Bitmap croppedBitmap;
//            
//            if (ownUser) {
//                croppedBitmap = ownCroppedBitmap;
//            } else {
//                croppedBitmap = userCroppedBitmap;
//            }
//            
//            if (croppedBitmap == null) {
//                Bitmap image = picRunner.getImage(user.getId(), user.getPic());
//                
//                if (image != null) {
//                    croppedBitmap = Util.cropBitmap(image, 100, 100);
//                    
//                    canvas.drawBitmap(croppedBitmap,
//                            point.x, point.y - 100, null);
//                    
//                    if (ownUser) {
//                        ownCroppedBitmap = croppedBitmap;
//                    } else {
//                        userCroppedBitmap = croppedBitmap;
//                    }
//                }
//            } else {
//                canvas.drawBitmap(croppedBitmap,
//                        point.x, point.y - 100, null);
//            }
//            
//            return point;
//        }
//    }
    
    private class MapOverlay extends ItemizedOverlay<UserItem> {
        /*
         * userItems[0] = Own User
         * userItems[1] = Friend User
         */
        private UserItem[] userItems = new UserItem[] {null, null};
        
        public MapOverlay() {
            super(null);
        }
        
        public void setOwnUser(Location currentLocation) {
            Log.d("SocialLocate", "Own user set / updated");
            userItems[0] = new UserItem(ownUser, Util.getGeoPoint(currentLocation), true);
            populate();
        }
        
        public void updateFriendUser(User friendUser) {
            Log.d("SocialLocate", "Friend user set / updated");
            userItems[1] = new UserItem(friendUser, Util.getGeoPoint(friendUser.getLocation()), false);
            populate();
        }
        
        @Override
        public void draw(Canvas canvas, MapView mapView, boolean shadow) {
            super.draw(canvas, mapView, false);
        }
        
        public void refresh() {
            super.populate();
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
            Log.d("SocialLocate", user.getName() + " overlay (re)created");
        }
        
        @Override
        public Drawable getMarker(int stateBitset) {
            if (drawableImage == null) {
                Bitmap image;
                if (isOwnUser) {
                    image = picRunner.getImage(ownUser.getId(), ownUser.getPic());
                } else {
                    image = picRunner.getImage(friendUser.getId(), friendUser.getPic());
                }
                
                if (image != null) {
                    drawableImage = new BitmapDrawable(slActivity.getResources(), image);
                    drawableImage.setBounds(0, 0, image.getWidth(), image.getHeight());
                    return drawableImage;
                } else {
                    Bitmap emptyBitmap = Bitmap.createBitmap(0, 0, Bitmap.Config.ARGB_8888);
                    return new BitmapDrawable(slActivity.getResources(), emptyBitmap);
                }
            }
            
            return drawableImage;
        }
    }
    
    public void setOwnUser(final User ownUser) {
        this.ownUser = ownUser;
        
        if (slActivity.getCurrentLocation() != null) {
            // Create overlay item
            mapOverlay.setOwnUser(slActivity.getCurrentLocation());
        }
    }
    
    public void updateUser(final User user) {
        this.friendUser = user;
        
        this.mapOverlay.updateFriendUser(user);
        
        final Bitmap bitmap = picRunner.getImage(user.getId(), user.getPic());
        
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
            
            Util.uiHandler.post(
                new Runnable() {
                    public void run() {
                        mapController.zoomToSpan(mapOverlay.getLatSpanE6(), mapOverlay.getLonSpanE6());
                    }
                }
            );
        } else {
            // No location so just center on friend
            centerPoint = Util.getGeoPoint(user.getLocation());
            mapController.setZoom(12);
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
        if (ownUser != null) {
            // Update own user geopoint
            mapOverlay.setOwnUser(newLocation);
        }
        
        if (friendUser != null) {
            // Have current location so center on midpoint
            final Location center = Util.getCenter(
                new Location[] {
                    newLocation,
                    friendUser.getLocation()
                }
            );
            
            Log.d("SocialLocate", "Animating to point due to location update");
            
            Util.uiHandler.post(
                new Runnable() {
                    public void run() {
                        mapController.zoomToSpan(mapOverlay.getLatSpanE6(), mapOverlay.getLonSpanE6());
                        mapController.animateTo(Util.getGeoPoint(center));
                    }
                }
            );
        }
    }
    
    public void onSLUpdate(List<User> friends) {
        // Will refresh UI
        if (friendUser != null) {
            updateUser(friendUser);
        }
    }

    public void onProfilePicDownloaded() {
        // Refresh data in OverlayItems
        this.mapOverlay.refresh();
        
        if (friendUser != null) {
            // Set image for user
            final Bitmap userBitmap = picRunner.getImage(friendUser.getId(), friendUser.getPic());
            
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
        }
    }
}
