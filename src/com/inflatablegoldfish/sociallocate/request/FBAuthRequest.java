package com.inflatablegoldfish.sociallocate.request;

import java.util.Deque;
import java.util.Iterator;

import android.content.SharedPreferences;
import android.os.Bundle;

import com.facebook.android.DialogError;
import com.facebook.android.Facebook;
import com.facebook.android.FacebookError;
import com.facebook.android.Facebook.DialogListener;
import com.inflatablegoldfish.sociallocate.SocialLocate;
import com.inflatablegoldfish.sociallocate.Util;
import com.inflatablegoldfish.sociallocate.foursquare.Foursquare;

public class FBAuthRequest extends Request {
    private RequestResult result;
    private Boolean resultReady;
    private Object resultLock = new Object();
    
    public FBAuthRequest(RequestManager manager, Facebook facebook,
            SocialLocate socialLocate, Foursquare foursquare) {
        
        super(manager, null, facebook, socialLocate, foursquare);
    }

    @Override
    public RequestResult execute() {
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
                manager.getCurrentActivity(),
                new String[] {"offline_access"},
                new DialogListener() {
                    public void onComplete(final Bundle values) {                        
                        SharedPreferences.Editor editor = Util.prefs.edit();
                        editor.putString("access_token", facebook.getAccessToken());
                        editor.putLong("access_expires", facebook.getAccessExpires());
                        editor.commit();
                        
                        synchronized (resultLock) {
                            result = RequestResult.SUCCESS;
                            resultReady = true;
                            resultLock.notify();
                        }
                        
                        Util.showToast(
                            "FB authentication OK",
                            manager.getCurrentActivity().getApplicationContext()
                        );
                    }

                    public void onFacebookError(final FacebookError e) {
                        synchronized (resultLock) {
                            result = RequestResult.AUTHFAIL;
                            resultReady = true;
                            resultLock.notify();
                        }
                    }
        
                    public void onError(final DialogError e) {
                        synchronized (resultLock) {
                            result = RequestResult.ERROR;
                            resultReady = true;
                            resultLock.notify();
                        }
                    }
        
                    public void onCancel() {
                        synchronized (resultLock) {
                            // TODO: Deal with FB cancel properly
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
        
        return RequestResult.SUCCESS;
    }

    @Override
    public void onAuthFail(Deque<Request> requestQueue) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onError(Deque<Request> requestQueue) {
        // TODO Auto-generated method stub

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
