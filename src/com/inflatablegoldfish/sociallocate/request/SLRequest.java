package com.inflatablegoldfish.sociallocate.request;

import java.util.Deque;
import java.util.List;

import com.facebook.android.Facebook;
import com.inflatablegoldfish.sociallocate.SocialLocate;
import com.inflatablegoldfish.sociallocate.User;
import com.inflatablegoldfish.sociallocate.foursquare.Foursquare;

public abstract class SLRequest extends Request {
    protected static final int NUM_RETRIES = 3;
    
    protected SLRequest(RequestManager manager,
            RequestListener<List<User>> listener, Facebook facebook,
            SocialLocate socialLocate, Foursquare foursquare) {
        
        super(manager, listener, facebook, socialLocate, foursquare);
    }
    
    /**
     * Add an SL auth request to the
     * front of the request queue
     */
    protected void addSLReAuth(Deque<Request> requestQueue) {
        synchronized(requestQueue) {
            requestQueue.addFirst(
                new SLAuthRequest(
                    manager,
                    
                    // Listener with no operations
                    new RequestListener<List<User>>() {
                        public void onError() {}
                        
                        public void onComplete(Object users) {}
                        
                        public void onCancel() {}
                    },
                    
                    facebook,
                    socialLocate,
                    foursquare
                )
            );
        }
    }
}
