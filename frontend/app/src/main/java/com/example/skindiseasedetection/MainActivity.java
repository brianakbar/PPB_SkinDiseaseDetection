package com.example.skindiseasedetection;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private Button buttonCamera;
    private Button buttonGallery;
    private Button buttonProceed;
    private ImageView selectedImage;
    private Bitmap selectImageBitmap = null;
    private float selectedImageRotation = 0f;
    private String serverURL = "http://192.168.100.147:5000";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        buttonCamera = findViewById(R.id.btn_camera);
        buttonGallery = findViewById(R.id.btn_gallery);
        buttonProceed = findViewById(R.id.btn_proceed);
        selectedImage = findViewById(R.id.img_selected);

        buttonCamera.setOnClickListener(this.onClickListener);
        buttonGallery.setOnClickListener(this.onClickListener);
        buttonProceed.setOnClickListener(this.onClickListener);
        selectedImage.setOnClickListener(this.onClickListener);
    }

    private View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int id = v.getId();
            if (id == R.id.btn_camera) {
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                launcherKamera.launch(intent);
            }
            else if (id == R.id.btn_gallery) {
                launcherGallery.launch("image/*");
            }
            else if (id == R.id.btn_proceed) {
                if (selectImageBitmap == null) return;

                classifyImage(selectImageBitmap);
            }
            else if (id == R.id.img_selected) {
                if (selectImageBitmap == null) return;

                selectedImageRotation += 90f;
                rotateImageBitmap(selectedImageRotation);
            }
        }
    };

    private ActivityResultLauncher<Intent> launcherKamera = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Intent data = result.getData();
                    if (data == null) {
                        return;
                    }

                    Bitmap bitmap = (Bitmap) data.getExtras().get("data");

                    showImage(bitmap);
                }
            }
    );

    private ActivityResultLauncher<String> launcherGallery = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);

                        showImage(bitmap);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
    );

    private void classifyImage(Bitmap bitmap) {
        Date date = new Date();
        String fileName = new SimpleDateFormat("yyyyMMdd-hh-mm-ss", Locale.US).format(date) + ".png";

        new Thread(() -> {
            HttpURLConnection connection;
            DataOutputStream writer = null;
            InputStream responseStream = null;

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

            try {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
                byte[] imageData = byteArrayOutputStream.toByteArray();

                URL url = new URL(MainActivity.this.serverURL);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "multipart/form-data;boundary=*****");
                connection.setRequestProperty("Accept", "application/json");
                connection.setDoOutput(true);

                writer = new DataOutputStream(connection.getOutputStream());
                writer.writeBytes("--*****\r\n");
                writer.writeBytes("Content-Disposition: form-data; name=\"foto\";filename=\"" + fileName + "\"\r\n");
                writer.writeBytes("\r\n");
                writer.write(imageData);
                writer.writeBytes("\r\n");
                writer.writeBytes("--*****--\r\n");

                int responseCode = connection.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this, "Error upload foto ke server + " + responseCode, Toast.LENGTH_SHORT).show();
                    });
                }

                responseStream = connection.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(responseStream));
                StringBuilder response = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                JSONObject jsonResponse = new JSONObject(response.toString());
                String predictedClass = jsonResponse.getString("predicted_class");
                float confidence = (float) jsonResponse.getDouble("confidence");
                runOnUiThread(() -> {
                    Intent intent = new Intent(MainActivity.this, ClassificationActivity.class);
                    ClassificationData data = new ClassificationData();
                    data.classification = predictedClass;
                    data.confidence = confidence;
                    data.image = selectImageBitmap;
                    data.imageRotation = selectedImageRotation;
                    intent.putExtra("classificationData", data);

                    startActivity(intent);
                });
            }
            catch (Exception e) {
                Log.d("Error", e.toString());
            }
            finally {
                try {
                    writer.close();
                }
                catch (Exception ignored) {}
            }
        }).start();
    }

    private void showImage(Bitmap image) {
        selectedImageRotation = 0f;
        selectImageBitmap = image;
        selectedImage.setImageBitmap(image);

        if (selectImageBitmap != null) buttonProceed.setVisibility(View.VISIBLE);
        else buttonProceed.setVisibility(View.INVISIBLE);
    }

    private void rotateImageBitmap(float angle) {
        if (selectImageBitmap == null) return;

        Matrix matrix = new Matrix();
        matrix.postRotate(angle);

        Bitmap rotatedBitmap = Bitmap.createBitmap(selectImageBitmap, 0, 0, selectImageBitmap.getWidth(), selectImageBitmap.getHeight(), matrix, true);

        selectedImage.setImageBitmap(rotatedBitmap);
    }
}