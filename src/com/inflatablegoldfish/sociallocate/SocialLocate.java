package com.inflatablegoldfish.sociallocate;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import android.location.Location;

import com.inflatablegoldfish.sociallocate.request.Request.RequestResult;
import com.inflatablegoldfish.sociallocate.request.Request.ResultCode;

public class SocialLocate {
    private static final String URL_PREFIX = "https://www.inflatablegoldfish.com/sociallocate/api.php?";
    
    public static final int UPDATES_PER_HOUR = 60;
    
    public SocialLocate() {
        CookieManager manager = new CookieManager();
        manager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        CookieHandler.setDefault(manager);
    }
    
    public RequestResult<List<User>> auth(String accessToken) {
        String url = URL_PREFIX + "action=auth"
                + "&access_token=" + accessToken;

        try {
            String response = Util.getURL(url, true);
            
            JSONObject jsonObject = new JSONObject(response);
            
            if (jsonObject.getInt("auth_status") == 0) {
                return new RequestResult<List<User>>(null, ResultCode.AUTHFAIL);
            } else {
                return new RequestResult<List<User>>(null, ResultCode.SUCCESS);
            }
        } catch (Exception e) {
            return new RequestResult<List<User>>(null, ResultCode.ERROR);
        }
    }
    
    public RequestResult<List<User>> initialFetch() {
        String url = URL_PREFIX + "action=initial_fetch";
        
        try {
            String response = Util.getURL(url, true);
            
            JSONObject jsonObject = new JSONObject(response);
            
            if (jsonObject.getInt("auth_status") == 0) {
                return new RequestResult<List<User>>(null, ResultCode.AUTHFAIL);
            } else {
                JSONObject ownDetails = jsonObject.getJSONObject("own_details");
                JSONArray friends = jsonObject.getJSONArray("friends");
                
                List<User> toReturn = new ArrayList<User>(friends.length() + 1);
                
                // Store own details in first user slot
                toReturn.add(
                    new User(
                        ownDetails.getInt("id"),
                        ownDetails.getString("name"),
                        ownDetails.getString("pic")
                    )
                );
                
                for (int i=0; i < friends.length(); i++) {
                    JSONObject friend = friends.getJSONObject(i);
                    
                    toReturn.add(
                        new User(
                            friend.getInt("id"),
                            friend.getDouble("lat"),
                            friend.getDouble("lng"),
                            friend.getString("name"),
                            friend.getString("pic")
                        )
                    );
                }
                
                return new RequestResult<List<User>>(toReturn, ResultCode.SUCCESS);
            }
        } catch (Exception e) {
            return new RequestResult<List<User>>(null, ResultCode.ERROR);
        }
    }
    
    public RequestResult<List<User>> fetch() {
        String url = URL_PREFIX + "action=fetch";
        
        try {
            String response = Util.getURL(url, true);
            
            JSONObject jsonObject = new JSONObject(response);
            
            if (jsonObject.getInt("auth_status") == 0) {
                return new RequestResult<List<User>>(null, ResultCode.AUTHFAIL);
            } else {
                JSONArray friends = jsonObject.getJSONArray("friends");
                
                List<User> toReturn = new ArrayList<User>(friends.length());
                
                for (int i=0; i < friends.length(); i++) {
                    JSONObject friend = friends.getJSONObject(i);
                    
                    toReturn.add(
                        new User(
                            friend.getInt("id"),
                            friend.getDouble("lat"),
                            friend.getDouble("long"),
                            friend.getString("name"),
                            friend.getString("pic")
                        )
                    );
                }
                
                return new RequestResult<List<User>>(toReturn, ResultCode.SUCCESS);
            }
        } catch (Exception e) {
            return new RequestResult<List<User>>(null, ResultCode.ERROR);
        }
    }
    
    public RequestResult<List<User>> updateLocation(Location location) {
        String url = URL_PREFIX + "action=update_location" + "&lat="
                + location.getLatitude() + "&lng=" + location.getLongitude();

        try {
            String response = Util.getURL(url, true);
            
            JSONObject jsonObject = new JSONObject(response);
            
            if (jsonObject.getInt("auth_status") == 0) {
                return new RequestResult<List<User>>(null, ResultCode.AUTHFAIL);
            } else {
                return new RequestResult<List<User>>(null, ResultCode.SUCCESS);
            }
        } catch (Exception e) {
            return new RequestResult<List<User>>(null, ResultCode.ERROR);
        }
    }
}
