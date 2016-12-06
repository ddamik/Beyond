package com.example.lee.tmap.Activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.lee.tmap.ApiService;
import com.example.lee.tmap.R;
import com.example.lee.tmap.UserException;
import com.example.lee.tmap.ValueObject.SimulationCoordinatesVO;
import com.example.lee.tmap.ValueObject.SimulationVO;
import com.example.lee.tmap.ValueObject.TmapDataVO;
import com.skp.Tmap.TMapData;
import com.skp.Tmap.TMapMarkerItem;
import com.skp.Tmap.TMapPoint;
import com.skp.Tmap.TMapPolyLine;
import com.skp.Tmap.TMapView;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.GsonConverterFactory;
import retrofit2.Response;
import retrofit2.Retrofit;


/**
 * Created by Lee on 2016-11-21.
 */
public class SimulationActivity extends AppCompatActivity {

    public static final String TAG = "SimulationActivity";

    // [ Map ]
    public static final String APP_KEY = "483f055b-19f2-3a22-a3fb-935bc1684b0b";
    public TMapData tMapData;
    private RelativeLayout tMapLayout;
    private TMapView tmapview = null;
    public static int ZOOM_LEVEL = 19;

    // [ GPS & Point ]
    private TMapPoint startPoint;
    private TMapPoint endPoint;
    private TMapPoint currentPoint;
    public double current_longitude = 0.0;
    public double current_latitude = 0.0;
    public double des_longitude = 0.0;
    public double des_latitude = 0.0;


    // [ 겹친 layout 정보 표시 ]
    ImageView img_direction;
    ImageButton img_btn_currentPoint, img_btn_exit;
    TextView tv_distance, tv_remain_distance, tv_arriaval_time;
    public UserException exception;

    // [ 총 남은거리 ]
    public int total_distance = 0;
    public String strRemain = "";


    // [ Simulation ]
    public ArrayList<SimulationVO> info_list = new ArrayList<>();
    public ArrayList<SimulationCoordinatesVO> coordinates_list = new ArrayList<>();

    // [ Simulation Variable ]
    static TMapPoint tpoint = null;
    static TMapMarkerItem tMapMarkerItem = null;

    static int simul_coordinates_index = 0;
    static int simul_info_index = 0;
    static int simul_turntype_index = 0;

    static double simul_next_latitude = 0.0;
    static double simul_next_longitude = 0.0;

    static double simul_destination_latitude = 0.0;
    static double simul_destination_longitude = 0.0;

    static double simul_current_latitude = 0.0;
    static double simul_current_longitude = 0.0;

    static boolean simul_destination_check = false;
    static boolean simul_direction_check = true;
    static boolean simul_next_check = true;
    static boolean simul_check50 = false;
    static boolean simul_check30 = false;
    static boolean simul_check10 = false;

    static double simul_addValue = 0.0;
    static double simul_coordi_distance = 0.0;       // [ 다음 coordinates_list 까지의 거리 ]
    static double simul_info_distance = 0.0;         // [ 다음 info_list 까지의 거리 ]
    static final double COORDINATES_DISTANCE = 3;

    static double simul_gap_latitude = 0.0;
    static double simul_gap_longitude = 0.0;
    static int simul_turnType = 0;

    static int current_distance = 0;
    static String strCurrentDistance = "";

    public static Thread thread;
    public static boolean threadFlag = true;

