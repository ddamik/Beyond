package com.example.lee.tmap.Fragment;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.lee.tmap.Activity.PathInfoActivity;
import com.example.lee.tmap.Adapter.GridViewAdapter;
import com.example.lee.tmap.ApiService;
import com.example.lee.tmap.R;
import com.example.lee.tmap.UserException;
import com.example.lee.tmap.ValueObject.RecentPathListVO;
import com.example.lee.tmap.ValueObject.RecentPathVO;
import com.example.lee.tmap.View.ClearEditText;
import com.skp.Tmap.TMapGpsManager;
import com.wang.avi.AVLoadingIndicatorView;

import java.util.ArrayList;
import java.util.UUID;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.GsonConverterFactory;
import retrofit2.Response;
import retrofit2.Retrofit;

/**
 * Created by Nam on 2016. 12. 5..
 */
public class HomeFragment extends Fragment {
    private final static String TAG = HomeFragment.class.getSimpleName();



    private GridView gridView;

    private Toolbar toolbar;
    private ClearEditText et_destination;
    String[] recentPathList;
    private int recentArrivalNameListLength;

    private AVLoadingIndicatorView bleWaveView;
    private ImageView watchImageView;
    private TextView bleMessageTextView;

    private NetworkInfo mobile;
    private NetworkInfo wifi;

    // gridview list
    private ArrayList<RecentPathListVO> destinationList;
    private double destination_longitude = 0.0;
    private double destination_latitude = 0.0;
    private String strArrivalName = "";

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView() is called.");
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        Log.i(TAG, "onViewCreated() is called.");
        super.onViewCreated(view, savedInstanceState);

        toolbar = (Toolbar) getActivity().findViewById(R.id.toolbar);
        et_destination = (ClearEditText) toolbar.findViewById(R.id.et_destination);
        gridView = (GridView) getView().findViewById(R.id.gridview);
        bleWaveView = (AVLoadingIndicatorView) view.findViewById(R.id.ble_wave_view);
        watchImageView = (ImageView) view.findViewById(R.id.iv_ble_status);
        bleMessageTextView = (TextView) view.findViewById(R.id.tv_ble_message);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        Log.i(TAG, "onActivityCreated() is called.");
        super.onActivityCreated(savedInstanceState);

        // gridview list
        destinationList = new ArrayList<>();
        gridView.setOnTouchListener(recentDestinationDisableScroll);
        gridView.setOnItemClickListener(recentDestinationOnItemClickListener);
        // 인터넷 연결 체크
        ConnectivityManager manager = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        mobile = manager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        wifi = manager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        watchImageView.setImageResource(R.drawable.ic_scan);
        bleMessageTextView.setText(R.string.ble_scan);
        watchImageView.bringToFront();

        et_destination.setOnFocusChangeListener(editTextOnFocusChangeListener);

        // UUID & Recent Path List
        String strUUID = this.GetDevicesUUID(getActivity());
        recentPathList = new String[9];

        // [ 최근에 검색한 경로 9개 ]
        Retrofit client = new Retrofit.Builder().baseUrl(ApiService.SERVER_URL).addConverterFactory(GsonConverterFactory.create()).build();
        ApiService apiService = client.create(ApiService.class);
        Call<RecentPathVO> call = apiService.getRecentPath();
        call.enqueue(new Callback<RecentPathVO>() {
            @Override
            public void onResponse(Call<RecentPathVO> call, Response<RecentPathVO> response) {
                if (response.isSuccessful()) {
                    RecentPathListVO vo;
                    String arrivalName = "";
                    String strLatitude = "";
                    String strLongitude = "";
                    double longitudeValue = 0.0;
                    double latitudeValue = 0.0;

                    recentArrivalNameListLength = response.body().getResult().size();
                    Log.i(TAG, "res length : " + recentArrivalNameListLength);
                    for (int i = 0; i < recentArrivalNameListLength; i++) {
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
                    gridView.setAdapter(new GridViewAdapter(getActivity(), recentPathList));
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

    private String GetDevicesUUID(Context mContext) {
        final TelephonyManager tm = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        final String tmDevice, tmSerial, androidId;
        tmDevice = "" + tm.getDeviceId();
        tmSerial = "" + tm.getSimSerialNumber();
        androidId = "" + android.provider.Settings.Secure.getString(getActivity().getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
        UUID deviceUuid = new UUID(androidId.hashCode(), ((long) tmDevice.hashCode() << 32) | tmSerial.hashCode());
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

        long searchDate = System.currentTimeMillis();
        String userUUID = "AAF3:0000:0000:0000";
        String strSearchDate = String.valueOf(searchDate);
        String strArrivalLongitude = String.valueOf(destination_longitude);
        String strArrivalLatitude = String.valueOf(destination_latitude);

        // [ DB 저장 ]
        Retrofit client = new Retrofit.Builder().baseUrl(ApiService.SERVER_URL).addConverterFactory(GsonConverterFactory.create()).build();
        ApiService apiService = client.create(ApiService.class);
        Call<ResponseBody> call = apiService.saveRoute(userUUID, strArrivalName, strSearchDate, strArrivalLongitude, strArrivalLatitude);
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
    }   // [ End recentPathClicked ]




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

                        Intent intent = new Intent(getActivity(), PathInfoActivity.class);
                        intent.putExtra("arrival_name", strArrivalName);
                        intent.putExtra("des_longitude", destination_longitude);
                        intent.putExtra("des_latitude", destination_latitude);
                        startActivity(intent);
                        getActivity().overridePendingTransition(R.anim.anim_slide_fade_in, R.anim.anim_slide_out_left);
                    }else{
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



    private View.OnFocusChangeListener editTextOnFocusChangeListener = new View.OnFocusChangeListener() {
        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            if (hasFocus) {
                Log.i(TAG, "lost the focus");
                if (wifi.isConnected() || mobile.isConnected()) {
                    Log.i(TAG, "[ 인터넷 연결이 완료됨. ] ");
                    ArrivalPathListFragment fragment = new ArrivalPathListFragment();
                    getActivity().getSupportFragmentManager().beginTransaction().replace(R.id.fragment, fragment).commit();
                } else {
                    Log.i(TAG, "[ 인터넷 연결이 필요함. ] ");
                    Toast.makeText(getActivity(), "[ 인터넷 연결이 필요합니다. ] ", Toast.LENGTH_LONG).show();
                }
            } else {
                Log.i(TAG, "lost the focus");
            }
        }
    };

    private View.OnTouchListener recentDestinationDisableScroll = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_MOVE) {
                return true;
            }
            return false;
        }
    };

    private AdapterView.OnItemClickListener recentDestinationOnItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if(position < recentArrivalNameListLength){
                    if( UserException.STATIC_CURRENT_LONGITUDE == 0 || UserException.STATIC_CURRENT_LATITUDE == 0 )
                        Toast.makeText(getActivity(), "[ GPS 활성화 중입니다. ]", Toast.LENGTH_LONG).show();
                    else                recentPathClicked(recentPathList[position]);
            }
        }
    };


    public void onStart() {
        Log.i(TAG, "onStart() is called.");
        super.onStart();
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy() is called.");
        super.onDestroy();
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
        bleWaveView.show();
    }
}
