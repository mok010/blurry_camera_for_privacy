package org.techtown.new_camera;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PointF;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceLandmark;
import com.google.mlkit.vision.pose.Pose;
import com.google.mlkit.vision.pose.PoseLandmark;

import org.techtown.new_camera.BitmapUtil;
import org.techtown.new_camera.FingerMainActivity;
import org.techtown.new_camera.ImageProcessor;

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AlbumActivity extends AppCompatActivity {

    ImageView imageView;
    Bitmap photoBitmap;
    Uri originalImageUri;
    int image_Val = 0;
    ExecutorService executor = Executors.newSingleThreadExecutor();
    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_album);

        Button button2 = findViewById(R.id.button2);
        button2.setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            startActivityForResult(intent, 1);
        });

        imageView = findViewById(R.id.imageView);


        Button button3 = findViewById(R.id.button3);
        button3.setOnClickListener(view -> {
            if (image_Val == 1) {
                // 터치 이벤트 비활성화
                getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);

                // ProgressDialog 추가
                progressDialog = new ProgressDialog(AlbumActivity.this);
                progressDialog.setMessage("보호중입니다...");
                progressDialog.setCancelable(false);
                progressDialog.show();

                executor.execute(() -> {
                    try {
                        List<Face> faces = ImageProcessor.processInputImage(photoBitmap, AlbumActivity.this).get();
                        Pose pose = ImageProcessor.processInputImagePose(photoBitmap, AlbumActivity.this).get();

                        Bitmap blurredBitmap = photoBitmap.copy(Bitmap.Config.ARGB_8888, true);

                        if (FingerMainActivity.isIrisBlurringOn) {
                            for (Face face : faces) {
                                float eyeRadius = face.getBoundingBox().width() * 0.03f;

                                FaceLandmark leftEye = face.getLandmark(FaceLandmark.LEFT_EYE);
                                if (leftEye != null) {
                                    PointF leftEyePos = leftEye.getPosition();
                                    blurredBitmap = BitmapUtil.blurCircularRegion(AlbumActivity.this, blurredBitmap, leftEyePos.x, leftEyePos.y, eyeRadius);
                                }

                                FaceLandmark rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE);
                                if (rightEye != null) {
                                    PointF rightEyePos = rightEye.getPosition();
                                    blurredBitmap = BitmapUtil.blurCircularRegion(AlbumActivity.this, blurredBitmap, rightEyePos.x, rightEyePos.y, eyeRadius);
                                }
                            }
                        }

                        if (FingerMainActivity.isFingerprintBlurringOn) {
                            PoseLandmark leftWrist = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST);
                            PoseLandmark leftIndex = pose.getPoseLandmark(PoseLandmark.LEFT_INDEX);
                            PoseLandmark leftPinky = pose.getPoseLandmark(PoseLandmark.LEFT_PINKY);

                            PoseLandmark rightWrist = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST);
                            PoseLandmark rightIndex = pose.getPoseLandmark(PoseLandmark.RIGHT_INDEX);
                            PoseLandmark rightPinky = pose.getPoseLandmark(PoseLandmark.RIGHT_PINKY);

                            if (leftWrist != null && leftIndex != null) {
                                Rect leftHandRect = getHandRectRegion(leftWrist, leftIndex);
                                PointF leftIndexPos = leftIndex.getPosition();
                                blurredBitmap = BitmapUtil.blurRectangularRegion(AlbumActivity.this, blurredBitmap, leftHandRect, leftIndexPos);
                            }
                            if (leftWrist != null && leftPinky != null) {
                                Rect leftHandRect = getHandRectRegion(leftWrist, leftPinky);
                                PointF leftPinkyPos = leftPinky.getPosition();
                                blurredBitmap = BitmapUtil.blurRectangularRegion(AlbumActivity.this, blurredBitmap, leftHandRect, leftPinkyPos);
                            }
                            if (leftPinky != null && leftIndex != null) {
                                Rect leftHandRect = getHandRectRegion(leftPinky, leftIndex);
                                PointF leftIndexPos = leftIndex.getPosition();
                                blurredBitmap = BitmapUtil.blurRectangularRegion(AlbumActivity.this, blurredBitmap, leftHandRect, leftIndexPos);
                            }

                            if (rightWrist != null && rightIndex != null) {
                                Rect rightHandRect = getHandRectRegion(rightWrist, rightIndex);
                                PointF rightIndexPos = rightIndex.getPosition();
                                blurredBitmap = BitmapUtil.blurRectangularRegion(AlbumActivity.this, blurredBitmap, rightHandRect, rightIndexPos);
                            }
                            if (rightWrist != null && rightPinky != null) {
                                Rect rightHandRect = getHandRectRegion(rightWrist, rightPinky);
                                PointF rightPinkyPos = rightPinky.getPosition();
                                blurredBitmap = BitmapUtil.blurRectangularRegion(AlbumActivity.this, blurredBitmap, rightHandRect, rightPinkyPos);
                            }
                            if (rightPinky != null && rightIndex != null) {
                                Rect rightHandRect = getHandRectRegion(rightPinky, rightIndex);
                                PointF rightIndexPos = rightIndex.getPosition();
                                blurredBitmap = BitmapUtil.blurRectangularRegion(AlbumActivity.this, blurredBitmap, rightHandRect, rightIndexPos);
                            }
                        }

                        Uri savedUri = saveImageToGallery(blurredBitmap);
                        if (savedUri != null && originalImageUri != null) {
                            deleteTempFile(originalImageUri);
                        }

                        Bitmap finalBlurredBitmap = blurredBitmap;
                        runOnUiThread(() -> {
                            imageView.setImageBitmap(finalBlurredBitmap);
                            // 터치 이벤트 활성화
                            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                            if (progressDialog != null && progressDialog.isShowing()) {
                                progressDialog.dismiss();
                            }
                            Toast.makeText(AlbumActivity.this, "변환 및 저장이 완료되었습니다:)", Toast.LENGTH_SHORT).show();
                        });

                    } catch (Exception e) {
                        e.printStackTrace();
                        runOnUiThread(() -> {
                            // 터치 이벤트 활성화
                            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                            if (progressDialog != null && progressDialog.isShowing()) {
                                progressDialog.dismiss();
                            }
                        });
                    }
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
            originalImageUri = uri;
            Log.d("이미지주소", String.valueOf(uri));
            photoBitmap = getBitmapFromUri(uri);
            imageView.setImageBitmap(photoBitmap);
            image_Val = 1;
        }
    }

    private Bitmap getBitmapFromUri(Uri uri) {
        try (ParcelFileDescriptor parcelFileDescriptor = getContentResolver().openFileDescriptor(uri, "r")) {
            FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
            return BitmapFactory.decodeFileDescriptor(fileDescriptor);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private Uri saveImageToGallery(Bitmap bitmap) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, System.currentTimeMillis() + ".jpg");
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);

        Uri imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        try (OutputStream out = getContentResolver().openOutputStream(imageUri)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            Toast.makeText(this, "이미지가 앨범에 저장되었습니다.", Toast.LENGTH_SHORT).show();
            return imageUri;
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "이미지 저장에 실패했습니다.", Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    private void deleteTempFile(Uri fileUri) {
        if (fileUri != null && "file".equals(fileUri.getScheme())) {
            File file = new File(fileUri.getPath());
            if (file.exists()) {
                if (file.delete()) {
                    Log.d("AlbumActivity", "임시 파일이 삭제되었습니다: " + fileUri.getPath());
                } else {
                    Log.e("AlbumActivity", "임시 파일 삭제 실패: " + fileUri.getPath());
                }
            } else {
                Log.e("AlbumActivity", "파일이 존재하지 않습니다: " + fileUri.getPath());
            }
        } else {
            Log.e("AlbumActivity", "삭제할 파일 경로가 올바르지 않습니다: " + fileUri);
        }
    }

    private Rect getHandRectRegion(PoseLandmark wrist, PoseLandmark index) {
        float distance = (float) Math.sqrt(
                Math.pow(index.getPosition().x - wrist.getPosition().x, 2) +
                        Math.pow(index.getPosition().y - wrist.getPosition().y, 2)
        );

        float centerX = index.getPosition().x;
        float centerY = index.getPosition().y;
        float halfSide = distance;

        float left = centerX - halfSide;
        float top = centerY - halfSide;
        float right = centerX + halfSide;
        float bottom = centerY + halfSide;

        return new Rect((int) left, (int) top, (int) right, (int) bottom);
    }
}
