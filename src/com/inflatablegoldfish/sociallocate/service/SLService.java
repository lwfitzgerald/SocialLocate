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

        // Display a notification about us starting.  We put an icon in the status bar.
        showNotification();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        locationHandler = new LocationHandler(this);
        
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

    /**
     * Show a notification while this service is running.
     */
    private void showNotification() {
        // In this sample, we'll use the same text for the ticker and the expanded notification
        CharSequence text = getText(R.string.service_starting);

        // Set the icon, scrolling text and timestamp
        Notification notification = new Notification(R.drawable.ic_launcher, text,
                System.currentTimeMillis());

        // The PendingIntent to launch our activity if the user selects this notification
        Intent activityIntent = new Intent(this, SLActivity.class);
        activityIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
                getBaseContext(), 0, activityIntent, 0);

        // Set the info for the views that show in the notification panel.
        notification.setLatestEventInfo(this, getText(R.string.notification_text),
                       text, pendingIntent);

        // Send the notification.
        notificationManager.notify(notificationID, notification);
    }
    
    public void updateLocation(Location location) {
        this.currentLocation = location;
        
        notificationManager.cancel(notificationID);
        
        Notification notification = new Notification(R.drawable.ic_launcher,
                "Location is: " + location.getLatitude() + ", "
                        + location.getLongitude(),
                System.currentTimeMillis());
        
        // The PendingIntent to launch our activity if the user selects this notification
        Intent activityIntent = new Intent(this, SLActivity.class);
        activityIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
                getBaseContext(), 0, activityIntent, 0);

        // Set the info for the views that show in the notification panel.
        notification.setLatestEventInfo(this, getText(R.string.notification_text),
                "Location is: " + location.getLatitude() + ", " + location.getLongitude(), pendingIntent);
        
        notificationManager.notify(notificationID, notification);
    }
}
