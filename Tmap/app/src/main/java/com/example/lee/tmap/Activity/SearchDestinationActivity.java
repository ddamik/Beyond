package com.example.lee.tmap.Activity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;

import com.example.lee.tmap.Adapter.ListViewAdapter;
import com.example.lee.tmap.POIItem;
import com.example.lee.tmap.R;
import com.example.lee.tmap.UserException;
import com.example.lee.tmap.ValueObject.ArrivalDataVO;
import com.skp.Tmap.TMapData;
import com.skp.Tmap.TMapGpsManager;
import com.skp.Tmap.TMapPOIItem;
import com.skp.Tmap.TMapPoint;
import com.skp.Tmap.TMapTapi;

import java.util.ArrayList;

import android.app.Activity;
import android.widget.ListView;
import android.widget.Toast;

public class SearchDestinationActivity extends Activity implements TMapGpsManager.onLocationChangedCallback{

    public static final String TAG = "SearchDestiActivity";
    public static final String APP_KEY = "483f055b-19f2-3a22-a3fb-935bc1684b0b";
    private Button btn_back, btn_searchDest, btn_viewmap;

    public static double des_longitude = 0.0;
    public static double des_latitude = 0.0;
    public static String des_name = "";


    /*
        POI Search
     */
    private EditText edit_destination;
    private TMapData tMapData = null;
    public static TMapPOIItem tMapPOIItem = null;
    public static final int radius = 30;
    public static final int SearchCount = 30;

    // [ POI 거리 위해 현재 위치를 MainActivity 에서 가져온다 ]
    public static double current_longitude = 0.0;
    public static double current_latitude = 0.0;
    TMapPoint currentPoint;

    /*
        ListView
     */
    private ListViewAdapter adapter;
    private ListView listview_destination;
    boolean autoCheck = false;


    /*==========
        1. POI 명칭 길이때문
     ==========*/
    public UserException exception;


    /*==========
        progress dialog // loading
     ==========*/
    private Handler mHandler;
    private ProgressDialog mProgressDialog;


