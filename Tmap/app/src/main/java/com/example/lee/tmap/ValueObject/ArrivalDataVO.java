package com.example.lee.tmap.ValueObject;

/**
 * Created by Lee on 2016-11-11.
 */
public class ArrivalDataVO {

    private String arrival_name;
    private double arrival_longitude;
    private double arrival_latitude;

    public ArrivalDataVO() {

    }

    public ArrivalDataVO(String arrival_name, double arrival_longitude, double arrival_latitude) {
        this.arrival_name = arrival_name;
        this.arrival_longitude = arrival_longitude;
        this.arrival_latitude = arrival_latitude;
    }

    public String getArrival_name() {
        return arrival_name;
    }

    public void setArrival_name(String arrival_name) {
        this.arrival_name = arrival_name;
    }

    public double getArrival_longitude() {
        return arrival_longitude;
    }

    public void setArrival_longitude(double arrival_longitude) {
        this.arrival_longitude = arrival_longitude;
    }

    public double getArrival_latitude() {
        return arrival_latitude;
    }

    public void setArrival_latitude(double arrival_latitude) {
        this.arrival_latitude = arrival_latitude;
    }
}
