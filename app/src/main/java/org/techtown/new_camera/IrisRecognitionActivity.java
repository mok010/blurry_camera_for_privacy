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

        Intent intent = getIntent();
        String imgPath = intent.getStringExtra("img");

        if (imgPath != null) {
            File imgFile = new File(imgPath);
            if (imgFile.exists()) {
                Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                imageView.setImageBitmap(myBitmap);

                // 홍채 인식 변환 기능 호출
                startIrisRecognition(myBitmap);
            } else {
                Toast.makeText(this, "이미지 파일을 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startIrisRecognition(Bitmap bitmap) {
        Toast.makeText(this, "홍채 인식 변환이 시작되었습니다.", Toast.LENGTH_SHORT).show();

        // 여기에 홍채 인식 알고리즘 추가
    }
}
