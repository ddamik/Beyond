package com.example.lee.tmap.Activity;


import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.example.lee.tmap.ApiService;
import com.example.lee.tmap.Fragment.ArrivalPathListFragment;
import com.example.lee.tmap.R;
import com.example.lee.tmap.UserException;
import com.example.lee.tmap.ValueObject.ReverseGeocodingVO;
import com.example.lee.tmap.ValueObject.TmapDataVO;
import com.skp.Tmap.TMapData;
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


public class PathInfoActivity extends AppCompatActivity {

    public static final String TAG = "PathInfoActivity";
    public static float ZOOM_LEVEL = 15;
    public static final String APP_KEY = "483f055b-19f2-3a22-a3fb-935bc1684b0b";

    /*
        Location
     */
    private TMapPoint startPoint;
    private TMapPoint endPoint;

    /*
       Map
    */
    private RelativeLayout tMapLayout = null;
    private TMapView tmapview = null;

    double current_longitude = 0.0;
    double current_latitude = 0.0;

    double des_longitude = 0.0;
    double des_latitude = 0.0;

    /*
        Markert
     */
    TMapMarkerItem des_marker;
    TMapMarkerItem current_marker;

    /*
        Path
     */
    TMapData tMapData;


    /*
        경로안내
     */
    Button btn_startGuide;
    public ArrayList<TmapDataVO> pathInfo;
    public static final int OPTION_DEFAULT = 0;


    /*
        모의주행
     */
    Button btn_simulation;

    /*
        텍스트 정보
     */
    TextView tv_start_point, tv_arrival_point, tv_arrival_time, tv_total_distance, tv_total_fare, tv_time;
    public static String arrival_name = SearchDestinationActivity.des_name;

