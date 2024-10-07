package org.techtown.new_camera;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.*;
import android.media.Image;
import android.media.ImageReader;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

//import com.google.mlkit.vision.face.Face;
//import com.google.mlkit.vision.face.FaceLandmark;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    private TextureView textureView;
    private CameraDevice cameraDevice;
    private CaptureRequest.Builder captureRequestBuilder;
    private CameraCaptureSession cameraCaptureSession;
    private static final int PERMISSIONS_REQUEST_CODE = 22;
    private String cameraId;
    private String[] cameraIds; // 카메라 ID를 저장할 배열
    private int cameraMode = 0; // 카메라 모드 (0: 첫 번째 후면 카메라, 1: 두 번째 후면 카메라, 2: 전면 카메라)
    private boolean isFrontCamera = false;
    private int rearCameraIndex = 0; // 후면 카메라 인덱스 (0: 첫 번째 후면 카메라, 1: 두 번째 후면 카메라)

    private boolean isCameraOpenRequested = false; // 카메라 열기 요청 상태


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

    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            cameraDevice = camera;
            createCameraPreview();
            Toast.makeText(MainActivity.this, "카메라 " + (cameraMode + 1) + " 번", Toast.LENGTH_SHORT).show();
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

        getCameraIds();
        if (chkPermission()) {
            openCamera(); // 권한이 있는 경우 카메라 열기
        } else {
            isCameraOpenRequested = true; // 권한 요청 후 카메라 열기를 요청
        }

        takePictureButton.setOnClickListener(v -> {
            takePicture();
        });

        albumButton.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), AlbumActivity.class);
            startActivity(intent);
        });

        rotateButton.setOnClickListener(v -> switchCamera());
    }

    private void getCameraIds() {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            List<String> cameraIdList = new ArrayList<>();
            for (String id : manager.getCameraIdList()) {
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(id);
                int lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (lensFacing == CameraCharacteristics.LENS_FACING_BACK || lensFacing == CameraCharacteristics.LENS_FACING_FRONT) {
                    cameraIdList.add(id);
                }
            }
            cameraIds = cameraIdList.stream().limit(3).toArray(String[]::new);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void getCameraId() {
        if (cameraIds != null && cameraIds.length > 0) {
            cameraId = cameraIds[cameraMode];
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
            manager.openCamera(cameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    cameraDevice = camera; // cameraDevice를 여기서 초기화
                    createCameraPreview(); // 카메라가 열린 후 미리보기 생성
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice camera) {
                    camera.close(); // 카메라가 끊어지면 닫기
                    cameraDevice = null;
                }

                @Override
                public void onError(@NonNull CameraDevice camera, int error) {
                    camera.close(); // 오류 발생 시 닫기
                    cameraDevice = null;
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void createCameraPreview() {
        if (cameraDevice == null) {
            // cameraDevice가 null인 경우 로그를 추가하여 원인을 파악
            Log.e("MainActivity", "cameraDevice is null, unable to create camera preview");
            return;
        }

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

    ////////////
    //////////

    /////////////

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

    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        closeCameraSession();
        return true;  // Surface가 파괴되었음을 알림
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
            Size[] jpegSizes = characteristics != null ? characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.JPEG) : null;

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
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, getJpegOrientation(characteristics));

            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, System.currentTimeMillis() + ".jpg");
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
            values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);

            Uri imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

            ImageReader.OnImageAvailableListener readerListener = reader1 -> {
                Image image = reader1.acquireLatestImage();
                if (image != null) {
                    ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                    byte[] bytes = new byte[buffer.remaining()];
                    buffer.get(bytes);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    int facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                    Bitmap processedBitmap;
                    if (facing == CameraCharacteristics.LENS_FACING_FRONT) {
                        processedBitmap = flipImageVertically(bitmap); // 전면 카메라의 경우 좌우 반전
                    } else {
                        processedBitmap = bitmap;
                    }

                    Bitmap rotatedBitmap = rotateImage(processedBitmap, facing == CameraCharacteristics.LENS_FACING_FRONT ? -90 : 90);

                    Uri bitmapUri = saveBitmapToFile(rotatedBitmap);
                    if (bitmapUri != null) {
                        Intent intent = new Intent(MainActivity.this, Camera_picture.class);
                        intent.putExtra("photoUri", bitmapUri.toString());
                        startActivity(intent); // 파일 URI를 넘기고 화면 전환
                    }
                    image.close();
                }
            };
            reader.setOnImageAvailableListener(readerListener, null);

            CameraCaptureSession.CaptureCallback captureListener = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
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
                public void onConfigureFailed(CameraCaptureSession session) {}
            }, null);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private Bitmap flipImageVertically(Bitmap bitmap) {
        Matrix matrix = new Matrix();
        matrix.preScale(1.0f, -1.0f);  // Y축 반전으로 상하 반전
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }
    private Uri saveImage(Bitmap bitmap, Uri imageUri) {
        try (OutputStream out = getContentResolver().openOutputStream(imageUri)) {
            if (out != null) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                out.flush(); // 버퍼를 비워줍니다.
                return imageUri; // 저장한 파일의 URI를 반환합니다.
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null; // 오류 발생 시 null을 반환합니다.
    }

    private Uri saveBitmapToFile(Bitmap bitmap) {
        String filename = "tempImage.jpg";
        File file = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), filename);

        try (FileOutputStream out = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            return Uri.fromFile(file); // 파일 URI 반환
        } catch (IOException e) {
            e.printStackTrace();
            return null; // 오류 발생 시 null 반환
        }
    }


    private Bitmap rotateImage(Bitmap img, int degree) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        return Bitmap.createBitmap(img, 0, 0, img.getWidth(), img.getHeight(), matrix, true);
    }



