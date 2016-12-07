package com.example.lee.tmap.Activity;


import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import com.example.lee.tmap.Fragment.ArrivalPathListFragment;
import com.example.lee.tmap.Fragment.HomeFragment;
import com.example.lee.tmap.PSoCBleService;
import com.example.lee.tmap.R;
import com.example.lee.tmap.UserException;
import com.example.lee.tmap.Utils.BackPressCloseHandler;
import com.example.lee.tmap.Utils.BeyondSingleton;
import com.example.lee.tmap.View.ClearEditText;
import com.skp.Tmap.TMapGpsManager;

import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;


public class MainActivity extends AppCompatActivity implements TMapGpsManager.onLocationChangedCallback {

    public static final String TAG = MainActivity.class.getSimpleName();

    private TMapGpsManager gps = null;
    public int GPS_MIN_TIME = 100;
    public int GPS_MIN_DISTANCE = 5;

    private static final int REQUEST_ENABLE_BLE = 1;
    private static final int REQUEST_SIMULATE_FINISH = 2;

    //This is required for Android 6.0 (Marshmallow)
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;

    // Variables to manage BLE connection
    public static boolean mConnectState = false;
    public static boolean mServiceConnected = false;
    private static PSoCBleService mPSoCBleService;

    private FragmentManager fm;
    private HomeFragment fragment;

    // Keep track of whether CapSense Notifications are on or off
    private static boolean CapSenseNotifyState = false;

    public static double cur_longitude = 0.0;
    public static double cur_latitude = 0.0;

    private Toolbar toolbar;

    private ClearEditText et_destination;

    private InputMethodManager imm;

    private NavigationView navigationView;

    private BackPressCloseHandler backPressCloseHandler;

    private ActionBarDrawerToggle toggle;

    private DrawerLayout mDrawerLayout;


    /**
     * This manages the lifecycle of the BLE service.
     * When the service starts we get the service object and initialize the service.
     */
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        /**
         * This is called when the PSoCCapSenseLedService is connected
         *
         * @param componentName the component name of the service that has been connected
         * @param service service being bound
         */
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            Log.i(TAG, "onServiceConnected");
            mPSoCBleService = ((PSoCBleService.LocalBinder) service).getService();
            mServiceConnected = true;
            mPSoCBleService.initialize();
            BeyondSingleton.getInstance().setBleConnectedStatus(true);
        }

        /**
         * This is called when the PSoCCapSenseService is disconnected.
         *
         * @param componentName the component name of the service that has been connected
         */
        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.i(TAG, "onServiceDisconnected");
            mPSoCBleService = null;
            BeyondSingleton.getInstance().setBleConnectedStatus(false);
        }
    };

    /**
     * This is called when the main activity is first created
     *
     * @param savedInstanceState is any state saved from prior creations of this activity
     */
    @TargetApi(Build.VERSION_CODES.M)
    // This is required for Android 6.0 (Marshmallow) to work    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate() is called.");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // UUID & Recent Path List
        String strUUID = this.GetDevicesUUID(this);
        Log.i(TAG, "[ String UUID ] : " + strUUID);

        //Add toolbar
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("");
        setSupportActionBar(toolbar);
        et_destination = (ClearEditText) toolbar.findViewById(R.id.et_destination);
        imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().add(R.id.fragment, new HomeFragment()).commit();
        }

        //set backPressCloseHandler
        backPressCloseHandler = new BackPressCloseHandler(this);

        //Initialize singleton
        BeyondSingleton.initializeInstance(getApplicationContext());
        BeyondSingleton.getInstance().setBleConnectedStatus(false);

        // Initialize service and connection state variable
