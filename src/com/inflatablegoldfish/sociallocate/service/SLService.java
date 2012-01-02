//package com.inflatablegoldfish.sociallocate.service;
//
//import java.util.Iterator;
//import java.util.LinkedList;
//import java.util.List;
//
//import com.inflatablegoldfish.sociallocate.ActivityLocationHandler;
//import com.inflatablegoldfish.sociallocate.R;
//import com.inflatablegoldfish.sociallocate.SLActivity;
//import com.inflatablegoldfish.sociallocate.Util;
//import com.inflatablegoldfish.sociallocate.request.RequestManager;
//
//import android.app.Notification;
//import android.app.NotificationManager;
//import android.app.PendingIntent;
//import android.app.Service;
//import android.content.Context;
//import android.content.Intent;
//import android.location.Location;
//import android.location.LocationManager;
//import android.os.Binder;
//import android.os.Handler;
//import android.os.IBinder;
//import android.preference.PreferenceManager;
//import android.widget.Toast;
//
//public class SLService extends Service {
//    private ActivityLocationHandler locationHandler;
//    private Location currentLocation;
//    
//    private NotificationManager notificationManager;
//    private Notification.Builder notificationBuilder;
//    private static int notificationID = R.string.notification_id;
//    
//    private final IBinder binder = new SLServiceBinder();
//    private boolean isStarted = false;
//    
//    private List<SLServiceListener> listeners = new LinkedList<SLServiceListener>();
//    
//    private RequestManager requestManager;
//
//    /**
//     * Class for clients to access.  Because we know this service always
//     * runs in the same process as its clients, we don't need to deal with
//     * IPC.
//     */
//    public class SLServiceBinder extends Binder {
//        public SLService getService() {
//            return SLService.this;
//        }
//    }
//
//    @Override
//    public void onCreate() {
//        // Set up request manager
//        requestManager = new RequestManager(this);
//        
//        // Set up custom IG SSL socket factory
//        Util.initIGSSLSocketFactory(getResources().openRawResource(R.raw.igkeystore));
//        
//        // Get preferences reference
//        Util.prefs = PreferenceManager.getDefaultSharedPreferences(this);
//        
//        // Create a handler for this thread
//        Util.uiHandler = new Handler();
//        
//        // Store notification manager
//        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
//        
//        // Set up notification builder
//        notificationBuilder = new Notification.Builder(this);
//        notificationBuilder.setSmallIcon(R.drawable.ic_launcher);
//        notificationBuilder.setOngoing(true);
//        
//        // The PendingIntent to launch our activity if the user selects this notification
//        Intent activityIntent = new Intent(this, SLActivity.class);
//        activityIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
//        
//        PendingIntent pendingIntent = PendingIntent.getActivity(
//                this, 0, activityIntent, 0);
//        
//        notificationBuilder.setContentIntent(pendingIntent);
//        
//        // Start requesting locations
//        locationHandler = new ActivityLocationHandler(this, LocationManager.NETWORK_PROVIDER);
//    }
//
//    @Override
//    public int onStartCommand(Intent intent, int flags, int startId) {
////        // Build starting notification
////        Notification notification = buildNotification(
////            getText(R.string.service_starting),
////            getText(R.string.service_running)
////        );
////
////        // Send the notification.
////        notificationManager.notify(notificationID, notification);
//        
//        isStarted = true;
//        
//        return START_STICKY;
//    }
//
//    @Override
//    public void onDestroy() {
//        // Stop asking for GPS updates
//        locationHandler.stopUpdates();
//        
//        // Cancel the persistent notification.
//        notificationManager.cancel(notificationID);
//
//        // Tell the user we stopped.
//        Toast.makeText(this, R.string.service_stopping, Toast.LENGTH_SHORT).show();
//    }
//
//    @Override
//    public IBinder onBind(Intent intent) {
//        // Switch to GPS as client is open
//        Util.showToast("Service switching to GPS", this);
//        //locationHandler.changeProvider(LocationManager.GPS_PROVIDER);
//        
//        return binder;
//    }
//    
//    @Override
//    public void onRebind(Intent intent) {
//        onBind(intent);
//    }
//    
//    @Override
//    public boolean onUnbind(Intent intent) {
//        // Switch to Network as client is closed
//        Util.showToast("Service switching to Network", this);
//        locationHandler.changeProvider(LocationManager.NETWORK_PROVIDER);
//        
//        // Activity closed so set request context to service
//        requestManager.updateContext(this);
//        
//        return true;
//    }
//    
//    /**
//     * Registers a new location update listener
//     * @param listener New listener
//     */
//    public void addListener(SLServiceListener listener) {
//        listeners.add(listener);
//    }
//    
//    /**
//     * Removes an existing listener
//     * @param listener Listener to remove
//     */
//    public void removeListener(SLServiceListener listener) {
//        Iterator<SLServiceListener> itr = listeners.iterator();
//        
//        while (itr.hasNext()) {
//            if (itr.next() == listener) {
//                itr.remove();
//            }
//        }
//    }
//    
//    private Notification buildNotification(CharSequence tickerText, CharSequence titleText) {
//        notificationBuilder.setTicker(tickerText);
//        notificationBuilder.setContentTitle(titleText);
//        
//        return notificationBuilder.getNotification();
//    }
//    
//    public void updateLocation(Location location) {
//        this.currentLocation = location;
//        
//        // Notify listeners
//        for (SLServiceListener listener : listeners) {
//            listener.locationUpdated(location);
//        }
//        
////        notificationManager.cancel(notificationID);
////        
////        String notificationText = "Location is: " + location.getLatitude() + ", " + location.getLongitude();
////        Notification notification = buildNotification(notificationText, notificationText);
////        
////        notificationManager.notify(notificationID, notification);
//    }
//    
//    public boolean isStarted() {
//        return isStarted;
//    }
//    
//    public RequestManager getRequestManager() {
//        return requestManager;
//    }
//    
//    /**
//     * Interface for classes wanted location updates
//     */
//    public static interface SLServiceListener {
//        /**
//         * Called when a new location is ready
//         * for delivery
//         * @param newLocation New Location
//         */
//        public void locationUpdated(final Location newLocation);
//    }
//}
