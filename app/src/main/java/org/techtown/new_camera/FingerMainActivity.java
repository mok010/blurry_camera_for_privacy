package org.techtown.new_camera;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import me.relex.circleindicator.CircleIndicator3;

public class FingerMainActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "BlurPrefs";
    private static final String KEY_IRIS_BLURRING = "iris_blurring";

    private ViewPager2 mPager;
    private FragmentStateAdapter pagerAdapter;
    private int num_page = 4;
    private CircleIndicator3 mIndicator;

    private Handler slideHandler = new Handler();
    private Runnable slideRunnable = new Runnable() {
        @Override
        public void run() {
            int currentItem = mPager.getCurrentItem();
            int nextItem = currentItem + 1;
            mPager.setCurrentItem(nextItem);
            slideHandler.postDelayed(this, 3000); // 3초마다 페이지 전환
        }
    };

    private boolean isIrisBlurringOn = true;
    private boolean isFingerprintBlurringOn = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_onoff);

        // ViewPager2 초기화
        mPager = findViewById(R.id.viewpager);
        pagerAdapter = new MyAdapter(this, num_page);
        mPager.setAdapter(pagerAdapter);

        // Indicator 설정
        mIndicator = findViewById(R.id.indicator);
        mIndicator.setViewPager(mPager);
        mIndicator.createIndicators(num_page, 0);
        mPager.setOrientation(ViewPager2.ORIENTATION_HORIZONTAL);

        // 초기 페이지 설정
        mPager.setCurrentItem(1000); // 시작점 설정
        mPager.setOffscreenPageLimit(4); // 최대 페이지 수

        mPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels);
                if (positionOffsetPixels == 0) {
                    mPager.setCurrentItem(position);
                }
            }

            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                mIndicator.animatePageSelected(position % num_page);
            }
        });

        // 자동 슬라이드 시작
        slideHandler.postDelayed(slideRunnable, 3000);

        // 버튼 초기화
        Button buttonIris = findViewById(R.id.button4);
        Button buttonFingerprint = findViewById(R.id.button3);
        Button buttonCamera = findViewById(R.id.button2);  // "카메라" 버튼
        Button buttonAlbum = findViewById(R.id.button);  // "앨범" 버튼

        // SharedPreferences 초기화
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        isIrisBlurringOn = prefs.getBoolean(KEY_IRIS_BLURRING, true);

        buttonIris.setText(isIrisBlurringOn ? "홍채 블러링 ON" : "홍채 블러링 OFF");
        buttonIris.setBackgroundResource(isIrisBlurringOn ? R.drawable.rounded_button_blue : R.drawable.rounded_button_grey);

        buttonIris.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isIrisBlurringOn = !isIrisBlurringOn;
                buttonIris.setText(isIrisBlurringOn ? "홍채 블러링 ON" : "홍채 블러링 OFF");
                buttonIris.setBackgroundResource(isIrisBlurringOn ? R.drawable.rounded_button_blue : R.drawable.rounded_button_grey);

                // 상태 저장
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean(KEY_IRIS_BLURRING, isIrisBlurringOn);
                editor.apply();
            }
        });

        buttonFingerprint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isFingerprintBlurringOn = !isFingerprintBlurringOn;
                buttonFingerprint.setText(isFingerprintBlurringOn ? "지문 블러링 ON" : "지문 블러링 OFF");
                buttonFingerprint.setBackgroundResource(isFingerprintBlurringOn ? R.drawable.rounded_button_blue : R.drawable.rounded_button_grey);
            }
        });

        // "카메라" 버튼 클릭 시 "앨범" 버튼과 동일하게 AlbumActivity로 이동하도록 수정
        buttonCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(FingerMainActivity.this, TransitionActivity.class);
                intent.putExtra("next_activity", "AlbumActivity");
                startActivity(intent);
            }
        });

        // "앨범" 버튼 클릭 시 AlbumActivity로 이동
        buttonAlbum.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(FingerMainActivity.this, TransitionActivity.class);
                intent.putExtra("next_activity", "AlbumActivity");
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 메모리 누수 방지를 위해 슬라이드 핸들러 콜백 제거
        slideHandler.removeCallbacks(slideRunnable);
    }
}
