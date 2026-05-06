package com.example.greenchallenger;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CameraActivity extends AppCompatActivity {

    private ImageView imagePreview;
    private Button btnUpload, btnRetakePhoto;
    private Uri currentPhotoUri;

    private final ActivityResultLauncher<Uri> takePictureLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), isSaved -> {
                if (isSaved && currentPhotoUri != null) {
                    imagePreview.setImageURI(currentPhotoUri);
                    btnUpload.setEnabled(true);
                    Toast.makeText(this, "사진이 성공적으로 촬영되었습니다!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "사진 촬영이 취소되었습니다.", Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<String> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    openCamera();
                } else {
                    Toast.makeText(this, "카메라 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
                    finish();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        imagePreview = findViewById(R.id.imagePreview);
        btnUpload = findViewById(R.id.btnUpload);
        btnRetakePhoto = findViewById(R.id.btnRetakePhoto);

        btnUpload.setEnabled(false);
        requestCameraAndOpen();

        btnUpload.setOnClickListener(v -> {
            if (currentPhotoUri == null) {
                Toast.makeText(this, "먼저 사진을 촬영해주세요!", Toast.LENGTH_SHORT).show();
                return;
            }

            Toast.makeText(this, "사진 인증 완료!", Toast.LENGTH_SHORT).show();
            android.content.Intent resultIntent = new android.content.Intent();
            resultIntent.putExtra("photoUri", currentPhotoUri.toString());
            setResult(RESULT_OK, resultIntent);
            finish();
        });

        btnRetakePhoto.setOnClickListener(v -> requestCameraAndOpen());
    }

    private void requestCameraAndOpen() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            openCamera();
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void openCamera() {
        try {
            File photoFile = createImageFile();
            currentPhotoUri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    photoFile
            );
            takePictureLauncher.launch(currentPhotoUri);
        } catch (IOException e) {
            Toast.makeText(this, "사진 파일을 만들 수 없습니다.", Toast.LENGTH_SHORT).show();
        } catch (IllegalArgumentException e) {
            Toast.makeText(this, "카메라 설정을 확인해주세요.", Toast.LENGTH_SHORT).show();
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.KOREA).format(new Date());
        String imageFileName = "mission_" + timeStamp + "_";
        File storageDir = new File(getExternalFilesDir(null), "mission_photos");

        if (!storageDir.exists() && !storageDir.mkdirs()) {
            throw new IOException("Cannot create photo directory.");
        }

        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }
}

