package com.example.lee.tmap.ValueObject;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;


/**
 * Created by Lee on 2016-10-26.
 */
public class ReverseGeocodingVO {

    @SerializedName("addressInfo") public AddressInfo addressInfo;
    public AddressInfo getAddressInfo() { return addressInfo; }

    public class AddressInfo {
        @SerializedName("fullAddress") public String fullAddress;
        @SerializedName("city_do") public String city_do;

        public String getCity_do() { return city_do; }
        public String getFullAddress() { return fullAddress; }
    }   // AddressInfo
}   // ReverseGeocodingVO
