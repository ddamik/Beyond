package com.example.lee.tmap.Activity;


import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import com.example.lee.tmap.PSoCBleService;
import com.example.lee.tmap.R;
import com.example.lee.tmap.UserException;
import com.example.lee.tmap.Utils.BeyondSingleton;
import com.example.lee.tmap.ValueObject.GuideDataVO;
import com.example.lee.tmap.ValueObject.SimulationCoordinatesVO;
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

    // 겹친 layout 정보 표시
    ImageView img_direction;
    ImageButton img_btn_currentPoint;
    Button img_btn_exit;
    TextView tv_distance, tv_remain_distance, tv_arriaval_time;

    /*
        User Exception
    */
    public UserException exception;

    // 주행
    public int guide_index = 0;
    public ArrayList<SimulationCoordinatesVO> pathList;
    public ArrayList<GuideDataVO> guideList;

    public int driveIndex = 0;
    public double driveDestinationLongitude = 0.0;
    public double driveDestinationLatitude = 0.0;
    public int driveTurnType = 0;
    public int beforeDriveTurnType = 0;

    public boolean driveDestinationCheck = false;
    public boolean driveDirectionCheck = false;
    public boolean driveDestination10 = false;
    public boolean driveDestination30 = false;
    public boolean driveDestination50 = false;

    public String strCurrentDistance = "";
    public double driveInfoDistance = 0.0;


    // ble
    private static PSoCBleService mPSoCBleService;
    private boolean isRunningPSocBleService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guide);

        Log.i(TAG, " [ Guide Activity On Create ] ");
        isRunningPSocBleService = BeyondSingleton.getInstance().getBleConnectedStatus();

        Log.i(TAG, "isServiceRunningCheck : " + isRunningPSocBleService);

        if (isRunningPSocBleService) {
            mPSoCBleService = new PSoCBleService();
            Log.i(TAG, "mPSoCBleService create in SimulationActivity onCreate");
        }

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


        pathList = new ArrayList<>();
        guideList = new ArrayList<>();
        tMapData = new TMapData();

        Log.i(TAG, "[ Temp Value ] Longitude & Latitude = " + tmpLongitude + " & " + tmpLatitude + " & " + tmpArrival_name);
        initPurean();
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
//                startActivity(new Intent(GuideActivity.this, MainActivity.class));
//                overridePendingTransition(R.anim.anim_slide_fade_in, R.anim.anim_slide_out_left);
                finish();
            }
        }); // [ 안내종료 버튼 ]

