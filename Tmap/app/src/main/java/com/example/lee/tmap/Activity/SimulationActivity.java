package com.example.lee.tmap.Activity;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.example.lee.tmap.ApiService;
import com.example.lee.tmap.R;
import com.example.lee.tmap.UserException;
import com.example.lee.tmap.ValueObject.SimulationCoordinatesVO;
import com.example.lee.tmap.ValueObject.SimulationVO;
import com.example.lee.tmap.ValueObject.TmapDataVO;
import com.skp.Tmap.TMapData;
import com.skp.Tmap.TMapPoint;
import com.skp.Tmap.TMapPolyLine;
import com.skp.Tmap.TMapView;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.GsonConverterFactory;
import retrofit2.Response;
import retrofit2.Retrofit;


/**
 * Created by Lee on 2016-11-21.
 */
public class SimulationActivity extends Activity {

    public static final String TAG = "SimulationActivity";

    // [ Map ]
    public static final String APP_KEY = "483f055b-19f2-3a22-a3fb-935bc1684b0b";
    public TMapData tMapData;
    private RelativeLayout tMapLayout = null;
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


    // [ Simulation ]
    public ArrayList<SimulationVO> info_list = new ArrayList<>();
    public ArrayList<SimulationCoordinatesVO> coordinates_list = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simulation);

        // [ User Exception ]
        exception = new UserException();

        // layout 겹치기
        Window window = getWindow();
        window.setContentView(R.layout.activity_guide);     // 바닥에 깔릴 layout
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

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 2;
        Bitmap bitmap = BitmapFactory.decodeResource(getApplicationContext().getResources(), R.drawable.guide_arrow_blue, options);


        tMapLayout = (RelativeLayout) findViewById(R.id.tmap);
        tmapview = new TMapView(this);
        tmapview.setSKPMapApiKey(APP_KEY);
        tmapview.setLanguage(TMapView.LANGUAGE_KOREAN);
        tmapview.setIcon(bitmap);                           // 아이콘 설정

        tmapview.setIconVisibility(true);                   // 현재 위치로 표시될 아이콘을 표시
        tmapview.setZoomLevel(ZOOM_LEVEL);                       // 지도레벨 설정 7~19
        tmapview.setMapType(TMapView.MAPTYPE_STANDARD);     // STANDARD: 일반지도 / SATELLITE: 위성지도[미지원] / HYBRID: 하이브리드[미지원] / TRAFFIC: 실시간 교통지도
        tmapview.setCompassMode(false);                      // 단말의 방향에 따라 움직이는 나침반 모드
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

            [ 1. 짝수 index 10m 이내까지 간다. ]
            [ 2. 짝수 index 에서 다음 turnType을 변경 ]
            [ 3. 그 다음 홀수 index에서 distance 설정 ]
            [ 4. 그 다음 index++ 후, 1번부터 반복 ]
            [ 5. turnType이 200이면 종료한다. ]
        */

        /*
            1. info_list 를 index 0부터 돌리고,
            2. info_list[index].turnType이 201이면 종료.
            3. coordinates_list를 index 0부터 돌
            3. coordinates_list[index] 와 info_list[index] 위도 경도를 바탕으로 거리를 구하고,
            4. 거리가 50m일때, 30m일때, 10m일때,
            5. 10m이내면 info_list의 index++;
         */
        int coordinates_index = 0;
        int info_index = 0;

        double next_latitude = 0.0;
        double next_longitude = 0.0;

        double destination_latitude = 0.0;
        double destination_longitude = 0.0;

        double current_latitude = coordinates_list.get(coordinates_index).getLatitude();
        double current_longitude = coordinates_list.get(coordinates_index).getLongitude();


        boolean destination_check = false;
        boolean direction_check = true;
        boolean next_check = true;
        boolean check50 = false;
        boolean check30 = false;
        boolean check10 = false;

        double addValue = 0.0;
        double coordi_distance = 0.0;       // [ 다음 coordinates_list 까지의 거리 ]
        double info_distance = 0.0;         // [ 다음 info_list 까지의 거리 ]
        double remain_disatnce = 0.0;
        double gap_latitude = 0.0;
        double gap_longitude = 0.0;

        while(true){
            if(info_list.get(info_index).getTurnType() == 0 ) info_index++;

            if(info_list.size() <= info_index || coordinates_list.size() <= coordinates_index){
                Log.i(TAG,"============================== [ SImulation ] ==============================");
                Log.i(TAG, "Information Index 값은 " + info_index + " 입니다.");
                Log.i(TAG, "Information List의 Size 값은 " + info_list.size()+ " 입니다.");
                Log.i(TAG, "Coordinates Index 값은 " + coordinates_index + " 입니다.");
                Log.i(TAG, "Coordinates List의 Size 값은 " + coordinates_list.size()+ " 입니다.");
                finish();
                break;
            }

            if(info_list.get(info_index).getTurnType() == 200 ){
                // [ 출발지 ]
                Log.i(TAG,"============================== [ SImulation ] ==============================");
                Log.i(TAG, "모의주행을 시작합니다.");
                img_direction.setImageResource(R.drawable.direction_11);
                info_index++;
                coordinates_index += 1;
                continue;
            }   // [ if(출발지) ]

            if(info_list.get(info_index).getTurnType() == 201){
                // [ 도착지 ]
                // [ destination_check : 도착지 10m 이내인 경우 true ]
                Log.i(TAG, "============================== [ SImulation ] ==============================");
                Log.i(TAG, "도착지 10m 이내입니다. 모의주행을 종료합니다.");
                destination_check = true;
                break;
            }   // [ if(도착지) ]

            if(direction_check) {
                // [ 10m 이내라서 다음 목표지점을 찾아야 한다. Coordinates 값이 아닌 Information 값 ]
                destination_latitude = info_list.get(info_index).getLatitude();
                destination_longitude = info_list.get(info_index).getLongitude();
                Log.i(TAG, "[ Information TurnType / Information_index ] : " + info_list.get(info_index).getTurnType() + " / " + info_index);

                direction_check = false;            //
                check10 = true;
                check30 = true;
                check50 = true;
                Log.i(TAG,"============================== [ SImulation ] ==============================");
                Log.i(TAG, "현재의 Infomation Index 값은 : " + info_index + " 입니다.");
                Log.i(TAG, "다음 목표지점의 경도와 위도 정보입니다. Latitude / Longitude : " + des_latitude + " / " + des_longitude);

                info_distance = exception.calDistance(current_latitude, current_longitude, des_latitude, des_longitude);
                Log.i(TAG, "현재 좌표값과 다음 Information까지의 거리는 : " + info_distance + " 입니다.");
                Log.i(TAG, "현재의 TurnType 값은 " + info_list.get(info_index).getTurnType() + " 입니다.");
                Log.i(TAG, info_list.get(info_index).getDescription());

            }   // [ if(direction_check) ]

            // [ 1. 현재와 다음 coordinates_list[i] 까지의 거리를 구한다. ]
            // [ 2. 차이만큼 위도+= 경도+= 해준다. ]
            if( next_check ) {
                next_latitude = coordinates_list.get(coordinates_index).getLatitude();
                next_longitude = coordinates_list.get(coordinates_index).getLongitude();
                addValue = exception.calDistance(current_latitude, current_longitude, next_latitude, next_longitude);
                gap_latitude = (next_latitude - current_latitude) / addValue;
                gap_longitude = (next_longitude - current_longitude) / addValue;
                next_check = false;     // [ 다음 coordinates 까지와의 거리가 2m 이내일때까지는 이 조건문에 들어올 필요가 없다. ]
                Log.i(TAG,"============================== [ SImulation ] ==============================");
                Log.i(TAG, "Coordinates 값을 변경합니다. ");
                Log.i(TAG, "Coordinates 까지의 거리는 " + addValue + "m 입니다.");
            }   // [ if(next_check) ]

            // [ Thread로 변경 부분 ]
                    while(true) {
                        current_latitude += gap_latitude;
                        current_longitude += gap_longitude;
                        coordi_distance = exception.calDistance(current_latitude, current_longitude, next_latitude, next_longitude);
                        info_distance = exception.calDistance(current_latitude, current_longitude, des_latitude, des_longitude);
//            Log.i(TAG,"============================== [ SImulation ] ==============================");
                        Log.i(TAG, "현재 좌표값과 다음 Coordinates까지의 거리는 : " + coordi_distance + " 입니다.");
                        // [ 현재의 경도 위도에 += gap 경도 위도 Thread 돌리기 ]
                        // [ 5m 이내로 되면 coordinates_index++ ]
                        // [ 다음 coordinates[i] 의 경도 위도 == destination 경도 위도 라면, turnType 변경 및 남은거리 변경 ]

                        if (coordi_distance < 2) {
                            Log.i(TAG, "============================== [ SImulation ] ==============================");
                            Log.i(TAG, "다음 Coordinates까지의 거리가 2m 이내입니다. ");
                            Log.i(TAG, "[ 현재 Information index 값은 : " + info_index + " 입니다. ]");
                            Log.i(TAG, "[ 현재 Information TurnType 값은 : " + info_list.get(info_index).getTurnType() + " 입니다. ]");
                            Log.i(TAG, "[ Next Latitude & Longitude ] : " + next_latitude + " / " + next_longitude);
                            Log.i(TAG, "[ Dest Latitude & Longitude ] : " + des_latitude + " / " + des_longitude);

                            if (next_latitude == destination_latitude && next_longitude == destination_longitude) {
                                Log.i(TAG, "[ 방향전환을 하겠습니다. ]");
                                info_index++;
                                coordinates_index++;
                                direction_check = true; // [ 방향전환 후, 다음 next_latitude와 next_longitude를 구하기 위해서. ]
                            } else {
                                Log.i(TAG, "[ Coordinates List 값만 추가하겠습니다. ]");
                                coordinates_index++;
                            }
                            next_check = true;      // [ 현재의 위치가 다음 coordinates 와의 거리가 2m이내로, 그 다음 coordinates를 쫓으면 된다. ]
                            break;
                        }   // [ if(distance < 5) ]


                        if (info_distance <= 50 && info_distance > 30 && check50) {
                            Log.i(TAG, "============================== [ SImulation ] ==============================");
                            Log.i(TAG, "목표지점 까지의 거리가 30-50m 사이입니다. ");
                            check50 = false;    // [ 한번만 출력하기 위해서이다. ]
                        } else if (info_distance <= 30 && info_distance > 10 && check30) {
                            Log.i(TAG, "============================== [ SImulation ] ==============================");
                            Log.i(TAG, "목표지점 까지의 거리가 10-30m 사이입니다. ");
                            check30 = false;    // [ 한번만 출력하기 위해서이다. ]
                        } else if (info_distance <= 10 && check10) {
                            Log.i(TAG, "============================== [ SImulation ] ==============================");
                            Log.i(TAG, "목표지점 까지의 거리가 10m 이내입니다. ");
                            check10 = false;    // [ 한번만 출력하기 위해서이다. ]
                        }
                    }   // [ while(true) -- gap += lat & lon ]
        }   // [ while(true) ]
    }   // [ End Simulation ]

    // [ 네비 정보 설정 ]
    public void setTextInfo(){

        int total_distance = 1334;
        int total_time = 215;
        int turnType = 200;
        int distance = 0;

        String arrival_time = exception.strArrival_time(total_time);
        String strRemain = exception.strRemainDistance(total_distance, total_distance);

        img_direction.setImageResource(R.drawable.direction_11);                       // 방향 이미지 ( 좌, 우, 유턴 )
        tv_distance = (TextView) findViewById(R.id.tv_distance);                            // 다음 안내지점까지의 거리
        tv_remain_distance.setText(strRemain);                // 총 남은 거리
        tv_arriaval_time.setText(arrival_time);                 // 도착 예상시간
    }   // [ End Set Text Info ]

    // 길 안내 시작
    public void getPathInfo(TMapPoint startPoint, TMapPoint endPoint) {
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
                        Log.i(TAG, "==================== [ GuidActivity Car Path " + i + " ]====================");

                        if (i == 0) {
                            String strDistance = exception.strDistance(response.body().getFeatures().get(i).getProperties().getTotalDistance());
                            tv_distance.setText(strDistance);

                            Log.i(TAG, "[ Properties ] Total Distance : " + response.body().getFeatures().get(i).getProperties().getTotalDistance());
                            Log.i(TAG, "[ Properties ] Total Time : " + response.body().getFeatures().get(i).getProperties().getTotalTime());
                            Log.i(TAG, "[ Properties ] Total Fare : " + response.body().getFeatures().get(i).getProperties().getTotalFare());
                            Log.i(TAG, "[ Properties ] Total TaxiFare : " + response.body().getFeatures().get(i).getProperties().getTaxiFare());
                            Log.i(TAG, "[ Properties ] Distance : " + response.body().getFeatures().get(i).getProperties().getDistance());
                            Log.i(TAG, "[ Properties ] Description : " + response.body().getFeatures().get(i).getProperties().getDescription());
                            Log.i(TAG, "[ Properties ] TurnType : " + response.body().getFeatures().get(i).getProperties().getTurnType());
                            Log.i(TAG, "[ Properties ] Index : " + response.body().getFeatures().get(i).getProperties().getIndex());

                        } else {
                            Log.i(TAG, "[ Properties ] Distance : " + response.body().getFeatures().get(i).getProperties().getDistance());
                            Log.i(TAG, "[ Properties ] Description : " + response.body().getFeatures().get(i).getProperties().getDescription());
                            Log.i(TAG, "[ Properties ] TurnType : " + response.body().getFeatures().get(i).getProperties().getTurnType());
                            Log.i(TAG, "[ Properties ] Index : " + response.body().getFeatures().get(i).getProperties().getIndex());

                        }   // if
                    }   // for
                }   // if(response.isSuccessful())
            }   // onResponse

            @Override
            public void onFailure(Call<TmapDataVO> call, Throwable t) {

            }
        }); // call.enqueue
    }   // getPathInfo
}
