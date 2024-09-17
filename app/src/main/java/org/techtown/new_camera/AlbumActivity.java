package org.techtown.new_camera;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceLandmark;
import com.google.mlkit.vision.pose.Pose;
import com.google.mlkit.vision.pose.PoseLandmark;

import java.io.FileDescriptor;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AlbumActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "BlurPrefs";
    private static final String KEY_IRIS_BLURRING = "iris_blurring";
    private static final String KEY_FINGERPRINT_BLURRING = "fingerprint_blurring";
    private boolean isIrisBlurringOn;
    private boolean isFingerprintBlurringOn;

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

        // SharedPreferences 초기화
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        isIrisBlurringOn = prefs.getBoolean(KEY_IRIS_BLURRING, true);
        isFingerprintBlurringOn = prefs.getBoolean(KEY_FINGERPRINT_BLURRING, true);

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
                frameLayout.removeAllViews();
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

        ExecutorService executor = Executors.newSingleThreadExecutor();
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
                    executor.execute(() -> {
                        try {
                            InputImage inputImage = InputImage.fromBitmap(photoBitmap, 0);
                            List<Face> faces = ImageProcessor.processInputImage(photoBitmap).get();
                            Pose poses = ImageProcessor.processInputImagePose(photoBitmap).get();
                            Bitmap blurredBitmap = photoBitmap.copy(Bitmap.Config.ARGB_8888, true);

                            if (isIrisBlurringOn && faces.size() > 0) {
                                // 얼굴 블러 처리
                                for (Face face : faces) {
                                    // 눈 블러 처리
                                    float eyeRadius = face.getBoundingBox().width() * 0.03f;
                                    FaceLandmark leftEye = face.getLandmark(FaceLandmark.LEFT_EYE);
                                    if (leftEye != null) {
                                        PointF leftEyePos = leftEye.getPosition();
                                        Rect leftEyeRect = new Rect(
                                                (int) (leftEyePos.x - eyeRadius),
                                                (int) (leftEyePos.y - eyeRadius),
                                                (int) (leftEyePos.x + eyeRadius),
                                                (int) (leftEyePos.y + eyeRadius)
                                        );
                                        blurredBitmap = AlbumActivity.BitmapUtil.blurRegion(blurredBitmap, leftEyeRect);
                                    }

                                    // 오른쪽 눈도 동일한 방식으로 처리
                                    FaceLandmark rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE);
                                    if (rightEye != null) {
                                        PointF rightEyePos = rightEye.getPosition();
                                        Rect rightEyeRect = new Rect(
                                                (int) (rightEyePos.x - eyeRadius),
                                                (int) (rightEyePos.y - eyeRadius),
                                                (int) (rightEyePos.x + eyeRadius),
                                                (int) (rightEyePos.y + eyeRadius)
                                        );
                                        blurredBitmap = AlbumActivity.BitmapUtil.blurRegion(blurredBitmap, rightEyeRect);
                                    }
                                }
                            }

                            // 손가락 블러 처리
                            if (isFingerprintBlurringOn) {
                                PoseLandmark leftIndex = poses.getPoseLandmark(PoseLandmark.LEFT_INDEX);
                                PoseLandmark leftPinky = poses.getPoseLandmark(PoseLandmark.LEFT_PINKY);
                                PoseLandmark leftWrist = poses.getPoseLandmark(PoseLandmark.LEFT_WRIST);


                                if (leftIndex != null && leftPinky != null && leftWrist != null) {
                                    // 검지, 새끼, 손목 좌표 추출
                                    PointF indexPos = leftIndex.getPosition();
                                    PointF pinkyPos = leftPinky.getPosition();
                                    PointF wristPos = leftWrist.getPosition();

                                    // 손목을 기준으로 반지름을 계산 (검지와 손목, 새끼와 손목의 거리)
                                    float indexRadius = (float) Math.sqrt(Math.pow(indexPos.x - wristPos.x, 2) + Math.pow(indexPos.y - wristPos.y, 2));
                                    float pinkyRadius = (float) Math.sqrt(Math.pow(pinkyPos.x - wristPos.x, 2) + Math.pow(pinkyPos.y - wristPos.y, 2));

                                    // 두 영역 생성 (손목에서 검지, 새끼 거리만큼의 원)
                                    Rect indexRect = new Rect(
                                            (int) (indexPos.x - indexRadius),
                                            (int) (indexPos.y - indexRadius),
                                            (int) (indexPos.x + indexRadius),
                                            (int) (indexPos.y + indexRadius)
                                    );

                                    Rect pinkyRect = new Rect(
                                            (int) (pinkyPos.x - pinkyRadius),
                                            (int) (pinkyPos.y - pinkyRadius),
                                            (int) (pinkyPos.x + pinkyRadius),
                                            (int) (pinkyPos.y + pinkyRadius)
                                    );

                                    // 손목 좌표의 색상 추출
                                    int wristColor = photoBitmap.getPixel((int) wristPos.x, (int) wristPos.y);

                                    // 살색 판단 기준을 설정 (예: 손목 색상에서 유사한 범위의 색상)
                                    float[] wristHsv = new float[3];
                                    Color.colorToHSV(wristColor, wristHsv);

                                    // 살색 범위 설정 (Hue, Saturation, Value의 범위 설정)
                                    float hueRange = 10; // 예: ±10도 허용 : 색조범위
                                    float saturationRange = 0.2f; // ±20% : 채도 범위
                                    float valueRange = 0.2f; // ±20% : 명도 범위

                                    // 블러 처리: 살색인 부분만 블러 처리
                                    for (int x = Math.max(0, indexRect.left); x < Math.min(photoBitmap.getWidth(), indexRect.right); x++) {
                                        for (int y = Math.max(0, indexRect.top); y < Math.min(photoBitmap.getHeight(), indexRect.bottom); y++) {
                                            int pixelColor = photoBitmap.getPixel(x, y);
                                            float[] pixelHsv = new float[3];
                                            Color.colorToHSV(pixelColor, pixelHsv);

                                            // 살색으로 판정되는 경우 블러 처리
                                            if (Math.abs(pixelHsv[0] - wristHsv[0]) <= hueRange &&
                                                    Math.abs(pixelHsv[1] - wristHsv[1]) <= saturationRange &&
                                                    Math.abs(pixelHsv[2] - wristHsv[2]) <= valueRange) {

                                                // 블러링할 영역 (인덱스 손가락)
                                                blurredBitmap = AlbumActivity.BitmapUtil.blurRegion(blurredBitmap, new Rect(x, y, x + 1, y + 1));
                                            }
                                        }
                                    }

                                    for (int x = Math.max(0, pinkyRect.left); x < Math.min(photoBitmap.getWidth(), pinkyRect.right); x++) {
                                        for (int y = Math.max(0, pinkyRect.top); y < Math.min(photoBitmap.getHeight(), pinkyRect.bottom); y++) {
                                            int pixelColor = photoBitmap.getPixel(x, y);
                                            float[] pixelHsv = new float[3];
                                            Color.colorToHSV(pixelColor, pixelHsv);

                                            // 살색으로 판정되는 경우 블러 처리
                                            if (Math.abs(pixelHsv[0] - wristHsv[0]) <= hueRange &&
                                                    Math.abs(pixelHsv[1] - wristHsv[1]) <= saturationRange &&
                                                    Math.abs(pixelHsv[2] - wristHsv[2]) <= valueRange) {

                                                // 블러링할 영역 (새끼 손가락)
                                                blurredBitmap = AlbumActivity.BitmapUtil.blurRegion(blurredBitmap, new Rect(x, y, x + 1, y + 1));
                                            }
                                        }
                                    }
                                }
                                PoseLandmark[] fingers = {poses.getPoseLandmark(PoseLandmark.LEFT_INDEX),
                                        poses.getPoseLandmark(PoseLandmark.LEFT_PINKY),
                                        poses.getPoseLandmark(PoseLandmark.LEFT_WRIST)};
                                blurredBitmap = applyBlurToFingers(blurredBitmap, fingers);
                            }

                            // 처리 완료 후 UI 업데이트
                            Bitmap finalBlurredBitmap = blurredBitmap;
                            runOnUiThread(() -> {
                                imageView.setImageBitmap(finalBlurredBitmap);
                                dialog.dismiss();
                                Toast.makeText(AlbumActivity.this, "변환이 완료되었습니다:).", Toast.LENGTH_SHORT).show();
                            });
                        } catch (Exception e) {
                            e.printStackTrace();
                            runOnUiThread(() -> dialog.dismiss());
                        }
                    });
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

    private static Bitmap applyBlurToFingers(Bitmap bitmap, PoseLandmark[] fingers) {
        Bitmap mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(mutableBitmap);
        Paint paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.FILL);
        paint.setAlpha(128);  // 반투명 빨간색

        for (PoseLandmark finger : fingers) {
            if (finger != null) {
                PointF fingerPos = finger.getPosition();

                // 지문 영역에 맞는 정사각형 좌표 계산
                float blurRadius = 30;  // 블러링할 정사각형의 반지름 (필요에 따라 조정)
                int leftX = (int) (fingerPos.x - blurRadius);  // 왼쪽 위 X 좌표
                int topY = (int) (fingerPos.y - blurRadius);   // 왼쪽 위 Y 좌표
                int rightX = (int) (fingerPos.x + blurRadius); // 오른쪽 아래 X 좌표
                int bottomY = (int) (fingerPos.y + blurRadius); // 오른쪽 아래 Y 좌표

                // 블러링 적용 (정사각형의 두 좌표 전달)
                mutableBitmap = AlbumActivity.BitmapUtil.blurRegion(mutableBitmap, new Rect(leftX, topY, rightX, bottomY));

                // 빨간색으로 해당 영역 덮기
                Rect redOverlayRect = new Rect(leftX, topY, rightX, bottomY);
                canvas.drawRect(redOverlayRect, paint);  // 해당 영역을 빨간색으로 덮음
            }
        }
        return mutableBitmap;
    }

    public static class BitmapUtil {

        public static class Size {
            public int width;
            public int height;

            public Size(int w, int h) {
                this.width = w;
                this.height = h;
            }
        }

        public static Bitmap blurRegion(Bitmap bitmap, Rect region) {
            int left = Math.max(0, region.left);
            int top = Math.max(0, region.top);
            int right = Math.min(bitmap.getWidth(), region.right);
            int bottom = Math.min(bitmap.getHeight(), region.bottom);

            if (left >= right || top >= bottom) {
                return bitmap;
            }

            Bitmap regionBitmap = Bitmap.createBitmap(bitmap, left, top, right - left, bottom - top);

            Bitmap blurredRegion = blur(regionBitmap);

            Bitmap mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
            Canvas canvas = new Canvas(mutableBitmap);

            canvas.drawBitmap(blurredRegion, left, top, null);

            return mutableBitmap;
        }

        public static Bitmap blur(Bitmap bitmap) {
            int iterations = 1;
            int radius = 8;
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            int[] inPixels = new int[width * height];
            int[] outPixels = new int[width * height];
            Bitmap blured = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            bitmap.getPixels(inPixels, 0, width, 0, 0, width, height);
            for (int i = 0; i < iterations; i++) {
                blur(inPixels, outPixels, width, height, radius);
                blur(outPixels, inPixels, height, width, radius);
            }
            blured.setPixels(inPixels, 0, width, 0, 0, width, height);
            return blured;
        }

        private static void blur(int[] in, int[] out, int width, int height, int radius) {
            int widthMinus1 = width - 1;
            int tableSize = 2 * radius + 1;
            int[] divide = new int[256 * tableSize];

            for (int index = 0; index < 256 * tableSize; index++) {
                divide[index] = index / tableSize;
            }

            int inIndex = 0;

            for (int y = 0; y < height; y++) {
                int outIndex = y;
                int ta = 0, tr = 0, tg = 0, tb = 0;

                for (int i = -radius; i <= radius; i++) {
                    int rgb = in[inIndex + clamp(i, 0, width - 1)];
                    ta += (rgb >> 24) & 0xff;
                    tr += (rgb >> 16) & 0xff;
                    tg += (rgb >> 8) & 0xff;
                    tb += rgb & 0xff;
                }

                for (int x = 0; x < width; x++) {
                    out[outIndex] = (divide[ta] << 24) | (divide[tr] << 16) | (divide[tg] << 8) | divide[tb];

                    int i1 = x + radius + 1;
                    if (i1 > widthMinus1) i1 = widthMinus1;
                    int i2 = x - radius;
                    if (i2 < 0) i2 = 0;
                    int rgb1 = in[inIndex + i1];
                    int rgb2 = in[inIndex + i2];

                    ta += ((rgb1 >> 24) & 0xff) - ((rgb2 >> 24) & 0xff);
                    tr += ((rgb1 & 0xff0000) - (rgb2 & 0xff0000)) >> 16;
                    tg += ((rgb1 & 0xff00) - (rgb2 & 0xff00)) >> 8;
                    tb += (rgb1 & 0xff) - (rgb2 & 0xff);
                    outIndex += height;
                }
                inIndex += width;
            }
        }

        private static int clamp(int x, int a, int b) {
            return (x < a) ? a : (x > b) ? b : x;
        }
    }

    // 커스텀 뷰 클래스
    private static class CustomView extends View {
        private Bitmap bitmap;
        private List<Face> faces;
        private Paint paint;
        private boolean isIrisBlurringOn;
        private Pose poses;
        private boolean isFingerprintBlurringOn;

        public CustomView(Context context, Bitmap bitmap, List<Face> faces, Pose poses, boolean isIrisBlurringOn, boolean isFingerprintBlurringOn) {
            super(context);
            this.bitmap = bitmap;
            this.faces = faces;
            this.poses = poses;
            this.isIrisBlurringOn = isIrisBlurringOn;
            this.isFingerprintBlurringOn = isFingerprintBlurringOn;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            // 원본 이미지를 캔버스에 그림
            canvas.drawBitmap(bitmap, 0, 0, null);

            // 얼굴 인식 및 홍채 블러 처리 후 빨간색 칠하기 추가
            if (faces != null && isIrisBlurringOn) {
                for (Face face : faces) {
                    // 얼굴 및 홍채 위치 기반 블러 처리 코드
                }
            }

            // 손가락 블러 처리 후 빨간색 칠하기
            if (poses != null && isFingerprintBlurringOn) {
                PoseLandmark leftIndex = poses.getPoseLandmark(PoseLandmark.LEFT_INDEX);
                PoseLandmark leftPinky = poses.getPoseLandmark(PoseLandmark.LEFT_PINKY);
                PoseLandmark leftWrist = poses.getPoseLandmark(PoseLandmark.LEFT_WRIST);

                PoseLandmark[] fingers = {leftIndex, leftPinky, leftWrist};

                Bitmap updatedBitmap = applyBlurToFingers(bitmap, fingers);  // 블러 및 빨간색 적용
                canvas.drawBitmap(updatedBitmap, 0, 0, null);  // 변경된 비트맵을 다시 그리기
            }
        }
    }
}
