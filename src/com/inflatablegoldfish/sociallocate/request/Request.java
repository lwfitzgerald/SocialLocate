package com.inflatablegoldfish.sociallocate.request;

import java.util.Deque;

import android.content.SharedPreferences;

import com.facebook.android.Facebook;
import com.inflatablegoldfish.sociallocate.SocialLocate;
import com.inflatablegoldfish.sociallocate.foursquare.Foursquare;

public abstract class Request {
    protected RequestListener<?> listener;
    protected int retries = 0;
    
    protected RequestManager manager;
    protected Facebook facebook;
    protected SocialLocate socialLocate;
    protected Foursquare foursquare;
    
    protected Request(RequestManager manager, RequestListener<?> listener, Facebook facebook,
            SocialLocate socialLocate, Foursquare foursquare) {
        
        this.manager = manager;
        this.listener = listener;
        this.facebook = facebook;
        this.socialLocate = socialLocate;
        this.foursquare = foursquare;
    }
    
    public abstract RequestResult execute();
    
    public abstract void onAuthFail(Deque<Request> requestQueue);
    
    public abstract void onError(Deque<Request> requestQueue);
    
    public abstract void addToQueue(Deque<Request> requestQueue);
    
    public static enum RequestResult { SUCCESS, AUTHFAIL, ERROR };
}
