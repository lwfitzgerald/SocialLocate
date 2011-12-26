package com.inflatablegoldfish.sociallocate.service;

import com.inflatablegoldfish.sociallocate.R;
import com.inflatablegoldfish.sociallocate.SLActivity;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Binder;
import android.os.IBinder;
import android.widget.Toast;

public class SLService extends Service {
    private NotificationManager notificationManager;

    private LocationHandler locationHandler;
    private Location currentLocation;

    // Unique Identification Number for the Notification.
    // We use it on Notification start, and to cancel it.
    private int notificationID = R.string.notification_id;
    
    // This is the object that receives interactions from clients.  See
    // RemoteService for a more complete example.
    private final IBinder binder = new SLServiceBinder();
    
    private boolean isStarted = false;
    
    private Notification.Builder notificationBuilder;

    /**
     * Class for clients to access.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with
     * IPC.
     */
    public class SLServiceBinder extends Binder {
        public SLService getService() {
            return SLService.this;
        }
    }

    @Override
    public void onCreate() {
        notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

        // Set up notification builder
        notificationBuilder = new Notification.Builder(this);
        notificationBuilder.setSmallIcon(R.drawable.ic_launcher);
        notificationBuilder.setOngoing(true);

        // The PendingIntent to launch our activity if the user selects this notification
        Intent activityIntent = new Intent(this, SLActivity.class);
        activityIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
                getBaseContext(), 0, activityIntent, 0);
        
        notificationBuilder.setContentIntent(pendingIntent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        locationHandler = new LocationHandler(this);

        // Build starting notification
        Notification notification = buildNotification(
            getText(R.string.service_starting),
            getText(R.string.service_running)
        );

        // Send the notification.
        notificationManager.notify(notificationID, notification);
        
        isStarted = true;
        
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        // Stop asking for GPS updates
        locationHandler.stopUpdates();
        
        // Cancel the persistent notification.
        notificationManager.cancel(notificationID);

        // Tell the user we stopped.
        Toast.makeText(this, R.string.service_stopping, Toast.LENGTH_SHORT).show();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
    
    private Notification buildNotification(CharSequence tickerText, CharSequence titleText) {
        notificationBuilder.setTicker(tickerText);
        notificationBuilder.setContentTitle(titleText);
        
        return notificationBuilder.getNotification();
    }
    
    public void updateLocation(Location location) {
        this.currentLocation = location;
        
        notificationManager.cancel(notificationID);
        
        String notificationText = "Location is: " + location.getLatitude() + ", " + location.getLongitude();
        Notification notification = buildNotification(notificationText, notificationText);
        
        notificationManager.notify(notificationID, notification);
    }
    
    public boolean isStarted() {
        return isStarted;
    }
}