    /*=========
        현재 위치를 항상 최신화 하기 위해서 --> 따로빼서 관리를 할 수 없을까?
        GPS
     */
    private TMapGpsManager tMapGpsManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_destination);

        // GPS
        tMapGpsManager = new TMapGpsManager(this);
        tMapGpsManager.setProvider(TMapGpsManager.NETWORK_PROVIDER);
        tMapGpsManager.OpenGps();


        // progress dialog
        mHandler = new Handler();

        // 데이터가공
        exception = new UserException();

        // POIItem 거리위함
        current_latitude = UserException.STATIC_CURRENT_LATITUDE;
        current_longitude = UserException.STATIC_CURRENT_LONGITUDE;
        currentPoint = new TMapPoint(current_latitude, current_longitude);
        /*
            통합검색 POI데이터
         */
        TMapTapi tmaptapi = new TMapTapi(this);
        tmaptapi.setSKPMapAuthentication(APP_KEY);
        tMapData = new TMapData();

        edit_destination = (EditText) findViewById(R.id.edit_destination);


        // Listview
        listview_destination = (ListView) findViewById(R.id.listview_destination);
        listview_destination.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                POIItem item = (POIItem) parent.getItemAtPosition(position);
                /*==========
                    if(autoCheck) : 명칭이름을 쓰는도중에 명칭이름이 나온 경우이다.
                    따라서, 명칭이름을 클릭하여 ListView를 새롭게 Update 해줘야하기 떄문에,
                    클릭한 명칭이름을 selectDestination으로 보내어, 해당 명칭을 검색했을때 나오는 명칭들로 ListView를 Update

                    else : 명칭이름을 사용자가 모두 입력했을 경우이다.
                    따라서, 클릭했을때에는, 명칭에서 위도, 경도가 포함되어있기때문에,
                    위도와 경도를 다음 액티비티로 전달해주면 된다.
                 */
                if (autoCheck) selectDestination(item.getmName().toString());
                else {
                    des_longitude = item.getPoint().getLongitude();
                    des_latitude = item.getPoint().getLatitude();
                    des_name = item.getmName().toString();

                    onDestinationClick(des_name, des_longitude, des_latitude);
                }   // eise - if( autoCheck )
            }   // onItemClick
        });


        /*
            AutoComplete
         */
        edit_destination = (EditText) findViewById(R.id.edit_destination);
        edit_destination.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                adapter = new ListViewAdapter();
                listview_destination.setAdapter(adapter);

                String search = edit_destination.getText().toString();
                tMapData.autoComplete(search, new TMapData.AutoCompleteListenerCallback() {
                    @Override
                    public void onAutoComplete(ArrayList<String> auto_list) {
                        for (int i = 0; i < auto_list.size(); i++) {
                            String name = auto_list.get(i);
                            adapter.autoListAddItem(name);
                        }   // for

                         /*
                            UI 변환
                         */
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        autoCheck = true;
                                        adapter.notifyDataSetChanged();
                                        // 해당 작업을 처리함
                                    }
                                });
                            }
                        }).start();
                    }   // onAutoComplete
                }); // tMapData.autoComplete
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });



        /*
            Button
         */
        // 목적지 검색
        btn_searchDest = (Button) findViewById(R.id.btn_searchDest);
        btn_searchDest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String search = edit_destination.getText().toString();
                selectDestination(search);
                                         /*
                            UI 변환
                         */
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                autoCheck = true;
                                adapter.notifyDataSetChanged();
                                // 해당 작업을 처리함
                            }
                        });
                    }
                }).start();

            }   // [ End onClick ]
        });

        // 경로안내 지도보기
        btn_viewmap = (Button) findViewById(R.id.btn_viewmap);
        btn_viewmap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tMapGpsManager.CloseGps();
                startActivity(new Intent(SearchDestinationActivity.this, PathInfoActivity.class));
                overridePendingTransition(R.anim.anim_slide_fade_in, R.anim.anim_slide_out_left);
            }
        });

        // 뒤로가기 버튼
        btn_back = (Button) findViewById(R.id.btn_back);
        btn_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tMapGpsManager.CloseGps();
                startActivity(new Intent(SearchDestinationActivity.this, MainActivity.class));                     // 우측으로 사라지기
                overridePendingTransition(R.anim.anim_slide_in_left, R.anim.anim_slide_out_right);
                finish();
            }
        });
    }   // onCreate


    public void selectDestination(String searchName) {
        String destination = "";
        if (autoCheck) destination = searchName;
        else destination = edit_destination.getText().toString();

        Log.i("SearchActivity", "destination : " + destination);

        /*
            새로 검색했을때, adapter를 새로 만들어서 listview를 초기화
         */
        adapter = new ListViewAdapter();
        listview_destination.setAdapter(adapter);

        /*
            명칭별 데이터
         */
        tMapData.findAddressPOI(destination, new TMapData.FindAddressPOIListenerCallback() {
            @Override
            public void onFindAddressPOI(ArrayList<TMapPOIItem> poiItem) {
                for (int i = 0; i < poiItem.size(); i++) {
                    tMapPOIItem = poiItem.get(i);

                    String name = exception.strPOIName(tMapPOIItem.getPOIName().toString());                      // POI 이름 가져오기
                    String address = tMapPOIItem.getPOIAddress().replace("null", "");       // POI 주소 가져오기

                    Log.i(TAG, "[ POI Distance ] : " + poiItem.get(i).getDistance(currentPoint));
                    TMapPoint point = tMapPOIItem.getPOIPoint();                            // POI의 Point
                    adapter.addItem(name, address, point);                                  // 리스트에 뿌려줄 수 있도록 어댑터에 추가
                }   // for
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                autoCheck = false;
                                adapter.notifyDataSetChanged();
                                // 해당 작업을 처리함
                            }
                        });
                    }
                }).start();
            }   // onFindAroundNamePOI
        }); // findAroundNamePOI
    }   // selectDestination


    private class ProgressAsync extends AsyncTask<Void, Void, Void> {

        ProgressDialog progressDialog = new ProgressDialog(SearchDestinationActivity.this);

        @Override
        protected void onPreExecute() {
            Log.i(TAG, "[ On Pre Execute ]");
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progressDialog.setMessage("로딩중입니다.");
            progressDialog.show();
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... params) {
            Log.i(TAG, "[ Do In Background ] ");
            while(true){
                if( UserException.STATIC_CURRENT_LONGITUDE == 0 || UserException.STATIC_CURRENT_LATITUDE == 0 ) {
                    continue;
                }
                else {
                    UserException.STATIC_CURRENT_GPS_CHECK = true;
                    if(UserException.STATIC_CURRENT_GPS_CHECK){
                        Intent intent = new Intent(SearchDestinationActivity.this, PathInfoActivity.class);
                        tMapGpsManager.CloseGps();
                        startActivity(intent);
                        overridePendingTransition(R.anim.anim_slide_fade_in, R.anim.anim_slide_out_left);
//                        Toast.makeText(getApplicationContext(), "페이지 전환", Toast.LENGTH_LONG).show();
                    }else{
                        Toast.makeText(getApplicationContext(), "GPS를 활성화 해주세요.", Toast.LENGTH_LONG).show();
                    }
                    break;
                }
            }
            return null;
        }   // doInBackground

        @Override
        protected void onPostExecute(Void aVoid) {
            Log.i(TAG, "[ On Post Execute ] ");
            progressDialog.dismiss();
            super.onPostExecute(aVoid);
        }   // onPostExecute
    }   // ProgressAsync

    public void onDestinationClick(String arrival_name, double arrival_longitude, double arrival_latitude ){

        Log.i(TAG, "[ On Destination Click ] ");
        ProgressAsync progressAsync = new ProgressAsync();
        progressAsync.execute();
        Log.i(TAG, "[ After Execute ]");


    }   // onDestinationClick

    @Override
    public void onLocationChange(Location location) {

        UserException.STATIC_CURRENT_LATITUDE = location.getLatitude();
        UserException.STATIC_CURRENT_LONGITUDE = location.getLongitude();

        Log.i(TAG, "[ SearchDestinationActivity On Location Change ]");
    }   // onLocationChange
}
