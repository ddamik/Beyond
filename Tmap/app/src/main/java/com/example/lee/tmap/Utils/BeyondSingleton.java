package com.example.lee.tmap.Utils;

import android.content.Context;

import com.example.lee.tmap.Activity.MainActivity;

/**
 * Created by Nam on 2016. 12. 8..
 */
public class BeyondSingleton {
    public static final String TAG = MainActivity.class.getSimpleName();

    private static BeyondSingleton sInstance;
    private boolean bleConnectedStatus;

    private BeyondSingleton(Context context) {
    }

    public static synchronized void initializeInstance(Context context) {
        if (sInstance == null) {
            sInstance = new BeyondSingleton(context);
        }
    }

    public static synchronized BeyondSingleton getInstance() {
        if (sInstance == null) {
            throw new IllegalStateException(BeyondSingleton.class.getSimpleName() +
                    " is not initialized, call initializeInstance(..) method first.");
        }
        return sInstance;
    }

    public boolean getBleConnectedStatus() {
        return bleConnectedStatus;
    }

    public void setBleConnectedStatus(boolean bleConnectedStatus) {
        this.bleConnectedStatus = bleConnectedStatus;
    }
}