    static int remain_distance = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simulation);

        // [ User Exception ]
        exception = new UserException();

        // layout 겹치기
        Window window = getWindow();
        window.setContentView(R.layout.activity_simulation);     // 바닥에 깔릴 layout
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        LinearLayout linear = (LinearLayout) inflater.inflate(R.layout.activity_simulation_info, null);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        window.addContentView(linear, params);

        // 겹친 layout 정보
        img_direction = (ImageView) findViewById(R.id.img_direction);                       // 방향 이미지 ( 좌, 우, 유턴 )
        img_btn_currentPoint = (ImageButton) findViewById(R.id.img_btn_currentPoint);       // 현재위치로 가는 버튼
        img_btn_exit = (ImageButton) findViewById(R.id.img_btn_exit);                       // 안내종료
        tv_distance = (TextView) findViewById(R.id.tv_distance);                            // 다음 안내지점까지의 거리
        tv_remain_distance = (TextView) findViewById(R.id.tv_remain_distance);              // 총 남은 거리
        tv_arriaval_time = (TextView) findViewById(R.id.tv_arrival_time);                   // 도착 예상시간

        tMapData = new TMapData();
        setInitLocation();
        initMapView();                  // 지도 초기화
        setGPSValue();                  // [ 시뮬레이션 GPS 값 설정 ]
        setPathInfo();                  // [ 시뮬레이션 방향 값 설정 ]
        setTextInfo();                  // [ 시뮬레이션 텍스트 정보 설정 ]
        // getPathInfo(startPoint, endPoint);

        tMapData.findPathDataWithType(TMapData.TMapPathType.CAR_PATH, startPoint, endPoint, new TMapData.FindPathDataListenerCallback() {
            @Override
            public void onFindPathData(TMapPolyLine tMapPolyLine) {
                tMapPolyLine.setLineColor(Color.BLUE);          // PolyLine 색상은 블루
                tMapPolyLine.setLineWidth(20);                  // 두께는 20
                tmapview.addTMapPath(tMapPolyLine);             // 지도에 추가
            }
        });
        tmapview.setCenterPoint(startPoint.getLongitude(), startPoint.getLatitude(), true);         // [ 현재 위치로 가기 ]

        // [ Simulation ]
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 2;
        Bitmap bitmap = BitmapFactory.decodeResource(getApplicationContext().getResources(), R.drawable.guide_arrow_blue, options);

        tpoint = new TMapPoint(startPoint.getLatitude(), startPoint.getLongitude());
        tMapMarkerItem = new TMapMarkerItem();
        tMapMarkerItem.setTMapPoint(tpoint);
        tMapMarkerItem.setVisible(TMapMarkerItem.VISIBLE);
        tMapMarkerItem.setIcon(bitmap);
        tMapMarkerItem.setPosition(1, 1);
        tmapview.addMarkerItem("", tMapMarkerItem);
        tmapview.setIconVisibility(true);

        // [ 안내종료 버튼 ]
        img_btn_exit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(), "모의주행을 종료합니다.", Toast.LENGTH_LONG).show();
                startActivity(new Intent(SimulationActivity.this, MainActivity.class));
                overridePendingTransition(R.anim.anim_slide_fade_in, R.anim.anim_slide_out_left);

                threadFlag = false;
                finish();
            }
        }); // [ 안내종료 버튼 ]

        img_btn_currentPoint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "[ Set Center Point ] ");
                tmapview.setCenterPoint(127.074347, 37.549635, true);
