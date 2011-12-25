package com.inflatablegoldfish.sociallocate;

public class User {
    private int id;
    private Double lat;
    private Double lng;
    private String name;
    private String pic;
    private boolean updatedInLastFetch = true;
    
    public User(int id, double lat, double lng, String name, String pic) {
        this.id = id;
        this.lat = lat;
        this.lng = lng;
        this.name = name;
        this.pic = pic;
    }
    
    public User(int id, String name, String pic) {
        this.id = id;
        this.lat = null;
        this.lng = null;
        this.name = name;
        this.pic = pic;
    }
    
    public void updateLocation(double lat, double lng) {
        this.lat = lat;
        this.lng = lng;
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

    public Double getLat() {
        return lat;
    }

    public Double getLng() {
        return lng;
    }

    public String getName() {
        return name;
    }

    public String getPic() {
        return pic;
    }
}
