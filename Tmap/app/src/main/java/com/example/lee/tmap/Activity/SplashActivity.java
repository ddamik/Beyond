package com.example.lee.tmap.Activity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;

import com.example.lee.tmap.R;

public class SplashActivity extends AppCompatActivity {

    public static int SPLASH_LENGTH = 3000;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Handler handler = new Handler(){
            @Override
            public void handleMessage(Message msg){
                finish();
            }
        };
        handler.sendEmptyMessageDelayed(0, SPLASH_LENGTH);
    }   // onCreate
}
