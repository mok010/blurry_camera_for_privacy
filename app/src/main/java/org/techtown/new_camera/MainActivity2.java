package org.techtown.new_camera;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.os.Build;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity2 extends AppCompatActivity {
    ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        imageView = findViewById(R.id.imageView);
        String img = getIntent().getStringExtra("img");

        // 이미지 불러오기
        Bitmap bitmap = BitmapFactory.decodeFile(img);

        // Matrix 객체 생성
        Matrix matrix = new Matrix();
        // 오른쪽으로 90도 회전
        matrix.postRotate(90);
        // 새로운 Bitmap 생성
        Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

        // 블러 처리 및 이미지 설정
        applyBlur(rotatedBitmap);
    }

    // 블러 효과 적용 메소드
    private void applyBlur(Bitmap bitmap) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12 이상에서 RenderEffect 사용
            RenderEffect blurEffect = RenderEffect.createBlurEffect(10f, 10f, Shader.TileMode.CLAMP);
            imageView.setRenderEffect(blurEffect);
        }

        // 이미지 뷰에 비트맵 설정
        imageView.setImageBitmap(bitmap);
    }
}

