package com.example.lee.tmap.Activity;

import android.content.Context;
import android.content.Intent;
import android.app.Activity;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.TextView;

import com.example.lee.tmap.ApiService;
import com.example.lee.tmap.R;
import com.skp.Tmap.TMapGpsManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class MainActivity extends Activity implements TMapGpsManager.onLocationChangedCallback {

    public static final String TAG = "MainActivity";

    public static double cur_longitude = 0.0;
    public static double cur_latitude = 0.0;
    private TMapGpsManager tMapGpsManager;

    /*
        검색
     */
    Button btn_searchDest = null;


    /*
        GridView
     */
//    private GridAdapter gridAdapter;
    private TextView tv_destination;
    private GridView gridView;
    private ArrayList<String>  destination_list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // startActivity(new Intent(this, SplashActivity.class));      // splash


        /*
            Search
         */
        btn_searchDest = (Button) findViewById(R.id.btn_searchDest);
        btn_searchDest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, SearchDestinationActivity.class));                     // 좌측으로 사라지기
                overridePendingTransition(R.anim.anim_slide_in_right, R.anim.anim_slide_out_left);      // 새로운 Activity Animation / 현재의 Activity Animation
                finish();
            }
        });

        /*
            GPS
         */
        tMapGpsManager = new TMapGpsManager(this);
        tMapGpsManager.setProvider(TMapGpsManager.NETWORK_PROVIDER);
        tMapGpsManager.OpenGps();


        /*
            GridView
         */

    }   // onCreate

    @Override
    public void onLocationChange(Location location) {
        cur_longitude = location.getLongitude();
        cur_latitude = location.getLatitude();

        Log.i(TAG, "[ SearchDestinationActivity ]");
        Log.i(TAG, "onLocationChange" + cur_longitude + " / " + cur_latitude);
    }   // onLocationChange


}
