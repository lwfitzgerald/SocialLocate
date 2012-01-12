package com.inflatablegoldfish.sociallocate;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.security.KeyStore;
import java.text.DecimalFormat;
import java.util.ArrayList;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import com.google.android.maps.GeoPoint;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.location.Location;
import android.os.Handler;
import android.util.Pair;
import android.widget.Toast;

public class Util {
    private static SSLSocketFactory IGSSLSocketFactory = null;
    
    public static Handler uiHandler;
    public static SharedPreferences prefs;
    
    private static final int TWO_MINUTES = 1000 * 60 * 2;
    
    private static final int CONNECT_TIMEOUT = 5; // Seconds
    private static final int READ_TIMEOUT = 10; // Seconds
    
    public static void initIGSSLSocketFactory(InputStream keystoreInputStream) {
        KeyStore keyStore;
        try {
            keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            
            keyStore.load(keystoreInputStream, "android".toCharArray());
            
            TrustManagerFactory tmf = 
                    TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(keyStore);
            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(null, tmf.getTrustManagers(), null);
            
            IGSSLSocketFactory = ctx.getSocketFactory();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Show a toast message
     * @param charSequence Message to show
     */
    public static void showToast(final CharSequence charSequence, final Context context) {
        Runnable runnable = new Runnable() {
            public void run() {
                Toast.makeText(context, charSequence, Toast.LENGTH_LONG).show();
            }
        };
        
        uiHandler.post(runnable);
    }
    
    public static String getURL(String url, boolean useIGOverride) throws IOException {
        HttpsURLConnection conn = (HttpsURLConnection) new URL(url).openConnection();
        
        if (useIGOverride) {
            // Override SSLSocketFactory to accept self-signed IG cert
            conn.setSSLSocketFactory(IGSSLSocketFactory);
        }
        
        conn.setRequestProperty("User-Agent", System.getProperties().
                    getProperty("http.agent") + " SocialLocate");
        
        conn.setConnectTimeout(CONNECT_TIMEOUT * 1000);
        conn.setReadTimeout(READ_TIMEOUT * 1000);
        
        return read(conn.getInputStream());
    }
    
    /**
     * Read request response
     * @param in Response stream
     * @return String containing response
     * @throws IOException
     */
    public static String read(InputStream in) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader r = new BufferedReader(new InputStreamReader(in), 1000);
        for (String line = r.readLine(); line != null; line = r.readLine()) {
            sb.append(line);
        }
        in.close();
        return sb.toString();
    }
    
    public static Bitmap getBitmap(String url) {
        Bitmap bm = null;
        try {
            URL aURL = new URL(url);
            URLConnection conn = aURL.openConnection();
            conn.connect();
            InputStream is = conn.getInputStream();
            BufferedInputStream bis = new BufferedInputStream(is);
            bm = BitmapFactory.decodeStream(new FlushedInputStream(is));
            bis.close();
            is.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return bm;
    }

    private static class FlushedInputStream extends FilterInputStream {
        public FlushedInputStream(InputStream inputStream) {
            super(inputStream);
        }

        @Override
        public long skip(long n) throws IOException {
            long totalBytesSkipped = 0L;
            while (totalBytesSkipped < n) {
                long bytesSkipped = in.skip(n - totalBytesSkipped);
                if (bytesSkipped == 0L) {
                    int b = read();
                    if (b < 0) {
                        break; // we reached EOF
                    } else {
                        bytesSkipped = 1; // we read one byte
                    }
                }
                totalBytesSkipped += bytesSkipped;
            }
            return totalBytesSkipped;
        }
    }
    
    /**
     * Get a location object from a given latitude
     * and longitude 
     * @param lat Latitude
     * @param lng Longitude
     * @return Location object for the location
     */
    public static Location getLocationObject(double lat, double lng) {
        Location location = new Location("sociallocate");
        location.setLatitude(lat);
        location.setLongitude(lng);
        return location;
    }
    
    /**
     * Get a GeoPoint from a location
     * @param location Location
     * @return GeoPoint version of provided location
     */
    public static GeoPoint getGeoPoint(Location location) {
        GeoPoint point = new GeoPoint(
            (int) (location.getLatitude() * 1E6),
            (int) (location.getLongitude() * 1E6)
        );
        
        return point;
    }
    
    /**
     * Determines whether one Location reading is better than the current Location fix
     * @param location The new Location that you want to evaluate
     * @param currentBestLocation The current Location fix, to which you want to compare the new one
     */
    public static boolean isBetterLocation(Location location, Location currentBestLocation) {
        if (currentBestLocation == null) {
            // A new location is always better than no location
            return true;
        }

        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
        boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
        boolean isNewer = timeDelta > 0;

        // If it's been more than two minutes since the current location, use the new location
        // because the user has likely moved
        if (isSignificantlyNewer) {
            return true;
        // If the new location is more than two minutes older, it must be worse
        } else if (isSignificantlyOlder) {
            return false;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(location.getProvider(),
                currentBestLocation.getProvider());

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            return true;
        } else if (isNewer && !isLessAccurate) {
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            return true;
        }
        return false;
    }
    
    /** Checks whether two providers are the same */
    private static boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }
    
    public static Location getCenter(Location[] points) {
        // Convert to radians
        ArrayList<Pair<Double, Double>> radianPoints = new ArrayList<Pair<Double, Double>>(points.length);
        
        // Store radian representations of the points
        for (int i=0; i < points.length; i++) {
            radianPoints.add(
                new Pair<Double, Double>(
                    Math.toRadians(points[i].getLatitude()),
                    Math.toRadians(points[i].getLongitude())
                )
            );
        }
        
        double x = 0, y = 0, z = 0;
        
        for (Pair<Double, Double> point : radianPoints) {
            x += Math.cos(point.first) * Math.cos(point.second);
            y += Math.cos(point.first) * Math.sin(point.second);
            z += Math.sin(point.first);
        }
        
        x /= points.length;
        y /= points.length;
        z /= points.length;
        
        double lng = Math.atan2(y, x);
        double hyp = Math.sqrt(x * x + y * y);
        double lat = Math.atan2(z, hyp);
        
        // Convert back to degrees
        lng = Math.toDegrees(lng);
        lat = Math.toDegrees(lat);
        
        Location location = new Location("sociallocate");
        location.setLatitude(lat);
        location.setLongitude(lng);
        
        return location;
    }
    
    public static String makeDistancePretty(Float distance) {
        distance = (float) distance.intValue();
        
        // Show in kilometers if greater than 100m
        if (distance >= 100) {
            DecimalFormat formatter = new DecimalFormat("0.0");
            
            return formatter.format(distance / 1000) + "km";
        } else {
            return distance + "m";
        }
    }
    
    public static Bitmap cropBitmap(Bitmap source, int desiredX, int desiredY) {
        desiredX = Math.min(desiredX, source.getWidth());
        desiredY = Math.min(desiredY, source.getHeight());
        
        Bitmap croppedImage = Bitmap.createBitmap(desiredX, desiredY, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(croppedImage);
     
        Rect srcRect = new Rect(0, 0, source.getWidth(), source.getHeight());
        Rect dstRect = new Rect(0, 0, desiredX, desiredY);
     
        int dx = (srcRect.width() - dstRect.width()) / 2;
        int dy = (srcRect.height() - dstRect.height());
     
        // If the srcRect is too big, use the center part of it.
        srcRect.inset(Math.max(0, dx), 0);
        srcRect.bottom = srcRect.bottom - dy;
     
        // If the dstRect is too big, use the center part of it.
        dstRect.inset(Math.max(0, -dx), 0);
     
        // Draw the cropped bitmap in the center
        canvas.drawBitmap(source, srcRect, dstRect, null);
     
        return croppedImage;
    }
}
