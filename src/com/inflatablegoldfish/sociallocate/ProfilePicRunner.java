package com.inflatablegoldfish.sociallocate;

/**
 * This file is from the Facebook "Hackbook" sample application
 * with some small modfifications for compatibility
 */

import java.util.Hashtable;
import java.util.Stack;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.widget.BaseAdapter;

/*
 * Fetch friends profile pictures request via AsyncTask
 */
public class ProfilePicRunner {

    private Hashtable<Integer, Bitmap> friendsImages;
    private Hashtable<Integer, String> positionRequested;
    private BaseAdapter listener;
    private int runningCount = 0;
    private Stack<ItemPair> queue;

    /*
     * 15 max async tasks at any given time.
     */
    final static int MAX_ALLOWED_TASKS = 15;

    public ProfilePicRunner() {
        friendsImages = new Hashtable<Integer, Bitmap>();
        positionRequested = new Hashtable<Integer, String>();
        queue = new Stack<ItemPair>();
    }
    
    /**
     * Create with the given listener
     * @param listener Listener to store
     */
    public ProfilePicRunner(BaseAdapter listener) {
        this();
        setListener(listener);
    }

    /*
     * Inform the listener when the image has been downloaded. listener is
     * FriendsList here.
     */
    public void setListener(BaseAdapter listener) {
        this.listener = listener;
        reset();
    }

    public void reset() {
        positionRequested.clear();
        runningCount = 0;
        queue.clear();
    }

    /*
     * If the profile picture has already been downloaded and cached, return it
     * else execute a new async task to fetch it - if total async tasks >15,
     * queue the request.
     */
    public Bitmap getImage(int uid, String url) {
        Bitmap image = friendsImages.get(uid);
        if (image != null) {
            return image;
        }
        if (!positionRequested.containsKey(uid)) {
            positionRequested.put(uid, "");
            if (runningCount >= MAX_ALLOWED_TASKS) {
                queue.push(new ItemPair(uid, url));
            } else {
                runningCount++;
                new GetProfilePicAsyncTask().execute(uid, url);
            }
        }
        return null;
    }

    public void getNextImage() {
        if (!queue.isEmpty()) {
            ItemPair item = queue.pop();
            new GetProfilePicAsyncTask().execute(item.uid, item.url);
        }
    }

    /*
     * Start a AsyncTask to fetch the request
     */
    private class GetProfilePicAsyncTask extends AsyncTask<Object, Void, Bitmap> {
        int uid;

        @Override
        protected Bitmap doInBackground(Object... params) {
            this.uid = (Integer) params[0];
            String url = (String) params[1];
            return Util.getBitmap(url);
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            runningCount--;
            if (result != null) {
                friendsImages.put(uid, result);
                listener.notifyDataSetChanged();
                getNextImage();
            }
        }
    }

    class ItemPair {
        int uid;
        String url;

        public ItemPair(int uid, String url) {
            this.uid = uid;
            this.url = url;
        }
    }
}
