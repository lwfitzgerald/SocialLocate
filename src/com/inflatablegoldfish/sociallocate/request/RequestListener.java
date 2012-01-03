package com.inflatablegoldfish.sociallocate.request;

import com.inflatablegoldfish.sociallocate.request.Request.ResultCode;

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
     * @param result Results of the request
     */
    public void onComplete(final Object result);
    
    /**
     * Called when a request fails. This may be
     * after a number of retries.
     * @param resultCode Either ERROR or AUTHFAIL
     */
    public void onError(final ResultCode resultCode);
    
    /**
     * Called when the request is cancelled
     */
    public void onCancel();
}
