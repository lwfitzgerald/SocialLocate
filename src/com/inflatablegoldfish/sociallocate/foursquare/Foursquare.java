package com.inflatablegoldfish.sociallocate.foursquare;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import android.location.Location;

import com.inflatablegoldfish.sociallocate.Util;
import com.inflatablegoldfish.sociallocate.request.RequestListener;

public class Foursquare {
    private static final String clientID = "EKQQB5PSUA3LJZJB0VK2IJJPFQ3QD0BX3ICW4ZW2NMOIU5ZR";
    private static final String clientSecret = "4ECVND4FTFX0DELN1BL2E2UKRWF3F24VSNPVG5B3DXF5544L";
    private static final String lastModified = "20111222";
    
    // Search radius in metres
    public static final int SEARCH_RADIUS = 1000;
    
    public void startFindMeetingPlaces(final Location ourLocation,
            final Location[] friendLocations, final FSRequestListener listener) {
        new Thread(new Runnable() {
            public void run() {
                Foursquare.this.findMeetingPlaces(ourLocation, friendLocations, listener);
            }
        }).start();
    }
    
    private void findMeetingPlaces(Location ourLocation, Location[] friendLocations, FSRequestListener listener) {
        // Combine our location and friend locations
        Location[] points = new Location[friendLocations.length+1];
        points[0] = ourLocation;
        
        for (int i=0; i < friendLocations.length; i++) {
            points[i+1] = friendLocations[i];
        }
        
        Location center = Util.getCenter(points);
        
        Venue[] venues = null;
        
        try {
            venues = getVenuesNear(ourLocation, center);
        } catch (Exception e) {
            listener.onError();
            return;
        }
        
        listener.onComplete(venues);
    }
    
    private Venue[] getVenuesNear(Location ourLocation, Location center) throws Exception {
        String url = "https://api.foursquare.com/v2/venues/search?"
                + "ll=" + center.getLatitude() + ',' + center.getLongitude()
                + "&client_id=" + clientID
                + "&client_secret=" + clientSecret
                + "&v=" + lastModified;
        
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        
        conn.setRequestProperty("User-Agent", System.getProperties().
                    getProperty("http.agent") + " SocialLocate");
        
        String response = Util.read(conn.getInputStream());
        
        JSONObject jsonObject = new JSONObject(response);
        
        JSONArray venues = jsonObject.getJSONObject("response").getJSONArray("venues");
        
        List<Venue> venueList = new LinkedList<Venue>();
        
        for (int i=0; i < venues.length(); i++) {
            JSONObject venueObj = venues.getJSONObject(i);
            JSONObject venueLoc = venueObj.getJSONObject("location");
            
            Venue venue = new Venue(
                venueObj.getString("id"),
                venueObj.getString("name"),
                venueLoc.getDouble("lat"),
                venueLoc.getDouble("lng")
            );
            
            // Calculate and store distance
            venue.setDistanceFrom(ourLocation);
            
            venueList.add(venue);
        }
        
        return venueList.toArray(new Venue[0]);
    }
    
    /**
     * Request completion listener
     */
    public static abstract class FSRequestListener implements RequestListener<Venue[]> {
        public void onComplete(Venue[] venues) {
            onComplete(true, venues);
        }
        
        public abstract void onComplete(boolean authed, Venue[] venues);
        
        public abstract void onError();
        
        public abstract void onRetry(int attemptNo);
    }
}
