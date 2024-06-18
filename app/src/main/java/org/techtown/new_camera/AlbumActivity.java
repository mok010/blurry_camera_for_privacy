package org.techtown.a3rdpage;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;

public class AlbumActivity extends AppCompatActivity {

    ImageView imageView;
    int image_Val=0;    //이미지를 선택하지 않고 처리하는 경우를 방지하기 위한 카운트, 1이 아니면 사진을 고르지 않았다고 판별.

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_album);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        //이전화면으로 돌아가는 버튼
        Button button=findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        //갤러리 화면 띄우는 버튼
        Button button2=findViewById(R.id.button2);
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent, 1);
            }
        });

        //앨범에서 선택한 이미지 표시하는 뷰
        imageView = findViewById(R.id.imageView);

        //블러 처리를 시작하는 버튼
        Button button3 = findViewById(R.id.button3);
        button3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(image_Val==1) {
                    // ProgressDialog 생성
                    ProgressDialog dialog = new ProgressDialog(AlbumActivity.this);
                    dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                    dialog.setMessage("보호중입니다...");
                    dialog.show();


                    //지금은 변환과정이 없기 때문에 임시로 딜레이를 추가하였습니다.
                    //이미지 변환과정이 있을 시에는 변환 과정이 끝나는 조건 하에 딜레이를 끝내는 쪽으로 작성하면 됩니다.
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            // ProgressDialog 없애기
                            dialog.dismiss();
                            Toast.makeText(AlbumActivity.this, "변환이 완료되었습니다:).", Toast.LENGTH_SHORT).show();
                        }
                    }, 5000);
                    image_Val=0;
                } else{
                    Toast.makeText(AlbumActivity.this, "이미지를 선택하지 않았습니다.", Toast.LENGTH_SHORT).show();
                }

            }
        });
    }

    //앨범에서 사진을 선택하면 uri를 따오고, uri를 bitmap으로 변환하여, imageView에 띄우는 함수
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch(requestCode) {
            case 1:
                if (resultCode == RESULT_OK) {
                    Uri uri = data.getData();
                    Log.d("이미지주소", String.valueOf(uri));
                    Bitmap photoBitmap = getBitmapFromUri(uri);
                    imageView.setImageBitmap(photoBitmap);    // 선택한 이미지 이미지뷰에 셋
                    image_Val=1;
                }
                break;
        }
    }

    //이미지 uri를 bitmap형태로 저장하는 함수
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