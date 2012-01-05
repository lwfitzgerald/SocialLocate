package com.inflatablegoldfish.sociallocate;

import com.inflatablegoldfish.sociallocate.ProfilePicRunner.ProfilePicRunnerListener;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class FriendView extends RelativeLayout implements ProfilePicRunnerListener {
    private User user;
    
    private ProfilePicRunner picRunner;
    
    private ImageView pic = null;
    private TextView name;
    private TextView lastUpdated;
    private TextView distance;
    
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
    }
    
    public void setPicRunner(ProfilePicRunner picRunner) {
        this.picRunner = picRunner;
    }
    
    public void updateUser(final User user) {
        Util.uiHandler.post(
            new Runnable() {
                public void run() {
                    pic.setImageBitmap(picRunner.getImage(user.getId(), user.getPic()));
                    
                    name.setText(user.getName());
                    lastUpdated.setText(user.getPrettyLastUpdated());
                    distance.setText(user.getPrettyDistance());
                }
            }
        );
    }

    public void onProfilePicDownloaded() {
        Util.uiHandler.post(
            new Runnable() {
                public void run() {
                    pic.setImageBitmap(picRunner.getImage(user.getId(), user.getPic()));
                    pic.invalidate();
                }
            }
        );
    }
}
