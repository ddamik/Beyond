package com.example.lee.tmap.ValueObject;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

/**
 * Created by Lee on 2016-11-11.
 */
public class RecentPathVO {

    @SerializedName("RESULT") public ArrayList<RESULT> result;
    public ArrayList<RESULT> getResult() { return result; }

    public class RESULT{
        @SerializedName("USER_UUID") public String userUUID;
        @SerializedName("ARRIVAL_NAME") public String arrivalName;
        @SerializedName("SEARCH_DATE") public String searchDate;
        @SerializedName("LATITUDE_VALUE") public String latitudeValue;
        @SerializedName("LONGITUDE_VALUE") public String longitudeValue;

        public RESULT() { }

        public RESULT(String arrivalName, String longitudeValue, String latitudeValue) {
            this.arrivalName = arrivalName;
            this.longitudeValue = longitudeValue;
            this.latitudeValue = latitudeValue;
        }

        public String getArrivalName() { return arrivalName; }
        public String getLatitudeValue() { return latitudeValue; }
        public String getLongitudeValue() { return longitudeValue; }
        public String getSearchDate() { return searchDate; }
        public String getUserUUID() { return userUUID; }
    }   // RESULT
}
