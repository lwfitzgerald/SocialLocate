package com.inflatablegoldfish.sociallocate;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.location.Location;

public class User {
    private int id;
    private Location location;
    private Float distance;
    private String name;
    private String pic;
    private boolean updatedInLastFetch = true;
    
    public User(int id, double lat, double lng, String name, String pic) {
        this.id = id;
        this.location = Util.getLocationObject(lat, lng);
        this.distance = null;
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
    
    public void updateLocation(Location location) {
        this.location = location;
    }
    
    public void markUpdated(boolean updated) {
        this.updatedInLastFetch = updated;
    }
    
    public boolean wasUpdatedInLastFetch() {
        return updatedInLastFetch;
    }
    
    public int getId() {
        return id;
    }

    public Location getLocation() {
        return location;
    }
    
    public Float getDistance() {
        return distance;
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
