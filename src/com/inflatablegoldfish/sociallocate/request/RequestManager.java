package com.inflatablegoldfish.sociallocate.request;

import java.util.Deque;
import java.util.LinkedList;

import android.content.Context;

import com.inflatablegoldfish.sociallocate.request.Request.RequestResult;

public class RequestManager implements Runnable {
    private Deque<Request> queue;
    private Boolean running = false;
    private volatile Context context;
    
    public RequestManager(Context context) {
        /*
         * ALL queue accesses MUST be synchronized!
         */
        this.queue = new LinkedList<Request>();
        
        this.context = context;
    }
    
    /**
     * Add a request to be executed and
     * immediately attempt to start executing
     * 
     * This is called from the main thread
     * @param toAdd Request to execute
     */
    public void addRequest(Request toAdd) {
        addRequestWithoutStarting(toAdd);
        
        startProcessing();
    }
    
    /**
     * Add a request to be executed but
     * do not start executing yet.
     * 
     * WARNING: If already processing,
     * requests WILL be executed despite
     * using this method
     * @param toAdd Request to execute
     */
    public void addRequestWithoutStarting(Request toAdd) {
        toAdd.addToQueue(queue);
    }
    
    /**
     * Attempt to start processing
     * requests if we are not already
     */
    public void startProcessing() {
        synchronized(running) {
            if (!running) {
                running = true;
                // Start processing
                new Thread(this).start();
            }
        }
    }
    
    /**
     * Called by Thread.start() to start
     * processing requests in another thread
     */
    public void run() {
        Request request;
        
        // Loop until no more requests to execute
        while (true) {
            synchronized(queue) {
                request = queue.peek();
            }
            
            if (request != null) {
                // Execute the request
                RequestResult<?> result = request.execute();
                
                switch (result.code) {
                case SUCCESS:
                    /*
                     * Completed successfully so remove
                     * if still at front of queue
                     */
                    synchronized(queue) {
                        if (queue.peek() == request) {
                            queue.poll();
                        }
                    }
                    
                    request.listener.onComplete(result.result);
                    
                    break;
                case AUTHFAIL:
                    request.onAuthFail(queue);
                    break;
                case ERROR:
                    request.onError(queue);
                    break;
                case CANCELLED:
                    request.onCancel(queue);
                }
            } else {
                break;
            }
        }
        
        synchronized(running) {
            boolean empty;
            
            synchronized(queue) {
                empty = queue.isEmpty();
            }
            
            if (!empty) {
                /*
                 * Handle race condition where something
                 * is added to the queue after we checked
                 * if it's empty and then the running check
                 * executes before we set it to false.
                 * 
                 * Without this, a thread would not be started
                 * as running would still be set to true when
                 * the check ran.
                 */
                
                // running = true so just restart a new thread
                new Thread(this).start();
            } else {
                running = false;
            }
        }
    }
    
    public Context getContext() {
        return context;
    }
    
    public void updateContext(Context newContext) {
        this.context = newContext;
    }
}
