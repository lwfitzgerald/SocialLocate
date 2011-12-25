package com.inflatablegoldfish.sociallocate;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;

import org.json.JSONArray;
import org.json.JSONObject;

import android.location.Location;

import com.inflatablegoldfish.sociallocate.request.Request.RequestResult;
import com.inflatablegoldfish.sociallocate.request.RequestListener;

public class SocialLocate {
    private static final String URL_PREFIX = "https://www.inflatablegoldfish.com/sociallocate/api.php?";
    
    public SocialLocate() {
        CookieManager manager = new CookieManager();
        manager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        CookieHandler.setDefault(manager);
    }
    
    public RequestResult auth(String accessToken, RequestListener<User[]> listener) {
        String url = URL_PREFIX + "action=auth"
                + "&access_token=" + accessToken;

        try {
            String response = Util.getURL(url, true);
            
            JSONObject jsonObject = new JSONObject(response);
            
            if (jsonObject.getInt("auth_status") == 0) {
                return RequestResult.AUTHFAIL;
            } else {
                listener.onComplete(null);
                return RequestResult.SUCCESS;
            }
        } catch (Exception e) {
            return RequestResult.ERROR;
        }
    }
    
    public RequestResult initialFetch(RequestListener<User[]> listener) {
        String url = URL_PREFIX + "action=initial_fetch";
        
        try {
            String response = Util.getURL(url, true);
            
            JSONObject jsonObject = new JSONObject(response);
            
            if (jsonObject.getInt("auth_status") == 0) {
                return RequestResult.AUTHFAIL;
            } else {
                JSONObject ownDetails = jsonObject.getJSONObject("own_details");
                JSONArray friends = jsonObject.getJSONArray("friends");
                
                User[] toReturn = new User[friends.length() + 1];
                
                // Store own details in first user slot
                toReturn[0] = new User(
                    ownDetails.getInt("id"),
                    ownDetails.getString("name"),
                    ownDetails.getString("pic")
                );
                
                for (int i=0; i < friends.length(); i++) {
                    JSONObject friend = friends.getJSONObject(i);
                    toReturn[i+1] = new User(
                        friend.getInt("id"),
                        friend.getDouble("lat"),
                        friend.getDouble("long"),
                        friend.getString("name"),
                        friend.getString("pic")
                    );
                }
                
                listener.onComplete(toReturn);
                return RequestResult.SUCCESS;
            }
        } catch (Exception e) {
            return RequestResult.ERROR;
        }
    }
    
    public RequestResult fetch(RequestListener<User[]> listener) {
        String url = URL_PREFIX + "action=fetch";
        
        try {
            String response = Util.getURL(url, true);
            
            JSONObject jsonObject = new JSONObject(response);
            
            if (jsonObject.getInt("auth_status") == 0) {
                return RequestResult.AUTHFAIL;
            } else {
                JSONArray friends = jsonObject.getJSONArray("friends");
                
                User[] toReturn = new User[friends.length()];
                
                for (int i=0; i < friends.length(); i++) {
                    JSONObject friend = friends.getJSONObject(i);
                    toReturn[i] = new User(
                        friend.getInt("id"),
                        friend.getDouble("lat"),
                        friend.getDouble("long"),
                        friend.getString("name"),
                        friend.getString("pic")
                    );
                }
                
                listener.onComplete(toReturn);
                return RequestResult.SUCCESS;
            }
        } catch (Exception e) {
            return RequestResult.ERROR;
        }
    }
    
    public RequestResult updateLocation(Location location, RequestListener<User[]> listener) {
        String url = URL_PREFIX + "action=update_location" + "&lat="
                + location.getLatitude() + "&long=" + location.getLongitude();

        try {
            String response = Util.getURL(url, true);
            
            JSONObject jsonObject = new JSONObject(response);
            
            if (jsonObject.getInt("auth_status") == 0) {
                return RequestResult.AUTHFAIL;
            } else {
                listener.onComplete(null);
                return RequestResult.SUCCESS;
            }
        } catch (Exception e) {
            return RequestResult.ERROR;
        }
    }
}
