package org.techtown.new_camera;

import android.app.ProgressDialog;
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
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceLandmark;
import com.google.mlkit.vision.pose.Pose;
import com.google.mlkit.vision.pose.PoseLandmark;

import java.io.FileDescriptor;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AlbumActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "BlurPrefs";
    private static final String KEY_IRIS_BLURRING = "iris_blurring";
    private static final String KEY_FINGERPRINT_BLURRING = "fingerprint_blurring";
    private boolean isIrisBlurringOn;
    private boolean isFingerprintBlurringOn;

    ImageView imageView;
    FrameLayout frameLayout;
    Bitmap photoBitmap;
    int image_Val = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_album);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        isIrisBlurringOn = prefs.getBoolean(KEY_IRIS_BLURRING, true);
        isFingerprintBlurringOn = prefs.getBoolean(KEY_FINGERPRINT_BLURRING, true);

        Button button = findViewById(R.id.button);
        button.setOnClickListener(view -> finish());

        Button button2 = findViewById(R.id.button2);
        button2.setOnClickListener(view -> {
            frameLayout.removeAllViews();
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            startActivityForResult(intent, 1);
        });

        imageView = findViewById(R.id.imageView);
        frameLayout = findViewById(R.id.frameLayout);

        Button button3 = findViewById(R.id.button3);
        ExecutorService executor = Executors.newSingleThreadExecutor();

        button3.setOnClickListener(view -> {
            if (image_Val == 1) {
                ProgressDialog dialog = new ProgressDialog(AlbumActivity.this);
                dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                dialog.setMessage("보호중입니다...");
                dialog.show();

                // 비동기 처리
                CompletableFuture<List<Face>> faceFuture = ImageProcessor.processInputImage(photoBitmap);
                CompletableFuture<Pose> poseFuture = ImageProcessor.processInputImagePose(photoBitmap);

                faceFuture.thenCombine(poseFuture, (faces, poses) -> {
                    Bitmap blurredBitmap = photoBitmap.copy(Bitmap.Config.ARGB_8888, true);

                    if (isIrisBlurringOn && faces.size() > 0) {
                        // 얼굴 블러 처리
                        for (Face face : faces) {
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
                                blurredBitmap = BitmapUtil.blurRegion(blurredBitmap, leftEyeRect);
                            }

                            FaceLandmark rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE);
                            if (rightEye != null) {
                                PointF rightEyePos = rightEye.getPosition();
                                Rect rightEyeRect = new Rect(
                                        (int) (rightEyePos.x - eyeRadius),
                                        (int) (rightEyePos.y - eyeRadius),
                                        (int) (rightEyePos.x + eyeRadius),
                                        (int) (rightEyePos.y + eyeRadius)
                                );
                                blurredBitmap = BitmapUtil.blurRegion(blurredBitmap, rightEyeRect);
                            }
                        }
                    }

                    // 손가락 블러 처리
                    if (isFingerprintBlurringOn) {
                        // 왼손
                        PoseLandmark leftIndex = poses.getPoseLandmark(PoseLandmark.LEFT_INDEX);
                        PoseLandmark leftPinky = poses.getPoseLandmark(PoseLandmark.LEFT_PINKY);
                        PoseLandmark leftWrist = poses.getPoseLandmark(PoseLandmark.LEFT_WRIST);

                        // 오른손
                        PoseLandmark rightIndex = poses.getPoseLandmark(PoseLandmark.RIGHT_INDEX);
                        PoseLandmark rightPinky = poses.getPoseLandmark(PoseLandmark.RIGHT_PINKY);
                        PoseLandmark rightWrist = poses.getPoseLandmark(PoseLandmark.RIGHT_WRIST);

                        // 손가락 블러 처리 로직 추가
                    }

                    return blurredBitmap;
                }).thenAccept(blurredBitmap -> {
                    runOnUiThread(() -> {
                        imageView.setImageBitmap(blurredBitmap);
                        dialog.dismiss();
                        Toast.makeText(AlbumActivity.this, "변환이 완료되었습니다:)", Toast.LENGTH_SHORT).show();
                    });
                }).exceptionally(e -> {
                    // 에러 처리
                    e.printStackTrace();
                    runOnUiThread(dialog::dismiss);
                    return null;
                });

                image_Val = 0;
            } else {
                Toast.makeText(AlbumActivity.this, "이미지를 선택하지 않았습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1 && resultCode == RESULT_OK) {
            Uri uri = data.getData();
            Log.d("이미지주소", String.valueOf(uri));
            photoBitmap = getBitmapFromUri(uri);
            imageView.setImageBitmap(photoBitmap);
            image_Val = 1;
        }
    }

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
            // (블러 처리 로직)
            return bitmap; // 블러 처리 로직은 필요에 따라 추가
        }
    }
}
