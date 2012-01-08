package com.inflatablegoldfish.sociallocate;

import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.location.Location;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.foound.widget.AmazingAdapter;
import com.inflatablegoldfish.sociallocate.PicRunner.PicRunnerListener;
import com.inflatablegoldfish.sociallocate.foursquare.Venue;

public class VenueListAdapter extends AmazingAdapter implements PicRunnerListener {
    private volatile List<Venue> venues = null;
    private Object venueLock = new Object();
    
    private PicRunner picRunner;
    
    private LayoutInflater mInflater;
    
    public VenueListAdapter(Context context, PicRunner picRunner) {
        this.picRunner = picRunner;
        picRunner.addListener(this);
        
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }
    
    public boolean isEmpty() {
        if (venues == null) {
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
        if (venues == null) {
            return 0;
        }
        
        return venues.size();
    }

    public Object getItem(int position) {
        synchronized (venueLock) {
            return venues.get(position);
        }
    }

    public long getItemId(int position) {
        return position;
    }

    public void updateVenues(List<Venue> venues) {
        this.venues = venues;
        
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
        
        if (venues != null) {
            new Thread(
                new Runnable() {
                    public void run() {
                        synchronized (venueLock) {
                            // Recalculate distances to friends
                            Venue.calculateDistances(venues, currentLocation);
                            
                            // Re-sort friends (exclude own user) by distance
                            Venue.sortByDistance(venues.subList(1, venues.size()));
                            
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
    protected void onNextPageRequested(int page) {}

    @Override
    protected void bindSectionHeader(View view, int position,
            boolean displaySectionHeader) {}

    @Override
    public View getAmazingView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder = null;
        
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.venue_item, null);
            holder = new ViewHolder();
            holder.pic = (ImageView) convertView.findViewById(R.id.venue_pic);
            holder.name = (TextView) convertView.findViewById(R.id.venue_name);
            holder.distance = (TextView) convertView.findViewById(R.id.venue_distance);
            convertView.setTag(holder);
        }
        
        holder = (ViewHolder) convertView.getTag();
        
        Venue venue;
        synchronized (venueLock) {
            venue = venues.get(position);
        }
        
        // Attempt to get photo from ready images or issue request
        Bitmap image = picRunner.getImage(venue.getIcon(), false);
        if (image != null) {
            holder.pic.setImageBitmap(image);
        }
        
        holder.name.setText(venue.getName());
        
        if (venue.getDistance() != null) {
            holder.distance.setText(venue.getPrettyDistance());
        }
        
        return convertView;
    }

    @Override
    public void configurePinnedHeader(View header, int position, int alpha) {}

    @Override
    public int getPositionForSection(int section) {
        return 0;
    }

    @Override
    public int getSectionForPosition(int position) {
        return 0;
    }

    @Override
    public Object[] getSections() {
        return null;
    }

    public void onProfilePicDownloaded() {
        // Refresh the UI
        Util.uiHandler.post(new Runnable() {
            public void run() {
                notifyDataSetChanged();
            }
        });
    }
    
    private static class ViewHolder {
        public ImageView pic;
        public TextView name;
        public TextView distance;
    }
}
