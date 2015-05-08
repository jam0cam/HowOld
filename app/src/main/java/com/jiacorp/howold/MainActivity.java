package com.jiacorp.howold;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageButton;

import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getName();

    @InjectView(R.id.grid)
    GridView mGridview;

    @InjectView(R.id.toolbar)
    Toolbar mToolbar;

    @InjectView(R.id.fab)
    ImageButton mFab;

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

        mGridview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(MainActivity.this, FaceActivity.class);
                intent.putExtra("path", imagePaths.get(position));

                String transitionName = getString(R.string.transition_name);

                if (Util.atLeastLollipop()) {
                    view.setTransitionName(transitionName);
                }

                ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(MainActivity.this, view, transitionName);
                ActivityCompat.startActivity(MainActivity.this, intent, options.toBundle());

            }
        });
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
