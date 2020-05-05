package com.sunstone;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;

public class LauncherActivity extends AppCompatActivity {
    private final int SPLASH_TIME_OUT = 2000;
    private Handler splashHandler;
    private Runnable splashRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher);

        if (android.os.Build.VERSION.SDK_INT < 23) {
            Toast.makeText(LauncherActivity.this, "Android version is too low.\nMinimum version is 6.0", Toast.LENGTH_SHORT).show();
            Intent homeIntent = new Intent(Intent.ACTION_MAIN);
            homeIntent.addCategory(Intent.CATEGORY_HOME);
            homeIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(homeIntent);
        }

        splashHandler = new Handler();
        splashRunnable = () -> {
            Intent homeActivity = new Intent(LauncherActivity.this, MainActivity.class);
            startActivity(homeActivity);
            finish();
        };

        splashHandler.postDelayed(splashRunnable, SPLASH_TIME_OUT);

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        splashHandler.removeCallbacks(splashRunnable);
    }
}
