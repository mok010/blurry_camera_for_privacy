package org.techtown.new_camera;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main1); // activity_main1.xml 설정

        // 1초 후에 MainActivity로 이동
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(SplashActivity.this, FingerMainActivity.class);
                startActivity(intent);
                finish(); // SplashActivity 종료
            }
        }, 1000); // 1000ms = 1초
    }
}

