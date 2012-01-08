package com.inflatablegoldfish.sociallocate.request;

import java.util.Deque;
import java.util.List;

import com.facebook.android.Facebook;
import com.inflatablegoldfish.sociallocate.SocialLocate;
import com.inflatablegoldfish.sociallocate.User;

public abstract class SLRequest extends Request {
    protected static final int NUM_RETRIES = 3;
    
    protected Facebook facebook;
    protected SocialLocate socialLocate;
    
    protected SLRequest(RequestManager manager,
            RequestListener<List<User>> listener, Facebook facebook,
            SocialLocate socialLocate) {
        
        super(manager, listener);
        
        this.facebook = facebook;
        this.socialLocate = socialLocate;
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
                        public void onComplete(Object users) {}
                        
                        public void onError(ResultCode resultCode) {}
                        
                        public void onCancel() {}
                    },
                    
                    facebook,
                    socialLocate
                )
            );
        }
    }
}
