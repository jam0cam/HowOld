package com.jiacorp.howold;

import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.HandlerThread;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.faceplusplus.api.FaceDetecter;
import com.google.gson.JsonObject;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;


public class FaceActivity extends AppCompatActivity implements FaceppDetect.DetectCallback {

    private static String TAG = FaceActivity.class.getName();

    @Inject
    FaceService mFaceService;

    @InjectView(R.id.image_view)
    ImageView mImageView;

    private List<Person> mPersons;

    private String mPath;
    private Bitmap mBitmap;
    private String mApiKey;
    private String mApiSecret;

    private Drawable mMaleDrawable;
    private Drawable mFemaleDrawable;

    private int mIconWidth;
    private int mIconHeight;

    private int mLabelWidth;
    private int mLabelHeight;

    private Paint mOrangePaint;
    private Paint mMalePaint;
    private Paint mFemalePaint;

    HandlerThread detectThread = null;
    private String[] messages;

    ProgressDialog mDialog;

    static {
        System.loadLibrary("faceppapi");
        System.loadLibrary("offlineapi");
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face);

        ButterKnife.inject(this);

        mPath = getIntent().getExtras().getString("path");

        mApiKey = getString(R.string.face_api_key);
        mApiSecret = getString(R.string.face_api_secret);

        mIconWidth = getResources().getDimensionPixelOffset(R.dimen.icon_width);
        mIconHeight = getResources().getDimensionPixelOffset(R.dimen.icon_height);

        mLabelWidth = getResources().getDimensionPixelOffset(R.dimen.label_width);
        mLabelHeight = getResources().getDimensionPixelOffset(R.dimen.label_height);

        messages = getResources().getStringArray(R.array.loading_messages);

        ActivityCompat.postponeEnterTransition(this);

        loadImage();

        ((MyApplication) getApplication()).inject(this);

        mFemaleDrawable = ResourcesCompat.getDrawable(getResources(), R.drawable.female_sixty, null);
        mMaleDrawable = ResourcesCompat.getDrawable(getResources(), R.drawable.male_sixty, null);

        mOrangePaint = new Paint();
        mOrangePaint.setStyle(Paint.Style.FILL);
        mOrangePaint.setColor(getResources().getColor(R.color.orange));

        mMalePaint = new Paint();
        mMalePaint.setStyle(Paint.Style.FILL);
        mMalePaint.setColor(getResources().getColor(R.color.male_blue));
        mMalePaint.setTextSize(getResources().getDimension(R.dimen.age_text_size));

