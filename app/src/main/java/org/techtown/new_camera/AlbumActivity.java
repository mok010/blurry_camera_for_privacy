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
                                Pose poses = ImageProcessor.processInputImagePose(photoBitmap).get();
                                Bitmap blurredBitmap = photoBitmap.copy(Bitmap.Config.ARGB_8888, true);
                                if (isIrisBlurringOn && faces.size() > 0) {
                                    // 블러 처리

                                    for (Face face : faces) {
                                        float eyeRadius = face.getBoundingBox().width() * 0.03f;

                                        // 왼쪽 눈 블러 처리
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

                                        // 오른쪽 눈 블러 처리
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
                                    //수정 필요 포인트 시작점
                                    //1. 일단 지문 on,off 탐지후 조건 문 시작
                                    //2. 지문에 필요한 기능 수행 후 blurredBitmap에 블러된 이미지 재할당
                                    //블러링은 blurredBitmap = AlbumActivity.BitmapUtil.blurRegion(blurredBitmap, rightEyeRect);
                                    //처럼 수행하면 되는데, 블러링 사각형의 4좌표를 담은 rect 개체와 blurredBitmap을 넘겨주기만 하면 돤다.

                                }
                                // 눈동자 블러링이 완료된 후
                                if (isFingerprintBlurringOn) {
                                    // 왼손의 각 손가락 끝 좌표 (엄지, 검지, 중지, 약지, 새끼)
                                    PoseLandmark[] leftFingers = {
                                            poses.getPoseLandmark(PoseLandmark.LEFT_THUMB),
//                                            poses.getPoseLandmark(PoseLandmark.LEFT_INDEX),
//                                            poses.getPoseLandmark(PoseLandmark.LEFT_MIDDLE_FINGER_TIP),  // 중지 끝 좌표
//                                            poses.getPoseLandmark(PoseLandmark.LEFT_RING_FINGER_TIP),    // 약지 끝 좌표
//                                            poses.getPoseLandmark(PoseLandmark.LEFT_PINKY)
                                    };

                                    // 오른손의 각 손가락 끝 좌표 (엄지, 검지, 중지, 약지, 새끼)
                                    PoseLandmark[] rightFingers = {
                                            poses.getPoseLandmark(PoseLandmark.RIGHT_THUMB),
//                                          poses.getPoseLandmark(PoseLandmark.RIGHT_INDEX),
//                                          poses.getPoseLandmark(PoseLandmark.RIGHT_MIDDLE_FINGER_TIP), // 중지 끝 좌표
//                                          poses.getPoseLandmark(PoseLandmark.RIGHT_RING_FINGER_TIP),   // 약지 끝 좌표
//                                          poses.getPoseLandmark(PoseLandmark.RIGHT_PINKY)
                                    };

                                    // 각 손가락에 대해 블러링 적용
                                    blurredBitmap = applyBlurToFingers(blurredBitmap, leftFingers);
                                    blurredBitmap = applyBlurToFingers(blurredBitmap, rightFingers);
                                }


                                // 최종 블러 처리된 이미지를 UI에 적용
                                Bitmap finalBlurredBitmap = blurredBitmap;
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        imageView.setImageBitmap(finalBlurredBitmap);
                                    }
                                });

//                                    Bitmap finalBlurredBitmap = blurredBitmap;

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

    private Bitmap applyBlurToFingers(Bitmap bitmap, PoseLandmark[] fingers) {
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
                bitmap = AlbumActivity.BitmapUtil.blurRegion(bitmap, new Rect(leftX, topY, rightX, bottomY));
            }
        }
        return bitmap;
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

        public CustomView(Context context, Bitmap bitmap, List<Face> faces,Pose poses ,boolean isIrisBlurringOn) {
            super(context);
            this.bitmap = bitmap;
            this.faces = faces;
            this.isIrisBlurringOn = isIrisBlurringOn;
            this.poses = poses;

            this.paint = new Paint();
            paint.setColor(Color.BLACK);
            paint.setStyle(Paint.Style.FILL);
            paint.setAlpha(128);  // 50% 불투명도 설정
        }
