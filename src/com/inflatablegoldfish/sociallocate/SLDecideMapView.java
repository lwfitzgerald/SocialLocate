package com.inflatablegoldfish.sociallocate;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;

public class SLDecideMapView extends SLBaseMapView {
    private Button acceptButton;
    private Button rejectButton;
    
    public SLDecideMapView(Context context) {
        super(context);
    }
    
    public SLDecideMapView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    
    public SLDecideMapView(Context context, AttributeSet attrs, int defStyle) {
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
        
        // Update the friend overlay
        this.userOverlay.updateFriendUser(user);
        
        // Set the values for the UI elements at the top of the view
        setTopForUser();
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
