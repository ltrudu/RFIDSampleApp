package com.zebra.rfid.demo.sdksample;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class SplashActivity extends AppCompatActivity {

    TextView tvStatus = null;
    TextView tvLoading = null;
    private Handler title_animation_handler;
    private Runnable title_animation_runnable;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash);

        // Force portrait orientation
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        tvStatus = findViewById(R.id.tvStatus);
        tvLoading = findViewById(R.id.tvLoading);
        if (MainApplication.permissionGranted == false) {
            setTitle(R.string.app_title);
            startPointsAnimations(getString(R.string.app_title), getString(R.string.loading_status));
            MainApplication.iMainApplicationCallback = new MainApplication.iMainApplicationCallback() {
                @Override
                public void onPermissionSuccess(String message) {
                    SplashActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            stopPointsAnimations();
                            tvStatus.setText("Success Granting Permissions.");
                            // Start MainActivity
                            Intent intent = new Intent(SplashActivity.this, TagInventoryActivity.class);
                            startActivity(intent);
                            finish();
                        }
                    });
                }

                @Override
                public void onPermissionError(String message) {
                    SplashActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            stopPointsAnimations();
                            tvStatus.setText(message);
                        }
                    });
                }

                @Override
                public void onPermissionDebug(String message) {
                    SplashActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            tvStatus.setText(message);
                        }
                    });

                }
            };

            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }
        else
        {
            stopPointsAnimations();
            Intent intent = new Intent(SplashActivity.this, TagInventoryActivity.class);
            startActivity(intent);
            finish();
        }
    }

    private void startPointsAnimations(String baseTitle, String baseLoadingStatus) {
        final int maxDots = 5;
        title_animation_handler = new Handler(Looper.getMainLooper());
        title_animation_runnable = new Runnable() {
            int dotCount = 0;

            @Override
            public void run() {
                StringBuilder title = new StringBuilder(baseTitle);
                StringBuilder loadingStatus = new StringBuilder(baseLoadingStatus);
                for (int i = 0; i < dotCount; i++) {
                    title.append(".");
                    loadingStatus.append(".");
                }
                setTitle(title.toString());
                tvLoading.setText(loadingStatus.toString());
                dotCount = (dotCount + 1) % (maxDots + 1);
                title_animation_handler.postDelayed(this, 500); // Update every 500 milliseconds
            }
        };
        title_animation_handler.post(title_animation_runnable);
    }

    private void stopPointsAnimations() {
        if (title_animation_handler != null && title_animation_runnable != null) {
            title_animation_handler.removeCallbacks(title_animation_runnable);
            title_animation_handler = null;
            title_animation_runnable = null;
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                setTitle(R.string.app_title);
                tvLoading.setText(R.string.loading_status);
            }
        });
    }
}