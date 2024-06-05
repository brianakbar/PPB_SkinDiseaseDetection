package com.example.skindiseasedetection;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Base64;

import java.io.ByteArrayOutputStream;

public class ClassificationData implements Parcelable {
    public String classification;
    public float confidence;
    public Bitmap image;
    public float imageRotation;

    public ClassificationData() {
    }

    // Parcelable implementation
    protected ClassificationData(Parcel in) {
        classification = in.readString();
        confidence = in.readFloat();
        imageRotation = in.readFloat();
        byte[] byteArray = in.createByteArray();
        if (byteArray != null) {
            image = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
        }
    }

    public static final Creator<ClassificationData> CREATOR = new Creator<ClassificationData>() {
        @Override
        public ClassificationData createFromParcel(Parcel in) {
            return new ClassificationData(in);
        }

        @Override
        public ClassificationData[] newArray(int size) {
            return new ClassificationData[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(classification);
        dest.writeFloat(confidence);
        dest.writeFloat(imageRotation);

        if (image != null) {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            image.compress(Bitmap.CompressFormat.PNG, 100, stream);
            byte[] byteArray = stream.toByteArray();
            dest.writeByteArray(byteArray);
        }
    }
}