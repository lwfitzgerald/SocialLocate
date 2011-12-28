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
import android.os.Handler;
import android.widget.Toast;

public class Util {
    private static SSLSocketFactory IGSSLSocketFactory = null;
    
    public static Handler uiHandler;
    public static SharedPreferences prefs;
    
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
}
