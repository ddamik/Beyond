package com.example.lee.tmap.Activity;


import android.content.Context;
import android.content.Intent;
import android.app.Activity;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
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
import com.example.lee.tmap.ValueObject.RecentPathVO;
import com.skp.Tmap.TMapGpsManager;

import java.util.ArrayList;
import java.util.UUID;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.GsonConverterFactory;
import retrofit2.Response;
import retrofit2.Retrofit;


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
    private ArrayList<String>  destination_list;
    String[] recentPathList;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // UUID & Recent Path List
        String strUUID = this.GetDevicesUUID(this);
        recentPathList = new String[9];

        Retrofit client = new Retrofit.Builder().baseUrl(ApiService.SERVER_URL).addConverterFactory(GsonConverterFactory.create()).build();
        ApiService apiService = client.create(ApiService.class);
        // Call<RecentPathVO> call = apiService.getRecentPath(strUUID);
        Call<RecentPathVO> call = apiService.getRecentPath();
        call.enqueue(new Callback<RecentPathVO>() {
            @Override
            public void onResponse(Call<RecentPathVO> call, Response<RecentPathVO> response) {
                if(response.isSuccessful()) {
                    Log.i(TAG, "[ On Response ] ");
                    int length = response.body().getResult().size();
                    for(int i=0; i<length ; i++){

                        recentPathList[i] = String.valueOf(response.body().getResult().get(i).getARRIVAL_NAME());
                        Log.i(TAG, "[ Response Body Arrival Name ]["+i+"] & RecentPath : " + response.body().getResult().get(i).getARRIVAL_NAME() + " / " + recentPathList[i]);
                    }   // for

                    /*==========
                        GridView [ recentPathList에 값을 넣기전에 GridView Adapter로 null 값을 보내버리기 때문에 여기에 위치함 ]
                    ==========*/
                    gridView = (GridView) findViewById(R.id.gridview);
                    gridView.setAdapter(new GridViewAdapter(MainActivity.this, recentPathList));
                    gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                            Toast.makeText(getApplicationContext(), recentPathList[position], Toast.LENGTH_LONG).show();
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

        /*
            GPS
         */
        tMapGpsManager = new TMapGpsManager(this);
        tMapGpsManager.setProvider(TMapGpsManager.NETWORK_PROVIDER);
        tMapGpsManager.OpenGps();


    }   // onCreate

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

}
