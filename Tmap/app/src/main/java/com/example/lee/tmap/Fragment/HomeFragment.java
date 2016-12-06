package com.example.lee.tmap.Fragment;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
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

import com.example.lee.tmap.Adapter.GridViewAdapter;
import com.example.lee.tmap.ApiService;
import com.example.lee.tmap.R;
import com.example.lee.tmap.ValueObject.RecentPathVO;
import com.example.lee.tmap.View.ClearEditText;
import com.skp.Tmap.TMapGpsManager;
import com.wang.avi.AVLoadingIndicatorView;

import java.util.UUID;

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

    private static final int GRID_VIEW_NUMBER = 12;
    private static final int DESTINATION_NUMBER = 9;

    private int arrivalPathListLength;
    private GridView gridView;

    private Toolbar toolbar;
    private ClearEditText et_destination;
    String[] recentPathList;


    private AVLoadingIndicatorView bleWaveView;
    private ImageView watchImageView;
    private TextView bleMessageTextView;

    private TMapGpsManager tMapGpsManager;
    private NetworkInfo mobile;
    private NetworkInfo wifi;

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
        recentPathList = new String[GRID_VIEW_NUMBER];

        Retrofit client = new Retrofit.Builder().baseUrl(ApiService.SERVER_URL).addConverterFactory(GsonConverterFactory.create()).build();
        ApiService apiService = client.create(ApiService.class);
        // Call<RecentPathVO> call = apiService.getRecentPath(strUUID);
        Call<RecentPathVO> call = apiService.getRecentPath();
        call.enqueue(new Callback<RecentPathVO>() {
            @Override
            public void onResponse(Call<RecentPathVO> call, Response<RecentPathVO> response) {
                if (response.isSuccessful()) {
                    Log.i(TAG, "[ On Response ] ");
                    arrivalPathListLength = response.body().getResult().size();
                    for (int i = 0; i < arrivalPathListLength; i++) {
                        recentPathList[i] = String.valueOf(response.body().getResult().get(i).getARRIVAL_NAME());
                        Log.i(TAG, "[ Response Body Arrival Name ][" + i + "] & RecentPath : " + response.body().getResult().get(i).getARRIVAL_NAME() + " / " + recentPathList[i]);
                        if (DESTINATION_NUMBER <= arrivalPathListLength) {
                            break;
                        }
                    }   // for

                    /*==========
                        GridView [ recentPathList에 값을 넣기전에 GridView Adapter로 null 값을 보내버리기 때문에 여기에 위치함 ]
                    ==========*/
                    gridView.setAdapter(new GridViewAdapter(getActivity(), recentPathList));

                    //disable scroll in GridView
                    gridView.setOnTouchListener(recentDestinationDisableScroll);
                    gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                            if (position <= arrivalPathListLength - 1) {
                                Toast.makeText(getActivity(), recentPathList[position], Toast.LENGTH_LONG).show();
                            }
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
