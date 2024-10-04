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
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceLandmark;
import com.google.mlkit.vision.pose.Pose;
import com.google.mlkit.vision.pose.PoseLandmark;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AlbumActivity extends AppCompatActivity {

    ImageView imageView;
    Bitmap photoBitmap;
    int image_Val = 0;

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
        ExecutorService executor = Executors.newSingleThreadExecutor();

        button3.setOnClickListener(view -> {
            if (image_Val == 1) {
                ProgressDialog dialog = new ProgressDialog(AlbumActivity.this);
                dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                dialog.setMessage("보호중입니다...");
                dialog.show();

                // 포즈 감지 및 블러링 처리
                executor.execute(() -> {
                    try {
                        List<Face> faces = ImageProcessor.processInputImage(photoBitmap, AlbumActivity.this).get();
                        Pose pose = ImageProcessor.processInputImagePose(photoBitmap, AlbumActivity.this).get();

                        Bitmap blurredBitmap = photoBitmap.copy(Bitmap.Config.ARGB_8888, true);

                        // 얼굴 블러 처리 (눈 중심)
                        if (FingerMainActivity.isIrisBlurringOn) {
                            for (Face face : faces) {
                                float eyeRadius = face.getBoundingBox().width() * 0.03f;  // 얼굴 크기에 따른 반경 설정

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

                        if(FingerMainActivity.isFingerprintBlurringOn){
                            PoseLandmark leftWrist = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST);
                            PoseLandmark leftIndex = pose.getPoseLandmark(PoseLandmark.LEFT_INDEX);
                            PoseLandmark leftPinky = pose.getPoseLandmark(PoseLandmark.LEFT_PINKY);


                            PoseLandmark rightWrist = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST);
                            PoseLandmark rightIndex = pose.getPoseLandmark(PoseLandmark.RIGHT_INDEX);
                            PoseLandmark rightPinky = pose.getPoseLandmark(PoseLandmark.RIGHT_PINKY);

// 왼손 사각형 영역 블러 처리
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
                            if(leftPinky != null&& leftIndex != null){
                                Rect leftHandRect = getHandRectRegion(leftPinky, leftIndex);
                                PointF leftIndexPos = leftIndex.getPosition();
                                blurredBitmap = BitmapUtil.blurRectangularRegion(AlbumActivity.this, blurredBitmap, leftHandRect, leftIndexPos);
                            }

// 오른손 사각형 영역 블러 처리
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
                            if(rightPinky != null&& rightIndex != null){
                                Rect rightHandRect = getHandRectRegion(rightPinky, rightIndex);
                                PointF rightIndexPos = rightIndex.getPosition();
                                blurredBitmap = BitmapUtil.blurRectangularRegion(AlbumActivity.this, blurredBitmap, rightHandRect, rightIndexPos);
                            }
                        }

                        // 블러 처리 후 앨범에 저장
                        saveImageToGallery(blurredBitmap);  // 블러 처리 후 이미지 저장

                        // UI 업데이트
                        Bitmap finalBlurredBitmap = blurredBitmap;
                        runOnUiThread(() -> {
                            imageView.setImageBitmap(finalBlurredBitmap);
                            dialog.dismiss();
                            Toast.makeText(AlbumActivity.this, "변환 및 저장이 완료되었습니다:)", Toast.LENGTH_SHORT).show();
                        });

                    } catch (Exception e) {
                        e.printStackTrace();
                        runOnUiThread(dialog::dismiss);
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
            Log.d("이미지주소", String.valueOf(uri));
            photoBitmap = getBitmapFromUri(uri);
            imageView.setImageBitmap(photoBitmap);
            image_Val = 1;
        }
    }



    // 이미지를 URI에서 비트맵으로 변환하는 메서드
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

    // 블러링된 이미지를 앨범에 저장하는 메서드 추가
    private void saveImageToGallery(Bitmap bitmap) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, System.currentTimeMillis() + ".jpg");  // 파일명
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);  // 앨범 경로

        Uri imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        try (OutputStream out = getContentResolver().openOutputStream(imageUri)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);  // JPEG로 저장
            Toast.makeText(this, "이미지가 앨범에 저장되었습니다.", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "이미지 저장에 실패했습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    private Rect getHandRectRegion(PoseLandmark wrist, PoseLandmark index) {
        // 손목에서 검지까지의 거리 계산
        float distance = (float) Math.sqrt(
                Math.pow(index.getPosition().x - wrist.getPosition().x, 2) +
                        Math.pow(index.getPosition().y - wrist.getPosition().y, 2)
        );

        // 검지 좌표를 사각형의 중심으로 설정
        float centerX = index.getPosition().x;
        float centerY = index.getPosition().y;

        // 거리의 2배를 한 변으로 하는 사각형 계산
        float halfSide =  (distance ); // 손목까지의 거리의 2배가 한 변이므로, halfSide는 거리 그대로 사용

        // 사각형의 좌상단과 우하단 좌표 계산
        float left = centerX - halfSide;
        float top = centerY - halfSide;
        float right = centerX + halfSide;
        float bottom = centerY + halfSide;

        // 정사각형 Rect 반환
        return new Rect((int)left, (int)top, (int)right, (int)bottom);
    }
}



