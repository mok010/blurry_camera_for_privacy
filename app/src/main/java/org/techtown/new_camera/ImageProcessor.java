package org.techtown.new_camera;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.BlurMaskFilter;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import com.google.mlkit.vision.pose.Pose;
import com.google.mlkit.vision.pose.PoseDetection;
import com.google.mlkit.vision.pose.PoseDetector;
import com.google.mlkit.vision.pose.PoseLandmark;
import com.google.mlkit.vision.pose.accurate.AccuratePoseDetectorOptions;
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ImageProcessor {

    private static final FaceDetectorOptions highAccuracyOpts =
            new FaceDetectorOptions.Builder()
                    .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                    .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                    .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                    .build();

    private static final FaceDetector detector = FaceDetection.getClient(highAccuracyOpts);

    public static CompletableFuture<List<Face>> processInputImage(Bitmap bitmap) {
        CompletableFuture<List<Face>> future = new CompletableFuture<>();
        InputImage image = InputImage.fromBitmap(bitmap, 0);

        detector.process(image)
                .addOnSuccessListener(new OnSuccessListener<List<Face>>() {
                    @Override
                    public void onSuccess(List<Face> faces) {
                        future.complete(faces);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(Exception e) {
                        future.completeExceptionally(e);
                    }
                });

        return future;
    }

    private static final PoseDetectorOptions options =
            new PoseDetectorOptions.Builder()
                    .setDetectorMode(PoseDetectorOptions.SINGLE_IMAGE_MODE)
                    .build();

    private static final PoseDetector poseDetector = PoseDetection.getClient(options);

    public static CompletableFuture<Bitmap> processInputImagePose(Bitmap bitmap, Context context) {
        CompletableFuture<Bitmap> futurePose = new CompletableFuture<>();
        InputImage image = InputImage.fromBitmap(bitmap, 0);

        poseDetector.process(image)
                .addOnSuccessListener(
                        new OnSuccessListener<Pose>() {
                            @Override
                            public void onSuccess(Pose pose) {
                                // 손목 랜드마크를 추출하여 블러링 적용
                                Bitmap blurredBitmap = applyBlurToWrists(bitmap, pose.getAllPoseLandmarks(), context);
                                futurePose.complete(blurredBitmap);
                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                futurePose.completeExceptionally(e);
                            }
                        });

        return futurePose;
    }

    private static Bitmap applyBlurToWrists(Bitmap bitmap, List<PoseLandmark> landmarks, Context context) {
        Bitmap mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(mutableBitmap);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setMaskFilter(new BlurMaskFilter(15, BlurMaskFilter.Blur.NORMAL));

        // 손목 랜드마크에 블러 적용
        for (PoseLandmark landmark : landmarks) {
            int type = landmark.getLandmarkType();
            if (type == PoseLandmark.LEFT_WRIST || type == PoseLandmark.RIGHT_WRIST) {
                float x = landmark.getPosition().x;
                float y = landmark.getPosition().y;
                canvas.drawCircle(x, y, 30, paint); // 30은 블러 반경
            }
        }

        return mutableBitmap;
    }
}
