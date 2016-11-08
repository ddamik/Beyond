package com.example.lee.tmap.Activity;


import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.example.lee.tmap.ApiService;
import com.example.lee.tmap.POIItem;
import com.example.lee.tmap.R;
import com.example.lee.tmap.TmapDataModel;
import com.skp.Tmap.TMapData;
import com.skp.Tmap.TMapGpsManager;
import com.skp.Tmap.TMapMarkerItem;
import com.skp.Tmap.TMapPoint;
import com.skp.Tmap.TMapPolyLine;
import com.skp.Tmap.TMapView;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.GsonConverterFactory;
import retrofit2.Response;
import retrofit2.Retrofit;


public class NavigationDisplayActivity extends Activity implements TMapGpsManager.onLocationChangedCallback {

    public static final String TAG = "NavigationActivity";
    public static float ZOOM_LEVEL = 15;
    public static final String APP_KEY = "483f055b-19f2-3a22-a3fb-935bc1684b0b";

    /*
        Location
     */
    private TMapPoint startPoint;
    private TMapPoint endPoint;
    private double distance;

    /*
       Map
    */
    private RelativeLayout tMapLayout = null;
    private TMapView tmapview = null;

    /*
        GPS
    */
    private TMapGpsManager gps = null;
    public static int GPS_MIN_TIME = 1000;
    public static int GPS_MIN_DISTANCE = 5;

    double current_longitude = 0.0;
    double current_latitude = 0.0;

    double des_longitude = 0.0;
    double des_latitude = 0.0;
    String des_name = "";

    /*
        Markert
     */
    TMapMarkerItem des_marker;
    TMapMarkerItem current_marker;

    /*
        Path
     */
    Button btn_searchPath;
    TMapData tMapData;


    /*
        경로안내
     */
    Button btn_startGuide;
    ArrayList<TMapPoint> passList;
    public static final int OPTION_DEFAULT = 0;
    public static final int OPTION_VALUE = OPTION_DEFAULT;


