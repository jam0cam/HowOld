package com.jiacorp.howold;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
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
import uk.co.senab.photoview.PhotoViewAttacher;


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

    private int mDefaultIconWidth;
    private int mDefaultIconHeight;

    private int mDefaultLabelWidth;
    private int mDefaultLabelHeight;

    private float mDefaultTextSize;

    private Paint mOrangePaint;
    private Paint mRedPaint;
    private Paint mMalePaint;
    private Paint mFemalePaint;

    private String[] messages;

    MaterialProgressDialog mDialog;

    private Uri mShareUri;
    private Tracker mTracker;
    private boolean mPaused;
    PhotoViewAttacher mAttacher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPaused = false;
        setContentView(R.layout.activity_face);
        ActivityCompat.postponeEnterTransition(this);

        ButterKnife.inject(this);
        ((MyApplication) getApplication()).inject(this);

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

        mDefaultIconWidth = getResources().getDimensionPixelOffset(R.dimen.icon_width);
        mDefaultIconHeight = getResources().getDimensionPixelOffset(R.dimen.icon_height);
        mDefaultTextSize = getResources().getDimension(R.dimen.age_text_size);

        mDefaultLabelWidth = getResources().getDimensionPixelOffset(R.dimen.label_width);
        mDefaultLabelHeight = getResources().getDimensionPixelOffset(R.dimen.label_height);

        messages = getResources().getStringArray(R.array.loading_messages);

        mFemaleDrawable = ResourcesCompat.getDrawable(getResources(), R.drawable.female_sixty, null);
        mMaleDrawable = ResourcesCompat.getDrawable(getResources(), R.drawable.male_sixty, null);

        mOrangePaint = new Paint();
        mOrangePaint.setStyle(Paint.Style.FILL);
        mOrangePaint.setColor(getResources().getColor(R.color.orange));

        //use the red paint
        mRedPaint = new Paint();
        mRedPaint.setColor(Color.RED);
        mRedPaint.setStrokeWidth(5);
        mRedPaint.setStyle(Paint.Style.STROKE);

        mMalePaint = new Paint();
        mMalePaint.setStyle(Paint.Style.FILL);
        mMalePaint.setColor(getResources().getColor(R.color.male_blue));

        mFemalePaint = new Paint();
        mFemalePaint.setStyle(Paint.Style.FILL);
        mFemalePaint.setColor(getResources().getColor(R.color.female_pink));

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

    private void setupProgressDialog() {
        mDialog = new MaterialProgressDialog(this);
        mDialog.setOnCancelListener(dialog -> {
            Log.d(TAG, "cancelled");
            mDialog = null;
            finish();
        });

        mDialog.setOnDismissListener(dialog -> {
            Log.d(TAG, "dismissed");
            mDialog = null;
        });

    }

    @Override
    protected void onResume() {
        mPaused = false;
        super.onResume();
    }

    @Override
    public void onBackPressed() {
        Log.d(TAG, "onBackPressed");
        mImageView.getViewTreeObserver().removeOnGlobalLayoutListener(mAttacher);
        mAttacher = null;

        super.onBackPressed();
    }

    @Override
    public void onPause() {
        Log.d(TAG, "onPause");
        super.onPause();

        mPaused = true;
        if ((mDialog != null) && mDialog.isShowing())
            mDialog.dismiss();
        mDialog = null;
    }

    private void loadDetectedImage() {
        Log.d(TAG, "loadDetectedImage");
        Glide.with(this)
                .load(mShareUri.getPath())
                .listener(new RequestListener<String, GlideDrawable>() {
                    @Override
                    public boolean onException(Exception e, String model, Target<GlideDrawable> target, boolean isFirstResource) {
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(GlideDrawable resource, String model, Target<GlideDrawable> target, boolean isFromMemoryCache, boolean isFirstResource) {
                        mAttacher = new PhotoViewAttacher(mImageView);
                        mAttacher.update();
                        return false;
                    }
                })
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

        if (mDialog == null) {
            setupProgressDialog();
        }
        mDialog.setMessage(messages[ran.nextInt(messages.length)]);
        new Handler().postDelayed(() -> mDialog.show(), 500);

        FaceppDetect detect = new FaceppDetect(this, mApiKey, mApiSecret);
        detect.detect(image);
    }

    private void drawFaces() {
        if (mPersons == null || mPersons.isEmpty()) {
            return;
        }

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

            p.faceBox = new Box(x-w, y-h, x+w, y+h);

            //draw the box to mark it out
            canvas.drawRect(p.faceBox.left, p.faceBox.top, p.faceBox.right, p.faceBox.bottom, mRedPaint);

            //coordinates for female icon drawing
            int left = (int)(x-w+20);
            int top = (int) (y+h+10);
            int right = left + mDefaultIconWidth;

            float labelWidth;
            float labelHeight;
            float iconWidth;
            float iconHeight;
            int leftIconPadding;
            int topBottomIconPadding;

            //this is the case where the face box is smaller than the label width we want to display,
            //so we should make everything else smaller.
            if (mDefaultLabelWidth > p.faceBox.getWidth()) {
                labelWidth = p.faceBox.getWidth();
                labelHeight = labelWidth / 1.6f;    //this is the width/height ratio
                leftIconPadding = 10;
                topBottomIconPadding = 5;
                iconWidth = labelWidth /4 ;
                iconHeight = iconWidth * 2;

                float textWidth = labelWidth - leftIconPadding - iconWidth - 2*leftIconPadding;
                float textHeight = labelHeight - 3*topBottomIconPadding;
                int textSize = determineMaxTextSize(String.valueOf(p.age), textWidth, textHeight);
                mFemalePaint.setTextSize(textSize);
                mMalePaint.setTextSize(textSize);

            } else {    //the face box is big, so use default values
                labelWidth = mDefaultLabelWidth;
                labelHeight = mDefaultLabelHeight;
                leftIconPadding = 20;
                topBottomIconPadding = 10;
                iconWidth = mDefaultIconWidth;
                iconHeight = mDefaultIconHeight;
                mFemalePaint.setTextSize(mDefaultTextSize);
                mMalePaint.setTextSize(mDefaultTextSize);
            }

            p.labelBox = new Box(x-w, y+h, x-w + labelWidth, y + h + labelHeight);

            //draws orange rectangle
            canvas.drawRect(p.labelBox.left, p.labelBox.top, p.labelBox.right, p.labelBox.bottom, mOrangePaint);


            //find the bounds of the drawable icon
            p.iconBox = new Box(p.labelBox.left + leftIconPadding, p.labelBox.top + topBottomIconPadding,
                    p.labelBox.left + leftIconPadding + iconWidth, p.labelBox.top + topBottomIconPadding + iconHeight);

            if (p.gender.equalsIgnoreCase("female")) {
                Log.d(TAG, "female detected");

                //draws female icon on top of the rectangle
                mFemaleDrawable.setBounds((int)p.iconBox.left, (int)p.iconBox.top, (int)p.iconBox.right, (int)p.iconBox.bottom);
                mFemaleDrawable.draw(canvas);

                //draws age in the rectangle
                canvas.drawText(String.valueOf(p.age), p.iconBox.right + leftIconPadding,
                        p.iconBox.bottom - topBottomIconPadding , mFemalePaint);
            } else {

                //draws female icon on top of the rectangle
                mMaleDrawable.setBounds((int)p.iconBox.left, (int)p.iconBox.top, (int)p.iconBox.right, (int)p.iconBox.bottom);
                mMaleDrawable.draw(canvas);

                //draws age in the rectangle
                canvas.drawText(String.valueOf(p.age), p.iconBox.right + leftIconPadding,
                        p.iconBox.bottom - topBottomIconPadding , mMalePaint);
            }


        }

        //save new image
        mBitmap = bitmap;
        storeDetectedImage(mBitmap);

        FaceActivity.this.runOnUiThread(() -> {
            //show the image
            mImageView.setImageBitmap(mBitmap);

            mAttacher = new PhotoViewAttacher(mImageView);
            mAttacher.update();

            Log.d(TAG, "finished drawing " + mPersons.size() + " faces onto the photo");
        });
    }

    /**
     * Retrieve the maximum text size to fit in a given width.
     * @param str (String): Text to check for size.
     * @param maxWidth (float): Maximum allowed width.
     * @param maxHeight
     * @return (int): The desired text size.
     */
    private int determineMaxTextSize(String str, float maxWidth, float maxHeight)
    {
        int size = 0;
        Paint paint = new Paint();


        if (str.length() > 1) {
            do {
                paint.setTextSize(++ size);
            } while(paint.measureText(str) < maxWidth);
        } else {
            //for single digit, worry about the height instead.
            Rect rect = new Rect();
            do {
                paint.setTextSize(++ size);
                paint.getTextBounds(str, 0, 1, rect);
            } while(rect.height() < maxHeight);

            size--;
        }

        return size;
    }

    @Override
    public void detectResult(JSONObject rst) {
        if (mDialog != null && mDialog.isShowing()) {
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

            if (!mPersons.isEmpty() && !mPaused) {
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

        if (!mPaused) {
            runOnUiThread(() -> {
                Toast.makeText(FaceActivity.this, "Unable to detect any faces, please try a different photo", Toast.LENGTH_LONG).show();
                mFab.setVisibility(View.GONE);
            });
        }
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
