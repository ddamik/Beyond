package com.example.lee.tmap.Activity;

import android.content.Intent;
import android.location.Location;
import android.nfc.Tag;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;

import com.example.lee.tmap.ListViewAdapter;
import com.example.lee.tmap.POIItem;
import com.example.lee.tmap.R;
import com.skp.Tmap.TMapData;
import com.skp.Tmap.TMapGpsManager;
import com.skp.Tmap.TMapPOIItem;
import com.skp.Tmap.TMapPoint;
import com.skp.Tmap.TMapTapi;

import java.text.DecimalFormat;
import java.util.ArrayList;

import android.app.Activity;
import android.widget.ListView;
import android.widget.Toast;

public class SearchDestinationActivity extends Activity {

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

    /*
        ListView
     */
    private ListViewAdapter adapter;
    private ListView listview_destination;
    boolean autoCheck = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_destination);

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
                if (autoCheck) selectDestination(item.getmName().toString());
                else {
                    Toast.makeText(getApplicationContext(), "페이지 전환", Toast.LENGTH_LONG).show();
                    des_longitude = item.getPoint().getLongitude();
                    des_latitude = item.getPoint().getLatitude();
                    des_name = item.getmName().toString();

                    startActivity(new Intent(SearchDestinationActivity.this, NavigationDisplayActivity.class));
                    overridePendingTransition(R.anim.anim_slide_fade_in, R.anim.anim_slide_out_left);
                }
            }
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
                selectDestination("");
            }
        });

        // 경로안내 지도보기
        btn_viewmap = (Button) findViewById(R.id.btn_viewmap);
        btn_viewmap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Intent navigation =
                startActivity(new Intent(SearchDestinationActivity.this, NavigationDisplayActivity.class));
                overridePendingTransition(R.anim.anim_slide_fade_in, R.anim.anim_slide_out_left);
            }
        });

        // 뒤로가기 버튼
        btn_back = (Button) findViewById(R.id.btn_back);
        btn_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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

                    String name = tMapPOIItem.getPOIName().toString();                      // POI 이름 가져오기
                    String address = tMapPOIItem.getPOIAddress().replace("null", "");       // POI 주소 가져오기

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
}
