package com.inflatablegoldfish.sociallocate.request;

import java.util.Deque;

public abstract class Request {
    protected RequestListener<?> listener;
    protected int retries = 1;
    
    protected RequestManager manager;
    
    protected Request(RequestManager manager, RequestListener<?> listener) {
        
        this.manager = manager;
        this.listener = listener;
    }
    
    public abstract RequestResult<?> execute();
    
    protected abstract void onAuthFail(Deque<Request> requestQueue);
    
    protected abstract void onError(Deque<Request> requestQueue);
    
    protected abstract void onCancel(Deque<Request> requestQueue);
    
    public abstract void addToQueue(Deque<Request> requestQueue);
    
    public static class RequestResult<Type> {
        public final Type result;
        public final ResultCode code;
        
        public RequestResult (Type result, ResultCode code) {
            this.result = result;
            this.code = code;
        }
    }
    
    public static enum ResultCode { SUCCESS, AUTHFAIL, ERROR, CANCELLED };
}
