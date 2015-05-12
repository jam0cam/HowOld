package com.jiacorp.howold;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.util.Log;

import com.facepp.http.HttpRequests;
import com.facepp.http.PostParameters;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;

public class FaceppDetect {
    private static final String TAG = FaceppDetect.class.getName();


    public interface DetectCallback {
        void detectResult(JSONObject rst);
    }

    DetectCallback callback = null;
    String apiKey;
    String apiSecret;

    public FaceppDetect(DetectCallback detectCallback, String key, String secret) {
        callback = detectCallback;
        apiKey = key;
        apiSecret = secret;
    }

    public void detect(final Bitmap image) {


        new Thread(new Runnable() {

            public void run() {
                HttpRequests httpRequests = new HttpRequests(apiKey, apiSecret, false, false);
                //Log.v(TAG, "image size : " + img.getWidth() + " " + img.getHeight());

                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                float scale = Math.min(1, Math.min(600f / image.getWidth(), 600f / image.getHeight()));
                Matrix matrix = new Matrix();
                matrix.postScale(scale, scale);

                Bitmap imgSmall = Bitmap.createBitmap(image, 0, 0, image.getWidth(), image.getHeight(), matrix, false);
                //Log.v(TAG, "imgSmall size : " + imgSmall.getWidth() + " " + imgSmall.getHeight());

                imgSmall.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                byte[] array = stream.toByteArray();

                try {
                    //detect
                    JSONObject result = httpRequests.detectionDetect(new PostParameters().setImg(array));
                    //finished , then call the callback function
                    if (callback != null) {
                        callback.detectResult(result);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.d(TAG, "network error" + e.getMessage());
                    if (callback != null) {
                        callback.detectResult(null);
                    }
                }

            }
        }).start();
    }
}
