package com.inflatablegoldfish.sociallocate.request;

import java.util.Deque;
import java.util.Iterator;

import com.inflatablegoldfish.sociallocate.foursquare.Foursquare;
import com.inflatablegoldfish.sociallocate.foursquare.Venue;

public class FSVenueRequest extends Request {
    private Foursquare foursquare;
    private String venueID;
    
    private static final int NUM_RETRIES = 3;
    
    public FSVenueRequest(String venueID, Foursquare foursquare,
            RequestManager manager, RequestListener<Venue> listener) {
        
        super(manager, listener);
        
        this.foursquare = foursquare;
        this.venueID = venueID;
    }
    
    @Override
    public RequestResult<Venue> execute() {
        return foursquare.getVenue(venueID);
    }

    @Override
    protected void onAuthFail(Deque<Request> requestQueue) {}

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
