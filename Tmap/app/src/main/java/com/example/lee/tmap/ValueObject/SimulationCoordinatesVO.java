package com.example.lee.tmap.ValueObject;

import java.util.ArrayList;

/**
 * Created by Lee on 2016-11-23.
 */
public class SimulationCoordinatesVO {

    private double latitude;
    private double longitude;

    public SimulationCoordinatesVO() { }

    public SimulationCoordinatesVO(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    @Override
    public String toString() {
        return "SimulationCoordinatesVO{" +
                "latitude=" + latitude +
                ", longitude=" + longitude +
                '}';
    }
}
