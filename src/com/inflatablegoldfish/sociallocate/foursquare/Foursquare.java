package com.inflatablegoldfish.sociallocate.foursquare;

import java.util.LinkedList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import android.location.Location;
import android.util.Log;

import com.inflatablegoldfish.sociallocate.Util;
import com.inflatablegoldfish.sociallocate.request.Request.RequestResult;
import com.inflatablegoldfish.sociallocate.request.Request.ResultCode;

public class Foursquare {
    private static final String clientID = "EKQQB5PSUA3LJZJB0VK2IJJPFQ3QD0BX3ICW4ZW2NMOIU5ZR";
    private static final String clientSecret = "4ECVND4FTFX0DELN1BL2E2UKRWF3F24VSNPVG5B3DXF5544L";
    private static final String lastModified = "20120101";
    
    private static final String emptyCategory = "https://foursquare.com/img/categories/none_256.png";
    
    // Search radius in metres
    public static final int SEARCH_RADIUS = 1000;
    private static final int RESULT_LIMIT = 10;
    
    public RequestResult<List<Venue>> getVenuesNear(Location location, Location ourLocation) {
        String url = "https://api.foursquare.com/v2/venues/explore?"
                + "ll=" + location.getLatitude() + ',' + location.getLongitude()
                + "&radius=" + SEARCH_RADIUS
                + "&limit=" + RESULT_LIMIT
                + "&client_id=" + clientID
                + "&client_secret=" + clientSecret
                + "&v=" + lastModified;
        
        try {
            String response = Util.getURL(url, false);
            
            JSONObject jsonObject = new JSONObject(response);
            
            JSONArray venues = jsonObject.getJSONObject("response")
                    .getJSONArray("groups").getJSONObject(0).getJSONArray("items");
            
            List<Venue> venueList = new LinkedList<Venue>();
            
            for (int i=0; i < venues.length(); i++) {
                JSONObject venueObj = venues.getJSONObject(i).getJSONObject("venue");
                JSONObject venueLoc = venueObj.getJSONObject("location");
                JSONArray venueCats = venueObj.getJSONArray("categories");
                
                Venue venue = new Venue(
                    venueObj.getString("id"),
                    venueObj.getString("name"),
                    venueLoc.getDouble("lat"),
                    venueLoc.getDouble("lng"),
                    (venueCats.length() > 0) ? venueCats.getJSONObject(0)
                            .getJSONObject("icon").getString("prefix")
                            + "256.png" : emptyCategory
                );
                
                // Calculate and store distance
                venue.setDistanceFrom(ourLocation);
                
                venueList.add(venue);
            }
            
            Log.d("SocialLocate", "Fetching venues OK");
            return new RequestResult<List<Venue>>(venueList, ResultCode.SUCCESS);
        } catch (Exception e) {
            Log.d("SocialLocate", "Error in fetching venues");
            return new RequestResult<List<Venue>>(null, ResultCode.ERROR);
        }
    }
}
