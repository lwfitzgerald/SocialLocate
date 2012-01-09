package com.inflatablegoldfish.sociallocate.request;

import java.util.Deque;
import java.util.Iterator;
import java.util.List;

import com.facebook.android.Facebook;
import com.inflatablegoldfish.sociallocate.SocialLocate;
import com.inflatablegoldfish.sociallocate.User;

public class SLMeetRequest extends SLRequest {
    private int friendID;
    private String venueID;
    
    public SLMeetRequest(int friendID, String venueID, RequestManager manager,
            RequestListener<List<User>> listener, Facebook facebook,
            SocialLocate socialLocate) {
        
        super(manager, listener, facebook, socialLocate);
        
        this.friendID = friendID;
        this.venueID = venueID;
    }

    @Override
    public RequestResult<Void> execute() {
        return socialLocate.meet(friendID, venueID);
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
