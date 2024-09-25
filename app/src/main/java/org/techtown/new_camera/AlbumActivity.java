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

                            // 왼손가락 블러 처리
                            if (isFingerprintBlurringOn) {
                                // 살색 범위 설정 (Hue, Saturation, Value의 범위 설정)
                                float hueRange = 10; // 예: ±10도 허용 : 색조범위
                                float saturationRange = 0.2f; // ±20% : 채도 범위
                                float valueRange = 0.2f; // ±20% : 명도 범위
                                float bitmapWidth = photoBitmap.getWidth();
                                float bitmapHeight = photoBitmap.getHeight();

                                int newColor = Color.parseColor("#FF0000");///수정 테스트 컬러

                                PoseLandmark leftIndex = poses.getPoseLandmark(PoseLandmark.LEFT_INDEX);
                                PoseLandmark leftPinky = poses.getPoseLandmark(PoseLandmark.LEFT_PINKY);
                                PoseLandmark leftWrist = poses.getPoseLandmark(PoseLandmark.LEFT_WRIST);

                                PointF indexPos_l = leftIndex.getPosition();
                                PointF pinkyPos_l = leftPinky.getPosition();
                                PointF wristPos_l = leftWrist.getPosition();


                                if (indexPos_l.x < 0 || indexPos_l.x >= bitmapWidth || indexPos_l.y < 0 || indexPos_l.y >= bitmapHeight ||
                                        pinkyPos_l.x < 0 || pinkyPos_l.x >= bitmapWidth || pinkyPos_l.y < 0 || pinkyPos_l.y >= bitmapHeight ||
                                        wristPos_l.x < 0 || wristPos_l.x >= bitmapWidth || wristPos_l.y < 0 || wristPos_l.y >= bitmapHeight) {
                                    // 왼손이 없을 경우 처리 생략
                                } else{
                                    // 손목을 기준으로 반지름을 계산 (검지와 손목, 새끼와 손목의 거리)
                                    float indexRadius_l = (float) Math.sqrt(Math.pow(indexPos_l.x - wristPos_l.x, 2) + Math.pow(indexPos_l.y - wristPos_l.y, 2));
                                    float pinkyRadius_l = (float) Math.sqrt(Math.pow(pinkyPos_l.x - wristPos_l.x, 2) + Math.pow(pinkyPos_l.y - wristPos_l.y, 2));

                                    // 두 영역 생성 (손목에서 검지, 새끼 거리만큼의 원)
                                    Rect indexRect_l = new Rect(
                                            (int) (indexPos_l.x - indexRadius_l * 1.5f),
                                            (int) (indexPos_l.y - indexRadius_l * 1.5f),
                                            (int) (indexPos_l.x + indexRadius_l * 1.5f),
                                            (int) (indexPos_l.y + indexRadius_l * 1.5f)
                                    );
                                    Rect pinkyRect_l = new Rect(
                                            (int) (pinkyPos_l.x - pinkyRadius_l * 1.5f),
                                            (int) (pinkyPos_l.y - pinkyRadius_l * 1.5f),
                                            (int) (pinkyPos_l.x + pinkyRadius_l * 1.5f),
                                            (int) (pinkyPos_l.y + pinkyRadius_l * 1.5f)
                                    );

                                    // 살색 판단 기준을 설정 (예: 검지 색상에서 유사한 범위의 색상)
                                    int indexColor_l = photoBitmap.getPixel((int) indexPos_l.x, (int) indexPos_l.y);
                                    float[] wristHsv_l = new float[3];
                                    Color.colorToHSV(indexColor_l, wristHsv_l);

                                    // 블러 처리: 살색인 부분만 블러 처리
                                    for (int x = Math.max(0, indexRect_l.left); x < Math.min(photoBitmap.getWidth(), indexRect_l.right); x++) {
                                        for (int y = Math.max(0, indexRect_l.top); y < Math.min(photoBitmap.getHeight(), indexRect_l.bottom); y++) {
                                            int pixelColor_l = photoBitmap.getPixel(x, y);
                                            float[] pixelHsv_l = new float[3];
                                            Color.colorToHSV(pixelColor_l, pixelHsv_l);

                                            // 살색으로 판정되는 경우 블러 처리
                                            if (Math.abs(pixelHsv_l[0] - wristHsv_l[0]) <= hueRange &&
                                                    Math.abs(pixelHsv_l[1] - wristHsv_l[1]) <= saturationRange &&
                                                    Math.abs(pixelHsv_l[2] - wristHsv_l[2]) <= valueRange) {

                                                // (임시)블러링 영역 (인덱스 손가락)
//                                                blurredBitmap = AlbumActivity.BitmapUtil.blurRegion(blurredBitmap, new Rect(x, y, x + 1, y + 1));
                                                blurredBitmap = changePixelColor(blurredBitmap, x, y, newColor);
                                            }
                                        }
                                    }
                                    for (int x = Math.max(0, pinkyRect_l.left); x < Math.min(photoBitmap.getWidth(), pinkyRect_l.right); x++) {
                                        for (int y = Math.max(0, pinkyRect_l.top); y < Math.min(photoBitmap.getHeight(), pinkyRect_l.bottom); y++) {
//
                                            int pixelColor_l = photoBitmap.getPixel(x, y);
                                            float[] pixelHsv_l = new float[3];
                                            Color.colorToHSV(pixelColor_l, pixelHsv_l);

                                            // 살색으로 판정되는 경우 블러 처리
                                            if (Math.abs(pixelHsv_l[0] - wristHsv_l[0]) <= hueRange &&
                                                    Math.abs(pixelHsv_l[1] - wristHsv_l[1]) <= saturationRange &&
                                                    Math.abs(pixelHsv_l[2] - wristHsv_l[2]) <= valueRange) {

                                                // (임시)블러링 영역 (새끼 손가락)
//                                                blurredBitmap = AlbumActivity.BitmapUtil.blurRegion(blurredBitmap, new Rect(x, y, x + 1, y + 1));
                                                blurredBitmap = changePixelColor(blurredBitmap, x, y, newColor);
                                            }
                                        }
                                    }
                                }

                                PoseLandmark rightIndex = poses.getPoseLandmark(PoseLandmark.RIGHT_INDEX);
                                PoseLandmark rightPinky = poses.getPoseLandmark(PoseLandmark.RIGHT_PINKY);
                                PoseLandmark rightWrist = poses.getPoseLandmark(PoseLandmark.RIGHT_WRIST);

                                PointF indexPos_r = rightIndex.getPosition();
                                PointF pinkyPos_r = rightPinky.getPosition();
                                PointF wristPos_r = rightWrist.getPosition();

                                if (indexPos_r.x < 0 || indexPos_r.x >= bitmapWidth || indexPos_r.y < 0 || indexPos_r.y >= bitmapHeight ||
                                        pinkyPos_r.x < 0 || pinkyPos_r.x >= bitmapWidth || pinkyPos_r.y < 0 || pinkyPos_r.y >= bitmapHeight ||
                                        wristPos_r.x < 0 || wristPos_r.x >= bitmapWidth || wristPos_r.y < 0 || wristPos_r.y >= bitmapHeight) {
                                    // 오른손이 없을 경우 처리 생략
                                } else {
                                    // 손목을 기준으로 반지름을 계산 (검지와 손목, 새끼와 손목의 거리)
                                    float indexRadius_r = (float) Math.sqrt(Math.pow(indexPos_r.x - wristPos_r.x, 2) + Math.pow(indexPos_r.y - wristPos_r.y, 2));
                                    float pinkyRadius_r = (float) Math.sqrt(Math.pow(pinkyPos_r.x - wristPos_r.x, 2) + Math.pow(pinkyPos_r.y - wristPos_r.y, 2));

                                    // 두 영역 생성 (손목에서 검지, 새끼 거리만큼의 원)
                                    Rect indexRect_r = new Rect(
                                            (int) (indexPos_r.x - indexRadius_r * 1.5f),
                                            (int) (indexPos_r.y - indexRadius_r * 1.5f),
                                            (int) (indexPos_r.x + indexRadius_r * 1.5f),
                                            (int) (indexPos_r.y + indexRadius_r * 1.5f)
                                    );
                                    Rect pinkyRect_r = new Rect(
                                            (int) (pinkyPos_r.x - pinkyRadius_r * 1.5f),
                                            (int) (pinkyPos_r.y - pinkyRadius_r * 1.5f),
                                            (int) (pinkyPos_r.x + pinkyRadius_r * 1.5f),
                                            (int) (pinkyPos_r.y + pinkyRadius_r * 1.5f)
                                    );

                                    /// 살색 판단 기준을 설정 (예: 검지 색상에서 유사한 범위의 색상)
                                    int indexColor_r = photoBitmap.getPixel((int) indexPos_r.x, (int) indexPos_r.y);
                                    float[] wristHsv_r = new float[3];
                                    Color.colorToHSV(indexColor_r, wristHsv_r);

                                    // 블러 처리: 살색인 부분만 블러 처리
                                    for (int x = Math.max(0, indexRect_r.left); x < Math.min(photoBitmap.getWidth(), indexRect_r.right); x++) {
                                        for (int y = Math.max(0, indexRect_r.top); y < Math.min(photoBitmap.getHeight(), indexRect_r.bottom); y++) {

                                            int pixelColor_r = photoBitmap.getPixel(x, y);
                                            float[] pixelHsv_r = new float[3];
                                            Color.colorToHSV(pixelColor_r, pixelHsv_r);

                                            // 살색으로 판정되는 경우 블러 처리
                                            if (Math.abs(pixelHsv_r[0] - wristHsv_r[0]) <= hueRange &&
                                                    Math.abs(pixelHsv_r[1] - wristHsv_r[1]) <= saturationRange &&
                                                    Math.abs(pixelHsv_r[2] - wristHsv_r[2]) <= valueRange) {

                                                // (임시)블러링 영역 (인덱스 손가락)
//                                                blurredBitmap = AlbumActivity.BitmapUtil.blurRegion(blurredBitmap, new Rect(x, y, x + 1, y + 1));
                                                blurredBitmap = changePixelColor(blurredBitmap, x, y, newColor);
                                            }
                                        }
                                    }
                                    for (int x = Math.max(0, pinkyRect_r.left); x < Math.min(photoBitmap.getWidth(), pinkyRect_r.right); x++) {
                                        for (int y = Math.max(0, pinkyRect_r.top); y < Math.min(photoBitmap.getHeight(), pinkyRect_r.bottom); y++) {

                                            int pixelColor_r = photoBitmap.getPixel(x, y);
                                            float[] pixelHsv_r = new float[3];
                                            Color.colorToHSV(pixelColor_r, pixelHsv_r);

                                            // 살색으로 판정되는 경우 블러 처리
                                            if (Math.abs(pixelHsv_r[0] - wristHsv_r[0]) <= hueRange &&
                                                    Math.abs(pixelHsv_r[1] - wristHsv_r[1]) <= saturationRange &&
                                                    Math.abs(pixelHsv_r[2] - wristHsv_r[2]) <= valueRange) {

                                                // (임시)블러링 영역 (새끼 손가락)
//                                                blurredBitmap = AlbumActivity.BitmapUtil.blurRegion(blurredBitmap, new Rect(x, y, x + 1, y + 1));
                                                blurredBitmap = changePixelColor(blurredBitmap, x, y, newColor);
                                            }
                                        }
                                    }
                                }
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

    private boolean isBetweenWristAndFinger(int x, int y, PointF wristPos, PointF fingerPos) {
        // 손목과 손가락 사이의 거리 계산 (Euclidean Distance)
        float distance = (float) Math.sqrt(Math.pow(fingerPos.x - wristPos.x, 2) + Math.pow(fingerPos.y - wristPos.y, 2));

        // 원래 중점 구하기
        float midX = (wristPos.x + fingerPos.x) / 2;
        float midY = (wristPos.y + fingerPos.y) / 2;

        // 손목 쪽으로 0.5배 당겨진 중점 계산
        float shiftFactor = 0.5f;
        float shiftedMidX = midX + (wristPos.x - midX) * shiftFactor;
        float shiftedMidY = midY + (wristPos.y - midY) * shiftFactor;

        // 2배 크기의 정사각형 영역의 반지름
        float halfSide = distance * 2f / 2;

        // 당겨진 중점을 기준으로 정사각형 범위 계산
        float left = shiftedMidX - halfSide;
        float right = shiftedMidX + halfSide;
        float top = shiftedMidY - halfSide;
        float bottom = shiftedMidY + halfSide;

        // 주어진 좌표 (x, y)가 1.5배 정사각형 범위 안에 있는지 확인
        return x >= left && x <= right && y >= top && y <= bottom;
    }

    public static Bitmap changePixelColor(Bitmap bitmap, int x, int y, int color) {
        // 비트맵 크기 내의 좌표인지 확인
        if (x >= 0 && x < bitmap.getWidth() && y >= 0 && y < bitmap.getHeight()) {
            // 해당 좌표의 픽셀 색상을 변경
            bitmap.setPixel(x, y, color);
        } else {
            // 좌표가 비트맵 범위를 벗어난 경우 처리
            throw new IllegalArgumentException("The pixel coordinates are out of the bitmap bounds.");
        }
        return bitmap;
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
}
