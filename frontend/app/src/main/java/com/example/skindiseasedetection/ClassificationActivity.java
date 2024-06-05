package com.example.skindiseasedetection;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class ClassificationActivity extends AppCompatActivity {
    private Button buttonBack;
    private TextView textClassification;
    private TextView textConfidence;
    private ImageView selectedImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_classification);
        buttonBack = findViewById(R.id.btn_back);
        textClassification = findViewById(R.id.classification);
        textConfidence = findViewById(R.id.confidence);
        selectedImage = findViewById(R.id.img_selected);

        buttonBack.setOnClickListener(v -> finish());

        Intent intent = getIntent();
        ClassificationData data = intent.getParcelableExtra("classificationData");
        if (data == null) return;

        textClassification.setText(data.classification);
        textConfidence.setText(String.valueOf(data.confidence));

        Bitmap image = rotateImageBitmap(data.image, data.imageRotation);
        if (image != null) selectedImage.setImageBitmap(image);
    }

    private Bitmap rotateImageBitmap(Bitmap bitmap, float angle) {
        if (bitmap == null) return null;

        Matrix matrix = new Matrix();
        matrix.postRotate(angle);

        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }
}
