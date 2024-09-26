package org.techtown.new_camera;

import android.content.Context;
import android.graphics.Bitmap;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.pose.Pose;
import com.google.mlkit.vision.pose.PoseDetection;
import com.google.mlkit.vision.pose.PoseDetector;
import com.google.mlkit.vision.pose.accurate.AccuratePoseDetectorOptions;
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ImageProcessor {

    // 얼굴 감지 설정: 고성능 모드, 랜드마크 및 분류 활성화
    private static final FaceDetectorOptions highAccuracyOpts =
            new FaceDetectorOptions.Builder()
                    .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)  // 정확성 우선
                    .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)  // 모든 랜드마크 감지
                    .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                    .build();

    // 얼굴 감지 클라이언트
    private static final FaceDetector faceDetector = FaceDetection.getClient(highAccuracyOpts);

    // 포즈 감지 설정
    private static final AccuratePoseDetectorOptions accuratePoseOptions =
            new AccuratePoseDetectorOptions.Builder()
                    .setDetectorMode(PoseDetectorOptions.SINGLE_IMAGE_MODE) // 단일 이미지 모드
                    .build();

    // 포즈 감지 클라이언트 생성
    private static final PoseDetector poseDetector = PoseDetection.getClient(accuratePoseOptions);

    /**
     * 얼굴을 감지하는 메서드
     * @param photoBitmap 얼굴을 감지할 비트맵 이미지
     * @return 얼굴 리스트를 비동기적으로 반환하는 CompletableFuture
     */
    public static CompletableFuture<List<Face>> processInputImage(Bitmap photoBitmap, Context context) {
        CompletableFuture<List<Face>> future = new CompletableFuture<>();
        InputImage image = InputImage.fromBitmap(photoBitmap, 0);

        faceDetector.process(image)
                .addOnSuccessListener(faces -> future.complete(faces))  // 얼굴 목록 반환
                .addOnFailureListener(e -> future.completeExceptionally(e));  // 실패 시 예외 반환

        return future;
    }

    /**
     * 포즈를 감지하는 메서드
     * @param photoBitmap 포즈를 감지할 비트맵 이미지
     * @return 감지된 포즈를 비동기적으로 반환하는 CompletableFuture
     */
    public static CompletableFuture<Pose> processInputImagePose(Bitmap photoBitmap, Context context) {
        CompletableFuture<Pose> futurePose = new CompletableFuture<>();
        InputImage image = InputImage.fromBitmap(photoBitmap, 0);

        poseDetector.process(image)
                .addOnSuccessListener(futurePose::complete)  // 포즈 반환
                .addOnFailureListener(e -> futurePose.completeExceptionally(e));  // 실패 시 예외 반환

        return futurePose;
    }
}