    /*
        User Exception
     */
    public UserException exception;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_path_info);

        // User Exception ( [ 도착시간 & 소요시간 & 통행요금 & 거리 스트링 변환 ] )
        exception = new UserException();

        // Text 정보 [ 위 ( 시작지점 & 도착지점 ) ]
        tv_start_point = (TextView) findViewById(R.id.tv_start_point);
        tv_arrival_point = (TextView) findViewById(R.id.tv_arrival_point);

        // [ 아래 ( 총 거리 & 도착시간 & 요금 ) ]
        tv_total_distance = (TextView) findViewById(R.id.tv_total_distance);
        tv_arrival_time = (TextView) findViewById(R.id.tv_arrival_time);
        tv_total_fare = (TextView) findViewById(R.id.tv_total_fare);
        tv_time = (TextView) findViewById(R.id.tv_time);


        pathInfo = new ArrayList<TmapDataVO>();
        tMapData = new TMapData();
        initLocation();                 // 시작할때의 현재위치를 가져온다. TMap은 Point 기준이기때문에 위도와 경도를 Point로 셋팅.
        initMapView();                  // 지도 초기화
        initPathInfo(startPoint);

        // initCurrentMarker(current_longitude, current_latitude);         // 현재 위치의 마커를 표시
        // initDestinationMarker(des_longitude, des_latitude, des_name);   // 도착 지점의 마커를 표시

        tMapData.findPathDataWithType(TMapData.TMapPathType.CAR_PATH, startPoint, endPoint, new TMapData.FindPathDataListenerCallback() {
            @Override
            public void onFindPathData(TMapPolyLine tMapPolyLine) {
                tMapPolyLine.setLineColor(Color.BLUE);
                tMapPolyLine.setLineWidth(20);
                tmapview.addTMapPath(tMapPolyLine);
            }
        });
        startGuide(startPoint, endPoint);
        /*
            Guide
         */
        btn_startGuide = (Button) findViewById(R.id.btn_startGuide);
        btn_startGuide.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(PathInfoActivity.this, GuideActivity.class);
                intent.putExtra("pathInfo", pathInfo);
                startActivity(intent);
            }
        });


        /*
            다른경로 [ Back ]
         */
        btn_simulation = (Button) findViewById(R.id.btn_simulation);
        btn_simulation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(PathInfoActivity.this, SimulationActivity.class);
                startActivity(intent);
            }
        });
    }   //onCreate


    public void initMapView() {
         /*
            T-map 초기 세팅
         */
        tMapLayout = (RelativeLayout) findViewById(R.id.path_info_tmap);
        tmapview = new TMapView(this);
        tmapview.setSKPMapApiKey(APP_KEY);
        tmapview.setLanguage(TMapView.LANGUAGE_KOREAN);
        tmapview.setIconVisibility(false);                   // 현재 위치로 표시될 아이콘을 표시
        tmapview.setZoom(ZOOM_LEVEL);                       // 지도레벨 설정 7~19
        tmapview.setMapType(TMapView.MAPTYPE_STANDARD);     // STANDARD: 일반지도 / SATELLITE: 위성지도[미지원] / HYBRID: 하이브리드[미지원] / TRAFFIC: 실시간 교통지도
        // tmapview.setCompassMode(true);                      // 단말의 방향에 따라 움직이는 나침반 모드
        tmapview.setTrackingMode(true);                     // 화면중심을 단말의 현재위치로 이동시켜주는 모드
        tMapLayout.addView(tmapview);
        // tmapview.setCenterPoint : 지도의 중심좌표를 이동
        // tmapview.setLocationPoint : 현재위치로 표시될 좌표의 위도, 경도를 설정

        // 현재 위치로 표시되는 좌표의 위도, 경도를 반환
        // TMapPoint tpoint = tmapview.getLocationPoint();
        // double Latitue = tpoint.getLatitude();
        // double Longitude = tpoint.getLongitude();
    }   // initMapView


    // 시작할때 현재 위치를 가져옴. [ GPS가 잡는 속도가 느려서 Search할때 현재의 위도와 경도를 얻고, 그 값을 가져온다. ]
    public void initLocation() {

//        des_latitude = SearchDestinationActivity.des_latitude;
//        des_latitude = SearchDestinationActivity.des_latitude;
//        arrival_name = SearchDestinationActivity.des_name;
        des_longitude = ArrivalPathListFragment.des_longitude;
        des_latitude = ArrivalPathListFragment.des_latitude;
        arrival_name = ArrivalPathListFragment.des_name;

        current_latitude = UserException.STATIC_CURRENT_LATITUDE;
        current_longitude = UserException.STATIC_CURRENT_LONGITUDE;

//        startPoint = new TMapPoint(current_longitude, current_latitude);
//        endPoint = new TMapPoint(des_longitude, des_latitude);

        startPoint = new TMapPoint(current_latitude, current_longitude);
        endPoint = new TMapPoint(des_latitude, des_longitude);

    }   // initLocation

    // 경로정보 보이기
    public void initPathInfo(TMapPoint startPoint) {

        String lat = String.valueOf(startPoint.getLatitude());
        String lon = String.valueOf(startPoint.getLongitude());
        String coordType = "WGS84GEO";
        String addressType = "A02";

        Log.i(TAG, "========== [ InitPathInfo ] ==========");
        Log.i(TAG, "[ initPathInfo StartPoint Latitude ] : " + lat);
        Log.i(TAG, "[ initPathInfo StartPoint Longitude ] : " + lon);

        Retrofit client = new Retrofit.Builder().baseUrl(ApiService.API_URL).addConverterFactory(GsonConverterFactory.create()).build();
        ApiService apiService = client.create(ApiService.class);
        Call<ReverseGeocodingVO> call = apiService.getAddress(lat, lon, coordType, addressType);
        call.enqueue(new Callback<ReverseGeocodingVO>() {
            @Override
            public void onResponse(Call<ReverseGeocodingVO> call, Response<ReverseGeocodingVO> response) {
                Log.i(TAG, "============================== [ Get Address Informatin ] ==============================");

                if (response.isSuccessful()) {
                    Log.i(TAG, "[ Get Address Info ] Address : " + response.body().getAddressInfo().getFullAddress());
                    Log.i(TAG, "[ Get Address Info ] Cit : " + response.body().getAddressInfo().getCity_do());
                    tv_start_point.setText(response.body().getAddressInfo().getFullAddress());
                    tv_arrival_point.setText(arrival_name);
                }   // successful
            }

            @Override
            public void onFailure(Call<ReverseGeocodingVO> call, Throwable t) {

            }
        });
    }

    // 길 안내 정보 셋팅
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
        Call<TmapDataVO> call = apiService.getGuidePath(endX, endY, reqCoordType, startX, startY);
        call.enqueue(new Callback<TmapDataVO>() {
            @Override
            public void onResponse(Call<TmapDataVO> call, Response<TmapDataVO> response) {
                if (response.isSuccessful()) {
                    Log.i(TAG, "[ onResponse ] is Success");
                    int length = response.body().getFeatures().size();
                    for (int i = 0; i < length; i++) {
                        Log.i(TAG, "==================== [ Car Path " + i + " ]====================");
                        if (i == 0) {
                            setMapView(response.body().getFeatures().get(i).getProperties().getTotalDistance());        // 지도 설정
                            Log.i(TAG, "[ Car Path ] Total Distance : " + response.body().getFeatures().get(i).getProperties().getTotalDistance());
                            Log.i(TAG, "[ Car Path ] Total Time : " + response.body().getFeatures().get(i).getProperties().getTotalTime());
                            Log.i(TAG, "[ Car Path ] Total Fare : " + response.body().getFeatures().get(i).getProperties().getTotalFare());
                            Log.i(TAG, "[ Car Path ] Total TaxiFare : " + response.body().getFeatures().get(i).getProperties().getTaxiFare());

                            int totalTime = response.body().getFeatures().get(i).getProperties().getTotalTime();
                            ;
                            int total_fare = response.body().getFeatures().get(i).getProperties().getTotalFare();
                            int total_distance = response.body().getFeatures().get(i).getProperties().getTotalDistance();

                            // [ 거리 & 시간 & 통행요금 ] 셋팅 완료
                            tv_arrival_time.setText(exception.strArrival_time(totalTime));        // Calendar에 초를 더함.
                            tv_time.setText(exception.strTime(totalTime));                        // 초 단위
                            tv_total_fare.setText(exception.strWon(total_fare));                  // won 단위
                            tv_total_distance.setText(exception.strDistance(total_distance));                // m 단위
                        }
                        Log.i(TAG, "[ Car Path ] Distance : " + response.body().getFeatures().get(i).getProperties().getDistance());
                        Log.i(TAG, "[ Car Path ] Description : " + response.body().getFeatures().get(i).getProperties().getDescription());
                        Log.i(TAG, "[ Car Path ] TurnType : " + response.body().getFeatures().get(i).getProperties().getTurnType());
//                        Log.i(TAG, "[ Car Path ] Coordinate : " + response.body().getFeatures().get(i).getGeoMetry().getCoordinates());
                    }
                }   // if(response.isSuccessful())
            }   // onResponse

            @Override
            public void onFailure(Call<TmapDataVO> call, Throwable t) {

            }
        }); // call.enqueue
    }   // startGuide


    // 경로안내 시작 누르기 전에, 전체경로를 보여주는 지도 셋팅부분
    public void setMapView(int total_distance) {
        int set_zoom_level = 0;                                             // 7-19 레벨 설정가능
        if (total_distance < 1000) set_zoom_level = 17;                    // 1km
        else if (total_distance < 5000) set_zoom_level = 14;               // 5km
        else if (total_distance < 10000) set_zoom_level = 12;              // 10km
        else if (total_distance < 30000) set_zoom_level = 11;              // 30km
        else if (total_distance < 70000) set_zoom_level = 10;              // 70km
        else if (total_distance < 150000) set_zoom_level = 9;              // 150km
        else set_zoom_level = 7;                                            // 그 이상

        Log.i(TAG, "[ Total Distance ] : " + total_distance + " / [ Set Zoom Level ] : " + set_zoom_level);
        double setLongitude = (startPoint.getLongitude() + endPoint.getLongitude()) / 2;
        double setLatitude = (startPoint.getLatitude() + endPoint.getLatitude()) / 2;

        tmapview.setCenterPoint(setLongitude, setLatitude, true);
        tmapview.setZoomLevel(set_zoom_level);
    }


    // 현재위치 마커찍기
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

    // 목적지 마커 찍기
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
}   // class
