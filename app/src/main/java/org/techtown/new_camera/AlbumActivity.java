package org.techtown.new_camera;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PointF;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
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
import java.util.List;
import java.util.concurrent.CompletableFuture;
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

                // 얼굴 감지 및 블러링 처리
                CompletableFuture<List<Face>> faceFuture = ImageProcessor.processInputImage(photoBitmap, AlbumActivity.this);
                // 포즈 감지 및 블러링 처리
                CompletableFuture<Pose> poseFuture = ImageProcessor.processInputImagePose(photoBitmap, AlbumActivity.this);

                // 얼굴과 포즈 모두에 블러링을 처리한 최종 이미지
                faceFuture.thenCombine(poseFuture, (faces, pose) -> {
                    // 얼굴 및 포즈 감지 결과 디버깅
                    if (faces == null || faces.isEmpty()) {
                        Toast.makeText(AlbumActivity.this, "얼굴이 감지되지 않았습니다.", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(AlbumActivity.this, "감지된 얼굴 수: " + faces.size(), Toast.LENGTH_SHORT).show();
                    }

                    // 포즈 감지 결과 확인
                    if (pose == null || pose.getAllPoseLandmarks().isEmpty()) {
                        Toast.makeText(AlbumActivity.this, "포즈가 감지되지 않았습니다.", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(AlbumActivity.this, "감지된 포즈 랜드마크 수: " + pose.getAllPoseLandmarks().size(), Toast.LENGTH_SHORT).show();
                    }

                    Bitmap faceBlurredBitmap = applyFaceBlur(photoBitmap, faces);
                    return applyPoseBlur(faceBlurredBitmap, pose);  // 얼굴 블러링 후 포즈 블러링
                }).thenAccept(blurredBitmap -> {
                    runOnUiThread(() -> {
                        imageView.setImageBitmap(blurredBitmap);
                        dialog.dismiss();
                        Toast.makeText(AlbumActivity.this, "변환이 완료되었습니다:)", Toast.LENGTH_SHORT).show();
                    });
                }).exceptionally(e -> {
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

    // 얼굴 블러 처리 메서드
    private Bitmap applyFaceBlur(Bitmap bitmap, List<Face> faces) {
        Bitmap blurredBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);

        // 얼굴 목록 순회하며 블러 처리
        for (Face face : faces) {
            // 왼쪽 눈의 좌표 확인 및 로그 출력
            if (face.getLandmark(FaceLandmark.LEFT_EYE) != null) {
                PointF leftEyePos = face.getLandmark(FaceLandmark.LEFT_EYE).getPosition();
                Log.d("FaceDetection", "Left eye position: " + leftEyePos);  // 디버깅을 위한 로그
                Rect leftEyeRect = new Rect((int) (leftEyePos.x - 20), (int) (leftEyePos.y - 20),
                        (int) (leftEyePos.x + 20), (int) (leftEyePos.y + 20));
                blurredBitmap = BitmapUtil.blurRegion(this, blurredBitmap, leftEyeRect);
            }

            // 오른쪽 눈의 좌표 확인 및 로그 출력
            if (face.getLandmark(FaceLandmark.RIGHT_EYE) != null) {
                PointF rightEyePos = face.getLandmark(FaceLandmark.RIGHT_EYE).getPosition();
                Log.d("FaceDetection", "Right eye position: " + rightEyePos);  // 디버깅을 위한 로그
                Rect rightEyeRect = new Rect((int) (rightEyePos.x - 20), (int) (rightEyePos.y - 20),
                        (int) (rightEyePos.x + 20), (int) (rightEyePos.y + 20));
                blurredBitmap = BitmapUtil.blurRegion(this, blurredBitmap, rightEyeRect);
            }
        }

        return blurredBitmap;  // 블러 처리된 비트맵 반환
    }

    // 포즈 블러 처리 메서드
    private Bitmap applyPoseBlur(Bitmap bitmap, Pose pose) {
        Bitmap blurredBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);

        // 포즈 랜드마크에 블러 처리
        PoseLandmark leftWrist = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST);
        PoseLandmark rightWrist = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST);
        PoseLandmark leftIndex = pose.getPoseLandmark(PoseLandmark.LEFT_INDEX);
        PoseLandmark rightIndex = pose.getPoseLandmark(PoseLandmark.RIGHT_INDEX);

        if (leftWrist != null && leftIndex != null) {
            Log.d("PoseDetection", "Left wrist position: " + leftWrist.getPosition());
            Log.d("PoseDetection", "Left index position: " + leftIndex.getPosition());

            Rect leftWristRect = new Rect((int) leftWrist.getPosition().x - 50, (int) leftWrist.getPosition().y - 50,
                    (int) leftWrist.getPosition().x + 50, (int) leftWrist.getPosition().y + 50);
            Rect leftIndexRect = new Rect((int) leftIndex.getPosition().x - 50, (int) leftIndex.getPosition().y - 50,
                    (int) leftIndex.getPosition().x + 50, (int) leftIndex.getPosition().y + 50);

            blurredBitmap = BitmapUtil.blurRegion(this, blurredBitmap, leftWristRect);
            blurredBitmap = BitmapUtil.blurRegion(this, blurredBitmap, leftIndexRect);
        } else {
            Log.d("PoseDetection", "Left wrist or left index not detected.");
        }

        if (rightWrist != null && rightIndex != null) {
            Log.d("PoseDetection", "Right wrist position: " + rightWrist.getPosition());
            Log.d("PoseDetection", "Right index position: " + rightIndex.getPosition());

            Rect rightWristRect = new Rect((int) rightWrist.getPosition().x - 50, (int) rightWrist.getPosition().y - 50,
                    (int) rightWrist.getPosition().x + 50, (int) rightWrist.getPosition().y + 50);
            Rect rightIndexRect = new Rect((int) rightIndex.getPosition().x - 50, (int) rightIndex.getPosition().y - 50,
                    (int) rightIndex.getPosition().x + 50, (int) rightIndex.getPosition().y + 50);

            blurredBitmap = BitmapUtil.blurRegion(this, blurredBitmap, rightWristRect);
            blurredBitmap = BitmapUtil.blurRegion(this, blurredBitmap, rightIndexRect);
        } else {
            Log.d("PoseDetection", "Right wrist or right index not detected.");
        }

        return blurredBitmap;
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
}
