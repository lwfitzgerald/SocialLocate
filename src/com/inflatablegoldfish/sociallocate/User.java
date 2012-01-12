package com.inflatablegoldfish.sociallocate;

import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import com.ocpsoft.pretty.time.PrettyTime;

import android.location.Location;

public class User {
    private int id;
    private Location location;
    private Float distance;
    private Date lastUpdated;
    private String name;
    private String pic;
    
    public User(int id, double lat, double lng, long lastUpdated, String name, String pic) {
        this(id, lat, lng, lastUpdated);
        
        this.name = name;
        this.pic = pic;
    }
    
    public User(int id, String name, String pic) {
        this.id = id;
        this.location = null;
        this.distance = null;
        this.name = name;
        this.pic = pic;
    }
    
    public User(int id, double lat, double lng, long lastUpdated) {
        this.id = id;
        this.location = Util.getLocationObject(lat, lng);
        this.distance = null;
        this.lastUpdated = new Date(Math.min(System.currentTimeMillis(), lastUpdated * 1000));
    }
    
    public void updateFromUser(User toUpdateFrom) {
        this.location = toUpdateFrom.location;
        this.lastUpdated = toUpdateFrom.lastUpdated;
    }
    
    public int getId() {
        return id;
    }

    public Location getLocation() {
        return location;
    }
    
    public Date getLastUpdated() {
        return lastUpdated;
    }
    
    public String getPrettyLastUpdated() {
        PrettyTime p = new PrettyTime();
        return p.format(lastUpdated);
    }
    
    public Float getDistance() {
        return distance;
    }
    
    public String getPrettyDistance() {
        return Util.makeDistancePretty(distance);
    }
    
    public void setDistanceFrom(Location location) {
        this.distance = location.distanceTo(this.location);
    }

    public String getName() {
        return name;
    }

    public String getPic() {
        return pic;
    }
    
    public static void calculateDistances(List<User> list, Location currentLocation) {
        for (User user : list) {
            if (user.getLocation() != null) {
                user.setDistanceFrom(currentLocation);
            }
        }
    }
    
    public static void sortByDistance(List<User> list) {
        Collections.sort(list, new Comparator<User>() {
            public int compare(User lhs, User rhs) {
                if (lhs.getDistance() == rhs.getDistance()) {
                    return 0;
                }
                
                return lhs.getDistance() < rhs.getDistance() ? -1 : 1;
            }
        });
    }
}
