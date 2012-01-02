package com.inflatablegoldfish.sociallocate;

import java.text.DecimalFormat;
import java.util.List;

import com.foound.widget.AmazingAdapter;

import android.content.Context;
import android.location.Location;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public class FriendListAdapter extends AmazingAdapter {
    private List<User> friends;
    private Object friendLock;
    
    private ProfilePicRunner picRunner = new ProfilePicRunner(this);
    
    private LayoutInflater mInflater;
    private CharSequence[] sectionTitles;
    
    private int youSectionStart = 0;
    private int nearSectionStart = 1;
    private int farSectionStart = 0;
    
    private static final int NEAR_DISTANCE = 1000;
    
    public FriendListAdapter(Context context) {
        friends = null;
        friendLock = new Object();
        
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        sectionTitles = new CharSequence[] {
            context.getText(R.string.you_section_title),
            context.getText(R.string.near_section_title),
            context.getText(R.string.far_section_title)
        };
    }
    
    public FriendListAdapter(Context context, List<User> friends) {
        this(context);
        
        this.friends = friends;
    }
    
    public boolean isEmpty() {
        if (friends == null) {
            /*
             * Whilst we're loading we need to
             * show the loading indicator,
             * not the empty view
             */
            return false;
        }
       
        return getCount() == 0;
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
    
    public void updateUsers(List<User> users) {
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
                            User.sortByDistance(friends.subList(1, friends.size()));
                            
                            // Recalculate the sections
                            recalculateSections();
                            
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
        if (displaySectionHeader) {
            view.findViewById(R.id.list_header).setVisibility(View.VISIBLE);
            TextView lSectionTitle = (TextView) view.findViewById(R.id.list_header);
            lSectionTitle.setText((CharSequence) getSections()[getSectionForPosition(position)]);
        } else {
            view.findViewById(R.id.list_header).setVisibility(View.GONE);
        }
    }

    @Override
    public View getAmazingView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder = null;
        
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.friend_item, null);
            holder = new ViewHolder();
            holder.picView = (ImageView) convertView.findViewById(R.id.profile_pic);
            holder.nameView = (TextView) convertView.findViewById(R.id.name);
            holder.distanceView = (TextView) convertView.findViewById(R.id.distance);
            convertView.setTag(holder);
        }
        
        holder = (ViewHolder) convertView.getTag();
        
        // Get friend for this view
        User friend = friends.get(position);
        
        // Attempt to get photo from ready images or issue request
        holder.picView.setImageBitmap(picRunner.getImage(friend.getId(), friend.getPic()));
        
        // Set name
        holder.nameView.setText(friend.getName());
        
        // Calculate and set distance
        if (friend.getDistance() != null) {
            double distance = friend.getDistance().intValue();
            
            // Show in kilometers if greater than 100m
            if (distance >= 100) {
                DecimalFormat formatter = new DecimalFormat("0.0");
                
                holder.distanceView.setText(formatter.format(distance / 1000) + "km");
            } else {
                holder.distanceView.setText(distance + "m");
            }
        }
        
        return convertView;
    }
    
    private void recalculateSections() {
        farSectionStart = 0;
        
        for (int i=1; i < friends.size(); i++) {
            if (friends.get(i).getDistance() > NEAR_DISTANCE) {
                farSectionStart = i;
                break;
            }
        }
    }

    @Override
    public void configurePinnedHeader(View header, int position, int alpha) {
        TextView lSectionHeader = (TextView)header;
        
        lSectionHeader.setText((CharSequence) getSections()[getSectionForPosition(position)]);
    }

    @Override
    public int getPositionForSection(int section) {
        if (section == 0) {
            return youSectionStart;
        } else if (section == 1) {
            return nearSectionStart;
        } else {
            return farSectionStart;
        }
    }

    @Override
    public int getSectionForPosition(int position) {
        if (position == 0) {
            return 0;
        }
        
        if (position >= farSectionStart) {
            return 2;
        } else {
            return 1;
        }
    }

    @Override
    public Object[] getSections() {
        return sectionTitles;
    }
    
    private static class ViewHolder {
        public ImageView picView;
        public TextView nameView;
        public TextView distanceView;
    }
}
