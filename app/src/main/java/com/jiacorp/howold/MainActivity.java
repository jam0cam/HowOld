package com.jiacorp.howold;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageButton;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getName();

    @InjectView(R.id.grid)
    GridView mGridview;

    @InjectView(R.id.toolbar)
    Toolbar mToolbar;

    @InjectView(R.id.fab)
    ImageButton mFab;

    List<String> mImagePaths;
    GridAdapter mAdapter;
    private Uri mNewPhotoUri;
    private String mCurrentPhotoPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.inject(this);


        mFab.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_camera_white, null));

        fetchImagePaths();

        //TODO: JIA: handle the case where there are no photos

        mAdapter = new GridAdapter(this, 0, mImagePaths);
        mGridview.setAdapter(mAdapter);

        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle(getString(R.string.your_photos));

        mGridview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(MainActivity.this, FaceActivity.class);
                intent.putExtra("path", mImagePaths.get(position));

                String transitionName = getString(R.string.transition_name);

                if (Util.atLeastLollipop()) {
                    view.setTransitionName(transitionName);
                }

                ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(MainActivity.this, view, transitionName);
                ActivityCompat.startActivity(MainActivity.this, intent, options.toBundle());

            }
        });

        CleanupAsynTask task = new CleanupAsynTask();
        task.execute(((MyApplication)getApplication()).getPrivateAppDirectory());

    }

    @OnClick(R.id.fab)
    public void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
                Log.e(TAG, "error" + ex.getMessage());
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                        Uri.fromFile(photoFile));
                startActivityForResult(takePictureIntent, 1);
            }
        }
    }

    private void galleryAddPic() {
        Log.d(TAG, "Sending broadcast");
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(mCurrentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }

    private void insertPicAtFrontOfGrid() {
        mImagePaths.add(0, mCurrentPhotoPath);
        mAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {

            galleryAddPic();
            insertPicAtFrontOfGrid();
            Intent intent = new Intent(this, FaceActivity.class);
            intent.putExtra("path", mCurrentPhotoPath);
            startActivity(intent);
        }

    }

    private void fetchImagePaths() {
        final String[] columns = { MediaStore.Images.Media.DATA, MediaStore.Images.Media._ID };
        final String orderBy = MediaStore.Images.Media.DATE_ADDED + " desc";
        //Stores all the images from the gallery in Cursor
        Cursor cursor = getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, columns, null,
                null, orderBy);
        //Total number of images
        int count = cursor.getCount();

        //Create an array to store path to all the images
        mImagePaths = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            cursor.moveToPosition(i);
            int dataColumnIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
            //Store the path of the image
            String path  = cursor.getString(dataColumnIndex);
            mImagePaths.add(path);
            Log.i("PATH", path);
        }
        cursor.close();
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), getString(R.string.app_name));
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }
}
