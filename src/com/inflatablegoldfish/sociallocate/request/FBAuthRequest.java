package com.inflatablegoldfish.sociallocate.request;

import java.util.Deque;
import java.util.Iterator;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import com.facebook.android.DialogError;
import com.facebook.android.Facebook;
import com.facebook.android.FacebookError;
import com.facebook.android.Facebook.DialogListener;
import com.inflatablegoldfish.sociallocate.Util;

public class FBAuthRequest extends Request {
    private RequestResult<Void> result;
    private Boolean resultReady;
    private Object resultLock = new Object();
    
    private static final int NUM_RETRIES = 3;
    
    private Facebook facebook;
    
    public FBAuthRequest(RequestManager manager, RequestListener<?> listener,
            Facebook facebook) {
        
        super(manager, listener);
        
        this.facebook = facebook;
    }

    @Override
    public RequestResult<Void> execute() {
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
            Util.uiHandler.post(
                new Runnable() {
                    public void run() {
                        facebook.authorize(
                            (Activity) manager.getContext(),
                            new String[] {"offline_access"},
                            new DialogListener() {
                                public void onComplete(final Bundle values) {
                                    Log.d("SocialLocate", "FB auth OK");
                                    
                                    SharedPreferences.Editor editor = Util.prefs.edit();
                                    editor.putString("access_token", facebook.getAccessToken());
                                    editor.putLong("access_expires", facebook.getAccessExpires());
                                    editor.commit();

                                    result = new RequestResult<Void>(null, ResultCode.SUCCESS);
                                    
                                    synchronized (resultLock) {
                                        resultReady = true;
                                        resultLock.notify();
                                    }
                                }

                                public void onFacebookError(final FacebookError e) {
                                    Log.d("SocialLocate", "Error during FB auth");
                                    
                                    result = new RequestResult<Void>(null, ResultCode.ERROR);
                                    
                                    synchronized (resultLock) {
                                        resultReady = true;
                                        resultLock.notify();
                                    }
                                }
                    
                                public void onError(final DialogError e) {
                                    Log.d("SocialLocate", "Error during FB auth");
                                    
                                    result = new RequestResult<Void>(null, ResultCode.ERROR);
                                    
                                    synchronized (resultLock) {
                                        resultReady = true;
                                        resultLock.notify();
                                    }
                                }
                    
                                public void onCancel() {
                                    Log.d("SocialLocate", "FB auth cancelled");
                                    
                                    result = new RequestResult<Void>(null, ResultCode.CANCELLED);
                                    
                                    synchronized (resultLock) {
                                        resultReady = true;
                                        resultLock.notify();
                                    }
                                }
                            }
                        );
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
        
        Log.d("SocialLocate", "FB auth loaded from file");
        return new RequestResult<Void>(null, ResultCode.SUCCESS);
    }

    @Override
    public void onAuthFail(Deque<Request> requestQueue) {}

    @Override
    public void onError(Deque<Request> requestQueue) {
        if (retries < NUM_RETRIES) {
            try {
                Thread.sleep((long) (1000 * Math.pow(3,retries)));
            } catch (InterruptedException e) {}
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
                            request.listener.onError(ResultCode.ERROR);
                        }
                    }
                }
            }
        }
    }
    
    @Override
    public void onCancel(Deque<Request> requestQueue) {
        Log.d("SocialLocate", "FB Auth cancelled");
        
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
            requestQueue.addLast(this);
        }
    }
}
