package org.techtown.new_camera;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.graphics.Rect;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.face.FaceLandmark;
import com.google.mlkit.vision.pose.Pose;
import com.google.mlkit.vision.pose.PoseDetection;
import com.google.mlkit.vision.pose.PoseDetector;
import com.google.mlkit.vision.pose.PoseLandmark;
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ImageProcessor {

    // 얼굴 감지를 위한 옵션: 고정밀 모드, 모든 랜드마크 및 분류 사용
    private static final FaceDetectorOptions highAccuracyOpts =
            new FaceDetectorOptions.Builder()
                    .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                    .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                    .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                    .build();

    // FaceDetector 인스턴스 생성
    private static final FaceDetector faceDetector = FaceDetection.getClient(highAccuracyOpts);

    // 포즈 감지를 위한 옵션 설정
    private static final PoseDetectorOptions poseOptions =
            new PoseDetectorOptions.Builder()
                    .setDetectorMode(PoseDetectorOptions.SINGLE_IMAGE_MODE)
                    .build();

    // PoseDetector 인스턴스 생성
    private static final PoseDetector poseDetector = PoseDetection.getClient(poseOptions);

    /**
     * 얼굴을 감지하는 메서드
     * @param photoBitmap 얼굴을 감지할 비트맵 이미지
     * @return 얼굴 리스트를 비동기적으로 반환하는 CompletableFuture
     */
    public static CompletableFuture<Bitmap> processInputImage(Bitmap photoBitmap) {
        CompletableFuture<Bitmap> future = new CompletableFuture<>();
        InputImage image = InputImage.fromBitmap(photoBitmap, 0);

        faceDetector.process(image)
                .addOnSuccessListener(new OnSuccessListener<List<Face>>() {
                    @Override
                    public void onSuccess(List<Face> faces) {
                        Bitmap blurredBitmap = photoBitmap.copy(Bitmap.Config.ARGB_8888, true);
                        Canvas canvas = new Canvas(blurredBitmap);

                        // 얼굴 목록에서 각 얼굴의 랜드마크 좌표를 리스트에 추가 및 블러 처리
                        for (Face face : faces) {
                            FaceLandmark leftEye = face.getLandmark(FaceLandmark.LEFT_EYE);
                            FaceLandmark rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE);
                            if (leftEye != null && rightEye != null) {
                                PointF leftEyePos = leftEye.getPosition();
                                PointF rightEyePos = rightEye.getPosition();
                                // 눈 주위 영역 블러 처리
                                Rect leftEyeRect = new Rect((int) (leftEyePos.x - 20), (int) (leftEyePos.y - 20),
                                        (int) (leftEyePos.x + 20), (int) (leftEyePos.y + 20));
                                Rect rightEyeRect = new Rect((int) (rightEyePos.x - 20), (int) (rightEyePos.y - 20),
                                        (int) (rightEyePos.x + 20), (int) (rightEyePos.y + 20));

                                // 블러 처리 함수 호출
                                blurredBitmap = BitmapUtil.blurRegion(blurredBitmap, leftEyeRect);
                                blurredBitmap = BitmapUtil.blurRegion(blurredBitmap, rightEyeRect);
                            }
                        }
                        future.complete(blurredBitmap);  // 블러링이 적용된 이미지를 반환
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        future.completeExceptionally(e);  // 에러 발생 시 예외 처리
                    }
                });

        return future;
    }

    /**
     * 포즈를 감지하는 메서드
     * @param photoBitmap 포즈를 감지할 비트맵 이미지
     * @return 감지된 포즈를 비동기적으로 반환하는 CompletableFuture
     */
    public static CompletableFuture<Bitmap> processInputImagePose(Bitmap photoBitmap) {
        CompletableFuture<Bitmap> futurePose = new CompletableFuture<>();
        InputImage image = InputImage.fromBitmap(photoBitmap, 0);

        poseDetector.process(image)
                .addOnSuccessListener(new OnSuccessListener<Pose>() {
                    @Override
                    public void onSuccess(Pose pose) {
                        Bitmap blurredBitmap = photoBitmap.copy(Bitmap.Config.ARGB_8888, true);
                        Canvas canvas = new Canvas(blurredBitmap);

                        // 포즈 랜드마크 목록에서 손목과 손가락 좌표를 이용해 블러 처리
                        PoseLandmark leftWrist = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST);
                        PoseLandmark rightWrist = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST);
                        PoseLandmark leftIndex = pose.getPoseLandmark(PoseLandmark.LEFT_INDEX);
                        PoseLandmark rightIndex = pose.getPoseLandmark(PoseLandmark.RIGHT_INDEX);

                        if (leftWrist != null && leftIndex != null) {
                            PointF leftWristPos = leftWrist.getPosition();
                            PointF leftIndexPos = leftIndex.getPosition();
                            // 손목과 검지 주변 영역 블러 처리
                            Rect leftWristRect = new Rect((int) (leftWristPos.x - 30), (int) (leftWristPos.y - 30),
                                    (int) (leftWristPos.x + 30), (int) (leftWristPos.y + 30));
                            Rect leftIndexRect = new Rect((int) (leftIndexPos.x - 30), (int) (leftIndexPos.y - 30),
                                    (int) (leftIndexPos.x + 30), (int) (leftIndexPos.y + 30));

                            blurredBitmap = BitmapUtil.blurRegion(blurredBitmap, leftWristRect);
                            blurredBitmap = BitmapUtil.blurRegion(blurredBitmap, leftIndexRect);
                        }

                        if (rightWrist != null && rightIndex != null) {
                            PointF rightWristPos = rightWrist.getPosition();
                            PointF rightIndexPos = rightIndex.getPosition();
                            // 오른손목과 검지 주변 영역 블러 처리
                            Rect rightWristRect = new Rect((int) (rightWristPos.x - 30), (int) (rightWristPos.y - 30),
                                    (int) (rightWristPos.x + 30), (int) (rightWristPos.y + 30));
                            Rect rightIndexRect = new Rect((int) (rightIndexPos.x - 30), (int) (rightIndexPos.y - 30),
                                    (int) (rightIndexPos.x + 30), (int) (rightIndexPos.y + 30));

                            blurredBitmap = BitmapUtil.blurRegion(blurredBitmap, rightWristRect);
                            blurredBitmap = BitmapUtil.blurRegion(blurredBitmap, rightIndexRect);
                        }

                        futurePose.complete(blurredBitmap);  // 블러링이 적용된 이미지를 반환
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        futurePose.completeExceptionally(e);  // 에러 발생 시 예외 처리
                    }
                });

        return futurePose;
    }

    public static class BitmapUtil {

        // 특정 영역을 블러 처리하는 함수
        public static Bitmap blurRegion(Bitmap bitmap, Rect region) {
            // 블러링 처리를 수행하는 코드
            return bitmap; // 실제 블러링 구현 필요
        }
    }
}
