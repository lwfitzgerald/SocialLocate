package com.inflatablegoldfish.sociallocate.request;

import java.util.Deque;
import java.util.Iterator;

import android.location.Location;

import com.facebook.android.Facebook;
import com.inflatablegoldfish.sociallocate.SocialLocate;
import com.inflatablegoldfish.sociallocate.User;
import com.inflatablegoldfish.sociallocate.foursquare.Foursquare;

public class SLUpdateRequest extends SLRequest {
    private Location location;
    
    public SLUpdateRequest(Location location, RequestManager manager,
            RequestListener<User[]> listener, Facebook facebook,
            SocialLocate socialLocate, Foursquare foursquare) {
        
        super(manager, listener, facebook, socialLocate, foursquare);
        
        this.location = location;
    }

    @SuppressWarnings("unchecked")
    @Override
    public RequestResult execute() {
        return socialLocate.updateLocation(location, (RequestListener<User[]>) listener);
    }

    @Override
    public void onAuthFail(Deque<Request> requestQueue) {
        addSLReAuth(requestQueue);
    }

    @Override
    public void onError(Deque<Request> requestQueue) {
        if (retries < NUM_RETRIES) {
            // TODO: Implement exponential backoff sleep here?
            retries++;
        } else {
            listener.onError();
            
            // Remove from queue (will be at front)
            requestQueue.poll();
        }
    }

    @Override
    public void onCancel(Deque<Request> requestQueue) {}

    @Override
    public void addToQueue(Deque<Request> requestQueue) {
        // Synchronize on the queue whilst iterating
        synchronized (requestQueue) {
            Iterator<Request> itr = requestQueue.iterator();
            
            /*
             * Find existing update requests and simply
             * update the location parameter
             */
            while (itr.hasNext()) {
                Request request = itr.next();
                
                if (request instanceof SLUpdateRequest) {
                    ((SLUpdateRequest) request).updateLocation(location);
                    return;
                }
            }
            
            // No other SL update requests so add to the queue
            requestQueue.addFirst(this);
        }
    }
    
    public void updateLocation(Location location) {
        this.location = location;
    }

}
