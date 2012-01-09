package com.inflatablegoldfish.sociallocate;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import android.graphics.Bitmap;
import android.os.AsyncTask;

/*
 * Fetch pictures via AsyncTask
 */
public class PicRunner {
    private HashMap<String, Bitmap> images = new HashMap<String, Bitmap>();
    private HashSet<String> requested = new HashSet<String>();
    private Queue<ItemPair> queue = new LinkedList<ItemPair>();
    private List<PicRunnerListener> listeners = new LinkedList<PicRunnerListener>();
    private int runningCount = 0;

    /*
     * 15 max async tasks at any given time.
     */
    private final static int MAX_ALLOWED_TASKS = 15;

    /**
     * Add listener to be informed when image downloaded
     * @param listener Listener to add
     */
    public void addListener(PicRunnerListener listener) {
        listeners.add(listener);
    }

    public void reset() {
        images.clear();
        requested.clear();
        queue.clear();
        listeners.clear();
        runningCount = 0;
    }

    /*
     * If the picture has already been downloaded and cached, return it
     * else execute a new async task to fetch it - if total async tasks > 15,
     * queue the request.
     */
    public Bitmap getImage(String url, boolean crop) {
        Bitmap image = images.get(url);
        if (image != null) {
            return image;
        }
        if (!requested.contains(url)) {
            requested.add(url);
            if (runningCount >= MAX_ALLOWED_TASKS) {
                queue.add(new ItemPair(url, crop));
            } else {
                runningCount++;
                new GetProfilePicAsyncTask().execute(url, crop);
            }
        }
        return null;
    }

    public void getNextImage() {
        if (!queue.isEmpty()) {
            ItemPair item = queue.remove();
            new GetProfilePicAsyncTask().execute(item.url, item.crop);
        }
    }

    /*
     * Start a AsyncTask to fetch the request
     */
    private class GetProfilePicAsyncTask extends AsyncTask<Object, Void, Bitmap> {
        private String url;
        private boolean crop;

        @Override
        protected Bitmap doInBackground(Object... params) {
            this.url = (String) params[0];
            this.crop = (Boolean) params[1];
            
            Bitmap image = Util.getBitmap(this.url);
            
            if (image != null) {
                // Crop if necessary
                if (this.crop) {
                    image = Util.cropBitmap(image, 100, 100);
                }
            }
            
            return image;
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            runningCount--;
            if (result != null) {
                images.put(url, result);
                
                for (PicRunnerListener listener : listeners) {
                    listener.onProfilePicDownloaded();
                }
                
                getNextImage();
            }
        }
    }

    private static class ItemPair {
        public String url;
        public boolean crop;

        public ItemPair(String url, boolean crop) {
            this.url = url;
            this.crop = crop;
        }
    }
    
    public static interface PicRunnerListener {
        public void onProfilePicDownloaded();
    }
}
