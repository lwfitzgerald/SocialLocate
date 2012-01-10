package com.inflatablegoldfish.sociallocate.request;

import java.util.Deque;
import java.util.Iterator;

import com.facebook.android.Facebook;
import com.inflatablegoldfish.sociallocate.SocialLocate;

public class SLRespondRequest extends SLRequest {
    private int friendID;
    private boolean response;
    
    public SLRespondRequest(int friendID, boolean response, RequestManager manager,
            RequestListener<Void> listener, Facebook facebook,
            SocialLocate socialLocate) {
        
        super(manager, listener, facebook, socialLocate);
        
        this.friendID = friendID;
        this.response = response;
    }
    
    @Override
    public RequestResult<Void> execute() {
        return socialLocate.respond(friendID, response);
    }

    @Override
    protected void onAuthFail(Deque<Request> requestQueue) {
        addSLReAuth(requestQueue);
    }

    @Override
    protected void onError(Deque<Request> requestQueue) {
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
                    if (itr.next() == this) {
                        itr.remove();
                        break;
                    }
                }
            }
            
            listener.onError(ResultCode.ERROR);
        }
    }

    @Override
    protected void onCancel(Deque<Request> requestQueue) {}

    @Override
    public void addToQueue(Deque<Request> requestQueue) {
        synchronized (requestQueue) {
            requestQueue.addLast(this);
        }
    }
}
