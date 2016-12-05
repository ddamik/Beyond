package com.example.lee.tmap.Activity;


import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.app.Activity;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.lee.tmap.Adapter.GridViewAdapter;
import com.example.lee.tmap.ApiService;
import com.example.lee.tmap.R;
import com.example.lee.tmap.UserException;
import com.example.lee.tmap.ValueObject.RecentPathListVO;
import com.example.lee.tmap.ValueObject.RecentPathVO;
import com.skp.Tmap.TMapGpsManager;

import java.util.ArrayList;
import java.util.UUID;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.GsonConverterFactory;
import retrofit2.Response;
import retrofit2.Retrofit;

import static com.example.lee.tmap.ValueObject.RecentPathVO.*;


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
    private ArrayList<RecentPathListVO> destinationList;
    String[] recentPathList;
    private double destination_longitude = 0.0;
    private double destination_latitude = 0.0;
    private String strArrivalName = "";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // UUID & Recent Path List
        String strUUID = this.GetDevicesUUID(this);
        Log.i(TAG, "[ String UUID ] : " + strUUID);
        recentPathList = new String[9];
        destinationList = new ArrayList<>();

        initRecentPath();
        initInternetAndButton();

        /*
            GPS
         */
        tMapGpsManager = new TMapGpsManager(this);
        tMapGpsManager.setProvider(TMapGpsManager.NETWORK_PROVIDER);
        tMapGpsManager.OpenGps();

    }   // onCreate

    public void initRecentPath(){
        // [ 최근에 검색한 경로 9개 ]
        Retrofit client = new Retrofit.Builder().baseUrl(ApiService.SERVER_URL).addConverterFactory(GsonConverterFactory.create()).build();
        ApiService apiService = client.create(ApiService.class);
        Call<RecentPathVO> call = apiService.getRecentPath();
        call.enqueue(new Callback<RecentPathVO>() {
            @Override
            public void onResponse(Call<RecentPathVO> call, Response<RecentPathVO> response) {
                if(response.isSuccessful()) {
                    RecentPathListVO vo;
                    String arrivalName = "";
                    String strLatitude = "";
                    String strLongitude = "";
                    double longitudeValue = 0.0;
                    double latitudeValue = 0.0;

                    int length = response.body().getResult().size();
                    for(int i=0; i<length ; i++){
                        arrivalName = response.body().getResult().get(i).getArrivalName();
                        strLongitude = response.body().getResult().get(i).getLatitudeValue();
                        Log.i(TAG, "[ Longitude ] = " + strLongitude);
                        longitudeValue = Double.parseDouble(strLongitude);
                        strLatitude = response.body().getResult().get(i).getLongitudeValue();
                        Log.i(TAG, "[ Latitude ] = " + strLatitude);
                        latitudeValue = Double.parseDouble(strLatitude);


                        recentPathList[i] = arrivalName;
                        vo = new RecentPathListVO(arrivalName, latitudeValue, longitudeValue);
                        destinationList.add(vo);
                    }   // for

                    /*==========
                        GridView [ recentPathList에 값을 넣기전에 GridView Adapter로 null 값을 보내버리기 때문에 여기에 위치함 ]
                    ==========*/
                    gridView = (GridView) findViewById(R.id.gridview);
                    gridView.setAdapter(new GridViewAdapter(MainActivity.this, recentPathList));
                    gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                            recentPathClicked(recentPathList[position]);
                        }
                    });
                }   // respsone.isSuccessful
            }   // onResponse

            @Override
            public void onFailure(Call<RecentPathVO> call, Throwable t) {
                Log.i(TAG, "[ on Failure ] ");
                Log.i(TAG, "[ Throwable ] : " + t.toString());
                Log.i(TAG, "[ RecentPathVO Call ] : " + call);
            }
        });
    }

    public void initInternetAndButton(){

        // 인터넷 연결 체크
        ConnectivityManager manager = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo mobile = manager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        final NetworkInfo wifi = manager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        /*
            Search
         */
        btn_searchDest = (Button) findViewById(R.id.btn_searchDest);
        btn_searchDest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // wifi 또는 모바일 네트워크 하나라도 연결이 되어있다면,
                if (wifi.isConnected() || mobile.isConnected()) {
                    Log.i(TAG, "[ 인터넷 연결이 완료됨. ] ");
                    tMapGpsManager.CloseGps();
                    startActivity(new Intent(MainActivity.this, SearchDestinationActivity.class));                     // 좌측으로 사라지기
                    overridePendingTransition(R.anim.anim_slide_in_right, R.anim.anim_slide_out_left);      // 새로운 Activity Animation / 현재의 Activity Animation
                } else {
                    Log.i(TAG, "[ 인터넷 연결이 필요함. ] ");
                    Toast.makeText(getApplicationContext(), "[ 인터넷 연결이 필요합니다. ] ", Toast.LENGTH_LONG).show();
                }
            }
        }); // btn_searchDest
    }   // [ End InitInternetAndButton ]


    @Override
    public void onLocationChange(Location location) {
        cur_longitude = location.getLongitude();
        cur_latitude = location.getLatitude();

        UserException.STATIC_CURRENT_LATITUDE = cur_latitude;
        UserException.STATIC_CURRENT_LONGITUDE = cur_longitude;

        Log.i(TAG, "[ MainActivity On Location Change ]");
        Log.i(TAG, "onLocationChange" + cur_longitude + " / " + cur_latitude);
    }   // onLocationChange

    // UUID
    private String GetDevicesUUID(Context mContext){
        final TelephonyManager tm = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        final String tmDevice, tmSerial, androidId;
        tmDevice = "" + tm.getDeviceId();
        tmSerial = "" + tm.getSimSerialNumber();
        androidId = "" + android.provider.Settings.Secure.getString(getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
        UUID deviceUuid = new UUID(androidId.hashCode(), ((long)tmDevice.hashCode() << 32) | tmSerial.hashCode());
        String deviceId = deviceUuid.toString();
        return deviceId;
    }

    // [ 최근 경로를 클릭했을 경우, PathInfoActivitiy로 전환 ]
    public void recentPathClicked(String arrival_name){

        strArrivalName = arrival_name;
        for(int i=0; i<destinationList.size(); i++){
            if(strArrivalName.equals(destinationList.get(i).getArrivalName())){
                destination_longitude = destinationList.get(i).getLongitudeValue();
                destination_latitude = destinationList.get(i).getLatitudeValue();
                break;
            }
        }

        ProgressAsync progressAsync = new ProgressAsync();
        progressAsync.execute();
    }   // [ End recentPathClicked ]

    private class ProgressAsync extends AsyncTask<Void, Void, Void> {

        ProgressDialog progressDialog = new ProgressDialog(MainActivity.this);

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
                        Log.i(TAG, "[ ArrivalName ]: " + strArrivalName);
                        Log.i(TAG, "[ Destination Longitude ]: " + destination_longitude);
                        Log.i(TAG, "[ Destination Latitude ]: " + destination_latitude);

                        Intent intent = new Intent(MainActivity.this, PathInfoActivity.class);
                        intent.putExtra("arrival_name", strArrivalName);
                        intent.putExtra("des_longitude", destination_longitude);
                        intent.putExtra("des_latitude", destination_latitude);
                        startActivity(intent);
                        overridePendingTransition(R.anim.anim_slide_fade_in, R.anim.anim_slide_out_left);
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

    //
    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        initRecentPath();
        initInternetAndButton();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // GPS중단
        tMapGpsManager.CloseGps();
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
}
