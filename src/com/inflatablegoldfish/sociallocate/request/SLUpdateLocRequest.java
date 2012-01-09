package com.inflatablegoldfish.sociallocate.request;

import java.util.Deque;
import java.util.Iterator;
import java.util.List;

import android.location.Location;

import com.facebook.android.Facebook;
import com.inflatablegoldfish.sociallocate.SocialLocate;
import com.inflatablegoldfish.sociallocate.User;

public class SLUpdateLocRequest extends SLRequest {
    private Location location;
    
    public SLUpdateLocRequest(Location location, RequestManager manager,
            RequestListener<List<User>> listener, Facebook facebook,
            SocialLocate socialLocate) {
        
        super(manager, listener, facebook, socialLocate);
        
        this.location = location;
    }

    @Override
    public RequestResult<Void> execute() {
        return socialLocate.updateLocation(location);
    }

    @Override
    public void onAuthFail(Deque<Request> requestQueue) {
        addSLReAuth(requestQueue);
    }

    @Override
    public void onError(Deque<Request> requestQueue) {
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
                
                if (request instanceof SLUpdateLocRequest) {
                    ((SLUpdateLocRequest) request).updateLocation(location);
                    return;
                }
            }
            
            // No other SL update requests so add to the queue
            requestQueue.addLast(this);
        }
    }
    
    private void updateLocation(Location location) {
        this.location = location;
    }
}
