package com.example.lee.tmap;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;


/**
 * Created by Lee on 2016-10-26.
 */
public class TmapDataModel {

    @SerializedName("features")
    private ArrayList<Features> features;
    public ArrayList<Features> getFeatures() { return features; }
    public class Features {
        @SerializedName("properties") public Properties properties;
        public Properties getProperties() { return properties; }
        public class Properties {
            @SerializedName("distance") public int distance;
            @SerializedName("description") public String description;
            @SerializedName("turnType") public int turnType;
            public int getDistance() { return distance; }
            public int getTurnType() { return turnType; }
            public String getDescription() { return description; }
        }   // Properties
    }   // Features
}   // TmapDataModel
