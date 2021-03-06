package com.inflatablegoldfish.sociallocate.foursquare;

import java.util.List;

import com.inflatablegoldfish.sociallocate.Util;

import android.location.Location;

public class Venue {
    private String id;
    private String name;
    private Location location;
    private Float distance;
    private String icon;
    
    public Venue(String id, String name, double lat, double lng, String icon) {
        this.id = id;
        this.name = name;
        this.location = Util.getLocationObject(lat, lng);
        this.distance = null;
        this.icon = icon;
    }
    
    public void setDistanceFrom(Location location) {
        this.distance = location.distanceTo(this.location);
    }
    
    public String getID() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public Location getLocation() {
        return location;
    }
    
    public Float getDistance() {
        return distance;
    }
    
    public String getPrettyDistance() {
        return Util.makeDistancePretty(distance);
    }
    
    public String getIcon() {
        return icon;
    }
    
    public String toString() {
        return "[" + id + "," + name + "," + "{" + location.getLatitude() + "," + location.getLongitude() + "}," + distance + "]";
    }
    
    public static void calculateDistances(List<Venue> list, Location currentLocation) {
        for (Venue venue : list) {
            if (venue.getLocation() != null) {
                venue.setDistanceFrom(currentLocation);
            }
        }
    }
}