//        startGuide();
    }   //onCreate

    public void initPurean() {

        int index;
        double latitude = 0.0;
        double longitude = 0.0;

        SimulationCoordinatesVO vo;

        index = 0;
        latitude = 37.473315;
        longitude = 127.099965;
        vo = new SimulationCoordinatesVO(latitude, longitude);
        pathList.add(vo);

        index = 1;
        latitude = 0.0;
        longitude = 0.0;
        vo = new SimulationCoordinatesVO(latitude, longitude);
        pathList.add(vo);

        index = 2;
        latitude = 37.472805;
        longitude = 127.100058;
        vo = new SimulationCoordinatesVO(latitude, longitude);
        pathList.add(vo);

        index = 3;
        latitude = 0.0;
        longitude = 0.0;
        vo = new SimulationCoordinatesVO(latitude, longitude);
        pathList.add(vo);

        index = 4;
        latitude = 37.467012;
        longitude = 127.094317;
        vo = new SimulationCoordinatesVO(latitude, longitude);
        pathList.add(vo);

        index = 5;
        latitude = 0.0;
        longitude = 0.0;
        vo = new SimulationCoordinatesVO(latitude, longitude);
        pathList.add(vo);

        index = 6;
        latitude = 37.466998;
        longitude = 127.097212;
        vo = new SimulationCoordinatesVO(latitude, longitude);
        pathList.add(vo);

        index = 7;
        latitude = 0.0;
        longitude = 0.0;
        vo = new SimulationCoordinatesVO(latitude, longitude);
        pathList.add(vo);

        index = 8;
        latitude = 37.466579;
        longitude = 127.097262;
        vo = new SimulationCoordinatesVO(latitude, longitude);
        pathList.add(vo);

    }


    // Text Setting
    public void initInfo(int totalDistance, int totalTime) {

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

    public void startGuide(){

        Log.i(TAG, " [ StartGuide ] ");
        Log.i(TAG, " [ Current Longitude ] : " + current_longitude);
        Log.i(TAG, " [ Current Latitude ] : " + current_latitude);
        Log.i(TAG, " [ ThreadFlag ] : " + threadFlag);
        uiThread = new Thread(){
            @Override
            public void run() {
                while(threadFlag){

                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    current_longitude = UserException.STATIC_CURRENT_LONGITUDE;
                    current_latitude = UserException.STATIC_CURRENT_LATITUDE;

                    if (guideList.get(driveIndex).getTurnType() == 0) {
                        Log.i(TAG, "============================== [ GuideActivity ] ==============================");
                        Log.i(TAG, "[ 출발지입니다. 네비게이션을 시작합니다. ]");
                        driveIndex++;
                        driveTurnType = guideList.get(driveIndex).getTurnType();
                        directionHandler.sendEmptyMessage(0);
                    }
                    handler.sendEmptyMessage(0);

                    // [ ArrayList들의 size보다 index가 같거나 크면 outOfIndex Error 따라서 예외처리를 해준다. ]
                    if (guideList.size() <= driveIndex) {
                        Log.i(TAG, "============================== [ GuideActivity ] ==============================");
                        Log.i(TAG, "Index의 값이 ArrayList의 Size를 넘었습니다.");
                        threadFlag = false;
                        finish();
                    }

                    // [ 도착지 ]
                    // [ simul_destination_check ]
                    // [ 안내를 종료하고, 메인 페이지로 넘기자. ]
                    if (guideList.get(driveIndex).getTurnType() == 201) {
                        // [ 도착지 ]
                        // [ destination_check : 도착지 10m 이내인 경우 true ]
                        threadFlag = false;

                        // [ Thread 안에서 Toast(Thread)를 실행시켜서 오류가 발생한다. ]
                        // [ 따라서, Handler를 사용한다ㅣ .]
                        Handler mHandler = new Handler(Looper.getMainLooper());
                        mHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getApplicationContext(), "도착지 부근입니다. 모의주행을 종료합니다.", Toast.LENGTH_LONG).show();
                            }
                        }, 0);
                        Log.i(TAG, "============================== [ GuideActivity ] ==============================");
                        Log.i(TAG, "도착지 10m 이내입니다. 모의주행을 종료합니다.");
                        driveDestinationCheck = true;

                        // [ 메인페이지로 넘기기 ]
                        startActivity(new Intent(GuideActivity.this, MainActivity.class));
                        overridePendingTransition(R.anim.anim_slide_fade_in, R.anim.anim_slide_out_left);
                        finish();
                    }   // [ if(도착지) ]

                    // [ 방향정보를 얻는 곳이다. ]
                    // [ info_list에서 다음 방향정보를 가지고있는 위도 경도값을 destination에 넣는다. ]
                    // [ direction_check 값은 false로 변경하여, 다음 방향을 알고싶을때까지 이 조건문을 건너뛴다. ]
                    // [ check10~50 은 목표지점까지의 반경거리를 한번씩만 체크하기위한 flag값이다. ]
                    if (driveDestinationCheck) {
                        // [ 10m 이내라서 다음 목표지점을 찾아야 한다. Coordinates 값이 아닌 Information 값 ]
                        driveDestinationLatitude = guideList.get(driveIndex).getLatitude();
                        driveDestinationLongitude = guideList.get(driveIndex).getLongitude();

                        beforeDriveTurnType = driveTurnType;

                        driveDirectionCheck = false;            // [ 계속해서 다음 방향을 알기위한 지점의 위도 경도값과 turnType을 알 필요가 없기 때문이다. ]
                        driveDestination10 = true;
                        driveDestination30 = true;
                        driveDestination50 = true;
                    }   // [ if(direction_check) ]

                    driveInfoDistance = exception.calDistance(current_latitude, current_longitude, driveDestinationLatitude, driveDestinationLongitude);      // [ 다음 방향안내 위도, 경도까지 거리 ]

                    // [ 목표지점 까지의 거리이다. ]
                    // [ 10m이내 & 10-30m & 30-50m 사이의 값들이다. ]
                    // [ 한번씩만 출력하기위해 flag값을 설정했다. ]
                    if (driveInfoDistance <= 50 && driveInfoDistance > 30 && driveDestination50)
                        driveDestination50 = false;
                    else if (driveInfoDistance <= 30 && driveInfoDistance > 10 && driveDestination30) {
                        Log.i(TAG, "============================== [ GuideActivity ] ==============================");
                        Log.i(TAG, " [ 30m 전입니다. OLED 알림을 주겠습니다. ] ");
                        updateTurnType(beforeDriveTurnType);      // 10m 방향알림
                        driveDestination30 = false;
                    } else if (driveInfoDistance <= 10 && driveDestination10) {
                        Log.i(TAG, "============================== [ GuideActivity ] ==============================");
                        Log.i(TAG, "[ 다음 방향안내를 진행하겠습니다. ]");
                        Log.i(TAG, "[ Before Drive TurnType ] : " + beforeDriveTurnType);

                        driveDestination10 = false;
                        driveIndex++;                       // 다음 방향 가져옴.
                        driveDirectionCheck = true;
                    }
                    handler.sendEmptyMessage(0);
                }

            }
        };
        uiThread.start();
    }
    @Override
    public void onLocationChange(Location location) {

        Log.i(TAG, "============================== [ GuideActivity ] ==============================");
        Log.i(TAG, " [ On Location Change ] : " + location.getLongitude() + " & " + location.getLatitude());
        // Toast.makeText(getApplicationContext(), "[ onLocationChange ] " + location.getLongitude() + " / " + location.getLatitude(), Toast.LENGTH_LONG).show();

        current_longitude = location.getLongitude();
        current_latitude = location.getLatitude();

        handler.sendEmptyMessage(0);

        startPoint.setLongitude(current_longitude);
        startPoint.setLatitude(current_latitude);


        if (guideList.get(driveIndex).getTurnType() == 0) {
            Log.i(TAG, "============================== [ GuideActivity ] ==============================");
            Log.i(TAG, "[ 출발지입니다. 네비게이션을 시작합니다. ]");
            driveIndex++;
            driveTurnType = guideList.get(driveIndex).getTurnType();
            directionHandler.sendEmptyMessage(0);
        }
        handler.sendEmptyMessage(0);

        // [ ArrayList들의 size보다 index가 같거나 크면 outOfIndex Error 따라서 예외처리를 해준다. ]
        if (guideList.size() <= driveIndex) {
            Log.i(TAG, "============================== [ GuideActivity ] ==============================");
            Log.i(TAG, "Index의 값이 ArrayList의 Size를 넘었습니다.");
            threadFlag = false;
            finish();
        }

        // [ 도착지 ]
        // [ simul_destination_check ]
        // [ 안내를 종료하고, 메인 페이지로 넘기자. ]
        if (guideList.get(driveIndex).getTurnType() == 201) {
            // [ 도착지 ]
            // [ destination_check : 도착지 10m 이내인 경우 true ]
            threadFlag = false;

            // [ Thread 안에서 Toast(Thread)를 실행시켜서 오류가 발생한다. ]
            // [ 따라서, Handler를 사용한다ㅣ .]
            Handler mHandler = new Handler(Looper.getMainLooper());
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), "도착지 부근입니다. 모의주행을 종료합니다.", Toast.LENGTH_LONG).show();
                }
            }, 0);
            Log.i(TAG, "============================== [ GuideActivity ] ==============================");
            Log.i(TAG, "도착지 10m 이내입니다. 모의주행을 종료합니다.");
            driveDestinationCheck = true;

            // [ 메인페이지로 넘기기 ]
            startActivity(new Intent(GuideActivity.this, MainActivity.class));
            overridePendingTransition(R.anim.anim_slide_fade_in, R.anim.anim_slide_out_left);
            finish();
        }   // [ if(도착지) ]

        // [ 방향정보를 얻는 곳이다. ]
        // [ info_list에서 다음 방향정보를 가지고있는 위도 경도값을 destination에 넣는다. ]
        // [ direction_check 값은 false로 변경하여, 다음 방향을 알고싶을때까지 이 조건문을 건너뛴다. ]
        // [ check10~50 은 목표지점까지의 반경거리를 한번씩만 체크하기위한 flag값이다. ]
        if (driveDestinationCheck) {
            // [ 10m 이내라서 다음 목표지점을 찾아야 한다. Coordinates 값이 아닌 Information 값 ]
            driveDestinationLatitude = guideList.get(driveIndex).getLatitude();
            driveDestinationLongitude = guideList.get(driveIndex).getLongitude();

            beforeDriveTurnType = driveTurnType;

            driveDirectionCheck = false;            // [ 계속해서 다음 방향을 알기위한 지점의 위도 경도값과 turnType을 알 필요가 없기 때문이다. ]
            driveDestination10 = true;
            driveDestination30 = true;
            driveDestination50 = true;
        }   // [ if(direction_check) ]

        driveInfoDistance = exception.calDistance(current_latitude, current_longitude, driveDestinationLatitude, driveDestinationLongitude);      // [ 다음 방향안내 위도, 경도까지 거리 ]

        // [ 목표지점 까지의 거리이다. ]
        // [ 10m이내 & 10-30m & 30-50m 사이의 값들이다. ]
        // [ 한번씩만 출력하기위해 flag값을 설정했다. ]
        if (driveInfoDistance <= 50 && driveInfoDistance > 30 && driveDestination50)
            driveDestination50 = false;
        else if (driveInfoDistance <= 30 && driveInfoDistance > 10 && driveDestination30) {
            Log.i(TAG, "============================== [ GuideActivity ] ==============================");
            Log.i(TAG, " [ 30m 전입니다. OLED 알림을 주겠습니다. ] ");
            updateTurnType(beforeDriveTurnType);      // 10m 방향알림
            driveDestination30 = false;
        } else if (driveInfoDistance <= 10 && driveDestination10) {
            Log.i(TAG, "============================== [ GuideActivity ] ==============================");
            Log.i(TAG, "[ 다음 방향안내를 진행하겠습니다. ]");
            Log.i(TAG, "[ Before Drive TurnType ] : " + beforeDriveTurnType);

            driveDestination10 = false;
            driveIndex++;                       // 다음 방향 가져옴.
            driveDirectionCheck = true;
        }
        handler.sendEmptyMessage(0);
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

        handler.sendEmptyMessage(0);
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


        // [ 푸른어린이집 ]
        double pLatitude = 37.473315;
        double pLongitude = 127.099965;

        // [ 세곡교회 ]
        double sLatitude = 37.466579;
        double sLongitude = 127.097262;

        startX = Double.toString(pLongitude);
        startY = Double.toString(pLatitude);
        endX = Double.toString(sLongitude);
        endY = Double.toString(sLatitude);

        Log.i(TAG, "[ Guide ] : Start Point : " + startX + " / " + startY);
        Log.i(TAG, "[ Guide ] : End Point : " + endX + " / " + endY);

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

                        }
                        Log.i(TAG, "[ Car Path ] Distance : " + response.body().getFeatures().get(i).getProperties().getDistance());
                        Log.i(TAG, "[ Car Path ] Description : " + response.body().getFeatures().get(i).getProperties().getDescription());
                        Log.i(TAG, "[ Car Path ] TurnType : " + response.body().getFeatures().get(i).getProperties().getTurnType());

                        int guideIndex = response.body().getFeatures().get(i).getProperties().getIndex();
                        int guideTurnType = response.body().getFeatures().get(i).getProperties().getTurnType();
                        double guideLongitude = pathList.get(i).getLongitude();
                        double guideLatitude = pathList.get(i).getLatitude();

                        GuideDataVO guideVO = new GuideDataVO(guideIndex, guideTurnType, guideLatitude, guideLongitude);
                        guideList.add(guideVO);

                    }
                }   // if(response.isSuccessful())
            }   // onResponse

            @Override
            public void onFailure(Call<TmapDataVO> call, Throwable t) {
                Log.i(TAG, "[ Guide Activity ] onFailure : " + call.toString());
                Log.i(TAG, "[ Guide Activity ] onFailure : " + t.toString());
            }
        }); // call.enqueue
    }   // getPathInfo

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Log.i(TAG, "[ Handler ] : " + startPoint.getLongitude() + " & " + startPoint.getLatitude());
            tmapview.setLocationPoint(current_longitude, current_latitude);

            // 거리 변경
            strCurrentDistance = exception.strDistance((int) driveInfoDistance);
            tv_distance.setText(strCurrentDistance);
            Log.i(TAG, "[[[[[ 다음 안내까지의 거리를 변경합니다. ]]]]] : " + strCurrentDistance);
        }
    };   // [ End Handler ]

    private Handler directionHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            changeDirectionImg(beforeDriveTurnType, driveTurnType);
        }
    };   // [ End directionHandler ]

    // [ Direction 이미지 변경 ]
    public void changeDirectionImg(int simul_before_turnType, int turnType) {
        Log.i(TAG, "[ 방향전환이 이뤄졌습니다. ]");
        this.updateTurnType(9);
        switch (turnType) {
            case 200:
                img_direction.setImageResource(R.drawable.direction_200);
                Log.i(TAG, "[ Direction 200 ] : 출발지입니다. ");
                break;
            case 201:
                img_direction.setImageResource(R.drawable.direction_201);
                Log.i(TAG, "[ Direction 201 ] : 도착지입니다. ");
                break;
//            case 0:
            case 11:
                img_direction.setImageResource(R.drawable.direction_11);
                Log.i(TAG, "[ Direction 011 ] : 직진입니다. ");
                break;
//            case 1:
            case 12:
                img_direction.setImageResource(R.drawable.direction_12);
                Log.i(TAG, "[ Direction 012 ] : 좌회전입니다. ");
                break;
//            case 2:
            case 13:
                img_direction.setImageResource(R.drawable.direction_13);
                Log.i(TAG, "[ Direction 013 ] : 우회전입니다. ");

                break;
//            case 3:
            case 14:
                img_direction.setImageResource(R.drawable.direction_14);
                Log.i(TAG, "[ Direction 014 ] : 유턴입니다. ");
                break;
        }

    }   // [ End Direction 이미지 변경 ]

    public void updateTurnType(int beforeTurnType) {
        switch (beforeTurnType) {
            case 200:
                this.firmwareConnection(4);
                break;
            case 201:
                this.firmwareConnection(5);
                break;
            case 11:
                this.firmwareConnection(0);
                break;
            case 12:
                this.firmwareConnection(1);
                if (isRunningPSocBleService) {
                    mPSoCBleService.writeDirectionCharacteristic(1);
                }
                break;
            case 13:
                this.firmwareConnection(2);
                if (isRunningPSocBleService) {
                    mPSoCBleService.writeDirectionCharacteristic(2);
                }
                break;
            case 14:
                this.firmwareConnection(3);
                if (isRunningPSocBleService) {
                    mPSoCBleService.writeDirectionCharacteristic(3);
                }
                break;
            case 9:
                this.firmwareConnection(9);
                if (isRunningPSocBleService) {
                    mPSoCBleService.writeDirectionCharacteristic(9);
                }
                Log.i(TAG, "[ Cancel Notification ] ");
                break;
        }
    }

    public void firmwareConnection(int turnType) {
        switch (turnType) {
            case 0:
                Log.i(TAG, "[ Direction 011 ] : 직진입니다. ");
                break;
            case 1:
                Log.i(TAG, "[ Direction 012 ] : 좌회전입니다. ");
                break;
            case 2:
                Log.i(TAG, "[ Direction 013 ] : 우회전입니다. ");
                break;
            case 3:
                Log.i(TAG, "[ Direction 014 ] : 유턴입니다. ");
                break;
            case 4:
                Log.i(TAG, "[ Direction 200 ] : 출발지입니다. ");
                break;
            case 5:
                Log.i(TAG, "[ Direction 201 ] : 도착지입니다. ");
                break;
            case 9:
                Log.i(TAG, "[ Firmware Connection Cancel Direction ]");
                break;
        }   // [ End switch ]
    }


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