//                simulation();
            }
        });

        simulation();
    }   // onCreate

    // [ StartPoint & EndPoint 설정 ]
    public void setInitLocation(){

        // [ 세종대학교 학술정보원 ]
        double sejong_latitude = 37.551251;
        double sejong_longitude = 127.073887;

        // [ 커먼 그라운드 ]
        double commonground_latitude = 37.541119;
        double commonground_longitude = 127.066079;

        // [ 건대 알라딘서점 ]
        double al_latitude = 37.540969;
        double al_longitude = 127.070838;

        current_latitude = sejong_latitude;
        current_longitude = sejong_longitude;

        des_latitude = al_latitude;
        des_longitude = al_longitude;

        startPoint = new TMapPoint(current_latitude, current_longitude);
        currentPoint = new TMapPoint(current_latitude, current_longitude);
        endPoint = new TMapPoint(des_latitude, des_longitude);

    }   // [ End Set Init Location ]

    // 지도 셋팅
    public void initMapView() {

        tMapLayout = (RelativeLayout) findViewById(R.id.simulation_tmap);
        tmapview = new TMapView(this);
        tmapview.setSKPMapApiKey(APP_KEY);
        tmapview.setLanguage(TMapView.LANGUAGE_KOREAN);

        tmapview.setIconVisibility(true);                   // 현재 위치로 표시될 아이콘을 표시
        tmapview.setZoomLevel(ZOOM_LEVEL);                       // 지도레벨 설정 7~19
        tmapview.setMapType(TMapView.MAPTYPE_STANDARD);     // STANDARD: 일반지도 / SATELLITE: 위성지도[미지원] / HYBRID: 하이브리드[미지원] / TRAFFIC: 실시간 교통지도
        tmapview.setCompassMode(true);                      // 단말의 방향에 따라 움직이는 나침반 모드
        tmapview.setTrackingMode(true);                     // 화면중심을 단말의 현재위치로 이동시켜주는 모드
        tmapview.setMapPosition(TMapView.POSITION_NAVI);    // 네비게이션 모드 ( 화면 중심의 아래쪽으로 중심좌표를 설정 )
        tMapLayout.addView(tmapview);
        // tmapview.setCenterPoint : 지도의 중심좌표를 이동
        // tmapview.setLocationPoint : 현재위치로 표시될 좌표의 위도, 경도를 설정

        // 현재 위치로 표시되는 좌표의 위도, 경도를 반환
        // TMapPoint tpoint = tmapview.getLocationPoint();
        // double Latitue = tpoint.getLatitude();
        // double Longitude = tpoint.getLongitude();
    }   // [ End initMapView ]

    // [ SimulationCoordinates 위도 & 경도 설정 ]
    public void setGPSValue(){

        SimulationCoordinatesVO vo;

        // latitude & longitude;
        double latitude = 0.0;
        double longitude = 0.0;

        // [ index = 0 & Start ]
        latitude = 37.551251;
        longitude = 127.073887;
        vo = new SimulationCoordinatesVO(latitude, longitude);
        coordinates_list.add(vo);

        latitude = 37.551084;       // [ 학술정보원과 아사당 사이길 ]
        longitude = 127.074111;
        vo = new SimulationCoordinatesVO(latitude, longitude);
        coordinates_list.add(vo);

        // [ index = 2 ]
        latitude = 37.550575;
        longitude = 127.074599;
        vo = new SimulationCoordinatesVO(latitude, longitude);
        coordinates_list.add(vo);

        latitude = 37.550288;       // [ 광개토관 주차장 앞 ]
        longitude = 127.074256;
        vo = new SimulationCoordinatesVO(latitude, longitude);
        coordinates_list.add(vo);

        // [ index = 4 ]
        latitude = 37.550019;
        longitude = 127.074013;
        vo = new SimulationCoordinatesVO(latitude, longitude);
        coordinates_list.add(vo);

        latitude = 37.549753;       // [ 세종관 앞 1 ]
        longitude = 127.074285;
        vo = new SimulationCoordinatesVO(latitude, longitude);
        coordinates_list.add(vo);

        latitude = 37.549707;       // [ 세종관 앞 2 ]
        longitude = 127.074314;
        vo = new SimulationCoordinatesVO(latitude, longitude);
        coordinates_list.add(vo);

        latitude = 37.549635;       // [ 세종관 앞 3 ]
        longitude = 127.074347;
        vo = new SimulationCoordinatesVO(latitude, longitude);
        coordinates_list.add(vo);

        // [ index = 6 ]
        latitude = 37.549293;
        longitude = 127.074414;
        vo = new SimulationCoordinatesVO(latitude, longitude);
        coordinates_list.add(vo);

        // [ index = 8 ]
        latitude = 37.549012;
        longitude = 127.075243;
        vo = new SimulationCoordinatesVO(latitude, longitude);
        coordinates_list.add(vo);

        latitude = 37.547405;       // [ 학교앞 사거리 ]
        longitude = 127.074288;     // [ 사거리 지나기 전 ]
        vo = new SimulationCoordinatesVO(latitude, longitude);
        coordinates_list.add(vo);

        latitude = 37.547117;       // [ 학교앞 사거리 ]
        longitude = 127.074048;     // [ 사거리 지나고 후 ]
        vo = new SimulationCoordinatesVO(latitude, longitude);
        coordinates_list.add(vo);

        // [ index = 10 ]
        latitude = 37.540992;
        longitude = 127.070828;
        vo = new SimulationCoordinatesVO(latitude, longitude);
        coordinates_list.add(vo);

        // [ End ]
        latitude = 37.540992;
        longitude = 127.070828;
        vo = new SimulationCoordinatesVO(latitude, longitude);
        coordinates_list.add(vo);

    }   // [ set GPS Value ]

    // [ 경로 가는곳마다 방향 설정 ]
    public void setPathInfo(){
        /*
                public int index;
                public int totalDistance;
                public int totalFare;
                public int totalTime;
                public int turnType;
                public int distance;
                public String description;
                public double longitude;
                public double latitude;
         */

        // [ init Value ]
        int index = 0;
        int totalDistance = 0;
        int totalFare = 0;
        int totalTime = 0;
        int turnType = 0;
        int distance = 0;
        String description = "";
        double longitude = 0.0;
        double latitude = 0.0;
        SimulationVO vo;

        // [ index = 0 ]
        index = 0;
        totalDistance = 1334;
        totalTime = 215;
        totalFare = 0;
        distance = 0;
        description = "일반도로를 따라 방면으로 96m 이동";
        turnType = 200;
        latitude = 37.551251;
        longitude = 127.073887;
        vo = new SimulationVO(index, totalDistance, totalTime, totalFare, distance, description, turnType, latitude, longitude);
        info_list.add(vo);

        // [ index = 1 ]
        index = 1;
        description = "일반도로, 96m";
        distance = 96;
        turnType = 0;
        latitude = 0.0;
        longitude = 0.0;
        vo = new SimulationVO(index, distance, turnType, description, latitude, longitude);
        info_list.add(vo);

        // [ index = 2 ]
        index = 2;
        distance = 0;
        description = "우회전 후 일반도로를 따라 83m 이동";
        turnType = 13;
        latitude = 37.550575;
        longitude = 127.074599;
        vo = new SimulationVO(index, distance, turnType, description, latitude, longitude);
        info_list.add(vo);

        // [ index = 3 ]
        index = 3;
        distance = 83;
        description = "일반도로, 83m";
        turnType = 0;
        latitude = 0.0;
        longitude = 0.0;
        vo = new SimulationVO(index, distance, turnType, description, latitude, longitude);
        info_list.add(vo);

        // [ index = 4 ]
        index = 4;
        distance = 0;
        description = "좌회전 후 일반도로를 따라 90m 이동";
        turnType = 12;
        latitude = 37.550019;
        longitude = 127.074013;
        vo = new SimulationVO(index, distance, turnType, description, latitude, longitude);
        info_list.add(vo);

        // [ index = 5 ]
        index = 5;
        distance = 90;
        description = "일반도로, 90m";
        turnType = 0;
        latitude = 0.0;
        longitude = 0.0;
        vo = new SimulationVO(index, distance, turnType, description, latitude, longitude);
        info_list.add(vo);

        // [ index = 6 ]
        index = 6;
        distance = 0;
        description = "좌회전 후 일반도로를 따라 84m 이동";
        turnType = 12;
        latitude = 37.549293;
        longitude = 127.074414;
        vo = new SimulationVO(index, distance, turnType, description, latitude, longitude);
        info_list.add(vo);

        // [ index = 7 ]
        index = 7;
        distance = 0;
        description = "일반도로, 84m";
        turnType = 0;
        latitude = 0.0;
        longitude = 0.0;
        vo = new SimulationVO(index, distance, turnType, description, latitude, longitude);
        info_list.add(vo);

        // [ index = 8 ]
        index = 8;
        distance = 0;
        description = "우회전 후 능동로를 따라 981m 이동";
        turnType = 13;
        latitude = 37.549012;
        longitude = 127.075243;
        vo = new SimulationVO(index, distance, turnType, description, latitude, longitude);
        info_list.add(vo);

        // [ index = 9 ]
        index = 9;
        distance = 981;
        description = "능동로, 981m";
        turnType = 0;
        latitude = 0.0;
        longitude = 0.0;
        vo = new SimulationVO(index, distance, turnType, description, latitude, longitude);
        info_list.add(vo);

        // [ index = 10 ]
        index = 10;
        distance = 0;
        description = "능동로를 따라 981m 직진입니다.";
        turnType = 11;
        latitude = 37.540992;
        longitude = 127.070828;
        vo = new SimulationVO(index, distance, turnType, description, latitude, longitude);
        info_list.add(vo);

        // [ index = 11 ] : 없는 값인데, 모의주행시, 도착지까지 주행하기 위해 추가했음.
        // 만약에 없다면, 도착지까지 직진인지 알림도 없음.
        index = 11;
        distance = 0;
        description = "도착";
        turnType = 201;
        latitude = 37.540992;
        longitude = 127.070828;
        vo = new SimulationVO(index, distance, turnType, description, latitude, longitude);
        info_list.add(vo);
    }   // [ End Set Path Info ]

    public void simulation(){
        /*
            [ 1. 짝수 ]
                turnType
                description
                coordinates 한개
            [ 2. 홀수 ]
                distance
                coordinates 여러개
        */

        /*
            1. info_list 를 index 0부터 돌리고,
            2. info_list[index].turnType이 201이면 종료.
            3. coordinates_list를 index 0부터 돌
            3. coordinates_list[index] 와 info_list[index] 위도 경도를 바탕으로 거리를 구하고,
            4. 거리가 50m일때, 30m일때, 10m일때,
            5. 10m이내면 info_list의 index++;
         */

        simul_current_latitude = coordinates_list.get(simul_coordinates_index).getLatitude();
        simul_current_longitude = coordinates_list.get(simul_coordinates_index).getLongitude();

        // [ 출발지 ]
        // [ img direction 이미지를 설정한다. ]
        // [ 다음 GPS지점과 다음 방향지점을 알기위해 각각의 index값을 ++ ]
        Toast.makeText(getApplicationContext(), "모의주행을 시작합니다.", Toast.LENGTH_LONG).show();
        Log.i(TAG, "============================== [ SImulation ] ==============================");
        Log.i(TAG, "모의주행을 시작합니다.");
//        img_direction.setImageResource(R.drawable.direction_11);
        simul_info_index += 2;
        simul_turnType = info_list.get(simul_info_index).getTurnType();

        simul_coordinates_index++;

        thread = new Thread(){
            @Override
            public void run() {
                while (threadFlag) {
                    try {
                        Thread.sleep(100);
                        // [ turnType == 0 인 경우는 목표지점까지의 거리정보뿐이기때문에 얻을 정보가 없다. 따라서, index를 추가하고 continue; ]
                        if (info_list.get(simul_info_index).getTurnType() == 0){
                            simul_info_index++;
                            simul_turnType = info_list.get(simul_info_index).getTurnType();
                            directionHandler.sendEmptyMessage(0);
                        }
                        handler.sendEmptyMessage(0);

                        // [ ArrayList들의 size보다 index가 같거나 크면 outOfIndex Error 따라서 예외처리를 해준다. ]
                        if (info_list.size() <= simul_info_index || coordinates_list.size() <= simul_coordinates_index) {
                            Log.i(TAG, "============================== [ SImulation Error ] ==============================");
                            Log.i(TAG, "Index의 값이 ArrayList의 Size를 넘었습니다.");
                            threadFlag = false;
                            finish();
                            break;
                        }

                        // [ 도착지 ]
                        // [ simul_destination_check ]
                        // [ 안내를 종료하고, 메인 페이지로 넘기자. ]
                        if (info_list.get(simul_info_index).getTurnType() == 201) {
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
                            Log.i(TAG, "============================== [ SImulation ] ==============================");
                            Log.i(TAG, "도착지 10m 이내입니다. 모의주행을 종료합니다.");
                            simul_destination_check = true;

                            // [ 메인페이지로 넘기기 ]
                            startActivity(new Intent(SimulationActivity.this, MainActivity.class));
                            overridePendingTransition(R.anim.anim_slide_fade_in, R.anim.anim_slide_out_left);
                            finish();
                        }   // [ if(도착지) ]

                        // [ 방향정보를 얻는 곳이다. ]
                        // [ info_list에서 다음 방향정보를 가지고있는 위도 경도값을 destination에 넣는다. ]
                        // [ direction_check 값은 false로 변경하여, 다음 방향을 알고싶을때까지 이 조건문을 건너뛴다. ]
                        // [ check10~50 은 목표지점까지의 반경거리를 한번씩만 체크하기위한 flag값이다. ]
                        if (simul_direction_check) {
                            // [ 10m 이내라서 다음 목표지점을 찾아야 한다. Coordinates 값이 아닌 Information 값 ]
                            simul_destination_latitude = info_list.get(simul_info_index).getLatitude();
                            simul_destination_longitude = info_list.get(simul_info_index).getLongitude();

                            simul_direction_check = false;            // [ 계속해서 다음 방향을 알기위한 지점의 위도 경도값과 turnType을 알 필요가 없기 때문이다. ]
                            simul_check10 = true;
                            simul_check30 = true;
                            simul_check50 = true;
                        }   // [ if(direction_check) ]

                        // [ 다음 GPS지점의 위도 경도값을 가져온다. ]
                        // [ coordinates_list에서 다음 GPS값을 가져와 next 변수에 넣는다. ]
                        // [ addValue : 모의주행을 1m단위로 하기위해 다음 GPS까지의 거리를 구해놓은 값이다. ]
                        // [ (다음위치 - 현재위치) / 거리 = 1m단위의 위도 경도값을 얻는다. ]
                        if (simul_next_check) {
                            simul_next_latitude = coordinates_list.get(simul_coordinates_index).getLatitude();                  // [ 다음 coordinates 위도 경도를 설정한다. ]
                            simul_next_longitude = coordinates_list.get(simul_coordinates_index).getLongitude();

                            simul_addValue = exception.calDistance(simul_current_latitude, simul_current_longitude, simul_next_latitude, simul_next_longitude); // [ 현재 위치에서 1m씩 이동하기 위해 다음 위치까지의 거리를 구한다. ]
                            simul_gap_latitude = (simul_next_latitude - simul_current_latitude) / simul_addValue;                                               // [ 현재 위치에 위도 경도를 더한다. ]
                            simul_gap_longitude = (simul_next_longitude - simul_current_longitude) / simul_addValue;
                            simul_next_check = false;        // [ 다음 coordinates 까지와의 거리가 2m 이내일때까지는 이 조건문에 들어올 필요가 없다. ]
                        }   // [ if(next_check) ]


                        // [ 현재 위도경도에 1m 단위 모의주행을 위한 위도경도값을 누적시킨다. ]
                        // [ TMap의 중심을 현재위치로 잡는다. ]
                        simul_current_latitude += simul_gap_latitude;                                           // [ 현재 위치에서 1m 단위로 경도 위도값을 추가해준다. ]
                        simul_current_longitude += simul_gap_longitude;

//                            tmapview.setLocationPoint(simul_current_longitude, simul_current_latitude);     // // [ 현재 위치로 가기 & 현재 위치를 지도의 중심으로 ]
                        tmapview.setCenterPoint(simul_current_longitude, simul_current_latitude, true);

                        simul_coordi_distance = exception.calDistance(simul_current_latitude, simul_current_longitude, simul_next_latitude, simul_next_longitude);                  // [ 다음 GPS 좌표까지 거리 ] / 곡선 처리를 위한 GPS
                        simul_info_distance = exception.calDistance(simul_current_latitude, simul_current_longitude, simul_destination_latitude, simul_destination_longitude);      // [ 다음 방향안내 위도, 경도까지 거리 ]

                        // [ 해야할 일 ]
                        // [ 총 남은거리를 1m씩 줄여야 한다. ]
                        // [ 이동거리를 누적하여 100m단위로 0.1km 씩 감소시킨다. ]
                        current_distance++;

                        // [ 목표지점 까지의 거리이다. ]
                        // [ 10m이내 & 10-30m & 30-50m 사이의 값들이다. ]
                        // [ 한번씩만 출력하기위해 flag값을 설정했다. ]
                        if (simul_info_distance <= 50 && simul_info_distance > 30 && simul_check50)
                            simul_check50 = false;
                        else if (simul_info_distance <= 30 && simul_info_distance > 10 && simul_check30)
                            simul_check30 = false;
                        else if (simul_info_distance <= 10 && simul_check10)
                            simul_check10 = false;

                        // [ 다음 GPS 정보까지의 거리가 COORDINATES_DISTANCE보다 적을 때 ]
                        if (simul_coordi_distance < COORDINATES_DISTANCE) {
                            Log.i(TAG, "============================== [ SImulation ] ==============================");
                            Log.i(TAG, "다음 Coordinates까지의 거리가 2m 이내입니다. ");

                            // [ 방향 전환 경도 위도와 다음 GPS의 경도 위도가 같으면 방향 전환을 해야할 때이다. ]
                            if (simul_next_latitude == simul_destination_latitude && simul_next_longitude == simul_destination_longitude) {
                                simul_info_index++;             // [ 방향 전환을 위해 information index 추가 ]
                                simul_coordinates_index++;      // [ 방향 전환과 동시에 GPS 값도 추가하여 다음 안내를 받는다. ]
                                simul_direction_check = true;   // [ 방향전환 후, 다음 next_latitude와 next_longitude를 구하기 위해서. ]

                                // [ turnType 바탕으로 img_direction 값 설정 ]
                                // [ simul_info_index 값이 추가되고 나서 방향이미지를 변경해줘야 한다. ]Log.i()
                            } else {
                                simul_coordinates_index++;      // [ 곡선을 위한 GPS값에 도달한 것이지, 방향 전환 값에 도달한 것이 아니기때문에, coordinates index만 추가한다. ]
                            }
                            simul_next_check = true;            // [ 현재의 위치가 다음 coordinates 와의 거리가 2m이내로, 그 다음 coordinates를 쫓으면 된다. ]
                        }   // [ if(distance < 5) ]
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }   // [ End try-catch ]


                }   // [ End Whlie ]
            }   // [ End Run ]
        };  // [ End Thread ]
        thread.start();

    }   // [ End Simulation ]

    // [ 네비 정보 설정 ]
    public void setTextInfo(){

        total_distance = 1334;
        int total_time = 215;
        int turnType = 200;
        int distance = 0;

        String arrival_time = exception.strArrival_time(total_time);
        strRemain = exception.strRemainDistance(total_distance, total_distance);

//        img_direction.setImageResource(R.drawable.direction_11);                       // 방향 이미지 ( 좌, 우, 유턴 )
        tv_distance = (TextView) findViewById(R.id.tv_distance);                            // 다음 안내지점까지의 거리
        tv_remain_distance.setText(strRemain);                // 총 남은 거리
        tv_arriaval_time.setText(arrival_time);                 // 도착 예상시간
    }   // [ End Set Text Info ]

    // [ Direction 이미지 변경 ]
    public void changeDirectionImg(int turnType){
        Log.i(TAG, "[ 방향전환이 이뤄졌습니다. TurnType은 : " + turnType + " 입니다. ] ");
        switch (turnType){
            case 200:
                img_direction.setImageResource(R.drawable.direction_200);
                Log.i(TAG, "[ Direction 200 ] : 출발지입니다. ");
                break;
            case 201:
                img_direction.setImageResource(R.drawable.direction_201);
                Log.i(TAG, "[ Direction 201 ] : 도착지입니다. ");
                break;
            case 11:
                img_direction.setImageResource(R.drawable.direction_11);
                Log.i(TAG, "[ Direction 011 ] : 직진입니다. ");
                break;
            case 12:
                img_direction.setImageResource(R.drawable.direction_12);
                Log.i(TAG, "[ Direction 012 ] : 좌회전입니다. ");
                break;
            case 13:
                img_direction.setImageResource(R.drawable.direction_13);
                Log.i(TAG, "[ Direction 013 ] : 우회전입니다. ");
                break;
            case 14:
                img_direction.setImageResource(R.drawable.direction_14);
                Log.i(TAG, "[ Direction 014 ] : 유턴입니다. ");
                break;
        }
    }   // [ End Direction 이미지 변경 ]

    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            // [ 다음 방향안내 지점까지의 남은 거리 ]
            strCurrentDistance = exception.strDistance((int)simul_info_distance);
            tv_distance.setText(strCurrentDistance);

            // [ 총 남은거리 ]
            remain_distance = total_distance - current_distance;
            Log.i(TAG, "[ Remain Distance ] : " + remain_distance);
            strRemain = exception.strRemainDistance(total_distance, remain_distance);
            tv_remain_distance.setText(strRemain);

            // [ 현재위치 Icon ]
            tpoint.setLatitude(current_latitude);
            tpoint.setLongitude(current_longitude);
            tMapMarkerItem.setTMapPoint(tpoint);

            tmapview.addMarkerItem("", tMapMarkerItem);
        }
    };   // [ End Handler ]

    private Handler directionHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            changeDirectionImg(simul_turnType);
        }
    };   // [ End directionHandler ]

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // GPS중단
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Thread 종료
        thread.interrupt();
        threadFlag = false;
    }
}