    /*
        현재위치
     */
    Button btn_currentPoint;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);

        tMapData = new TMapData();
        initMapView();
        initGPS();
        initLocation();

        initCurrentMarker(current_longitude, current_latitude);
        initDestinationMarker(des_longitude, des_latitude, des_name);
        setZoomLevel();



        /*
            Search & Show Path
         */
        btn_searchPath = (Button) findViewById(R.id.btn_searchPath);
        btn_searchPath.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tMapData.findPathDataWithType(TMapData.TMapPathType.CAR_PATH, startPoint, endPoint, new TMapData.FindPathDataListenerCallback() {
                    @Override
                    public void onFindPathData(TMapPolyLine tMapPolyLine) {
                        tmapview.addTMapPath(tMapPolyLine);
                    }
                });
            }   // btn.onClick
        });


        /*
            Guide
         */
        btn_startGuide = (Button) findViewById(R.id.btn_startGuide);
        btn_startGuide.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startGuide(startPoint, endPoint);
            }
        });


        /*
            현재위치
         */
        btn_currentPoint = (Button) findViewById(R.id.btn_currentPoint);
        btn_currentPoint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(), "StartPoint : " + startPoint.getLongitude() + " / " + startPoint.getLatitude(), Toast.LENGTH_LONG).show();
                tmapview.setCenterPoint(startPoint.getLatitude(), startPoint.getLongitude());       // Tmap은 Longitude와 Latitude가 바껴서 들어간다.
                tmapview.animate();
            }
        });
   }   //onCreate



    public void initLocation() {
        des_longitude = SearchDestinationActivity.des_longitude;
        des_latitude = SearchDestinationActivity.des_latitude;
        des_name = SearchDestinationActivity.des_name;

        current_latitude = MainActivity.cur_latitude;
        current_longitude = MainActivity.cur_longitude;

//        startPoint = new TMapPoint(current_longitude, current_latitude);
//        endPoint = new TMapPoint(des_longitude, des_latitude);

        startPoint = new TMapPoint(current_latitude, current_longitude);
        endPoint = new TMapPoint(des_latitude, des_longitude);

    }   // initLocation




    public void setZoomLevel() {

    }   // setZoomLevel

    public void initMapView() {
         /*
            T-map 초기 세팅
         */
        tMapLayout = (RelativeLayout) findViewById(R.id.tmap);
        tmapview = new TMapView(this);
        tmapview.setSKPMapApiKey(APP_KEY);
        tmapview.setLanguage(TMapView.LANGUAGE_KOREAN);
        tmapview.setIconVisibility(true);                   // 현재 위치로 표시될 아이콘을 표시
        tmapview.setZoom(ZOOM_LEVEL);                       // 지도레벨 설정 7~19
        tmapview.setMapType(TMapView.MAPTYPE_STANDARD);     // STANDARD: 일반지도 / SATELLITE: 위성지도[미지원] / HYBRID: 하이브리드[미지원] / TRAFFIC: 실시간 교통지도
        tmapview.setCompassMode(true);                      // 단말의 방향에 따라 움직이는 나침반 모드
        tmapview.setTrackingMode(true);                     // 화면중심을 단말의 현재위치로 이동시켜주는 모드
        tMapLayout.addView(tmapview);
        // tmapview.setCenterPoint : 지도의 중심좌표를 이동
        // tmapview.setLocationPoint : 현재위치로 표시될 좌표의 위도, 경도를 설정

        // 현재 위치로 표시되는 좌표의 위도, 경도를 반환
        // TMapPoint tpoint = tmapview.getLocationPoint();
        // double Latitue = tpoint.getLatitude();
        // double Longitude = tpoint.getLongitude();
    }   // initMapView

    public void initGPS() {
        gps = new TMapGpsManager(NavigationDisplayActivity.this);
        gps.setMinTime(GPS_MIN_TIME);
        gps.setMinDistance(GPS_MIN_DISTANCE);
        // gps.setProvider(TMapGpsManager.NETWORK_PROVIDER);       // 현재 위치를 가져온다.
        gps.setProvider(TMapGpsManager.LOCATION_SERVICE);
        gps.OpenGps();


    }   // initGPS

    @Override
    public void onLocationChange(Location location) {
        Log.i(TAG, "[ onLocationChange ] : Longitude / Latitude : " + location.getLongitude() + " / " + location.getLatitude());
        Toast.makeText(getApplicationContext(), "[ onLocationChange ] " + location.getLongitude() + " / " + location.getLatitude(), Toast.LENGTH_LONG).show();

        current_longitude = location.getLongitude();
        current_latitude = location.getLatitude();

        startPoint.setLongitude(current_longitude);
        startPoint.setLatitude(current_latitude);

        tmapview.setLocationPoint(location.getLongitude(), location.getLatitude());
    }

    public void initCurrentMarker(double longitude, double latitude) {
        current_marker = new TMapMarkerItem();
        TMapPoint current_point = new TMapPoint(latitude, longitude);
        current_marker.setTMapPoint(current_point);
        current_marker.setVisible(TMapMarkerItem.VISIBLE);

        Bitmap bitmap = BitmapFactory.decodeResource(getApplicationContext().getResources(), R.drawable.poi_dot);
        current_marker.setIcon(bitmap);
        current_marker.setID("출발");

        tmapview.setCenterPoint(longitude, latitude, true);
        tmapview.addMarkerItem("출발", current_marker);               // 다른 마커와 "String" 부분이 겹치면 안된다.
    }   // initCurrentMarker

    public void initDestinationMarker(double longitude, double latitude, String des_name) {
        des_marker = new TMapMarkerItem();
        TMapPoint des_point = new TMapPoint(latitude, longitude);
        des_marker.setTMapPoint(des_point);
        des_marker.setVisible(TMapMarkerItem.VISIBLE);

        Bitmap bitmap = BitmapFactory.decodeResource(getApplicationContext().getResources(), R.drawable.poi_dot);
        des_marker.setIcon(bitmap);
        des_marker.setID("도착");

        tmapview.setCenterPoint(longitude, latitude, true);
        tmapview.addMarkerItem("도착", des_marker);

    }   // initDestinationMarker


    public void startGuide(TMapPoint startPoint, TMapPoint endPoint) {
        tmapview.removeAllMarkerItem();

        // 위도(Latitude) : 37 / 경도(Longitude) : 127
        String startX = Double.toString(startPoint.getLongitude());
        String startY = Double.toString(startPoint.getLatitude());
        String endX = Double.toString(endPoint.getLongitude());
        String endY = Double.toString(endPoint.getLatitude());
        String reqCoordType = "WGS84GEO";

        Log.i(TAG, "Start Point : " + startX + " / " + startY);
        Log.i(TAG, "End Point : " + endX + " / " + endY);

        Retrofit client = new Retrofit.Builder().baseUrl(ApiService.API_URL).addConverterFactory(GsonConverterFactory.create()).build();
        ApiService apiService = client.create(ApiService.class);
        Call<TmapDataModel> call = apiService.getGuidePath(startX, startY, endX, endY,reqCoordType);
        call.enqueue(new Callback<TmapDataModel>() {
            @Override
            public void onResponse(Call<TmapDataModel> call, Response<TmapDataModel> response) {
                if(response.isSuccessful()){
                    Log.i(TAG, "[ onResponse ] is Success");
                    int length = response.body().getFeatures().size();
                    for(int i=0 ; i<length ; i++){
                        Log.i(TAG, "==================== [ Car Path " + i +" ]====================");
                        Log.i(TAG, "[ Car Path ] Distance : " + response.body().getFeatures().get(i).getProperties().getDistance());
                        Log.i(TAG, "[ Car Path ] Description : " + response.body().getFeatures().get(i).getProperties().getDescription());
                        Log.i(TAG, "[ Car Path ] TurnType : " + response.body().getFeatures().get(i).getProperties().getTurnType());
                    }

                }   // if(response.isSuccessful())
            }   // onResponse

            @Override
            public void onFailure(Call<TmapDataModel> call, Throwable t) {

            }
        }); // call.enqueue
    }   // startGuide
}   // class
