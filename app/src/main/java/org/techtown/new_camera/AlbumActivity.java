package org.techtown.new_camera;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;
import android.os.Handler;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.FaceLandmark;

import org.techtown.new_camera.ImageProcessor;

import java.io.FileDescriptor;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class AlbumActivity extends AppCompatActivity {

    ImageView imageView;
    FrameLayout frameLayout;  // 커스텀 뷰를 추가할 레이아웃
    Bitmap photoBitmap;  // photoBitmap 변수를 클래스 수준에서 선언
    int image_Val=0;    // 이미지를 선택하지 않고 처리하는 경우를 방지하기 위한 카운트, 1이 아니면 사진을 고르지 않았다고 판별.

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_album);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 이전 화면으로 돌아가는 버튼
        Button button = findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        // 갤러리 화면 띄우는 버튼
        Button button2 = findViewById(R.id.button2);
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent, 1);
            }
        });

        // 앨범에서 선택한 이미지 표시하는 뷰
        imageView = findViewById(R.id.imageView);
        frameLayout = findViewById(R.id.frameLayout);  // 커스텀 뷰를 추가할 레이아웃 초기화

        // 블러 처리를 시작하는 버튼
        Button button3 = findViewById(R.id.button3);
        button3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (image_Val == 1) {
                    // ProgressDialog 생성
                    ProgressDialog dialog = new ProgressDialog(AlbumActivity.this);
                    dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                    dialog.setMessage("보호중입니다...");
                    dialog.show();

                    // 변환 과정 시 딜레이 추가
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            // ProgressDialog 없애기
                            dialog.dismiss();
                            Toast.makeText(AlbumActivity.this, "변환이 완료되었습니다:).", Toast.LENGTH_SHORT).show();
                        }
                    }, 5000);
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                InputImage inputImage = InputImage.fromBitmap(photoBitmap, 0);
                                List<Face> faces = ImageProcessor.processInputImage(photoBitmap).get();
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        CustomView customView = new CustomView(AlbumActivity.this, photoBitmap, faces);
                                        frameLayout.addView(customView);  // CustomView 추가
                                    }
                                });
                            } catch (ExecutionException | InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }).start();
                    image_Val = 0;
                } else {
                    Toast.makeText(AlbumActivity.this, "이미지를 선택하지 않았습니다.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    // 앨범에서 사진을 선택하면 uri를 따오고, uri를 bitmap으로 변환하여, imageView에 띄우는 함수
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case 1:
                if (resultCode == RESULT_OK) {
                    Uri uri = data.getData();
                    Log.d("이미지주소", String.valueOf(uri));
                    photoBitmap = getBitmapFromUri(uri);  // photoBitmap 초기화
                    imageView.setImageBitmap(photoBitmap);    // 선택한 이미지를 이미지뷰에 셋
                    image_Val = 1;
                }
                break;
        }
    }

    // 이미지 uri를 bitmap 형태로 저장하는 함수
    private Bitmap getBitmapFromUri(Uri uri) {
        try {
            ParcelFileDescriptor parcelFileDescriptor = getContentResolver().openFileDescriptor(uri, "r");
            FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
            Bitmap bitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor);
            parcelFileDescriptor.close();
            return bitmap;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    // 커스텀 뷰 클래스
    private static class CustomView extends View {
        private Bitmap bitmap;
        private List<Face> faces;
        private Paint paint;

        public CustomView(Context context, Bitmap bitmap, List<Face> faces) {
            super(context);
            this.bitmap = bitmap;
            this.faces = faces;
            this.paint = new Paint();
            paint.setColor(Color.BLACK);
            paint.setStyle(Paint.Style.FILL);
            paint.setAlpha(128);  // 50% 불투명도 설정
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            // 화면 크기와 비트맵 크기를 구합니다.
            int canvasWidth = canvas.getWidth();
            int canvasHeight = canvas.getHeight();
            int bitmapWidth = bitmap.getWidth();
            int bitmapHeight = bitmap.getHeight();

            // 스케일을 계산합니다.
            float scale = Math.min((float) canvasWidth / bitmapWidth, (float) canvasHeight / bitmapHeight);

            // 비트맵을 스케일과 위치를 적용하여 그립니다.
            float scaledWidth = scale * bitmapWidth;
            float scaledHeight = scale * bitmapHeight;
            float left = (canvasWidth - scaledWidth) / 2;
            float top = (canvasHeight - scaledHeight) / 2;

            canvas.drawBitmap(bitmap, null, new Rect((int) left, (int) top, (int) (left + scaledWidth), (int) (top + scaledHeight)), null);

            // 얼굴 인식 결과를 그립니다.
            for (Face face : faces) {
                float eyeRadius = face.getBoundingBox().width() * 0.03f; // 얼굴 너비의 3%를 눈의 반지름으로 사용

                // 왼쪽 눈의 위치를 가져옵니다.
                FaceLandmark leftEye = face.getLandmark(FaceLandmark.LEFT_EYE);
                if (leftEye != null) {
                    PointF leftEyePos = leftEye.getPosition();
                    float leftEyeX = left + leftEyePos.x * scale;
                    float leftEyeY = top + leftEyePos.y * scale;
                    canvas.drawCircle(leftEyeX, leftEyeY, eyeRadius, paint);  // 눈의 크기만큼 원을 그립니다.
                }

                // 오른쪽 눈의 위치를 가져옵니다.
                FaceLandmark rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE);
                if (rightEye != null) {
                    PointF rightEyePos = rightEye.getPosition();
                    float rightEyeX = left + rightEyePos.x * scale;
                    float rightEyeY = top + rightEyePos.y * scale;
                    canvas.drawCircle(rightEyeX, rightEyeY, eyeRadius, paint);  // 눈의 크기만큼 원을 그립니다.
                }
            }
        }
    }
}