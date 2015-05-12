package com.jiacorp.howold;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;


public class FaceActivity extends AppCompatActivity implements FaceppDetect.DetectCallback {

    private static String TAG = FaceActivity.class.getName();

    @Inject
    FaceService mFaceService;

    @InjectView(R.id.image_view)
    ImageView mImageView;

    @InjectView(R.id.fab)
    ImageButton mFab;

    private List<Person> mPersons;

    private Uri mPath;
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

    private String[] messages;

    ProgressDialog mDialog;

    private Uri mShareUri;
    private Tracker mTracker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face);
        ActivityCompat.postponeEnterTransition(this);

        ButterKnife.inject(this);
        ((MyApplication) getApplication()).inject(this);

        mDialog = new ProgressDialog(this);

        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        mTracker = ((MyApplication)getApplication()).getTracker();


        if (savedInstanceState != null) {
            mShareUri = savedInstanceState.getParcelable("uri");
        }

        mFab.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.abc_ic_menu_share_mtrl_alpha, null));

        if (Util.atLeastLollipop()) {
            getWindow().setStatusBarColor(getResources().getColor(R.color.black));
        }


        mApiKey = getString(R.string.face_api_key);
        mApiSecret = getString(R.string.face_api_secret);

        mIconWidth = getResources().getDimensionPixelOffset(R.dimen.icon_width);
        mIconHeight = getResources().getDimensionPixelOffset(R.dimen.icon_height);

        mLabelWidth = getResources().getDimensionPixelOffset(R.dimen.label_width);
        mLabelHeight = getResources().getDimensionPixelOffset(R.dimen.label_height);

        messages = getResources().getStringArray(R.array.loading_messages);

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


        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if (type.startsWith("image/")) {

                mTracker.send(new HitBuilders.EventBuilder()
                        .setCategory(GoogleAnalytics.CAT_FACE)
                        .setAction(GoogleAnalytics.ACTION_INCOMING_SHARE)
                        .build());

                Uri imageUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                if (imageUri != null) {
                    mPath = imageUri;
                } else {
                    Log.e(TAG, "shared intent received, but there is no stream uri");
                    finish();
                }
            } else {
                Log.e(TAG, "shared intent received, it is not of image type");
                finish();
            }
        } else {
            // Handle other intents, such as being started from the home screen

            mTracker.send(new HitBuilders.EventBuilder()
                    .setCategory(GoogleAnalytics.CAT_FACE)
                    .setAction(GoogleAnalytics.ACTION_LAUNCHED)
                    .build());

            mPath = intent.getExtras().getParcelable("path");
        }

        if (mShareUri == null) {
            loadOriginalImage();
        } else {
            //there is already a detected image, saved in mShareUri
            loadDetectedImage();
        }


    }

    @Override
    public void onPause() {
        super.onPause();

        if ((mDialog != null) && mDialog.isShowing())
            mDialog.dismiss();
        mDialog = null;
    }

    private void loadDetectedImage() {
        Log.d(TAG, "loadDetectedImage");
        Glide.with(this)
                .load(mShareUri.getPath())
                .into(mImageView);
    }

    private void loadOriginalImage() {
        Log.d(TAG, "loadOriginalImage");

        if (mPath.toString().contains("content")) {
            //must load the image via URI, this is probably an image received as an incoming share
            Glide.with(this)
                    .load(mPath)
                    .asBitmap()
                    .listener(new RequestListener<Uri, Bitmap>() {
                        @Override
                        public boolean onException(Exception e, Uri model, Target<Bitmap> target, boolean isFirstResource) {
                            Log.e(TAG, "exception loading image with path:" + mPath.getPath() + " :: " + e.getMessage());
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Bitmap resource, Uri model, Target<Bitmap> target, boolean isFromMemoryCache, boolean isFirstResource) {
                            ActivityCompat.startPostponedEnterTransition(FaceActivity.this);
                            mBitmap = resource;
                            detectFace(resource);
                            return false;
                        }
                    })
                    .into(mImageView);

        } else {
            //this is loading from the main list, it's a local file. Load via path
            Glide.with(this)
                    .load(mPath.getPath())
                    .asBitmap()
                    .listener(new RequestListener<String, Bitmap>() {
                        @Override
                        public boolean onException(Exception e, String model, Target<Bitmap> target, boolean isFirstResource) {
                            Log.e(TAG, "exception loading image with path:" + mPath.getPath() + " :: " + e.getMessage());
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
    }


    private void detectFace(Bitmap image) {
        faceDetect(image);
    }

    private void faceDetect(Bitmap image) {
        Random ran = new Random();
        mDialog.setMessage(messages[ran.nextInt(messages.length)]);
        mDialog.setCancelable(false);
        mDialog.show();

        FaceppDetect detect = new FaceppDetect(this, mApiKey, mApiSecret);
        detect.detect(image);


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
        storeDetectedImage(mBitmap);

        FaceActivity.this.runOnUiThread(new Runnable() {

            public void run() {
                //show the image
                mImageView.setImageBitmap(mBitmap);
                Log.d(TAG, "finished drawing " + mPersons.size() + " faces onto the photo");
            }
        });

    }

    @Override
    public void detectResult(JSONObject rst) {
        if (mDialog != null) {
            mDialog.dismiss();
        }

        if (rst == null) {
            mTracker.send(new HitBuilders.EventBuilder()
                    .setCategory(GoogleAnalytics.CAT_FACE)
                    .setAction(GoogleAnalytics.ACTION_FACE_DETECTION)
                    .setLabel(GoogleAnalytics.LABEL_FAILED)
                    .build());
            handleError();
            return;
        }

        mTracker.send(new HitBuilders.EventBuilder()
                .setCategory(GoogleAnalytics.CAT_FACE)
                .setAction(GoogleAnalytics.ACTION_FACE_DETECTION)
                .setLabel(GoogleAnalytics.LABEL_SUCCESS)
                .build());

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

            if (!mPersons.isEmpty()) {
                drawFaces();
            } else {
                mTracker.send(new HitBuilders.EventBuilder()
                        .setCategory(GoogleAnalytics.CAT_FACE)
                        .setAction(GoogleAnalytics.ACTION_FACE_DETECTION)
                        .setLabel(GoogleAnalytics.LABEL_NO_FACES)
                        .build());
                handleError();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void handleError() {
        if (mPersons != null && !mPersons.isEmpty()) {
            return;
        }
        runOnUiThread(() -> {
            Toast.makeText(FaceActivity.this, "Unable to detect any faces, please try a different photo", Toast.LENGTH_LONG).show();
            mFab.setVisibility(View.GONE);
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelable("uri", mShareUri);
    }

    @OnClick(R.id.fab)
    public void share() {
        if (mShareUri == null) {
            return;
        }

        mTracker.send(new HitBuilders.EventBuilder()
                .setCategory(GoogleAnalytics.CAT_FACE)
                .setAction(GoogleAnalytics.ACTION_SHARE)
                .build());

        Log.d(TAG, "sharing image");

        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);

        shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, getResources().getString(R.string.share_text));
        shareIntent.putExtra(Intent.EXTRA_STREAM, mShareUri);
        shareIntent.setType("image/jpeg");
        startActivity(Intent.createChooser(shareIntent, getResources().getText(R.string.send_to)));
    }

    private void storeDetectedImage(Bitmap image) {
        File pictureFile = getOutputMediaFile();
        if (pictureFile == null) {
            Log.d(TAG,
                    "Error creating media file, check storage permissions: ");// e.getMessage());
            return;
        }
        try {
            FileOutputStream fos = new FileOutputStream(pictureFile);
            image.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.close();

            Log.d(TAG, "Temp photo stored here:" + pictureFile.getAbsolutePath());
            mShareUri = Uri.fromFile(pictureFile);

        } catch (FileNotFoundException e) {
            Log.d(TAG, "File not found: " + e.getMessage());
        } catch (IOException e) {
            Log.d(TAG, "Error accessing file: " + e.getMessage());
        }
    }

    /** Create a File for saving an image or video */
    private  File getOutputMediaFile(){
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.
        File mediaStorageDir = new File(((MyApplication)getApplication()).getPrivateAppDirectory());

        // Create the storage directory if it does not exist
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                return null;
            }
        }
        // Create a media file name
        String timeStamp = new SimpleDateFormat("ddMMyyyy_HHmm").format(new Date());
        File mediaFile;
        String mImageName="MI_"+ timeStamp +".jpg";
        mediaFile = new File(mediaStorageDir.getPath() + File.separator + mImageName);
        return mediaFile;
    }

}
