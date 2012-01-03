package com.inflatablegoldfish.sociallocate.request;

import java.util.Deque;
import java.util.Iterator;
import java.util.List;

import com.facebook.android.Facebook;
import com.inflatablegoldfish.sociallocate.SocialLocate;
import com.inflatablegoldfish.sociallocate.User;

public class SLFetchRequest extends SLRequest {
    public SLFetchRequest(RequestManager manager,
            RequestListener<List<User>> listener, Facebook facebook,
            SocialLocate socialLocate) {
        
        super(manager, listener, facebook, socialLocate);
    }

    @Override
    public RequestResult<List<User>> execute() {
        return socialLocate.fetch();
    }

    @Override
    protected void onAuthFail(Deque<Request> requestQueue) {
        // Add an SL auth request to the queue
        addSLReAuth(requestQueue);
    }

    @Override
    protected void onError(Deque<Request> requestQueue) {
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
    protected void onCancel(Deque<Request> requestQueue) {}
    
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
