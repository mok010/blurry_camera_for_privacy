package org.techtown.new_camera;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;

public class TransitionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main4);

        // 2초 후에 다음 액티비티로 이동
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = getIntent();
                String nextActivity = intent.getStringExtra("next_activity");

                if ("MainActivity".equals(nextActivity)) {
                    startActivity(new Intent(TransitionActivity.this, MainActivity.class));
                } else if ("AlbumActivity".equals(nextActivity)) {
                    startActivity(new Intent(TransitionActivity.this, AlbumActivity.class));
                }
                finish();
            }
        }, 2000); // 2000ms = 2초
    }
}

