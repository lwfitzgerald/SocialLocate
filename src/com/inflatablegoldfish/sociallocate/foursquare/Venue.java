package com.inflatablegoldfish.sociallocate.foursquare;

import com.inflatablegoldfish.sociallocate.Util;

import android.location.Location;

public class Venue {
    public String id;
    public String name;
    public Location location;
    public Float distance;
    
    public Venue(String id, String name, double lat, double lng) {
        this.id = id;
        this.name = name;
        this.location = Util.getLocationObject(lat, lng);
        this.distance = null;
    }
    
    public void setDistanceFrom(Location location) {
        this.distance = location.distanceTo(this.location);
    }
    
    public String toString() {
        return "[" + id + "," + name + "," + "{" + location.getLatitude() + "," + location.getLongitude() + "}," + distance + "]";
    }
}
