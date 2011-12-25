package com.inflatablegoldfish.sociallocate.foursquare;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import com.inflatablegoldfish.sociallocate.Util;
import com.inflatablegoldfish.sociallocate.request.RequestListener;

public class Foursquare {
    private static final String clientID = "EKQQB5PSUA3LJZJB0VK2IJJPFQ3QD0BX3ICW4ZW2NMOIU5ZR";
    private static final String clientSecret = "4ECVND4FTFX0DELN1BL2E2UKRWF3F24VSNPVG5B3DXF5544L";
    private static final String lastModified = "20111222";
    
    public void startFindMeetingPlaces(final double[] ourLocation,
            final double[][] friendLocations, final FSRequestListener listener) {
        new Thread(new Runnable() {
            public void run() {
                Foursquare.this.findMeetingPlaces(ourLocation, friendLocations, listener);
            }
        }).start();
    }
    
    private void findMeetingPlaces(double[] ourLocation, double[][] friendLocations, FSRequestListener listener) {
        // Combine our location and friend locations
        double[][] points = new double[friendLocations.length+1][];
        points[0] = ourLocation.clone();
        
        for (int i=0; i < friendLocations.length; i++) {
            points[i+1] = friendLocations[i];
        }
        
        double[] center = getCenter(points);
        
        Venue[] venues = null;
        
        try {
            venues = getVenuesNear(ourLocation, center);
        } catch (Exception e) {
            listener.onError();
            return;
        }
        
        listener.onComplete(venues);
    }
    
    private double[] getCenter(double[][] points) {
        // Convert to radians
        for (int i=0; i < points.length; i++) {
            points[i][0] = Math.toRadians(points[i][0]);
            points[i][1] = Math.toRadians(points[i][1]);
        }
        
        double x = 0, y = 0, z = 0;
        
        for (double[] point : points) {
            x += Math.cos(point[0]) * Math.cos(point[1]);
            y += Math.cos(point[0]) * Math.sin(point[1]);
            z += Math.sin(point[0]);
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
        
        return new double[] { lat, lng };
    }
    
    private Venue[] getVenuesNear(double[] ourLocation, double[] location) throws Exception {
        String url = "https://api.foursquare.com/v2/venues/search?"
                + "ll=" + location[0] + ',' + location[1]
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
            JSONObject venue = venues.getJSONObject(i);
            JSONObject venueLoc = venue.getJSONObject("location");
            
            venueList.add(new Venue(
                venue.getString("id"),
                venue.getString("name"),
                venueLoc.getDouble("lat"),
                venueLoc.getDouble("lng"),
                
                // Calculate distance between us and venue
                calculateDistance(
                    ourLocation,
                    new double[] {
                        venueLoc.getDouble("lat"),
                        venueLoc.getDouble("lng")
                    }
                )
            ));
        }
        
        return venueList.toArray(new Venue[0]);
    }
    
    private double calculateDistance(double[] point1, double[] point2) {
        double R = 6372797.56; // Radius of the earth in m
        
        // Convert to radians
        double lat1 = Math.toRadians(point1[0]);
        double lng1 = Math.toRadians(point1[1]);
        double lat2 = Math.toRadians(point2[0]);
        double lng2 = Math.toRadians(point2[1]);
        
        double dLat = lat2 - lat1;
        double dLon = lng2 - lng1; 
        
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                Math.cos(lat1) * Math.cos(lat2) * 
                Math.sin(dLon/2) * Math.sin(dLon/2); 
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a)); 
        
        return R * c; // Distance in m
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
