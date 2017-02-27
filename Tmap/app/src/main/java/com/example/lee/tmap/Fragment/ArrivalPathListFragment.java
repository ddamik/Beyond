package com.example.lee.tmap.Fragment;

import android.app.ProgressDialog;
import android.content.Intent;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.example.lee.tmap.Activity.PathInfoActivity;
import com.example.lee.tmap.Adapter.ListViewAdapter;
import com.example.lee.tmap.ApiService;
import com.example.lee.tmap.POIItem;
import com.example.lee.tmap.R;
import com.example.lee.tmap.UserException;
import com.example.lee.tmap.View.ClearEditText;
import com.skp.Tmap.TMapData;
import com.skp.Tmap.TMapGpsManager;
import com.skp.Tmap.TMapPOIItem;
import com.skp.Tmap.TMapPoint;
import com.skp.Tmap.TMapTapi;

import java.util.ArrayList;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.GsonConverterFactory;
import retrofit2.Response;
import retrofit2.Retrofit;

/**
 * Created by Nam on 2016. 12. 6..
 */
public class ArrivalPathListFragment extends Fragment implements TMapGpsManager.onLocationChangedCallback {
    private final static String TAG = ArrivalPathListFragment.class.getSimpleName();
    public static final String APP_KEY = "483f055b-19f2-3a22-a3fb-935bc1684b0b";

    public static double cur_longitude = 0.0;
    public static double cur_latitude = 0.0;

    private Toolbar toolbar;
    private ClearEditText et_destination;
    private LinearLayout layout_search;

    public static double des_longitude = 0.0;
    public static double des_latitude = 0.0;
    public static String des_name = "";