        mFemalePaint = new Paint();
        mFemalePaint.setStyle(Paint.Style.FILL);
        mFemalePaint.setColor(getResources().getColor(R.color.female_pink));
        mFemalePaint.setTextSize(getResources().getDimension(R.dimen.age_text_size));

    }

    private void loadImage() {
        Glide.with(this)
                .load(mPath)
                .asBitmap()
                .listener(new RequestListener<String, Bitmap>() {
                    @Override
                    public boolean onException(Exception e, String model, Target<Bitmap> target, boolean isFirstResource) {
                        Log.e(TAG, "exception loading image" + e.getMessage());
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Bitmap resource, String model, Target<Bitmap> target, boolean isFromMemoryCache, boolean isFirstResource) {
                        ActivityCompat.startPostponedEnterTransition(FaceActivity.this);
                        mBitmap = resource;
                        detectFace(resource);
                        return false;
                    }
                })
                .into(mImageView);
    }


    private void detectFace(Bitmap image) {
        faceDetect(image);
    }


    private void localMyDetect(Bitmap image) {


        float scale = Math.min(1, Math.min(600f / image.getWidth(), 600f / image.getHeight()));
        Matrix matrix = new Matrix();
        matrix.postScale(scale, scale);

        Bitmap imgSmall = Bitmap.createBitmap(image, 0, 0, image.getWidth(), image.getHeight(), matrix, false);
        //Log.v(TAG, "imgSmall size : " + imgSmall.getWidth() + " " + imgSmall.getHeight());


        FaceDetecter detecter = new FaceDetecter();
//        detecter.init(this, "5mizjzcsify5094mocb4");
        detecter.init(this, mApiKey);

        FaceDetecter.Face[] faceinfo = detecter.findFaces(imgSmall);// 进行人脸检测

        Log.d(TAG, "face found");
    }

    private Bitmap convert(Bitmap bitmap, Bitmap.Config config) {
        Bitmap convertedBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), config);
        Canvas canvas = new Canvas(convertedBitmap);
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        canvas.drawBitmap(bitmap, 0, 0, paint);
        return convertedBitmap;
    }

    private void retrofitDetect(Bitmap image) {

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        float scale = Math.min(1, Math.min(600f / image.getWidth(), 600f / image.getHeight()));
        Matrix matrix = new Matrix();
        matrix.postScale(scale, scale);

        Bitmap imgSmall = Bitmap.createBitmap(image, 0, 0, image.getWidth(), image.getHeight(), matrix, false);
        //Log.v(TAG, "imgSmall size : " + imgSmall.getWidth() + " " + imgSmall.getHeight());

        imgSmall.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        byte[] array = stream.toByteArray();

        String url = "https://pbs.twimg.com/profile_images/1731241160/image.jpg";
        mFaceService.detectFaceImage(array, "glass,pose,gender,age,race,smiling")
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.newThread())
                .subscribe(new Subscriber<JsonObject>() {
                    @Override
                    public void onCompleted() {


                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e(TAG, "error:" + e.getMessage());
                    }

                    @Override
                    public void onNext(JsonObject jsonObject) {

                        Log.d(TAG, "rval received");
                    }
                });

    }

    private void faceDetect(Bitmap image) {
        FaceppDetect detect = new FaceppDetect(this, mApiKey, mApiSecret);
        detect.detect(image);

        mDialog = new ProgressDialog(this);
        Random ran = new Random();
        mDialog.setMessage(messages[ran.nextInt(messages.length)]);
        mDialog.setCancelable(false);
        mDialog.show();

    }

    private void drawFaces() {
        if (mPersons == null || mPersons.isEmpty()) {
            return;
        }

        //use the red paint
        Paint paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStrokeWidth(Math.max(mBitmap.getWidth(), mBitmap.getHeight()) / 100f);

        //create a new canvas
        Bitmap bitmap = Bitmap.createBitmap(mBitmap.getWidth(), mBitmap.getHeight(), mBitmap.getConfig());
        Canvas canvas = new Canvas(bitmap);
        canvas.drawBitmap(mBitmap, new Matrix(), null);


        for (Person p : mPersons) {
            float x, y, w, h;
            //get the center point
            x = p.centerX;
            y = p.centerY;

            //get face size
            w = p.width;
            h = p.height;

            //change percent value to the real size
            x = x / 100 * mBitmap.getWidth();
            w = w / 100 * mBitmap.getWidth() * 0.7f;
            y = y / 100 * mBitmap.getHeight();
            h = h / 100 * mBitmap.getHeight() * 0.7f;

            //draw the box to mark it out
            canvas.drawLine(x - w, y - h, x - w, y + h, paint);
            canvas.drawLine(x - w, y - h, x + w, y - h, paint);
            canvas.drawLine(x + w, y + h, x - w, y + h, paint);
            canvas.drawLine(x + w, y + h, x + w, y - h, paint);

            //coordinates for female icon drawing
            int left = (int)(x-w+20);
            int top = (int) (y+h+10);
            int right = left + mIconWidth;

            //draws orange rectangle
            canvas.drawRect(x - w, y + h, x - w + mLabelWidth, y + h + mLabelHeight, mOrangePaint);

            if (p.gender.equalsIgnoreCase("female")) {
                Log.d(TAG, "female detected");

                //draws female icon on top of the rectangle
                mFemaleDrawable.setBounds(left, top, right, top + mIconHeight);
                mFemaleDrawable.draw(canvas);

                //draws age in the rectangle
                canvas.drawText(String.valueOf(p.age), right + 20, top + mIconHeight - 20 , mFemalePaint);
            } else {

                //draws female icon on top of the rectangle
                mMaleDrawable.setBounds(left, top, right, top + mIconHeight);
                mMaleDrawable.draw(canvas);

                //draws age in the rectangle
                canvas.drawText(String.valueOf(p.age), right + 20, top + mIconHeight - 20 , mMalePaint);
            }


        }

        //save new image
        mBitmap = bitmap;

        FaceActivity.this.runOnUiThread(new Runnable() {

            public void run() {
                //show the image
                mImageView.setImageBitmap(mBitmap);
                Log.d(TAG, "finished drawing " + mPersons.size() + " faces onto the photo");
            }
        });

    }

    private void localDetect(Bitmap image) {
//
//        detecter = new FaceDetecter();
//        detecter.init(this, mApiKey);
//
//        Face[] faceinfo = detecter.findFaces(image);// 进行人脸检测
//
//        Log.d(TAG, "face found");
    }

    @Override
    public void detectResult(JSONObject rst) {
        if (rst == null) {
            showError();
        }

        try {
            JSONArray arr = (JSONArray) rst.get("face");

            mPersons = new ArrayList<>();

            for (int i=0; i<arr.length(); i++) {
                JSONObject obj = (JSONObject) arr.get(i);

                Person p = new Person();
                p.age = (int) ((JSONObject)((JSONObject)obj.get("attribute")).get("age")).get("value");
                p.gender = (String) ((JSONObject)((JSONObject)obj.get("attribute")).get("gender")).get("value");
                p.race = (String) ((JSONObject)((JSONObject)obj.get("attribute")).get("race")).get("value");

                p.centerX =  Float.parseFloat (((JSONObject)((JSONObject)obj.get("position")).get("center")).getString("x"));
                p.centerY = Float.parseFloat (((JSONObject)((JSONObject)obj.get("position")).get("center")).getString("y"));
                p.width = Float.parseFloat (((JSONObject)obj.get("position")).getString("width"));
                p.height = Float.parseFloat(((JSONObject) obj.get("position")).getString("height"));

                mPersons.add(p);

                Log.d(TAG, "Person received: age:" + p.age + ", gender:" + p.gender + ", race:" + p.race + ", X:"
                        + p.centerX + ", Y:" + p.centerY + ", W:" + p.width + ", H:" + p.height);
            }

            mDialog.dismiss();

            if (!mPersons.isEmpty()) {
                drawFaces();
            } else {
                showError();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void showError() {
        runOnUiThread(() -> Toast.makeText(FaceActivity.this, "Unable to detect any faces, please try a different photo", Toast.LENGTH_LONG).show());
    }
//
//    @Override
//    protected void onDestroy() {
//        super.onDestroy();
//        detecter.release(this);// 释放引擎
//    }

}
