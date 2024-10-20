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

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

public class Camera_picture extends AppCompatActivity {

    ImageView imageView;
    Bitmap photoBitmap;
    int image_Val = 0;

    ProgressDialog dialog;
    ExecutorService executor = Executors.newSingleThreadExecutor();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cam_pic);

        imageView = findViewById(R.id.imageView);
        Button button3 = findViewById(R.id.button3);
        Button button = findViewById(R.id.button);

        Toast.makeText(this,
                "홍채 블러링: " + (FingerMainActivity.isIrisBlurringOn ? "ON" : "OFF") +
                        ", 지문 블러링: " + (FingerMainActivity.isFingerprintBlurringOn ? "ON" : "OFF"),
                Toast.LENGTH_LONG).show();

        // Intent에서 파일 URI 받기
        String photoUriString = getIntent().getStringExtra("photoUri");
        if (photoUriString != null) {
            Uri photoUri = Uri.parse(photoUriString);
            photoBitmap = getBitmapFromUri(photoUri);

            if (photoBitmap != null) {
                imageView.setImageBitmap(photoBitmap); // 이미지 뷰에 비트맵 설정
                image_Val = 1; // 이미지가 정상적으로 로드되었음을 표시
            } else {
                Toast.makeText(this, "이미지를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
            }

            button3.setOnClickListener(view -> {
                if (image_Val == 1) {
                    ProgressDialog dialog = new ProgressDialog(Camera_picture.this);
                    dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                    dialog.setMessage("보호중입니다...");
                    dialog.setCancelable(false);  // 다이얼로그를 취소할 수 없도록 설정
                    dialog.show();

                    AtomicReference<Bitmap> blurredBitmapRef = new AtomicReference<>(photoBitmap.copy(Bitmap.Config.ARGB_8888, true));

                    // 포즈 감지 및 블러링 처리
                    executor.execute(() -> {
                        try {
                            if (photoBitmap != null) {
                                List<Face> faces = ImageProcessor.processInputImage(photoBitmap, Camera_picture.this).get();
                                Pose pose = ImageProcessor.processInputImagePose(photoBitmap, Camera_picture.this).get();
                                Bitmap blurredBitmap = photoBitmap.copy(Bitmap.Config.ARGB_8888, true);

                                // 얼굴 블러 처리 (눈 중심)
                                if (FingerMainActivity.isIrisBlurringOn) {
                                    for (Face face : faces) {
                                        float eyeRadius = face.getBoundingBox().width() * 0.03f;  // 얼굴 크기에 따른 반경 설정
                                        FaceLandmark leftEye = face.getLandmark(FaceLandmark.LEFT_EYE);
                                        if (leftEye != null) {
                                            PointF leftEyePos = leftEye.getPosition();
                                            blurredBitmap = BitmapUtil.blurCircularRegion(Camera_picture.this, blurredBitmap, leftEyePos.x, leftEyePos.y, eyeRadius);
                                        }
                                        FaceLandmark rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE);
                                        if (rightEye != null) {
                                            PointF rightEyePos = rightEye.getPosition();
                                            blurredBitmap = BitmapUtil.blurCircularRegion(Camera_picture.this, blurredBitmap, rightEyePos.x, rightEyePos.y, eyeRadius);
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

                                    // 왼손 사각형 영역 블러 처리
                                    if (leftWrist != null && leftIndex != null) {
                                        Rect leftHandRect = getHandRectRegion(leftWrist, leftIndex);
                                        leftHandRect = clampRectToBitmap(leftHandRect, blurredBitmap);
                                        PointF leftIndexPos = leftIndex.getPosition();
                                        blurredBitmap = BitmapUtil.blurRectangularRegion(Camera_picture.this, blurredBitmap, leftHandRect, leftIndexPos);
                                    }
                                    if (leftWrist != null && leftPinky != null) {
                                        Rect leftHandRect = getHandRectRegion(leftWrist, leftPinky);
                                        leftHandRect = clampRectToBitmap(leftHandRect, blurredBitmap);
                                        PointF leftPinkyPos = leftPinky.getPosition();
                                        blurredBitmap = BitmapUtil.blurRectangularRegion(Camera_picture.this, blurredBitmap, leftHandRect, leftPinkyPos);
                                    }
                                    if (leftPinky != null && leftIndex != null) {
                                        Rect leftHandRect = getHandRectRegion(leftPinky, leftIndex);
                                        leftHandRect = clampRectToBitmap(leftHandRect, blurredBitmap);
                                        PointF leftIndexPos = leftIndex.getPosition();
                                        blurredBitmap = BitmapUtil.blurRectangularRegion(Camera_picture.this, blurredBitmap, leftHandRect, leftIndexPos);
                                    }

                                    // 오른손 사각형 영역 블러 처리
                                    if (rightWrist != null && rightIndex != null) {
                                        Rect rightHandRect = getHandRectRegion(rightWrist, rightIndex);
                                        rightHandRect = clampRectToBitmap(rightHandRect, blurredBitmap);
                                        PointF rightIndexPos = rightIndex.getPosition();
                                        blurredBitmap = BitmapUtil.blurRectangularRegion(Camera_picture.this, blurredBitmap, rightHandRect, rightIndexPos);
                                    }
                                    if (rightWrist != null && rightPinky != null) {
                                        Rect rightHandRect = getHandRectRegion(rightWrist, rightPinky);
                                        rightHandRect = clampRectToBitmap(rightHandRect, blurredBitmap);
                                        PointF rightPinkyPos = rightPinky.getPosition();
                                        blurredBitmap = BitmapUtil.blurRectangularRegion(Camera_picture.this, blurredBitmap, rightHandRect, rightPinkyPos);
                                    }
                                    if (rightPinky != null && rightIndex != null) {
                                        Rect rightHandRect = getHandRectRegion(rightPinky, rightIndex);
                                        rightHandRect = clampRectToBitmap(rightHandRect, blurredBitmap);
                                        PointF rightIndexPos = rightIndex.getPosition();
                                        blurredBitmap = BitmapUtil.blurRectangularRegion(Camera_picture.this, blurredBitmap, rightHandRect, rightIndexPos);
                                    }
                                }
                                blurredBitmapRef.set(blurredBitmap);

                                // 블러링 결과 저장
                                boolean saveSuccess = saveImageToGallery(blurredBitmap);
                                deleteTempFile(photoUri); // 임시 파일 삭제

                                runOnUiThread(() -> {
                                    imageView.setImageBitmap(photoBitmap); // 블러링 후 업데이트
                                    Toast.makeText(Camera_picture.this, "블러링 및 저장 완료", Toast.LENGTH_SHORT).show();
                                    dialog.dismiss();
                                });
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            runOnUiThread(() -> {
                                Toast.makeText(Camera_picture.this, "블러링에 실패했습니다.", Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                            });
                        }
                    });
                } else {
                    Toast.makeText(Camera_picture.this, "이미지를 선택하지 않았습니다.", Toast.LENGTH_SHORT).show();
                }
            });
            button.setOnClickListener(view -> {
                Intent intent = new Intent(Camera_picture.this, MainActivity.class);
                startActivity(intent);
            });
        }
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

    private Rect clampRectToBitmap(Rect rect, Bitmap bitmap) {
        int left = Math.max(0, rect.left);
        int top = Math.max(0, rect.top);
        int right = Math.min(bitmap.getWidth(), rect.right);
        int bottom = Math.min(bitmap.getHeight(), rect.bottom);
        return new Rect(left, top, right, bottom);
    }

    private void deleteTempFile(Uri fileUri) {
        if (fileUri != null && "file".equals(fileUri.getScheme())) {
            File file = new File(fileUri.getPath());
            if (file.exists()) {
                if (file.delete()) {
                    Log.d("Camera_picture", "임시 파일이 삭제되었습니다: " + fileUri.getPath());
                } else {
                    Log.e("Camera_picture", "임시 파일 삭제 실패: " + fileUri.getPath());
                }
            } else {
                Log.e("Camera_picture", "파일이 존재하지 않습니다: " + fileUri.getPath());
            }
        } else {
            Log.e("Camera_picture", "삭제할 파일 경로가 올바르지 않습니다: " + fileUri);
        }
    }

    // 이미지를 URI에서 비트맵으로 변환하는 메서드
    private Bitmap getBitmapFromUri(Uri uri) {
        try (ParcelFileDescriptor parcelFileDescriptor = getContentResolver().openFileDescriptor(uri, "r")) {
            FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
            return BitmapFactory.decodeFileDescriptor(fileDescriptor);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Bitmap을 저장 후 저장 성공 여부 반환
    private boolean saveImageToGallery(Bitmap bitmap) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, System.currentTimeMillis() + ".jpg");
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);

        Uri imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        try (OutputStream out = getContentResolver().openOutputStream(imageUri)) {
            if (out != null) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                out.flush();
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
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
    protected void onDestroy() {
        super.onDestroy();
        // ProgressDialog 종료
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
        // 비트맵 메모리 해제
        if (photoBitmap != null && !photoBitmap.isRecycled()) {
            photoBitmap.recycle();
            photoBitmap = null;
        }
        executor.shutdownNow();
    }
}
