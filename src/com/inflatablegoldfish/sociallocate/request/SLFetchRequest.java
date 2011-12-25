package com.inflatablegoldfish.sociallocate.request;

import java.util.Deque;
import java.util.Iterator;

import com.facebook.android.Facebook;
import com.inflatablegoldfish.sociallocate.SocialLocate;
import com.inflatablegoldfish.sociallocate.User;
import com.inflatablegoldfish.sociallocate.foursquare.Foursquare;

public class SLFetchRequest extends SLRequest {

    public SLFetchRequest(RequestManager manager, RequestListener<User[]> listener,
            Facebook facebook, SocialLocate socialLocate, Foursquare foursquare) {
        
        super(manager, listener, facebook, socialLocate, foursquare);
    }

    @SuppressWarnings("unchecked")
    @Override
    public RequestResult execute() {
        return socialLocate.fetch((RequestListener<User[]>) listener);
    }

    @Override
    public void onAuthFail(Deque<Request> requestQueue) {
        // Add an SL auth request to the queue
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
             * Only one fetch is necessary so only add
             * to the queue if one doesn't already exist.
             */
            while (itr.hasNext()) {
                Request request = itr.next();
                
                if (request instanceof SLFetchRequest) {
                    return;
                }
            }
            
            // No other SL fetch requests so add to the queue
            requestQueue.addFirst(this);
        }
    }
}
