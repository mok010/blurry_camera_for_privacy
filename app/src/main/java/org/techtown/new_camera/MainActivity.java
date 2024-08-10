package org.techtown.new_camera;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
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
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.File;
import java.io.FileNotFoundException;
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
    //private static final int REQUEST_CAMERA_PERMISSION = 200;

    //카메라, 이미지 저장소 접근 권한 요청 코드
    private static final int PERMISSIONS_REQUEST_CODE = 22;

    private String cameraId;
    private boolean isFrontCamera = false;

    // TextureView의 상태를 감지하는 리스너
    private final TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            // 텍스쳐뷰가 사용 가능할 때 카메라를 여는 함수 호출
            openCamera();
        }

        // 텍스처 크기가 변경될 때 호출
        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        }

        // 텍스쳐가 파괴될 때 호출
        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        // 텍스쳐가 업데이트될 때 호출
        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        }
    };

    // 카메라 상태 콜백
    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            // 카메라가 성공적으로 열리면 CameraDevice 인스턴스를 할당하고 미리보기 시작
            cameraDevice = camera;
            createCameraPreview();
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            // 카메라 연결이 끊기면 닫음
            cameraDevice.close();
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            // 에러 발생시 카메라 닫고 null 할당
            cameraDevice.close();
            cameraDevice = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textureView = findViewById(R.id.textureView);
        // TextureView에 리스너 설정
        textureView.setSurfaceTextureListener(textureListener);
        Button takePictureButton = findViewById(R.id.btn_takepicture);
        ImageButton rotateButton = findViewById(R.id.btn_rotate);
        ImageButton albumButton = findViewById(R.id.btn_album);
        Button convertButton = findViewById(R.id.btn_convert); // 변환 시작 버튼

        //권한 확인 요청
        if (chkPermission()){
            Toast.makeText(this, "위험 권한 승인함", Toast.LENGTH_SHORT).show();
        }

        takePictureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 사진 촬영 버튼 클릭 이벤트
                takePicture(); // 사진 촬영 메소드 호출
            }
        });

        // 변환 시작 버튼 클릭 리스너 추가
        convertButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startIrisRecognition(); // 홍채 인식 변환 시작 메소드 호출
                }
            });

        // 권한 확인 요청
        if (chkPermission()) {
            Toast.makeText(this, "위험 권한 승인함", Toast.LENGTH_SHORT).show();
        }

        albumButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //AlbumActivity.class와 연결
                Intent intent = new Intent(getApplicationContext(),AlbumActivity.class);
                startActivity(intent);
            }
        });

        rotateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 전면/후면 카메라 전환 메소드 호출
                switchCamera();
            }
        });

        /*back_to_cameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ActivityCompat.checkSelfPermission(SaveActivity2.this, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    bindPreview();
                }
                Intent intent = new Intent(getApplicationContext(),MainActivity.class);
                startActivity(intent);
            }
        });
        */
    }

    private void getCameraId() {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            for (String id : manager.getCameraIdList()) {
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(id);
                if (characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK) {
                    if (!isFrontCamera) {
                        cameraId = id;
                    }
                } else if (characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT) {
                    if (isFrontCamera) {
                        cameraId = id;
                    }
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    // 카메라 미리보기를 생성하는 메소드
    private void createCameraPreview() {
        try {
            SurfaceTexture texture = textureView.getSurfaceTexture();
            assert texture != null;
            // 텍스처의 크기를 카메라 미리보기 크기로 설정
            texture.setDefaultBufferSize(1920, 1080);
            Surface surface = new Surface(texture);
            // 미리보기를 위한 CaptureRequest.Builder 설정
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface);
            // 카메라 캡처 세션 생성
            cameraDevice.createCaptureSession(Collections.singletonList(surface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    // 카메라가 이미 닫혔다면 아무 작업도 수행하지 않음
                    if (cameraDevice == null) {
                        return;
                    }
                    // 캡처 세션 시작
                    cameraCaptureSession = session;
                    updatePreview();
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                    // 구성 실패시 Toast 메시지 출력
                    Toast.makeText(MainActivity.this, "Configuration change", Toast.LENGTH_SHORT).show();
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    // 카메라를 여는 메소드
    private void openCamera() {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        getCameraId();
        try {
            // 카메라 권한 체크
            if (ActivityCompat.checkSelfPermission(this, "android.permission.CAMERA") != PackageManager.PERMISSION_GRANTED) {
                //권한 요청 따른 함수에 넣겠습니다.
                //ActivityCompat.requestPermissions(this, new String[]{"android.permission.CAMERA"}, REQUEST_CAMERA_PERMISSION);
                return;
            }
            // 카메라 오픈, 상태 콜백과 핸들러 설정
            manager.openCamera(cameraId, stateCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    // 카메라 미리보기를 업데이트하는 메소드
    private void updatePreview() {
        if (cameraDevice == null) {
            Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show();
            return;
        }
        // 자동 모드로 미리보기 설정
        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);
        try {
            // 미리보기 요청을 반복하여 캡처 세션에 제출
            cameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(), null, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        // 액티비티가 일시정지되면 카메라를 닫음
        super.onPause();
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

            // 파일 경로 설정
            File directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            if (directory != null && !directory.exists()) {
                directory.mkdirs();
            }
            String path = directory + "/" + fileName;
            File file = new File(path);

            ImageReader.OnImageAvailableListener readerListener = new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    Image image = null;
                    try {
                        image = reader.acquireLatestImage();
                        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                        byte[] bytes = new byte[buffer.capacity()];
                        buffer.get(bytes);
                        save(bytes);
                        // 미디어 스캐너 호출
                        MediaScannerConnection.scanFile(MainActivity.this, new String[]{file.getAbsolutePath()}, null, new MediaScannerConnection.OnScanCompletedListener() {
                            @Override
                            public void onScanCompleted(String path, Uri uri) {
                                // 스캔 완료 후 호출되는 콜백
                            }
                        });
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        if (image != null) {
                            image.close();
                        }
                    }
                }

                private void save(byte[] bytes) throws IOException {
                    OutputStream output = null;
                    try {
                        output = new FileOutputStream(file);
                        output.write(bytes);
                    } finally {
                        if (null != output) {
                            output.close();
                        }
                    }
                }
            };

            reader.setOnImageAvailableListener(readerListener, null);

            final CameraCaptureSession.CaptureCallback captureListener = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    Toast.makeText(MainActivity.this, "Saved: " + file, Toast.LENGTH_SHORT).show();
                    createCameraPreview();
                    Intent intent = new Intent(MainActivity.this, MainActivity2.class);
                    intent.putExtra("img", path);
                    startActivity(intent);
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
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void switchCamera() {
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
        isFrontCamera = !isFrontCamera;
        openCamera();
    }

    //권한 요청하는 메소드들
    public boolean chkPermission() {
        // 위험 권한을 모두 승인했는지 여부
        boolean mPermissionsGranted = false;
        // 승인 받기 위한 권한 목록: 미디어 접근, 카메라
        String[] mRequiredPermissions = new String[]{
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.CAMERA
        };

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // 필수 권한을 가지고 있는지 확인한다.
            mPermissionsGranted = hasPermissions(mRequiredPermissions);

            // 필수 권한 중에 한 개라도 없는 경우
            if (!mPermissionsGranted) {
                // 권한을 요청한다.
                ActivityCompat.requestPermissions(MainActivity.this, mRequiredPermissions, PERMISSIONS_REQUEST_CODE);
            }
        } else {
            mPermissionsGranted = true;
        }

        return mPermissionsGranted;
    }

    public boolean hasPermissions(String[] permissions) {
        // 필수 권한을 가지고 있는지 확인한다.
        for (String permission : permissions) {
            if (checkCallingOrSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            // 권한을 모두 승인했는지 여부
            boolean chkFlag = false;
            // 승인한 권한은 0 값, 승인 안한 권한은 -1을 값으로 가진다.
            for (int g : grantResults) {
                if (g == -1) {
                    chkFlag = true;
                    break;
                }
            }

            // 권한 중 한 개라도 승인 안 한 경우
            if (chkFlag){
                chkPermission();
            }
        }
    }
}