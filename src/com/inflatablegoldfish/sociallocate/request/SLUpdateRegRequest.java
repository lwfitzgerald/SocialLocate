package com.inflatablegoldfish.sociallocate.request;

import java.util.Deque;
import java.util.Iterator;
import java.util.List;

import com.facebook.android.Facebook;
import com.inflatablegoldfish.sociallocate.SocialLocate;
import com.inflatablegoldfish.sociallocate.User;

public class SLUpdateRegRequest extends SLRequest {
    private String registrationID;
    
    public SLUpdateRegRequest(String registrationID, RequestManager manager,
            RequestListener<List<User>> listener, Facebook facebook,
            SocialLocate socialLocate) {
        
        super(manager, listener, facebook, socialLocate);
        
        this.registrationID = registrationID;
    }

    @Override
    public RequestResult<List<User>> execute() {
        return socialLocate.updateRegistration(registrationID);
    }

    @Override
    public void onAuthFail(Deque<Request> requestQueue) {
        addSLReAuth(requestQueue);
    }

    @Override
    public void onError(Deque<Request> requestQueue) {
        if (retries < NUM_RETRIES) {
            try {
                Thread.sleep((long) (1000 * Math.pow(3, retries)));
            } catch (InterruptedException e) {}
            retries++;
        } else {
            // Remove from queue
            synchronized(requestQueue) {
                Iterator<Request> itr = requestQueue.iterator();
                
                while (itr.hasNext()) {
                    // Remove this and error on meet requests
                    Request request = itr.next();
                    
                    if (request == this) {
                        itr.remove();
                    } else if (request instanceof SLMeetRequest) {
                        // Call error handler on dependent meet request
                        itr.remove();
                        request.listener.onError(ResultCode.ERROR);
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
             * Only one update is necessary so only add
             * to the queue if one doesn't already exist.
             */
            while (itr.hasNext()) {
                Request request = itr.next();
                
                if (request instanceof SLUpdateRegRequest) {
                    return;
                }
            }
            
            // No other update requests so add to the queue
            requestQueue.addLast(this);
        }
    }
}
