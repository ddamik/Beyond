package com.example.lee.tmap.ValueObject;

/**
 * Created by Lee on 2016-11-19.
 */
public class GeoMetryVO {

    private double longitude;
    private double latitude;

    public GeoMetryVO() {
    }

    public GeoMetryVO(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    @Override
    public String toString() {
        return "GeoMetryVO{" +
                "latitude=" + latitude +
                ", longitude=" + longitude +
                '}';
    }
}
