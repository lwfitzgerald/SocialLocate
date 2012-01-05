package com.inflatablegoldfish.sociallocate;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.foound.widget.AmazingAdapter;
import com.inflatablegoldfish.sociallocate.ProfilePicRunner.ProfilePicRunnerListener;

import android.content.Context;
import android.location.Location;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public class FriendListAdapter extends AmazingAdapter implements ProfilePicRunnerListener {
    private volatile List<User> friends;
    private Object friendLock;
    
    private ProfilePicRunner picRunner;
    
    private LayoutInflater mInflater;
    private CharSequence[] sectionTitles;
    
    private static final int YOU_SECTION_NO = 0;
    private static final int NEAR_SECTION_NO = 1;
    private static final int FAR_SECTION_NO = 2;
    private static final int FRIENDS_SECTION_NO = 3;
    
    private int youSectionStart = 0;
    private int friendsSectionStart = 1;
    private int nearSectionStart = 1;
    private int farSectionStart = 0;
    
    private static final int NEAR_DISTANCE = 1000;
    
    public FriendListAdapter(Context context, ProfilePicRunner picRunner) {
        friends = null;
        friendLock = new Object();
        
        this.picRunner = picRunner;
        picRunner.addListener(this);
        
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        sectionTitles = new CharSequence[] {
            context.getText(R.string.you_section_title),
            context.getText(R.string.near_section_title),
            context.getText(R.string.far_section_title),
            context.getText(R.string.friends_section_title),
        };
    }
    
    public FriendListAdapter(Context context, ProfilePicRunner picRunner, List<User> friends) {
        this(context, picRunner);
        
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
        synchronized (friendLock) {
            return friends.get(position);
        }
    }

    public long getItemId(int position) {
        return position;
    }
    
    public void onProfilePicDownloaded() {
        // Called when profile pic download completes
        notifyDataSetChanged();
    }
    
    public void updateUsers(List<User> users) {
        if (friends == null) {
            this.friends = users;
        } else {
            synchronized (friendLock) {
                List<User> newUserList = new ArrayList<User>(friends.size());
            
                // Add own user
                newUserList.add(friends.get(0));
                
                for (User updateUser : users) {
                    Iterator<User> itr = friends.iterator();
                    
                    while (itr.hasNext()) {
                        User oldUser = itr.next();
                        
                        if (oldUser.getId() == updateUser.getId()) {
                            // Update old user using update user
                            updateUser.updateFromUser(updateUser);
                            
                            // Add to new list
                            newUserList.add(oldUser);
                            
                            // Remove from old list
                            itr.remove();
                            
                            break;
                        }
                    }
                }
                
                friends = newUserList;
            }
        }
        
        Util.uiHandler.post(
            new Runnable() {
                public void run() {
                    notifyDataSetChanged();
                }
            }
        );
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
                        synchronized (friendLock) {
                            // Recalculate distances to friends
                            User.calculateDistances(friends, currentLocation);
                            
                            // Re-sort friends (exclude own user) by distance
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
            holder.pic = (ImageView) convertView.findViewById(R.id.profile_pic);
            holder.name = (TextView) convertView.findViewById(R.id.name);
            holder.distance = (TextView) convertView.findViewById(R.id.distance);
            holder.lastUpdated = (TextView) convertView.findViewById(R.id.last_updated);
            holder.separatorBar = convertView.findViewById(R.id.list_separator_bar);
            convertView.setTag(holder);
        }
        
        holder = (ViewHolder) convertView.getTag();
        
        // Get friend for this view
        User friend;
        synchronized (friendLock) {
             friend = friends.get(position);
        }
        
        // Attempt to get photo from ready images or issue request
        holder.pic.setImageBitmap(picRunner.getImage(friend.getId(), friend.getPic()));
        
        // Set name
        holder.name.setText(friend.getName());
        
        
        if (position == 0) {
            // Own user so mark as not clickable
            convertView.setClickable(false);
        } else {
            // Set last updated if not own user
            holder.lastUpdated.setText(friend.getPrettyLastUpdated());
        }
        
        // Calculate and set distance
        if (friend.getDistance() != null) {
            holder.distance.setText(friend.getPrettyDistance());
        }
        
        // Show / Hide section separator part
        holder.separatorBar
                .setVisibility(showSeparator(position) ? View.VISIBLE : View.GONE);
        
        return convertView;
    }
    
    /**
     * Return whether or not the position should
     * show it's separator part (at the bottom)
     * @param position Position
     * @return True if should be shown
     */
    private boolean showSeparator(int position) {
        // No friends means no separators
        if (getCount() == 0) {
            return false;
        }
        
        // If you have friends, you need a separator after "YOU"
        if (position == 0) {
            return true;
        }
        
        /*
         * No distances there can only be "FRIENDS"
         * and the separator is already set above
         */
        if (!distancesAvailable()) {
            return false;
        }
        
        // Add separator for element before far section
        if (position == farSectionStart - 1) {
            return true;
        }
        
        return false;
    }
    
    private void recalculateSections() {
        farSectionStart = 0;
        
        synchronized (friendLock) {
            for (int i=1; i < friends.size(); i++) {
                if (friends.get(i).getDistance() > NEAR_DISTANCE) {
                    farSectionStart = i;
                    break;
                }
            }
        }
    }

    @Override
    public void configurePinnedHeader(View header, int position, int alpha) {
        TextView lSectionHeader = (TextView)header.findViewById(R.id.list_header);
        
        lSectionHeader.setText((CharSequence) getSections()[getSectionForPosition(position)]);
    }

    @Override
    public int getPositionForSection(int section) {
        if (section == YOU_SECTION_NO) {
            return youSectionStart;
        } else if (section == NEAR_SECTION_NO) {
            return nearSectionStart;
        } else if (section == FAR_SECTION_NO) {
            return farSectionStart;
        } else { // section == FRIENDS_SECTION_NO
            return friendsSectionStart;
        }
    }

    /**
     * Returns if distances are available
     * @return True if distances available
     */
    private boolean distancesAvailable() {
        if (friends == null) {
            // No friends so obviously ready
            return true;
        }
        
        synchronized (friendLock) {
            if (friends.get(1) == null) {
                // No friends so obviously ready
                return true;
            }
        
            return friends.get(1).getDistance() != null;
        }
    }
    
    @Override
    public int getSectionForPosition(int position) {
        if (position == 0) {
            return YOU_SECTION_NO;
        }
        
        // No distances so just show "FRIENDS"
        if (!distancesAvailable()) {
            return FRIENDS_SECTION_NO;
        }
        
        if (position >= farSectionStart) {
            return FAR_SECTION_NO;
        } else {
            return NEAR_SECTION_NO;
        }
    }

    @Override
    public Object[] getSections() {
        return sectionTitles;
    }
    
    @Override
    public boolean areAllItemsEnabled() {
        // So we can disabled the top (own) row
        return false;
    }
    
    @Override
    public boolean isEnabled(int position) {
        // All rows enabled except own details row
        return position != 0;
    }
    
    private static class ViewHolder {
        public ImageView pic;
        public TextView name;
        public TextView distance;
        public TextView lastUpdated;
        public View separatorBar;
    }
}
