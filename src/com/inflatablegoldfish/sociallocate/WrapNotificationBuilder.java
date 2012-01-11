package com.inflatablegoldfish.sociallocate;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import android.app.PendingIntent;
import android.content.Context;

public class WrapNotificationBuilder {
    private Class<?> nbClass;
    private Object notificationBuilder;

    public WrapNotificationBuilder(Context context) throws Exception {
        nbClass = Class.forName("android.app.Notification.Builder");
        Constructor<?> constructor = nbClass.getConstructor(new Class[] {Context.class});
        
        notificationBuilder = constructor.newInstance(context);
    }
    
    public Object setSmallIcon(int icon) {
        try {
            Method method = nbClass.getMethod("setSmallIcon", new Class[] {Integer.TYPE});
            return method.invoke(notificationBuilder, new Integer(icon));
        } catch (Exception e) {
            return null;
        }
    }
    
    public Object setAutoCancel(boolean autoCancel) {
        try {
            Method method = nbClass.getMethod("setAutoCancel", new Class[] {Boolean.TYPE});
            return method.invoke(notificationBuilder, new Boolean(autoCancel));
        } catch (Exception e) {
            return null;
        }
    }
    
    public Object setDefaults(int defaults) {
        try {
            Method method = nbClass.getMethod("setDefaults", new Class[] {Integer.TYPE});
            return method.invoke(notificationBuilder, new Integer(defaults));
        } catch (Exception e) {
            return null;
        }
    }
    
    public Object setTicker(CharSequence tickerText) {
        try {
            Method method = nbClass.getMethod("setTicker", new Class[] {CharSequence.class});
            return method.invoke(notificationBuilder, tickerText);
        } catch (Exception e) {
            return null;
        }
    }
    
    public Object setContentTitle(CharSequence title) {
        try {
            Method method = nbClass.getMethod("setContentTitle", new Class[] {CharSequence.class});
            return method.invoke(notificationBuilder, title);
        } catch (Exception e) {
            return null;
        }
    }
    
    public Object setContentText(CharSequence text) {
        try {
            Method method = nbClass.getMethod("setContentText", new Class[] {CharSequence.class});
            return method.invoke(notificationBuilder, text);
        } catch (Exception e) {
            return null;
        }
    }
    
    public Object setContentIntent(PendingIntent intent) {
        try {
            Method method = nbClass.getMethod("setContentIntent", new Class[] {PendingIntent.class});
            return method.invoke(notificationBuilder, intent);
        } catch (Exception e) {
            return null;
        }
    }
    
    public Object getNotification() {
        try {
            Method method = nbClass.getMethod("getNotification");
            return method.invoke(notificationBuilder);
        } catch (Exception e) {
            return null;
        }
    }
}
