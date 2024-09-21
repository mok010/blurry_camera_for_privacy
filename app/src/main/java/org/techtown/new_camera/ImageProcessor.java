package org.techtown.new_camera;

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
    public static CompletableFuture<List<Face>> processInputImage(Bitmap photoBitmap) {
        CompletableFuture<List<Face>> future = new CompletableFuture<>();
        InputImage image = InputImage.fromBitmap(photoBitmap, 0);

        faceDetector.process(image)
                .addOnSuccessListener(new OnSuccessListener<List<Face>>() {
                    @Override
                    public void onSuccess(List<Face> faces) {
                        List<Rect> faceBoundsList = new ArrayList<>(); // 얼굴 좌표를 담을 리스트

                        // 얼굴 목록에서 각 얼굴의 랜드마크 좌표를 리스트에 추가
                        for (Face face : faces) {
                            FaceLandmark leftEye = face.getLandmark(FaceLandmark.LEFT_EYE);
                            FaceLandmark rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE);
                            if (leftEye != null && rightEye != null) {
                                PointF leftEyePos = leftEye.getPosition();
                                PointF rightEyePos = rightEye.getPosition();
                                // 예시: 눈 주위 영역을 리스트에 추가
                                faceBoundsList.add(new Rect(
                                        (int) (leftEyePos.x - 20), (int) (leftEyePos.y - 20),
                                        (int) (leftEyePos.x + 20), (int) (leftEyePos.y + 20)));
                                faceBoundsList.add(new Rect(
                                        (int) (rightEyePos.x - 20), (int) (rightEyePos.y - 20),
                                        (int) (rightEyePos.x + 20), (int) (rightEyePos.y + 20)));
                            }
                        }
                        // 필요한 경우 faceBoundsList로 추가적인 처리 (예: 블러링)
                        future.complete(faces);  // 얼굴 목록을 반환
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
    public static CompletableFuture<Pose> processInputImagePose(Bitmap photoBitmap) {
        CompletableFuture<Pose> futurePose = new CompletableFuture<>();
        InputImage image = InputImage.fromBitmap(photoBitmap, 0);

        poseDetector.process(image)
                .addOnSuccessListener(new OnSuccessListener<Pose>() {
                    @Override
                    public void onSuccess(Pose pose) {
                        List<Rect> poseBoundsList = new ArrayList<>(); // 포즈 좌표를 담을 리스트

                        // 포즈 랜드마크 목록에서 각 손가락과 손목 좌표를 리스트에 추가
                        PoseLandmark leftWrist = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST);
                        PoseLandmark rightWrist = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST);
                        PoseLandmark leftIndex = pose.getPoseLandmark(PoseLandmark.LEFT_INDEX);
                        PoseLandmark rightIndex = pose.getPoseLandmark(PoseLandmark.RIGHT_INDEX);

                        if (leftWrist != null && leftIndex != null) {
                            PointF leftWristPos = leftWrist.getPosition();
                            PointF leftIndexPos = leftIndex.getPosition();
                            // 왼 손목과 검지 주변 영역을 리스트에 추가
                            poseBoundsList.add(new Rect(
                                    (int) (leftWristPos.x - 30), (int) (leftWristPos.y - 30),
                                    (int) (leftWristPos.x + 30), (int) (leftWristPos.y + 30)));
                            poseBoundsList.add(new Rect(
                                    (int) (leftIndexPos.x - 30), (int) (leftIndexPos.y - 30),
                                    (int) (leftIndexPos.x + 30), (int) (leftIndexPos.y + 30)));
                        }

                        if (rightWrist != null && rightIndex != null) {
                            PointF rightWristPos = rightWrist.getPosition();
                            PointF rightIndexPos = rightIndex.getPosition();
                            // 오른 손목과 검지 주변 영역을 리스트에 추가
                            poseBoundsList.add(new Rect(
                                    (int) (rightWristPos.x - 30), (int) (rightWristPos.y - 30),
                                    (int) (rightWristPos.x + 30), (int) (rightWristPos.y + 30)));
                            poseBoundsList.add(new Rect(
                                    (int) (rightIndexPos.x - 30), (int) (rightIndexPos.y - 30),
                                    (int) (rightIndexPos.x + 30), (int) (rightIndexPos.y + 30)));
                        }

                        // 필요한 경우 poseBoundsList로 추가적인 블러링 처리
                        futurePose.complete(pose);  // 감지된 포즈 반환
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
}