//        mServiceConnected = false;
//        mConnectState = false;

        //This section required for Android 6.0 (Marshmallow)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android M Permission check 
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("This app needs location access ");
                builder.setMessage("Please grant location access so this app can detect devices.");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    public void onDismiss(DialogInterface dialog) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
                    }
                });
                builder.show();
            }
        } //End of section for Android 6.0 (Marshmallow)

        //Add DrawerLayout
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        toggle = new ActionBarDrawerToggle(
                this, mDrawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close) {
            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                Log.i(TAG, "onDrawerClosed is called");

                Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment);
                if (currentFragment.getClass().getName().equals(HomeFragment.class.getName())) {
                    Fragment fragment = null;
                    final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                    fragment = new HomeFragment();
                    ft.replace(R.id.fragment, fragment).commit();
                }
            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                Log.i(TAG, "onDrawerOpened is called");
                imm.hideSoftInputFromWindow(et_destination.getWindowToken(), 0);
            }
        };
        mDrawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        navigationView = (NavigationView) findViewById(R.id.navigation_view);
        navigationView.setNavigationItemSelectedListener(navigationItemSelectedListener);


    }   // onCreate


    // When drawerLayout is activated, fragment can be replaced.
    NavigationView.OnNavigationItemSelectedListener navigationItemSelectedListener = new NavigationView.OnNavigationItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(MenuItem item) {
            mDrawerLayout.closeDrawer(GravityCompat.START);

            Fragment fragment = null;
            final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            switch (item.getItemId()) {
                case R.id.home:
                    fragment = new HomeFragment();
                    ft.replace(R.id.fragment, fragment).commit();
                    break;
            }

            return true;
        }
    };


    // UUID
    private String GetDevicesUUID(Context mContext) {
        final TelephonyManager tm = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        final String tmDevice, tmSerial, androidId;
        tmDevice = "" + tm.getDeviceId();
        tmSerial = "" + tm.getSimSerialNumber();
        androidId = "" + android.provider.Settings.Secure.getString(getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
        UUID deviceUuid = new UUID(androidId.hashCode(), ((long) tmDevice.hashCode() << 32) | tmSerial.hashCode());
        String deviceId = deviceUuid.toString();
        return deviceId;
    }

    public boolean onOptionsItemSelected(MenuItem item) {

        Fragment fragment = null;
        ArrivalPathListFragment arrivalPathListFragment = null;
        final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();

        switch (item.getItemId()) {
            case R.id.connect_bluetooth_menu:
                searchBluetooth();
                return true;
            case R.id.back_menu:

                fm = getSupportFragmentManager();
                arrivalPathListFragment = (ArrivalPathListFragment) fm.findFragmentById(R.id.fragment);
                arrivalPathListFragment.removeDestinationTextChangedListener();// remove textWatcher
                fragment = new HomeFragment();
                ft.replace(R.id.fragment, fragment).commit();
                imm.hideSoftInputFromWindow(et_destination.getWindowToken(), 0);

                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onLocationChange(Location location) {
        cur_longitude = location.getLongitude();
        cur_latitude = location.getLatitude();

        Log.i(TAG, "[ MainActivity User Exception Location ]");
        Log.i(TAG, "[ Longitude & Latitude ] : " + UserException.STATIC_CURRENT_LONGITUDE + " & " + UserException.STATIC_CURRENT_LATITUDE);
        UserException.STATIC_CURRENT_LATITUDE = cur_latitude;
        UserException.STATIC_CURRENT_LONGITUDE = cur_longitude;

        Log.i(TAG, "[ MainActivity On Location Change ]");
        Log.i(TAG, "onLocationChange" + cur_longitude + " / " + cur_latitude);
    }   // onLocationChange

    public void initGPS() {
        Log.i(TAG, "[ HomeFragment init GPS ]");
        gps = new TMapGpsManager(MainActivity.this);
        gps.setMinTime(GPS_MIN_TIME);
        gps.setMinDistance(GPS_MIN_DISTANCE);
        gps.setProvider(TMapGpsManager.GPS_PROVIDER);
        gps.OpenGps();
    }   // initGPS


    //
    @Override
    protected void onStart() {
        super.onStart();
    }


    //This method required for Android 6.0 (Marshmallow)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("Permission for 6.0:", "Coarse location permission granted");
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Functionality limited");
                    builder.setMessage("Since location access has not been granted, this app will not be able to discover beacons when in the background.");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                        }
                    });
                    builder.show();
                }
            }
        }
    } //End of section for Android 6.0 (Marshmallow)

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BLE && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onBackPressed() {
        // When drawerLayout is activated, use the back button to call closeDrawer().
        if (mDrawerLayout != null && mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            mDrawerLayout.closeDrawer(GravityCompat.START);
        } else {
//            super.onBackPressed();
            backPressCloseHandler.onBackPressed();
        }
    }

    @Override
    protected void onResume() {
        Log.i(TAG, "onResume() is called.");
        super.onResume();
        initGPS();
        // Register the broadcast receiver. This specified the messages the main activity looks for from the PSoCCapSenseLedService
        final IntentFilter filter = new IntentFilter();
        filter.addAction(PSoCBleService.ACTION_BLESCAN_CALLBACK);
        filter.addAction(PSoCBleService.ACTION_CONNECTED);
        filter.addAction(PSoCBleService.ACTION_DISCONNECTED);
        filter.addAction(PSoCBleService.ACTION_SERVICES_DISCOVERED);
        filter.addAction(PSoCBleService.ACTION_DATA_RECEIVED);
        registerReceiver(mBleUpdateReceiver, filter);

        startBluetooth();
        new Timer().schedule(new TimerTask() {
            public void run() {
                // UI 및 로직
                searchBluetooth();
                Log.i(TAG, "Timer run searchBluetooth in onResume()");

            }
        }, 3000); // 3초후 실행
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "[ Close GPS ]");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i(TAG, "onPause() is called.");
        super.onPause();
        unregisterReceiver(mBleUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        Log.i(TAG, "onDestroy() is called.");
        super.onDestroy();
        // Close and unbind the service when the activity goes away
        mPSoCBleService.close();
        Disconnect();
        unbindService(mServiceConnection);
        mPSoCBleService = null;
        mServiceConnected = false;
    }

    public void startBluetooth() {

        // Find BLE service and adapter
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter mBluetoothAdapter = bluetoothManager.getAdapter();

        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BLE);
        }

        // Start the BLE Service
        Log.d(TAG, "Starting BLE Service");
        Intent gattServiceIntent = new Intent(getApplicationContext(), PSoCBleService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

       /* // Disable the start button and turn on the search  button
        start_button.setEnabled(false);
        search_button.setEnabled(true);*/
        Log.d(TAG, "Bluetooth is Enabled");
//        searchBluetooth();
    }


    public void searchBluetooth() {
        if (mServiceConnected) {
            mPSoCBleService.scan();
        }

        /* After this we wait for the scan callback to detect that a device has been found */
        /* The callback broadcasts a message which is picked up by the mGattUpdateReceiver */
    }


    public void connectBluetooth() {
        mPSoCBleService.connect();

        /* After this we wait for the gatt callback to report the device is connected */
        /* That event broadcasts a message which is picked up by the mGattUpdateReceiver */
    }

    public void discoverServices() {
        /* This will discover both services and characteristics */
        mPSoCBleService.discoverServices();

        /* After this we wait for the gatt callback to report the services and characteristics */
        /* That event broadcasts a message which is picked up by the mGattUpdateReceiver */
    }


    public void Disconnect() {
        mPSoCBleService.disconnect();

        /* After this we wait for the gatt callback to report the device is disconnected */
        /* That event broadcasts a message which is picked up by the mGattUpdateReceiver */
    }

    /**
     * Listener for BLE event broadcasts
     */
    private final BroadcastReceiver mBleUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            switch (action) {
                case PSoCBleService.ACTION_BLESCAN_CALLBACK:
                    // Disable the search button and enable the connect button
                    connectBluetooth();
                    break;

                case PSoCBleService.ACTION_CONNECTED:
                    /* This if statement is needed because we sometimes get a GATT_CONNECTED */
                    /* action when sending Capsense notifications */
                    if (!mConnectState) {

                        mConnectState = true;
                        BeyondSingleton.getInstance().setBleConnectedStatus(true);
                        Log.d(TAG, "Connected to Device");
                        discoverServices();
                    }
                    break;
                case PSoCBleService.ACTION_DISCONNECTED:
                    mConnectState = false;
                    Log.d(TAG, "Disconnected");
                    break;
                case PSoCBleService.ACTION_SERVICES_DISCOVERED:
                    Log.d(TAG, "Services Discovered");
                    fm = getSupportFragmentManager();
                    fragment = (HomeFragment) fm.findFragmentById(R.id.fragment);
                    fragment.updateBluetoothConnectedView();// 페어링 완료 메시지 표시하는 UI 메서드
                    break;
                case PSoCBleService.ACTION_DATA_RECEIVED:
                    // This is called after a notify or a read completes
                    // Check LED switch Setting
                    if (mPSoCBleService.getLedSwitchState()) {
//                        led_switch.setChecked(true);
                    } else {
//                        led_switch.setChecked(false);
                    }
                    // Get CapSense Slider Value
                    String CapSensePos = mPSoCBleService.getCapSenseValue();
                    if (CapSensePos.equals("-1")) {  // No Touch returns 0xFFFF which is -1
                        if (!CapSenseNotifyState) { // Notifications are off
//                            mCapsenseValue.setText(R.string.NotifyOff);
                        } else { // Notifications are on but there is no finger on the slider
//                            mCapsenseValue.setText(R.string.NoTouch);
                        }
                    } else { // Valid CapSense value is returned
//                        mCapsenseValue.setText(CapSensePos);
                        Log.d(TAG, "CapsensePos : " + CapSensePos);
                    }
                default:
                    break;
            }
        }
    };
}
