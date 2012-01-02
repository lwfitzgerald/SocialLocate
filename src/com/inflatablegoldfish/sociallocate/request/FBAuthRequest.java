package com.inflatablegoldfish.sociallocate.request;

import java.util.Deque;
import java.util.Iterator;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.facebook.android.DialogError;
import com.facebook.android.Facebook;
import com.facebook.android.FacebookError;
import com.facebook.android.Facebook.DialogListener;
import com.inflatablegoldfish.sociallocate.SocialLocate;
import com.inflatablegoldfish.sociallocate.User;
import com.inflatablegoldfish.sociallocate.Util;
import com.inflatablegoldfish.sociallocate.foursquare.Foursquare;

public class FBAuthRequest extends Request {
    private RequestResult<User[]> result;
    private Boolean resultReady;
    private Object resultLock = new Object();
    
    private static final int NUM_RETRIES = 3;
    
    public FBAuthRequest(RequestManager manager, RequestListener<?> listener, Facebook facebook,
            SocialLocate socialLocate, Foursquare foursquare) {
        
        super(manager, listener, facebook, socialLocate, foursquare);
    }

    @Override
    public RequestResult<User[]> execute() {
        resultReady = false;
        
        String access_token = Util.prefs.getString("access_token", null);
        long expires = Util.prefs.getLong("access_expires", 0);
        
        if (access_token != null) {
            facebook.setAccessToken(access_token);
        }
        
        if (expires != 0) {
            facebook.setAccessExpires(expires);
        }
        
        if (!facebook.isSessionValid()) {
            facebook.authorize(
                (Activity) manager.getContext(),
                new String[] {"offline_access"},
                new DialogListener() {
                    public void onComplete(final Bundle values) {                        
                        SharedPreferences.Editor editor = Util.prefs.edit();
                        editor.putString("access_token", facebook.getAccessToken());
                        editor.putLong("access_expires", facebook.getAccessExpires());
                        editor.commit();

                        result = new RequestResult<User[]>(null, ResultCode.SUCCESS);
                        
                        synchronized (resultLock) {
                            resultReady = true;
                            resultLock.notify();
                        }
                    }

                    public void onFacebookError(final FacebookError e) {
                        result = new RequestResult<User[]>(null, ResultCode.ERROR);
                        
                        synchronized (resultLock) {
                            resultReady = true;
                            resultLock.notify();
                        }
                    }
        
                    public void onError(final DialogError e) {
                        result = new RequestResult<User[]>(null, ResultCode.ERROR);
                        
                        synchronized (resultLock) {
                            resultReady = true;
                            resultLock.notify();
                        }
                    }
        
                    public void onCancel() {
                        result = new RequestResult<User[]>(null, ResultCode.CANCELLED);
                        
                        synchronized (resultLock) {
                            resultReady = true;
                            resultLock.notify();
                        }
                    }
                }
            );
            
            synchronized(resultLock) {
                if (!resultReady) {
                    try {
                        resultLock.wait();
                    } catch (InterruptedException e1) {}
                }
            }
            
            return result;
        }
        
        return new RequestResult<User[]>(null, ResultCode.SUCCESS);
    }

    @Override
    public void onAuthFail(Deque<Request> requestQueue) {}

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
                    
                    if (request instanceof SLRequest
                            || request instanceof FBAuthRequest) {
                        // Remove from queue
                        itr.remove();
                        
                        // We will call our listener's
                        // own error handler here too
                        
                        // Call listener's error handler
                        if (request.listener != null) {
                            request.listener.onError();
                        }
                    }
                }
            }
        }
    }
    
    @Override
    public void onCancel(Deque<Request> requestQueue) {
        /*
         * Need to remove all later dependent
         * operations
         */
        synchronized (requestQueue) {
            Iterator<Request> itr = requestQueue.iterator();
            
            while (itr.hasNext()) {
                Request request = itr.next();
                
                if (request instanceof SLRequest
                        || request instanceof FBAuthRequest) {
                    // Remove from queue
                    itr.remove();
                    
                    // We will call our listener's
                    // own cancel handler here too
                    
                    // Call listener's error handler
                    if (request.listener != null) {
                        request.listener.onCancel();
                    }
                }
            }
        }
    }

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
                
                if (request instanceof FBAuthRequest) {
                    return;
                }
            }
            
            // No other SL auth requests so add to the queue
            requestQueue.addFirst(this);
        }
    }
}
