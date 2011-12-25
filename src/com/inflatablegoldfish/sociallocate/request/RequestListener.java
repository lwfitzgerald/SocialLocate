package com.inflatablegoldfish.sociallocate.request;

/**
 * Callbacks for when requests complete/abort
 *
 * The operations in the onComplete/onError
 * normally update the UI
 * 
 * @param <ReturnType> Return type
 */
public interface RequestListener<ReturnType> {
    /**
     * Called when a request completed successfully
     * @param toReturn Results of the request
     */
    public void onComplete(final ReturnType toReturn);
    
    /**
     * Called when a request fails. This may be
     * after a number of retries.
     */
    public void onError();
    
    /**
     * Called when the request is cancelled
     */
    public void onCancel();
}
