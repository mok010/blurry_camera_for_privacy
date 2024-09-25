package org.techtown.new_camera;

import android.content.Context;
import android.graphics.Bitmap;
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

    // 얼굴 감지를 위한 설정: 고성능 모드, 랜드마크와 분류를 모두 활성화
    private static final FaceDetectorOptions highAccuracyOpts =
            new FaceDetectorOptions.Builder()
                    .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                    .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                    .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                    .build();

    private static final FaceDetector faceDetector = FaceDetection.getClient(highAccuracyOpts);

    // 포즈 감지를 위한 설정
    private static final PoseDetectorOptions poseOptions =
            new PoseDetectorOptions.Builder()
                    .setDetectorMode(PoseDetectorOptions.SINGLE_IMAGE_MODE)
                    .build();

    private static final PoseDetector poseDetector = PoseDetection.getClient(poseOptions);

    /**
     * 얼굴을 감지하고 블러링을 적용하는 메서드 (리스트로 처리)
     */
    public static CompletableFuture<Bitmap> processInputImage(Bitmap photoBitmap, Context context) {
        CompletableFuture<Bitmap> future = new CompletableFuture<>();
        InputImage image = InputImage.fromBitmap(photoBitmap, 0);

        faceDetector.process(image)
                .addOnSuccessListener(faces -> {
                    Bitmap blurredBitmap = photoBitmap.copy(Bitmap.Config.ARGB_8888, true);

                    // 얼굴의 랜드마크 좌표를 저장할 리스트
                    List<Rect> faceLandmarkRects = new ArrayList<>();

                    // 얼굴 목록에서 랜드마크를 가져와 리스트에 추가
                    for (Face face : faces) {
                        FaceLandmark leftEye = face.getLandmark(FaceLandmark.LEFT_EYE);
                        FaceLandmark rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE);
                        if (leftEye != null && rightEye != null) {
                            PointF leftEyePos = leftEye.getPosition();
                            PointF rightEyePos = rightEye.getPosition();

                            // 눈 주위의 영역을 리스트에 추가
                            faceLandmarkRects.add(new Rect((int) leftEyePos.x - 20, (int) leftEyePos.y - 20,
                                    (int) leftEyePos.x + 20, (int) leftEyePos.y + 20));
                            faceLandmarkRects.add(new Rect((int) rightEyePos.x - 20, (int) rightEyePos.y - 20,
                                    (int) rightEyePos.x + 20, (int) rightEyePos.y + 20));
                        }
                    }

                    // 리스트에 담긴 모든 랜드마크 영역에 대해 블러 처리
                    for (Rect rect : faceLandmarkRects) {
                        blurredBitmap = BitmapUtil.blurRegion(context, blurredBitmap, rect);
                    }

                    future.complete(blurredBitmap);  // 블러링이 적용된 이미지를 반환
                })
                .addOnFailureListener(e -> future.completeExceptionally(e));

        return future;
    }

    /**
     * 포즈를 감지하고 블러링을 적용하는 메서드 (리스트로 처리)
     */
    public static CompletableFuture<Bitmap> processInputImagePose(Bitmap photoBitmap, Context context) {
        CompletableFuture<Bitmap> futurePose = new CompletableFuture<>();
        InputImage image = InputImage.fromBitmap(photoBitmap, 0);

        poseDetector.process(image)
                .addOnSuccessListener(pose -> {
                    Bitmap blurredBitmap = photoBitmap.copy(Bitmap.Config.ARGB_8888, true);

                    // 포즈 랜드마크 좌표를 저장할 리스트
                    List<Rect> poseLandmarkRects = new ArrayList<>();

                    // 포즈 랜드마크에서 손목과 손가락 좌표를 리스트에 추가
                    PoseLandmark leftWrist = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST);
                    PoseLandmark rightWrist = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST);
                    PoseLandmark leftIndex = pose.getPoseLandmark(PoseLandmark.LEFT_INDEX);
                    PoseLandmark rightIndex = pose.getPoseLandmark(PoseLandmark.RIGHT_INDEX);

                    if (leftWrist != null && leftIndex != null) {
                        PointF leftWristPos = leftWrist.getPosition();
                        PointF leftIndexPos = leftIndex.getPosition();
                        // 손목과 검지 주변 영역을 리스트에 추가
                        poseLandmarkRects.add(new Rect((int) leftWristPos.x - 30, (int) leftWristPos.y - 30,
                                (int) leftWristPos.x + 30, (int) leftWristPos.y + 30));
                        poseLandmarkRects.add(new Rect((int) leftIndexPos.x - 30, (int) leftIndexPos.y - 30,
                                (int) leftIndexPos.x + 30, (int) leftIndexPos.y + 30));
                    }

                    if (rightWrist != null && rightIndex != null) {
                        PointF rightWristPos = rightWrist.getPosition();
                        PointF rightIndexPos = rightIndex.getPosition();
                        // 오른손목과 검지 주변 영역을 리스트에 추가
                        poseLandmarkRects.add(new Rect((int) rightWristPos.x - 30, (int) rightWristPos.y - 30,
                                (int) rightWristPos.x + 30, (int) rightWristPos.y + 30));
                        poseLandmarkRects.add(new Rect((int) rightIndexPos.x - 30, (int) rightIndexPos.y - 30,
                                (int) rightIndexPos.x + 30, (int) rightIndexPos.y + 30));
                    }

                    // 리스트에 담긴 모든 랜드마크 영역에 대해 블러 처리
                    for (Rect rect : poseLandmarkRects) {
                        blurredBitmap = BitmapUtil.blurRegion(context, blurredBitmap, rect);
                    }

                    futurePose.complete(blurredBitmap);  // 블러링이 적용된 이미지를 반환
                })
                .addOnFailureListener(e -> futurePose.completeExceptionally(e));

        return futurePose;
    }
}
