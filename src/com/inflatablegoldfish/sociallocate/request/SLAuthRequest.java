package com.inflatablegoldfish.sociallocate.request;

import java.util.Deque;
import java.util.Iterator;
import java.util.List;

import com.facebook.android.Facebook;
import com.inflatablegoldfish.sociallocate.SocialLocate;
import com.inflatablegoldfish.sociallocate.User;
import com.inflatablegoldfish.sociallocate.Util;

import android.content.SharedPreferences;

public class SLAuthRequest extends SLRequest {
    public SLAuthRequest(RequestManager manager,
            RequestListener<List<User>> listener, Facebook facebook,
            SocialLocate socialLocate) {

        super(manager, listener, facebook, socialLocate);
    }
    
    @Override
    public RequestResult<List<User>> execute() {
        // Get the FB access token
        String accessToken = Util.prefs.getString("access_token", null);
        
        return socialLocate.auth(accessToken);
    }

    @Override
    public void onAuthFail(Deque<Request> requestQueue) {
        // Facebook token must be invalid so clear it!
        SharedPreferences.Editor editor = Util.prefs.edit();
        editor.remove("access_token");
        editor.remove("access_expires");
        editor.commit();
        
        // Clear stored cookies
        socialLocate.clearCookies();
        
        // Might not be set if called from receiver
        if (facebook != null) {
            facebook.setAccessToken(null);
        }
        
        if (manager.getContext() != null) {
            // Create new FB auth request and put at front of queue
            synchronized(requestQueue) {
                requestQueue.addFirst(
                    new FBAuthRequest(
                        manager,
                        new RequestListener<User[]>() {
                            public void onComplete(Object result) {}
            
                            public void onError(ResultCode resultCode) {}
            
                            public void onCancel() {}
                        },
                        facebook,
                        socialLocate
                    )
                );
            }
        } else {
            /*
             * Activity not open so cannot do Facebook auth
             * 
             * So silently remove all dependent requests from queue
             */
            synchronized (requestQueue) {
                Iterator<Request> itr = requestQueue.iterator();
                
                while (itr.hasNext()) {
                    Request request = itr.next();
                    
                    if (request instanceof SLRequest
                            || request instanceof FBAuthRequest) {
                        // Remove from queue
                        itr.remove();
                        
                        // Call authfail listeners
                        listener.onError(ResultCode.AUTHFAIL);
                    }
                }
            }
        }
    }

    @Override
    public void onError(Deque<Request> requestQueue) {
        if (retries < NUM_RETRIES) {
            // Request will be reattempted by manager
            // TODO: Implement exponential backoff sleep here?
            retries++;
        } else {
            /*
             * Need to remove all later dependent
             * operations
             */
            synchronized (requestQueue) {
                Iterator<Request> itr = requestQueue.iterator();
                
                while (itr.hasNext()) {
                    Request request = itr.next();
                    
                    /*
                     * If we encounter any other SL requests
                     * remove them
                     */
                    if (request instanceof SLRequest) {
                        // We will call our listener's
                        // own error handler here too
                        
                        // Call listener's error handler
                        if (request.listener != null) {
                            request.listener.onError(ResultCode.ERROR);
                        }
                        
                        // Remove from queue
                        itr.remove();
                    }
                }
            }
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
             * Only one auth is necessary so only add
             * to the queue if one doesn't already exist
             */
            while (itr.hasNext()) {
                Request request = itr.next();
                
                if (request instanceof SLAuthRequest) {
                    return;
                }
            }
            
            // No other SL auth requests so add to the queue
            requestQueue.addLast(this);
        }
    }
}
