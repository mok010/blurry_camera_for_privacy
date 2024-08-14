package org.techtown.new_camera;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;

public class IrisRecognitionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_iris_recognition);

        ImageView imageView = findViewById(R.id.iris_image);

        // MainActivity로부터 이미지 경로를 가져옴
        Intent intent = getIntent();
        String imgPath = intent.getStringExtra("img");

        if (imgPath != null) {
            File imgFile = new File(imgPath);
            if (imgFile.exists()) {
                Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                imageView.setImageBitmap(myBitmap);

                // 홍채 인식 처리 (여기서는 Toast 메시지로 대체)
                Toast.makeText(this, "홍채 인식이 시작되었습니다!", Toast.LENGTH_SHORT).show();

                // 홍채 인식 알고리즘 추가
            }
        }
    }
}