//
//        @Override
//        protected void onDraw(Canvas canvas) {
//            super.onDraw(canvas);
//
//
//            // 화면 크기와 비트맵 크기를 구합니다.
//            int canvasWidth = canvas.getWidth();
//            int canvasHeight = canvas.getHeight();
//            int bitmapWidth = bitmap.getWidth();
//            int bitmapHeight = bitmap.getHeight();
//
//            // 스케일을 계산합니다.
//            float scale = Math.min((float) canvasWidth / bitmapWidth, (float) canvasHeight / bitmapHeight);
//
//            // 비트맵을 스케일과 위치를 적용하여 그립니다.
//            float scaledWidth = scale * bitmapWidth;
//            float scaledHeight = scale * bitmapHeight;
//            float left = (canvasWidth - scaledWidth) / 2;
//            float top = (canvasHeight - scaledHeight) / 2;
//
//            canvas.drawBitmap(bitmap, null, new Rect((int) left, (int) top, (int) (left + scaledWidth), (int) (top + scaledHeight)), null);
//
//
//            // 얼굴 인식 결과를 그립니다.
//
//
//            //손 인식결과를 그립니다.
//            PoseLandmark leftWrist = poses.getPoseLandmark(PoseLandmark.LEFT_WRIST);
//            PoseLandmark leftPinky = poses.getPoseLandmark(PoseLandmark.LEFT_PINKY);
//            PoseLandmark leftIndex = poses.getPoseLandmark(PoseLandmark.LEFT_INDEX);
//            PoseLandmark leftThumb = poses.getPoseLandmark(PoseLandmark.LEFT_THUMB);
//            PointF lw = Objects.requireNonNull(leftWrist).getPosition();
//            PointF lp = Objects.requireNonNull(leftPinky).getPosition();
//            PointF li = Objects.requireNonNull(leftIndex).getPosition();
//            PointF lt = Objects.requireNonNull(leftThumb).getPosition();
//
//            PoseLandmark rightWrist = poses.getPoseLandmark(PoseLandmark.RIGHT_WRIST);
//            PoseLandmark rightPinky = poses.getPoseLandmark(PoseLandmark.RIGHT_PINKY);
//            PoseLandmark rightIndex = poses.getPoseLandmark(PoseLandmark.RIGHT_INDEX);
//            PoseLandmark rightThumb = poses.getPoseLandmark(PoseLandmark.RIGHT_THUMB);
//            PointF rw = Objects.requireNonNull(rightWrist).getPosition();
//            PointF rp = Objects.requireNonNull(rightPinky).getPosition();
//            PointF ri = Objects.requireNonNull(rightIndex).getPosition();
//            PointF rt = Objects.requireNonNull(rightThumb).getPosition();
//
//
//
//            canvas.drawText("left_wrist",left+lw.x*scale,top+lw.y*scale,paint);
//            canvas.drawText("left_pinky",left+lp.x*scale,top+lp.y*scale,paint);
//            canvas.drawText("left_index",left+li.x*scale,top+li.y*scale,paint);
//            canvas.drawText("left_thumb",left+lt.x*scale,top+lt.y*scale,paint);
//
//            canvas.drawText("right_wrist",left+rw.x*scale,top+rw.y*scale,paint);
//            canvas.drawText("right_pinky",left+rp.x*scale,top+rp.y*scale,paint);
//            canvas.drawText("right_index",left+ri.x*scale,top+ri.y*scale,paint);
//            canvas.drawText("right_thumb",left+rt.x*scale,top+rt.y*scale,paint);
//
//
//            float leftRadius= (float) Math.sqrt(Math.pow((li.x-lw.x),2)+Math.pow((li.y-lw.y),2 ));
//            leftRadius=leftRadius*scale;
//            float rightRadius= (float) Math.sqrt(Math.pow((ri.x-rw.x),2)+Math.pow((ri.y-rw.y),2 ));
//            rightRadius=rightRadius*scale;
//
//            canvas.drawCircle(left+li.x*scale,top+li.y*scale,leftRadius,paint);
//            canvas.drawCircle(left+ri.x*scale,top+ri.y*scale,rightRadius,paint);
//        }
    }
}
