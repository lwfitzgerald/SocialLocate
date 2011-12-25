package com.inflatablegoldfish.sociallocate;

public class User {
    private int id;
    private Double lat;
    private Double lng;
    private String name;
    private String pic;
    
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
