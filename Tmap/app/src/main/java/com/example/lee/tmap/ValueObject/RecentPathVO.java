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
        @SerializedName("USER_UUID") public String USER_UUID;
        @SerializedName("ARRIVAL_NAME") public String ARRIVAL_NAME;
        @SerializedName("IS_FAVORITE") public String IS_FAVORITE;
        @SerializedName("SEARCH_DATE") public String SEARCH_DATE;

        public String getUSER_UUID() { return USER_UUID; }
        public String getARRIVAL_NAME() { return ARRIVAL_NAME; }
        public String getIS_FAVORITE() { return IS_FAVORITE; }
        public String getSEARCH_DATE() { return SEARCH_DATE; }
    }   // RESULT
}
