package com.example.lee.tmap.ValueObject;

/**
 * Created by Lee on 2016-12-06.
 */
public class RecentPathListVO {

    private String arrivalName;
    private double longitudeValue;
    private double latitudeValue;

    public RecentPathListVO() {
    }

    public RecentPathListVO(String arrivalName, double latitudeValue, double longitudeValue) {
        this.arrivalName = arrivalName;
        this.latitudeValue = latitudeValue;
        this.longitudeValue = longitudeValue;
    }

    public String getArrivalName() {
        return arrivalName;
    }

    public void setArrivalName(String arrivalName) {
        this.arrivalName = arrivalName;
    }

    public double getLatitudeValue() {
        return latitudeValue;
    }

    public void setLatitudeValue(double latitudeValue) {
        this.latitudeValue = latitudeValue;
    }

    public double getLongitudeValue() {
        return longitudeValue;
    }

    public void setLongitudeValue(double longitudeValue) {
        this.longitudeValue = longitudeValue;
    }

    @Override
    public String toString() {
        return "RecentPathListVO{" +
                "arrivalName='" + arrivalName + '\'' +
                ", longitudeValue=" + longitudeValue +
                ", latitudeValue=" + latitudeValue +
                '}';
    }
}
