package com.inflatablegoldfish.sociallocate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.KeyStore;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Handler;
import android.widget.Toast;

public class Util {
    private static SSLSocketFactory IGSSLSocketFactory = null;
    
    public static Handler uiHandler;
    public static SharedPreferences prefs;
    
    private static final int TWO_MINUTES = 1000 * 60 * 2;
    
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
                Toast.makeText(context, charSequence, Toast.LENGTH_SHORT).show();
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
}
