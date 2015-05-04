package com.jiacorp.howold;

import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.widget.GridView;

import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;


public class MainActivity extends AppCompatActivity {

    @InjectView(R.id.grid)
    GridView mGridview;

    @InjectView(R.id.toolbar)
    Toolbar mToolbar;

    List<String> imagePaths;
    GridAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.inject(this);

        fetchImagePaths();

        //TODO: JIA: handle the case where there are no photos

        mAdapter = new GridAdapter(this, 0, imagePaths);
        mGridview.setAdapter(mAdapter);

        setSupportActionBar(mToolbar);

        getSupportActionBar().setTitle(getString(R.string.your_photos));
    }

    private void fetchImagePaths() {
        final String[] columns = { MediaStore.Images.Media.DATA, MediaStore.Images.Media._ID };
        final String orderBy = MediaStore.Images.Media._ID;
        //Stores all the images from the gallery in Cursor
        Cursor cursor = getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, columns, null,
                null, orderBy);
        //Total number of images
        int count = cursor.getCount();

        //Create an array to store path to all the images
        imagePaths = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            cursor.moveToPosition(i);
            int dataColumnIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
            //Store the path of the image
            String path  = cursor.getString(dataColumnIndex);
            imagePaths.add(path);
            Log.i("PATH", path);
        }
    }
}
