package com.inflatablegoldfish.sociallocate.foursquare;

public class Venue {
    public String id;
    public String name;
    public double lat;
    public double lng;
    public double distance;
    
    public Venue(String id, String name, double lat, double lng, double distance) {
        this.id = id;
        this.name = name;
        this.lat = lat;
        this.lng = lng;
        this.distance = distance;
    }
    
    public String toString() {
        return "[" + id + "," + name + "," + "{" + lat + "," + lng + "}," + distance + "]";
    }
}
