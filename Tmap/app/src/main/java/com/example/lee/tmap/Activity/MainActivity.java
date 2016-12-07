package com.example.lee.tmap.Activity;


import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.location.Location;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;

import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.lee.tmap.Adapter.GridViewAdapter;
import com.example.lee.tmap.ApiService;
import com.example.lee.tmap.Fragment.HomeFragment;
import com.example.lee.tmap.R;
import com.example.lee.tmap.UserException;
import com.example.lee.tmap.ValueObject.RecentPathListVO;
import com.example.lee.tmap.ValueObject.RecentPathVO;

import com.skp.Tmap.TMapGpsManager;

import java.util.ArrayList;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements TMapGpsManager.onLocationChangedCallback {

    public static final String TAG = MainActivity.class.getSimpleName();

    private TMapGpsManager gps = null;
    public int GPS_MIN_TIME = 100;
    public int GPS_MIN_DISTANCE = 5;

    public static double cur_longitude = 0.0;
    public static double cur_latitude = 0.0;

    private Toolbar toolbar;

    private NavigationView navigationView;

    private ActionBarDrawerToggle toggle;

    private DrawerLayout mDrawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        // UUID & Recent Path List
        String strUUID = this.GetDevicesUUID(this);
        Log.i(TAG, "[ String UUID ] : " + strUUID);

        //Add toolbar
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("");
        setSupportActionBar(toolbar);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment, new HomeFragment()).commit();
        }


        //Add DrawerLayout
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        toggle = new ActionBarDrawerToggle(
                this, mDrawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close) {
            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                Log.i(TAG, "onDrawerClosed is called");
            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                Log.i(TAG, "onDrawerOpened is called");
            }
        };
        mDrawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        navigationView = (NavigationView) findViewById(R.id.navigation_view);
        navigationView.setNavigationItemSelectedListener(navigationItemSelectedListener);


    }   // onCreate



    // When drawerLayout is activated, fragment can be replaced.
    NavigationView.OnNavigationItemSelectedListener navigationItemSelectedListener = new NavigationView.OnNavigationItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(MenuItem item) {
            mDrawerLayout.closeDrawer(GravityCompat.START);

            Fragment fragment = null;
            final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            switch (item.getItemId()) {
                case R.id.home:
                    fragment = new HomeFragment();
                    break;
            }
            ft.replace(R.id.fragment, fragment).commit();
            return true;
        }
    };


    // UUID
    private String GetDevicesUUID(Context mContext){
        final TelephonyManager tm = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        final String tmDevice, tmSerial, androidId;
        tmDevice = "" + tm.getDeviceId();
        tmSerial = "" + tm.getSimSerialNumber();
        androidId = "" + android.provider.Settings.Secure.getString(getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
        UUID deviceUuid = new UUID(androidId.hashCode(), ((long)tmDevice.hashCode() << 32) | tmSerial.hashCode());
        String deviceId = deviceUuid.toString();
        return deviceId;
    }

    @Override
    public void onLocationChange(Location location) {
        cur_longitude = location.getLongitude();
        cur_latitude = location.getLatitude();

        Log.i(TAG, "[ MainActivity User Exception Location ]");
        Log.i(TAG, "[ Longitude & Latitude ] : " + UserException.STATIC_CURRENT_LONGITUDE + " & " + UserException.STATIC_CURRENT_LATITUDE);
        UserException.STATIC_CURRENT_LATITUDE = cur_latitude;
        UserException.STATIC_CURRENT_LONGITUDE = cur_longitude;

        Log.i(TAG, "[ MainActivity On Location Change ]");
        Log.i(TAG, "onLocationChange" + cur_longitude + " / " + cur_latitude);
    }   // onLocationChange

    public void initGPS() {
        Log.i(TAG, "[ HomeFragment init GPS ]");
        gps = new TMapGpsManager(MainActivity.this);
        gps.setMinTime(GPS_MIN_TIME);
        gps.setMinDistance(GPS_MIN_DISTANCE);
        gps.setProvider(TMapGpsManager.GPS_PROVIDER);
        gps.OpenGps();
    }   // initGPS

    //
    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        initGPS();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "[ Close GPS ]");
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Thread 종료
    }
}
