package org.techtown.new_camera;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceLandmark;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    // 카메라 미리보기를 위한 TextureView
    private TextureView textureView;

    // 카메라 디바이스를 참조하기 위한 객체
    private CameraDevice cameraDevice;

    // 캡처 요청을 만들기 위한 Builder
    private CaptureRequest.Builder captureRequestBuilder;

    // 카메라 캡처 세션
    private CameraCaptureSession cameraCaptureSession;
    String fileName = "IMG_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".jpg";

    // 카메라 사용 권한 요청 코드
    private static final int PERMISSIONS_REQUEST_CODE = 22;

    private String cameraId;
    private boolean isFrontCamera = false;

    // TextureView의 상태를 감지하는 리스너
    private final TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            openCamera();  // 텍스쳐뷰가 사용 가능할 때 카메라를 여는 함수 호출
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        }
    };

    // 카메라 상태 콜백
    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            cameraDevice = camera;
            createCameraPreview();
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            cameraDevice.close();
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            cameraDevice.close();
            cameraDevice = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textureView = findViewById(R.id.textureView);
        textureView.setSurfaceTextureListener(textureListener);

        Button takePictureButton = findViewById(R.id.btn_takepicture);
        ImageButton rotateButton = findViewById(R.id.btn_rotate);
        ImageButton albumButton = findViewById(R.id.btn_album);

        if (chkPermission()) {
            Toast.makeText(this, "권한 승인 완료", Toast.LENGTH_SHORT).show();
        }

        takePictureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePicture();  // 사진 촬영 메소드 호출
            }
        });

        albumButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), AlbumActivity.class);
                startActivity(intent);
            }
        });

        rotateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchCamera();
            }
        });
    }

    private void getCameraId() {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            for (String id : manager.getCameraIdList()) {
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(id);
                if (characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK && !isFrontCamera) {
                    cameraId = id;
                } else if (characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT && isFrontCamera) {
                    cameraId = id;
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void openCamera() {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        getCameraId();
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA}, PERMISSIONS_REQUEST_CODE);
                return;
            }
            manager.openCamera(cameraId, stateCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void createCameraPreview() {
        try {
            SurfaceTexture texture = textureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(1920, 1080);
            Surface surface = new Surface(texture);
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface);
            cameraDevice.createCaptureSession(Collections.singletonList(surface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    if (cameraDevice == null) return;
                    cameraCaptureSession = session;
                    updatePreview();
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                    Toast.makeText(MainActivity.this, "카메라 구성 실패", Toast.LENGTH_SHORT).show();
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void updatePreview() {
        if (cameraDevice == null) return;
        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        try {
            cameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(), null, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        closeCameraSession();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        closeCameraSession();
    }

    private void closeCameraSession() {
        if (cameraCaptureSession != null) {
            cameraCaptureSession.close();
            cameraCaptureSession = null;
        }
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
    }

    private void takePicture() {
        if (cameraDevice == null) return;
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraDevice.getId());
            Size[] jpegSizes = null;
            if (characteristics != null) {
                jpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.JPEG);
            }

            int width = 480;
            int height = 640;
            if (jpegSizes != null && jpegSizes.length > 0) {
                width = jpegSizes[0].getWidth();
                height = jpegSizes[0].getHeight();
            }

            ImageReader reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);
            List<Surface> outputSurfaces = new ArrayList<>(2);
            outputSurfaces.add(reader.getSurface());
            outputSurfaces.add(new Surface(textureView.getSurfaceTexture()));

            final CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(reader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);

            File directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            if (!directory.exists()) directory.mkdirs();
            String path = directory + "/" + fileName;
            File file = new File(path);

            ImageReader.OnImageAvailableListener readerListener = new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    Image image = reader.acquireLatestImage();
                    ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                    byte[] bytes = new byte[buffer.capacity()];
                    buffer.get(bytes);

                    Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    processFaceAndBlur(bitmap, path);

                    image.close();
                }
            };

            reader.setOnImageAvailableListener(readerListener, null);

            CameraCaptureSession.CaptureCallback captureListener = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    Toast.makeText(MainActivity.this, "사진 저장됨: " + file, Toast.LENGTH_SHORT).show();
                    createCameraPreview();
                }
            };

            cameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    try {
                        session.capture(captureBuilder.build(), captureListener, null);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                    Toast.makeText(MainActivity.this, "캡처 실패", Toast.LENGTH_SHORT).show();
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void processFaceAndBlur(Bitmap bitmap, String path) {
        ImageProcessor.processInputImage(bitmap, this).thenAccept(faces -> {
            // faces는 List<Face> 타입이어야 합니다.
            if (faces != null) {
                Bitmap blurredBitmap = applyBlur(bitmap, faces);  // 블러 처리
                saveImageAndSendToNextActivity(blurredBitmap, path);
            }
        }).exceptionally(e -> {
            handleCameraError(e);
            return null;
        });
    }

    private Bitmap applyBlur(Bitmap bitmap, List<Face> faces) {
        Bitmap blurredBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);

        // 얼굴 목록을 순회하며 각 얼굴에 블러 처리 적용
        for (Face face : faces) {
            FaceLandmark leftEye = face.getLandmark(FaceLandmark.LEFT_EYE);
            FaceLandmark rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE);

            if (leftEye != null) {
                PointF leftEyePos = leftEye.getPosition();
                Rect leftEyeRect = new Rect(
                        (int) (leftEyePos.x - 20), (int) (leftEyePos.y - 20),
                        (int) (leftEyePos.x + 20), (int) (leftEyePos.y + 20)
                );
                blurredBitmap = BitmapUtil.blurRegion(this, blurredBitmap, leftEyeRect);  // 블러 처리
            }

            if (rightEye != null) {
                PointF rightEyePos = rightEye.getPosition();
                Rect rightEyeRect = new Rect(
                        (int) (rightEyePos.x - 20), (int) (rightEyePos.y - 20),
                        (int) (rightEyePos.x + 20), (int) (rightEyePos.y + 20)
                );
                blurredBitmap = BitmapUtil.blurRegion(this, blurredBitmap, rightEyeRect);  // 블러 처리
            }
        }

        return blurredBitmap;
    }



    private void handleCameraError(Throwable e) {
        Toast.makeText(this, "카메라 오류가 발생했습니다: " + e.getMessage(), Toast.LENGTH_SHORT).show();
    }

    private void saveImageAndSendToNextActivity(Bitmap bitmap, String path) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
            values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);

            Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

            try (OutputStream out = getContentResolver().openOutputStream(uri)) {
                if (out != null) {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                    Toast.makeText(this, "사진이 갤러리에 저장되었습니다.", Toast.LENGTH_SHORT).show();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        } else {
            File file = new File(path);
            try (OutputStream out = new FileOutputStream(file)) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            } catch (IOException e) {
                e.printStackTrace();
            }

            MediaScannerConnection.scanFile(this, new String[]{file.getAbsolutePath()}, null, (path1, uri) -> {
                Toast.makeText(this, "사진이 갤러리에 저장되었습니다.", Toast.LENGTH_SHORT).show();
            });
        }

        Intent intent = new Intent(MainActivity.this, MainActivity2.class);
        intent.putExtra("img", path);
        startActivity(intent);
    }

    private void switchCamera() {
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
        isFrontCamera = !isFrontCamera;
        openCamera();
    }

    public boolean chkPermission() {
        String[] permissions = {Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.CAMERA};
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            boolean hasPermissions = hasPermissions(permissions);
            if (!hasPermissions) {
                ActivityCompat.requestPermissions(MainActivity.this, permissions, PERMISSIONS_REQUEST_CODE);
            }
            return hasPermissions;
        }
        return true;
    }

    private boolean hasPermissions(String[] permissions) {
        for (String permission : permissions) {
            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            boolean chkFlag = false;
            for (int g : grantResults) {
                if (g == -1) {
                    chkFlag = true;
                    break;
                }
            }
            if (chkFlag) chkPermission();
        }
    }
}
