package com.example.lee.tmap;

import android.os.Parcel;
import android.os.Parcelable;

import com.skp.Tmap.TMapPOIItem;
import com.skp.Tmap.TMapPoint;

import java.text.Collator;
import java.util.Comparator;

/**
 * Created by Lee on 2016-10-20.
 */
public class POIItem {

    private String mName;
    private String mAddress;
    private String mDistance;
    private TMapPoint point;


    public POIItem() {
    }

    public POIItem(String mName, String mAddress) {
        this.mName = mName;
        this.mAddress = mAddress;
    }

    public String getmName() {
        return mName;
    }

    public void setmName(String mName) {
        this.mName = mName;
    }

    public String getmAddress() {
        return mAddress;
    }

    public void setmAddress(String mAddress) {
        this.mAddress = mAddress;
    }

    public TMapPoint getPoint() {
        return point;
    }

    public void setPoint(TMapPoint point) {
        this.point = point;
    }

    public String getmDistance() {
        return mDistance;
    }

    public void setmDistance(String mDistance) {
        this.mDistance = mDistance;
    }
}
