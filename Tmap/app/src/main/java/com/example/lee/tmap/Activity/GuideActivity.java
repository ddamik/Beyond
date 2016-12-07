package com.example.lee.tmap.Activity;


import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.lee.tmap.ApiService;
import com.example.lee.tmap.R;
import com.example.lee.tmap.UserException;
import com.example.lee.tmap.ValueObject.GuideDataVO;
import com.example.lee.tmap.ValueObject.TmapDataVO;
import com.skp.Tmap.TMapData;
import com.skp.Tmap.TMapGpsManager;
import com.skp.Tmap.TMapPoint;
import com.skp.Tmap.TMapPolyLine;
import com.skp.Tmap.TMapView;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.GsonConverterFactory;
import retrofit2.Response;
import retrofit2.Retrofit;


public class GuideActivity extends AppCompatActivity implements TMapGpsManager.onLocationChangedCallback {

    public static final String TAG = "GuideActivity";
    public static int ZOOM_LEVEL = 19;
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

    /*
        GPS
    */
    private TMapGpsManager gps = null;
    public int GPS_MIN_TIME = 100;
    public int GPS_MIN_DISTANCE = 5;

    double current_longitude = 0.0;
    double current_latitude = 0.0;

    double des_longitude = 0.0;
    double des_latitude = 0.0;
    String des_name = "";


    // 경로 그리기
    TMapData tMapData;
    private boolean threadFlag = true;
    private Thread uiThread;

    // 주행
    public int guide_index = 0;
    public ArrayList<TmapDataVO> path_list;

    // 겹친 layout 정보 표시
    ImageView img_direction;
    ImageButton img_btn_currentPoint;
    Button img_btn_exit;
    TextView tv_distance, tv_remain_distance, tv_arriaval_time;

    /*
        User Exception
    */
    public UserException exception;

