package com.example.lee.tmap.ValueObject;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

/**
 * Created by Lee on 2016-12-07.
 */
public class GuideDataVO {
    private int index;
    private int totalDistance;
    private int totalFare;
    private int totalTime;
    private int turnType;
    private int distance;
    private String description;
    private double longitude;
    private double latitude;

    public GuideDataVO() { }

    public GuideDataVO(int index, int turnType, double latitude, double longitude) {
        this.index = index;
        this.turnType = turnType;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public GuideDataVO(String description, int distance, int index, double latitude, double longitude, int turnType) {
        this.description = description;
        this.distance = distance;
        this.index = index;
        this.latitude = latitude;
        this.longitude = longitude;
        this.turnType = turnType;
    }

    public GuideDataVO(int index, int totalDistance, int totalTime, int totalFare, int distance, String description, int turnType, double latitude, double longitude) {
        this.index = index;
        this.totalDistance = totalDistance;
        this.totalTime = totalTime;
        this.totalFare = totalFare;
        this.description = description;
        this.turnType = turnType;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public GuideDataVO(int index, int distance, int turnType, String description, double latitude, double longitude) {
        this.index = index;
        this.description = description;
        this.distance = distance;
        this.turnType = turnType;
        this.latitude = latitude;
        this.longitude = longitude;
    }


    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getDistance() { return distance; }
    public void setDistance(int distance) { this.distance = distance; }

    public int getIndex() { return index; }
    public void setIndex(int index) { this.index = index; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public int getTotalDistance() { return totalDistance; }
    public void setTotalDistance(int totalDistance) { this.totalDistance = totalDistance; }

    public int getTotalFare() { return totalFare; }
    public void setTotalFare(int totalFare) { this.totalFare = totalFare; }

    public int getTotalTime() { return totalTime; }
    public void setTotalTime(int totalTime) { this.totalTime = totalTime; }

    public int getTurnType() { return turnType; }
    public void setTurnType(int turnType) { this.turnType = turnType; }

    @Override
    public String toString() {
        return "SimulationVO{" +
                "description='" + description + '\'' +
                ", index=" + index +
                ", totalDistance=" + totalDistance +
                ", totalFare=" + totalFare +
                ", totalTime=" + totalTime +
                ", turnType=" + turnType +
                ", distance=" + distance +
                ", longitude=" + longitude +
                ", latitude=" + latitude +
                '}';
    }
}
