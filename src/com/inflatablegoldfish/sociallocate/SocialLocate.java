package com.inflatablegoldfish.sociallocate;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpCookie;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.SharedPreferences;
import android.location.Location;
import android.util.Log;

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
                Log.w("SocialLocate", "Auth fail in authing");
                return new RequestResult<List<User>>(null, ResultCode.AUTHFAIL);
            } else {
                Log.i("SocialLocate", "Authing OK");
                return new RequestResult<List<User>>(null, ResultCode.SUCCESS);
            }
        } catch (Exception e) {
            Log.e("SocialLocate", "Error in authing");
            return new RequestResult<List<User>>(null, ResultCode.ERROR);
        }
    }
    
    public RequestResult<List<User>> initialFetch() {
        String url = URL_PREFIX + "action=initial_fetch";
        
        try {
            String response = Util.getURL(url, true);
            
            JSONObject jsonObject = new JSONObject(response);
            
            if (jsonObject.getInt("auth_status") == 0) {
                Log.w("SocialLocate", "Auth fail in initial fetch");
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
            Log.e("SocialLocate", "Error in initial fetch");
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
                Log.w("SocialLocate", "Auth fail in update");
                return new RequestResult<List<User>>(null, ResultCode.AUTHFAIL);
            } else {
                return new RequestResult<List<User>>(null, ResultCode.SUCCESS);
            }
        } catch (Exception e) {
            Log.e("SocialLocate", "Error in update");
            return new RequestResult<List<User>>(null, ResultCode.ERROR);
        }
    }
    
    public void saveCookies() {
        // Save cookies
        HashMap<String, String> cookie = cookieToStrings();
        if (cookie != null) {
            SharedPreferences.Editor editor = Util.prefs.edit();
            editor.putString("cookie_name", cookie.get("name"));
            editor.putString("cookie_value", cookie.get("value"));
            editor.putString("cookie_domain", cookie.get("domain"));
            editor.putString("cookie_path", cookie.get("path"));
            editor.putString("cookie_expires", cookie.get("expires"));
            editor.putString("cookie_version", cookie.get("version"));
            editor.commit();
        }
    }
    
    public void loadCookies() {
        if (Util.prefs.contains("cookie_name")) {
            Log.i("SocialLocate", "Cookie loading...");
            HttpCookie cookie = new HttpCookie(
                Util.prefs.getString("cookie_name", ""),
                Util.prefs.getString("cookie_value", "")
            );
            
            cookie.setDomain(Util.prefs.getString("cookie_domain", ""));
            cookie.setPath(Util.prefs.getString("cookie_path", ""));
            cookie.setVersion(Integer.valueOf(Util.prefs.getString("cookie_version", "")));
            cookie.setMaxAge(Long.valueOf(Util.prefs.getString("cookie_expires", "")));
            
            CookieManager mgr = (CookieManager) CookieHandler.getDefault();
            
            try {
                mgr.getCookieStore().add(new URI(URL_PREFIX), cookie);
            } catch (URISyntaxException e) {}
        }
    }
    
    public void clearCookies() {
        SharedPreferences.Editor editor = Util.prefs.edit();
        editor.remove("cookie_name");
        editor.remove("cookie_value");
        editor.remove("cookie_domain");
        editor.remove("cookie_path");
        editor.remove("cookie_expires");
        editor.remove("cookie_version");
        editor.commit();
    }
    
    private HashMap<String, String> cookieToStrings() {
        HashMap<String, String> map = new HashMap<String, String>();
        
        CookieManager mgr = (CookieManager) CookieHandler.getDefault();
        
        List<HttpCookie> cookieList = null;
        
        try {
            cookieList = mgr.getCookieStore().get(new URI(URL_PREFIX));
        } catch (URISyntaxException e) {}
        
        if (cookieList.size() == 0) {
            return null;
        }
        
        HttpCookie cookie = cookieList.get(0);
        
        map.put("name", cookie.getName());
        map.put("value", cookie.getValue());
        map.put("domain", cookie.getDomain());
        map.put("path", cookie.getPath());
        map.put("expires", String.valueOf(cookie.getMaxAge()));
        map.put("version", String.valueOf(cookie.getVersion()));
        
        return map;
    }
}