    /*
        POI Search
     */
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

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView() is called.");
        setHasOptionsMenu(true);
        return inflater.inflate(R.layout.fragment_arrival_path_list, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        Log.i(TAG, "onViewCreated() is called.");
        super.onViewCreated(view, savedInstanceState);

        toolbar = (Toolbar) getActivity().findViewById(R.id.toolbar);
        et_destination = (ClearEditText) toolbar.findViewById(R.id.et_destination);
        listview_destination = (ListView) getView().findViewById(R.id.listview_destination);

    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        Log.i(TAG, "onActivityCreated() is called.");
        super.onActivityCreated(savedInstanceState);

        listview_destination.invalidate();
        et_destination.addTextChangedListener(destinationTextWatcher);
        listview_destination.setOnItemClickListener(destinationListViewOnItemClickListener);

        // GPS
        tMapGpsManager = new TMapGpsManager(getActivity());
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
        TMapTapi tmaptapi = new TMapTapi(getActivity());
        tmaptapi.setSKPMapAuthentication(APP_KEY);
        tMapData = new TMapData();



        /*
        // 뒤로가기 버튼
        btn_back = (Button) getView().findViewById(R.id.btn_back);
        btn_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tMapGpsManager.CloseGps();
                HomeFragment fragment = new HomeFragment();
                getActivity().getSupportFragmentManager().beginTransaction().replace(R.id.fragment, fragment).commit();
            }
        });
        */
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_arrival_path_list, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    public void selectDestination(String searchName) {
        String destination = "";
        if (autoCheck) destination = searchName;
        else destination = et_destination.getText().toString();

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


                TextWatchAsync textWatchAsync = new TextWatchAsync();
                textWatchAsync.execute(false);

            }   // onFindAroundNamePOI
        }); // findAroundNamePOI
    }   // selectDestination


    private class ProgressAsync extends AsyncTask<Void, Void, Void> {

        ProgressDialog progressDialog = new ProgressDialog(getActivity());

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
            while (true) {
                if (UserException.STATIC_CURRENT_LONGITUDE == 0 || UserException.STATIC_CURRENT_LATITUDE == 0) {
                    continue;
                } else {
                    UserException.STATIC_CURRENT_GPS_CHECK = true;
                    if (UserException.STATIC_CURRENT_GPS_CHECK) {

                        Intent intent = new Intent(getActivity(), PathInfoActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        intent.putExtra("des_longitude", des_longitude);
                        intent.putExtra("des_latitude", des_latitude);
                        intent.putExtra("arrival_name", des_name);
                        startActivity(intent);
                        getActivity().overridePendingTransition(R.anim.anim_slide_fade_in, R.anim.anim_slide_out_left);
                    } else {
                        Toast.makeText(getActivity(), "GPS를 활성화 해주세요.", Toast.LENGTH_LONG).show();
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

    public void onDestinationClick(String arrival_name, double arrival_longitude, double arrival_latitude) {

        long searchDate = System.currentTimeMillis();
        String userUUID = "AAF3:0000:0000:0000";
        String arrivalName = arrival_name;
        String strSearchDate = String.valueOf(searchDate);
        String strArrivalLongitude = String.valueOf(arrival_longitude);
        String strArrivalLatitude = String.valueOf(arrival_latitude);

        // [ DB 저장 ]
        Retrofit client = new Retrofit.Builder().baseUrl(ApiService.SERVER_URL).addConverterFactory(GsonConverterFactory.create()).build();
        ApiService apiService = client.create(ApiService.class);
        Call<ResponseBody> call = apiService.saveRoute(userUUID, arrivalName, strSearchDate, strArrivalLongitude, strArrivalLatitude);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    ProgressAsync progressAsync = new ProgressAsync();
                    progressAsync.execute();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {

            }
        }); // [ End Retrofit( Save Route ) ]

    }   // onDestinationClick


    @Override
    public void onLocationChange(Location location) {
        UserException.STATIC_CURRENT_LATITUDE = location.getLatitude();
        UserException.STATIC_CURRENT_LONGITUDE = location.getLongitude();

        Log.i(TAG, "[ SearchDestinationActivity On Location Change ]");
    }

    public void removeDestinationTextChangedListener(){
        et_destination.setText("");
        et_destination.clearFocus();
        et_destination.removeTextChangedListener(destinationTextWatcher);
    }


    private AdapterView.OnItemClickListener destinationListViewOnItemClickListener = new AdapterView.OnItemClickListener() {
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
    };


    private TextWatcher destinationTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            adapter = new ListViewAdapter();
            listview_destination.setAdapter(adapter);

            String search = et_destination.getText().toString();
            tMapData.autoComplete(search, new TMapData.AutoCompleteListenerCallback() {
                @Override
                public void onAutoComplete(ArrayList<String> auto_list) {
                    for (int i = 0; i < auto_list.size(); i++) {
                        String name = auto_list.get(i);
                        adapter.autoListAddItem(name);
                    }   // for


                    //   UI 변환

                    TextWatchAsync textWatchAsync = new TextWatchAsync();
                    textWatchAsync.execute(true);

                }   // onAutoComplete
            }); // tMapData.autoComplete
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    };


    private class TextWatchAsync extends AsyncTask<Boolean, Boolean, Boolean> {


        @Override
        protected void onPreExecute() {
            Log.i(TAG, "[ On Pre Execute ]");
            super.onPreExecute();
        }

        @Override
        protected Boolean doInBackground(Boolean... params) {
            return params[0];
        }   // doInBackground

        @Override
        protected void onPostExecute(Boolean aVoid) {
            Log.i(TAG, "[ On Post Execute ] ");
            super.onPostExecute(aVoid);
            autoCheck = aVoid;
            adapter.notifyDataSetChanged();
        }   // onPostExecute
    }   // ProgressAsync


    @Override
    public void onStart() {
        Log.i(TAG, "onStart() is called.");
        super.onStart();
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy() is called.");
        super.onDestroy();
        tMapGpsManager.CloseGps();
    }

    @Override
    public void onStop() {
        Log.i(TAG, "onStop() is called.");
        super.onStop();
    }

    @Override
    public void onPause() {
        Log.i(TAG, "onPause() is called.");
        super.onPause();
    }

    @Override
    public void onResume() {
        Log.i(TAG, "onResume() is called.");
        super.onResume();
    }


}
