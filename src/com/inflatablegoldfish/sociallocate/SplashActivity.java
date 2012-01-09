package com.inflatablegoldfish.sociallocate;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class SplashActivity extends Activity {
    private static long splashDelay = 1200;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash);
        
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                finish();
                Intent slIntent = new Intent().setClass(SplashActivity.this, SLArrangeMeet.class);
                startActivity(slIntent);
            }
        };

        Timer timer = new Timer();
        timer.schedule(task, splashDelay);
    }
}