    private double coordiLongitude;
    private double coordiLatitude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guide);

        Intent intent = getIntent();
        String tmpArrival_name = intent.getStringExtra("arrival_name");
        double tmpLongitude = intent.getDoubleExtra("des_longitude", 0.0);
        double tmpLatitude = intent.getDoubleExtra("des_latitude", 0.0);
        int totalTime = intent.getIntExtra("totalTime", 0);
        int totalDistance = intent.getIntExtra("totalDistance", 0);


        // User Exception ( [ 도착시간 & 소요시간 & 통행요금 & 거리 스트링 변환 ] )
        exception = new UserException();

        // layout 겹치기
        Window window = getWindow();
        window.setContentView(R.layout.activity_guide);     // 바닥에 깔릴 layout
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        LinearLayout linear = (LinearLayout) inflater.inflate(R.layout.activity_guide_info, null);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        window.addContentView(linear, params);

        // 겹친 layout 정보
        img_direction = (ImageView) findViewById(R.id.img_direction);                       // 방향 이미지 ( 좌, 우, 유턴 )
        img_btn_currentPoint = (ImageButton) findViewById(R.id.img_btn_currentPoint);       // 현재위치로 가는 버튼
        img_btn_exit = (Button) findViewById(R.id.img_btn_exit);                       // 안내종료
        tv_distance = (TextView) findViewById(R.id.tv_distance);                            // 다음 안내지점까지의 거리
        tv_remain_distance = (TextView) findViewById(R.id.tv_remain_distance);              // 총 남은 거리
        tv_arriaval_time = (TextView) findViewById(R.id.tv_arrival_time);                   // 도착 예상시간



        path_list = new ArrayList<>();  // 주행정보
        tMapData = new TMapData();

        Log.i(TAG, "[ Temp Value ] Longitude & Latitude = " + tmpLongitude + " & " + tmpLatitude + " & " + tmpArrival_name);
        initLocation(tmpLongitude, tmpLatitude, tmpArrival_name);                 // 시작할때의 현재위치를 가져온다. TMap은 Point 기준이기때문에 위도와 경도를 Point로 셋팅.
        initGPS();                      // GPS ghkftjdghk
        initMapView();                  // 지도 초기화
        initInfo(totalDistance, totalTime);

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 2;
        Bitmap bitmap = BitmapFactory.decodeResource(getApplicationContext().getResources(), R.drawable.guide_arrow_blue, options);
        tmapview.setIcon(bitmap);
        tmapview.setIconVisibility(true);                   // 현재 위치로 표시될 아이콘을 표시

        // 경로 PolyLine 그리기
        tMapData.findPathDataWithType(TMapData.TMapPathType.CAR_PATH, startPoint, endPoint, new TMapData.FindPathDataListenerCallback() {
            @Override
            public void onFindPathData(TMapPolyLine tMapPolyLine) {
                tMapPolyLine.setLineColor(Color.BLUE);          // PolyLine 색상은 블루
                tMapPolyLine.setLineWidth(20);                  // 두께는 20
                tmapview.addTMapPath(tMapPolyLine);             // 지도에 추가
            }
        });
        getPathInfo(startPoint, endPoint);                      //


        // 현재 위치로 가기
        img_btn_currentPoint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tmapview.setCenterPoint(startPoint.getLongitude(), startPoint.getLatitude(), true);
            }
        });

        // [ 안내종료 버튼 ]
        img_btn_exit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(), "안내를 종료합니다.", Toast.LENGTH_LONG).show();
                startActivity(new Intent(GuideActivity.this, MainActivity.class));
                overridePendingTransition(R.anim.anim_slide_fade_in, R.anim.anim_slide_out_left);
                finish();
            }
        }); // [ 안내종료 버튼 ]

        uiThread = new Thread() {
            @Override
            public void run() {
                handler.sendEmptyMessage(0);
            }
        };
        uiThread.start();

   }   //onCreate

    // Text Setting
    public void initInfo(int totalDistance, int totalTime){

        String arrival_time = exception.strArrival_time(totalTime);
        String strRemain = exception.strRemainDistance(totalDistance, totalDistance);

        tv_distance = (TextView) findViewById(R.id.tv_distance);                            // 다음 안내지점까지의 거리
        tv_remain_distance.setText(strRemain);                // 총 남은 거리
        tv_arriaval_time.setText(arrival_time);                 // 도착 예상시간
    }


    // 지도 셋팅
    public void initMapView() {
         /*
            T-map 초기 세팅
         */

        tMapLayout = (RelativeLayout) findViewById(R.id.guide_tmap);
        tmapview = new TMapView(this);
        tmapview.setSKPMapApiKey(APP_KEY);
        tmapview.setLanguage(TMapView.LANGUAGE_KOREAN);

        tmapview.setZoomLevel(ZOOM_LEVEL);                       // 지도레벨 설정 7~19
        tmapview.setMapType(TMapView.MAPTYPE_STANDARD);     // STANDARD: 일반지도 / SATELLITE: 위성지도[미지원] / HYBRID: 하이브리드[미지원] / TRAFFIC: 실시간 교통지도
        tmapview.setCompassMode(true);                      // 단말의 방향에 따라 움직이는 나침반 모드
        tmapview.setTrackingMode(true);                     // 화면중심을 단말의 현재위치로 이동시켜주는 모드
        tmapview.setMapPosition(TMapView.POSITION_NAVI);    // 네비게이션 모드 ( 화면 중심의 아래쪽으로 중심좌표를 설정 )
        tMapLayout.addView(tmapview);
    }   // initMapView

    // GPS 활성화
    public void initGPS() {
        gps = new TMapGpsManager(GuideActivity.this);
        gps.setMinTime(GPS_MIN_TIME);
        gps.setMinDistance(GPS_MIN_DISTANCE);
        // gps.setProvider(TMapGpsManager.NETWORK_PROVIDER);       // 현재 위치를 가져온다.
        gps.setProvider(TMapGpsManager.GPS_PROVIDER);
        gps.OpenGps();
    }   // initGPS

    @Override
    public void onLocationChange(Location location) {

        // Toast.makeText(getApplicationContext(), "[ onLocationChange ] " + location.getLongitude() + " / " + location.getLatitude(), Toast.LENGTH_LONG).show();

        current_longitude = location.getLongitude();
        current_latitude = location.getLatitude();

        startPoint.setLongitude(current_longitude);
        startPoint.setLatitude(current_latitude);

        uiThread = new Thread() {
            @Override
            public void run() {
                while (threadFlag) {
                    handler.sendEmptyMessage(0);
                }
            }
        };
        uiThread.start();
        Log.i(TAG, "[ onLocationChange ] : Longitude / Latitude : " + location.getLongitude() + " / " + location.getLatitude());
//        tmapview.setCenterPoint(startPoint.getLongitude(), startPoint.getLatitude(), true);
    }

    // 시작할때 현재 위치를 가져옴. [ GPS가 잡는 속도가 느려서 Search할때 현재의 위도와 경도를 얻고, 그 값을 가져온다. ]
    public void initLocation(double tmpLongitude, double tmpLatitude, String strArrivalName) {
        des_longitude = tmpLongitude;
        des_latitude = tmpLatitude;
        des_name = strArrivalName;


        current_latitude = UserException.STATIC_CURRENT_LATITUDE;
        current_longitude = UserException.STATIC_CURRENT_LONGITUDE;

        // 맨 처음 StartPoint와 EndPoint를 설정.
        startPoint = new TMapPoint(current_latitude, current_longitude);
        endPoint = new TMapPoint(des_latitude, des_longitude);

        Log.i(TAG, "[ Start Point ] : " + startPoint.getLongitude() + " / " + startPoint.getLatitude());
    }   // initLocation

    // 길 안내 시작
    public void getPathInfo(TMapPoint startPoint, TMapPoint endPoint) {
        Log.i(TAG, " [ Get Path Information ] ");
        tmapview.removeAllMarkerItem();

        // 위도(Latitude) : 37 / 경도(Longitude) : 127
        String startX = Double.toString(startPoint.getLongitude());
        String startY = Double.toString(startPoint.getLatitude());
        String endX = Double.toString(endPoint.getLongitude());
        String endY = Double.toString(endPoint.getLatitude());
        String reqCoordType = "WGS84GEO";


        Log.i(TAG, "[ Guide ] : Start Point : " + startX + " / " + startY);
        Log.i(TAG, "[ Guide ] : End Point : " + endX + " / " + endY);

        Retrofit client = new Retrofit.Builder().baseUrl(ApiService.API_URL).addConverterFactory(GsonConverterFactory.create()).build();
        ApiService apiService = client.create(ApiService.class);
        Call<GuideDataVO> call = apiService.getGuidePathResult(endX, endY, reqCoordType, startX, startY);
        call.enqueue(new Callback<GuideDataVO>() {

            @Override
            public void onResponse(Call<GuideDataVO> call, Response<GuideDataVO> response) {
                if (response.isSuccessful()) {
                    Log.i(TAG, "[ onResponse ] is Success");
                    int length = response.body().getFeatures().size();
                    for (int i = 0; i < length; i++) {
                        Log.i(TAG, "==================== [ GuideActivity Car Path " + i + " ]====================");
                        if (i == 0) {
                            //
                        }

                        Log.i(TAG, "[ Car Path ] Distance : " + response.body().getFeatures().get(i).getProperties().getDistance());
                        Log.i(TAG, "[ Car Path ] Description : " + response.body().getFeatures().get(i).getProperties().getDescription());
                        Log.i(TAG, "[ Car Path ] TurnType : " + response.body().getFeatures().get(i).getProperties().getTurnType());

//                        coordiLongitude = response.body().getFeatures().get(i).getGeoMetry().getCoordinates().get(0).getLongitude();
//                        coordiLatitude = response.body().getFeatures().get(i).getGeoMetry().getCoordinates().get(0).getLatitude();

                        Log.i(TAG, "[ Guide Path Coordinates ] ");
                        Log.i(TAG, "[ Longitude & Latitude ] : " + coordiLongitude + " & " + coordiLatitude);
                    }   // for
                }   // if(response.isSuccessful())
            }   // onResponse

            @Override
            public void onFailure(Call<GuideDataVO> call, Throwable t) {
                Log.i(TAG, "[ GuideActivity ] onFailure");
                Log.i(TAG, "========================= [ onFailure ] : " + call.toString() + "=========================");
                Log.i(TAG, "========================= [ onFailure ] : " + t.toString() + "=========================");
            }
        }); // call.enqueue
    }   // getPathInfo

    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            Log.i(TAG, "[ Handler ] : " + startPoint.getLongitude() + " & " + startPoint.getLatitude());
            tmapview.setLocationPoint(startPoint.getLongitude(), startPoint.getLatitude());
        }
    };   // [ End Handler ]

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
//        gps.OpenGps();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // GPS중단
        gps.CloseGps();
                    threadFlag = false;
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
}   // class
