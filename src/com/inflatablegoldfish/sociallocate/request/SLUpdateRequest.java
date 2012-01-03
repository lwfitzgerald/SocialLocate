package com.inflatablegoldfish.sociallocate.request;

import java.util.Deque;
import java.util.Iterator;
import java.util.List;

import android.location.Location;

import com.facebook.android.Facebook;
import com.inflatablegoldfish.sociallocate.SocialLocate;
import com.inflatablegoldfish.sociallocate.User;

public class SLUpdateRequest extends SLRequest {
    private Location location;
    
    public SLUpdateRequest(Location location, RequestManager manager,
            RequestListener<List<User>> listener, Facebook facebook,
            SocialLocate socialLocate) {
        
        super(manager, listener, facebook, socialLocate);
        
        this.location = location;
    }

    @Override
    public RequestResult<List<User>> execute() {
        return socialLocate.updateLocation(location);
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
            // Remove from queue (will be at front)
            synchronized(requestQueue) {
                requestQueue.poll();
            }
            
            listener.onError();
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
