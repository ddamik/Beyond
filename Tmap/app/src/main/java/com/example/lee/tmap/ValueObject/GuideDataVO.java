package com.example.lee.tmap.ValueObject;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

/**
 * Created by Lee on 2016-12-07.
 */
public class GuideDataVO {
    @SerializedName("features")
    public ArrayList<Features> features;

    // [ GuideDataVO Constructure ]
    public GuideDataVO() { }
    public GuideDataVO(ArrayList<Features> features) { this.features = features; }

    // [ TMapDataVO Getter Setter ]
    public ArrayList<Features> getFeatures() { return features; }
    public void setFeatures(ArrayList<Features> features) { this.features = features; }

    public class Features {

        // [ Features Constructure ]
        public Features() { }
        public Features(GeoMetry geoMetry, Properties properties) {
            this.geoMetry = geoMetry;
            this.properties = properties;
        }

        // [ Featyres Getter Setter ]
        @SerializedName("geometry") public GeoMetry geoMetry;
        @SerializedName("properties") public Properties properties;
        public Properties getProperties() { return properties; }
        public GeoMetry getGeoMetry() { return geoMetry; }
        public void setGeoMetry(GeoMetry geoMetry) { this.geoMetry = geoMetry; }
        public void setProperties(Properties properties) { this.properties = properties; }

        public class GeoMetry {

            public GeoMetry() { }
            public GeoMetry(ArrayList<GeoMetryVO> coordinates, int index, int num, String type) {
                this.coordinates = coordinates;
                this.index = index;
                this.num = num;
                this.type = type;
            }

            @SerializedName("type") public String type;
            @SerializedName("coordinates") public ArrayList<GeoMetryVO> coordinates;
            public int index;
            public int num;

            public String getType() { return type; }
            public ArrayList<GeoMetryVO> getCoordinates() { return coordinates; }
            public int getIndex() { return index; }
            public int getNum() { return num; }

            public void setCoordinates(ArrayList<GeoMetryVO> coordinates) { this.coordinates = coordinates; }
            public void setIndex(int index) { this.index = index; }
            public void setNum(int num) { this.num = num; }
            public void setType(String type) { this.type = type; }

            @Override
            public String toString() {
                return "GeoMetry{" +
                        "coordinates=" + coordinates +
                        ", type='" + type + '\'' +
                        ", index=" + index +
                        ", num=" + num +
                        '}';
            }
        }   // [ End Geometry ]

        public class Properties {

            // [ Properties Constructure ]
            public Properties() { }
            public Properties(String description, int distance, int index, int taxiFare, int totalDistance, int totalFare, int totalTime, int turnType) {
                this.description = description;
                this.distance = distance;
                this.index = index;
                this.taxiFare = taxiFare;
                this.totalDistance = totalDistance;
                this.totalFare = totalFare;
                this.totalTime = totalTime;
                this.turnType = turnType;
            }

            // [ Properties Getter Setter ]
            @SerializedName("totalDistance") public int totalDistance;
            @SerializedName("totalTime") public int totalTime;
            @SerializedName("totalFare") public int totalFare;
            @SerializedName("taxiFare") public int taxiFare;
            @SerializedName("distance") public int distance;
            @SerializedName("index") public int index;
            @SerializedName("description") public String description;
            @SerializedName("turnType") public int turnType;

            public int getTotalDistance() { return totalDistance; }
            public int getTotalTime() { return totalTime; }
            public int getTotalFare() { return totalFare; }
            public int getTaxiFare() { return taxiFare; }
            public int getDistance() { return distance; }
            public int getTurnType() { return turnType; }
            public String getDescription() { return description; }
            public int getIndex() { return index; }

            public void setDescription(String description) { this.description = description; }
            public void setDistance(int distance) { this.distance = distance; }
            public void setIndex(int index) { this.index = index; }
            public void setTaxiFare(int taxiFare) { this.taxiFare = taxiFare; }
            public void setTotalDistance(int totalDistance) { this.totalDistance = totalDistance; }
            public void setTotalFare(int totalFare) { this.totalFare = totalFare; }
            public void setTotalTime(int totalTime) { this.totalTime = totalTime; }
            public void setTurnType(int turnType) { this.turnType = turnType; }

            @Override
            public String toString() {
                return "Properties{" +
                        "description='" + description + '\'' +
                        ", totalDistance=" + totalDistance +
                        ", totalTime=" + totalTime +
                        ", totalFare=" + totalFare +
                        ", taxiFare=" + taxiFare +
                        ", distance=" + distance +
                        ", index=" + index +
                        ", turnType=" + turnType +
                        '}';
            }
        }   // Properties
    }   // Features

    @Override
    public String toString() {
        return "TmapDataVO{" +
                "features=" + features +
                '}';
    }
}
