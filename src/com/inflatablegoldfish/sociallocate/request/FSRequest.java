package com.inflatablegoldfish.sociallocate.request;

import java.util.Deque;
import java.util.Iterator;
import java.util.List;

import android.location.Location;

import com.inflatablegoldfish.sociallocate.foursquare.Foursquare;
import com.inflatablegoldfish.sociallocate.foursquare.Venue;

public class FSRequest extends Request {
    private Foursquare foursquare;
    private Location location;
    private Location ourLocation;
    
    private static final int NUM_RETRIES = 3;
    
    public FSRequest(Location location, Location ourLocation, Foursquare foursquare,
            RequestManager manager, RequestListener<List<Venue>> listener) {
        
        super(manager, listener);
        
        this.foursquare = foursquare;
        this.location = location;
        this.ourLocation = location;
    }

    @Override
    public RequestResult<List<Venue>> execute() {
        return foursquare.getVenuesNear(location, ourLocation);
    }

    @Override
    protected void onAuthFail(Deque<Request> requestQueue) {}

    @Override
    protected void onError(Deque<Request> requestQueue) {
        if (retries < NUM_RETRIES) {
            try {
                Thread.sleep((long) (1000 * Math.pow(3,retries)));
            } catch (InterruptedException e) {}
            retries++;
        } else {
            // Remove from queue
            synchronized(requestQueue) {
                Iterator<Request> itr = requestQueue.iterator();
                
                while (itr.hasNext()) {
                    if (itr.next() == this) {
                        itr.remove();
                        break;
                    }
                }
            }
            
            listener.onError(ResultCode.ERROR);
        }
    }

    @Override
    protected void onCancel(Deque<Request> requestQueue) {}

    @Override
    public void addToQueue(Deque<Request> requestQueue) {
        synchronized (requestQueue) {
            Iterator<Request> itr = requestQueue.iterator();
            
            /*
             * Find existing requests and simply
             * update the location parameter
             */
            while (itr.hasNext()) {
                Request request = itr.next();
                
                if (request instanceof FSRequest) {
                    ((FSRequest) request).updateLocation(location);
                    return;
                }
            }
            
            // No other requests so add to the queue
            requestQueue.addFirst(this);
        }
    }
    
    private void updateLocation(Location location) {
        this.location = location;
    }
}