//    private Bitmap applyBlur(Bitmap bitmap, List<Face> faces) {
//        return bitmap;  // 여기에서 얼굴 영역 블러 처리 로직 추가
//    }
//
//    private void handleCameraError(Throwable e) {
//        Toast.makeText(this, "카메라 오류가 발생했습니다: " + e.getMessage(), Toast.LENGTH_SHORT).show();
//    }

    private int getJpegOrientation(CameraCharacteristics characteristics) {
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        int sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        switch (rotation) {
            case Surface.ROTATION_0:
                return sensorOrientation;
            case Surface.ROTATION_90:
                return (sensorOrientation + 90) % 360;
            case Surface.ROTATION_180:
                return (sensorOrientation + 180) % 360;
            case Surface.ROTATION_270:
                return (sensorOrientation + 270) % 360;
            default:
                return sensorOrientation;
        }
    }
    private void switchCamera() {
        rearCameraIndex++;
        cameraMode = rearCameraIndex % cameraIds.length;
        closeCameraSession();
        openCamera();
    }


    private boolean chkPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                showPermissionDeniedDialog();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, PERMISSIONS_REQUEST_CODE);
            }
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (isCameraOpenRequested) {
                    openCamera();  // 권한이 부여된 경우 카메라 열기
                    isCameraOpenRequested = false;  // 요청 후 상태 초기화
                }
            } else {
                // 권한이 거부된 경우 처리
                if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                    showPermissionDeniedDialog();
                } else {
                    showSettingsRedirectDialog();
                }
            }
        }
    }

    // 카메라 상태 콜백

    // 권한이 거부된 경우 안내 다이얼로그 표시
    private void showPermissionDeniedDialog() {
        new AlertDialog.Builder(this)
                .setTitle("권한이 필요합니다")
                .setMessage("카메라 사용을 위해 권한이 필요합니다. 권한을 허용해주세요.")
                .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // 다시 권한 요청
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{Manifest.permission.CAMERA}, PERMISSIONS_REQUEST_CODE);
                    }
                })
                .setNegativeButton("취소", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        // 앱 종료 또는 다른 처리
                        finish();
                    }
                })
                .show();
    }

    // 설정으로 이동하는 다이얼로그 표시 (사용자가 '다시 묻지 않음'을 선택했을 경우)
    private void showSettingsRedirectDialog() {
        new AlertDialog.Builder(this)
                .setTitle("권한 설정 필요")
                .setMessage("카메라 권한이 필요합니다. 설정에서 권한을 허용해주세요.")
                .setPositiveButton("설정으로 이동", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // 설정 화면으로 이동하기 전에 전 레이아웃으로 변경
                        setContentView(R.layout.activity_main); // 전 레이아웃으로 변경
                        openCamera(); // 카메라를 열기 위해 호출 (필요한 경우)

                        // 설정 화면으로 이동
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts("package", getPackageName(), null);
                        intent.setData(uri);
                        startActivity(intent);
                    }
                })
                .setNegativeButton("취소", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        finish(); // 앱 종료 또는 다른 처리
                    }
                })
                .show();
    }

    // 전체화면 모드 설정 함수
    private void setFullScreenMode() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }
}
//

//

//

//
