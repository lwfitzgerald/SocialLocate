package com.inflatablegoldfish.sociallocate;

import java.text.DecimalFormat;
import java.util.List;

import com.foound.widget.AmazingAdapter;

import android.content.Context;
import android.location.Location;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class FriendListAdapter extends AmazingAdapter {
    private List<User> friends = null;
    private Object friendLock = new Object();
    
    private LayoutInflater mInflater;
    private CharSequence[] sectionTitles;
    
    public FriendListAdapter(Context context) {
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        sectionTitles = new CharSequence[] {
            context.getText(R.string.near_section_title),
            context.getText(R.string.far_section_title)
        };
    }
    
    public FriendListAdapter(Context context, List<User> friends) {
        this(context);
        
        this.friends = friends;
    }
    
    public int getCount() {
        if (friends == null) {
            return 0;
        }
        
        return friends.size();
    }

    public Object getItem(int position) {
        return friends.get(position);
    }

    public long getItemId(int position) {
        return position;
    }
    
    public void updateFriends(List<User> users) {
        this.friends = users;
        
        notifyDataSetChanged();
    }
    
    public void updateDistances(final Location currentLocation) {
        /*
         * Perform operations in new thread so as not to
         * hang UI thread for those with large numbers
         * of friends
         */
        
        if (friends != null) {
            new Thread(
                new Runnable() {
                    public void run() {
                        synchronized(friendLock) {
                            // Recalculate distances to friends
                            User.calculateDistances(friends, currentLocation);
                            
                            // Re-sort friends by distance
                            User.sortByDistance(friends);
                            
                            // Refresh the UI
                            Util.uiHandler.post(new Runnable() {
                                public void run() {
                                    notifyDataSetChanged();
                                }
                            });
                        }
                    }
                }
            ).start();
        }
    }

    @Override
    protected void onNextPageRequested(int page) {
        return;
    }

    @Override
    protected void bindSectionHeader(View view, int position,
            boolean displaySectionHeader) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public View getAmazingView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder = null;
        
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.friend_item, null);
            holder = new ViewHolder();
            holder.nameView = (TextView) convertView.findViewById(R.id.name);
            holder.distanceView = (TextView) convertView.findViewById(R.id.distance);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        
        User friend = friends.get(position);
        
        holder.nameView.setText(friend.getName());
        
        if (friend.getDistance() != null) {
            double distance = friend.getDistance().intValue();
            
            // Show in kilometers if greater than 100m
            if (distance >= 100) {
                DecimalFormat formatter = new DecimalFormat(".0");
                
                holder.distanceView.setText(formatter.format(distance / 1000) + "km");
            } else {
                holder.distanceView.setText(distance + "m");
            }
        }
        
        return convertView;
    }

    @Override
    public void configurePinnedHeader(View header, int position, int alpha) {}

    @Override
    public int getPositionForSection(int section) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getSectionForPosition(int position) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public Object[] getSections() {
        return sectionTitles;
    }
    
    private static class ViewHolder {
        public TextView nameView;
        public TextView distanceView;
    }
}
