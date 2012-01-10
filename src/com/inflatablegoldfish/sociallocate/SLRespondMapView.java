package com.inflatablegoldfish.sociallocate;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;

public class SLRespondMapView extends SLBaseMapView {
    private Button acceptButton;
    private Button rejectButton;
    
    public SLRespondMapView(Context context) {
        super(context);
    }
    
    public SLRespondMapView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    
    public SLRespondMapView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
    
    @Override
    public void onFinishInflate() {
        super.onFinishInflate();
        
        // Set up buttons
        acceptButton = (Button) findViewById(R.id.accept_button);
        acceptButton.setOnClickListener(this);
        
        rejectButton = (Button) findViewById(R.id.reject_button);
        rejectButton.setOnClickListener(this);
    }

    @Override
    public void updateUser(final User user, boolean forceCenter) {
        this.friendUser = user;
        this.initiallyCentered = false;
        
        // Update the friend overlay
        this.userOverlay.updateFriendUser(user);
        
        // Fit zoom around users
        mapController.zoomToSpan(userOverlay.getLatSpanE6(), userOverlay.getLonSpanE6());
        
        // Set the values for the UI elements at the top of the view
        setTopForUser();
    }
    
    @Override
    public void onProfilePicDownloaded() {
        super.onProfilePicDownloaded();
        
        if (friendUser != null) {
            // Set image for user
            final Bitmap userBitmap = picRunner.getImage(friendUser.getPic(), true);
            
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
        }
        
        if (venue != null) {
            // Set image for venue
            final Bitmap venueBitmap = picRunner.getImage(venue.getIcon(), false);
            
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
        // TODO handle button presses
        if (view == acceptButton) {
            
        } else { // rejectButton
            
        }
    }

    @Override
    public void onBackPressed() {
        // TODO reply with reject?
        
        activity.finish();
    }
}
