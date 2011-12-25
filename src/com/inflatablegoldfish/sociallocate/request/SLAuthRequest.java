package com.inflatablegoldfish.sociallocate.request;

import java.util.Deque;
import java.util.Iterator;

import com.facebook.android.Facebook;
import com.inflatablegoldfish.sociallocate.SocialLocate;
import com.inflatablegoldfish.sociallocate.SocialLocate.SLRequestListener;
import com.inflatablegoldfish.sociallocate.Util;
import com.inflatablegoldfish.sociallocate.foursquare.Foursquare;

import android.content.SharedPreferences;

public class SLAuthRequest extends SLRequest {
    public SLAuthRequest(RequestManager manager, SLRequestListener listener,
            Facebook facebook, SocialLocate socialLocate, Foursquare foursquare) {

        super(manager, listener, facebook, socialLocate, foursquare);
    }
    
    @Override
    public RequestResult execute() {
        // Get the FB access token
        String accessToken = Util.prefs.getString("access_token", null);
        
        return socialLocate.auth(accessToken, (SLRequestListener) listener);
    }

    @Override
    public void onAuthFail(Deque<Request> requestQueue) {
        // Facebook token must be invalid so clear it!
        SharedPreferences.Editor editor = Util.prefs.edit();
        editor.remove("access_token");
        editor.remove("access_expires");
        editor.commit();
        
        facebook.setAccessToken(null);
        
        // Create new FB auth request and queue it!
        new FBAuthRequest(
            manager,
            facebook,
            socialLocate,
            foursquare
        ).addToQueue(requestQueue);
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
                            request.listener.onError();
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
            requestQueue.addFirst(this);
        }
    }
}
