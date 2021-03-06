package com.jiacorp.howold;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;


public class MainActivity extends AppCompatActivity implements GridView.MultiChoiceModeListener, SwipeRefreshLayout.OnRefreshListener {
    private static final String TAG = MainActivity.class.getName();

    @InjectView(R.id.grid)
    GridView mGridview;

    @InjectView(R.id.toolbar)
    Toolbar mToolbar;

    @InjectView(R.id.fab)
    ImageButton mFab;

    @InjectView(R.id.empty_view)
    FrameLayout mEmptyView;

    @InjectView(R.id.swipe_container)
    SwipeRefreshLayout mSwipeContainer;

    List<String> mImagePaths;
    GridAdapter mAdapter;
    private String mCurrentPhotoPath;

    private Tracker mTracker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.inject(this);

        ArrayList<Integer> selectedItems = null;
        if (savedInstanceState != null) {
            mCurrentPhotoPath = savedInstanceState.getString("mCurrentPhotoPath");
            selectedItems = (ArrayList<Integer>) savedInstanceState.getSerializable("selectedItems");
        }

        mTracker = ((MyApplication)getApplication()).getTracker();

        mTracker.send(new HitBuilders.EventBuilder()
                .setCategory(GoogleAnalytics.CAT_MAIN)
                .setAction(GoogleAnalytics.ACTION_LAUNCHED)
                .build());

        mFab.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.mipmap.ic_camera_white, null));

        refreshList();

        //TODO: JIA: handle the case where there are no photos

        mGridview.setChoiceMode(GridView.CHOICE_MODE_MULTIPLE_MODAL);
        mGridview.setMultiChoiceModeListener(this);
        mGridview.setEmptyView(mEmptyView);
        if (selectedItems != null && !selectedItems.isEmpty()) {
            mAdapter.setSelectedItems(selectedItems);
            mAdapter.notifyDataSetChanged();
        }


        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle(getString(R.string.your_photos));

        mGridview.setOnItemClickListener((parent, view, position, id) -> {
            Intent intent = new Intent(MainActivity.this, FaceActivity.class);

            ImageView imageView = (ImageView) view.findViewById(R.id.image_view);

            Uri imageUri = new Uri.Builder()
                    .path(mImagePaths.get(position))
                    .build();

            intent.putExtra("path", imageUri);

            String transitionName = getString(R.string.transition_name);

            if (Util.atLeastLollipop()) {
                imageView.setTransitionName(transitionName);
            }

            mTracker.send(new HitBuilders.EventBuilder()
                    .setCategory(GoogleAnalytics.CAT_MAIN)
                    .setAction(GoogleAnalytics.ACTION_CLICKED_IMAGE)
                    .build());


            ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(MainActivity.this, imageView, transitionName);
            ActivityCompat.startActivity(MainActivity.this, intent, options.toBundle());

        });

        CleanupAsynTask task = new CleanupAsynTask();
        task.execute(((MyApplication)getApplication()).getPrivateAppDirectory());

        mSwipeContainer.setOnRefreshListener(this);
        mSwipeContainer.setColorSchemeResources(R.color.orange, R.color.orange, R.color.orange);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("mCurrentPhotoPath", mCurrentPhotoPath);

        if (mAdapter != null && !mAdapter.getSelectedItems().isEmpty()) {
            outState.putSerializable("selectedItems", mAdapter.getSelectedItems());
        }
    }

    @OnClick(R.id.fab)
    public void dispatchTakePictureIntent() {
        AnimatorUtils.revealAnimationOut(mFab);

        mTracker.send(new HitBuilders.EventBuilder()
                .setCategory(GoogleAnalytics.CAT_MAIN)
                .setAction(GoogleAnalytics.ACTION_NEW_PHOTO)
                .build());

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
        if (mCurrentPhotoPath != null) {
            //on rotation, this can be null
            Log.d(TAG, "Sending broadcast");
            Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            File f = new File(mCurrentPhotoPath);
            Uri contentUri = Uri.fromFile(f);
            mediaScanIntent.setData(contentUri);
            this.sendBroadcast(mediaScanIntent);
        }
    }

    private void insertPicAtFrontOfGrid() {
        if (mCurrentPhotoPath != null) {
            //on rotation, this can be null
            mImagePaths.add(0, mCurrentPhotoPath);
            mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        AnimatorUtils.revealAnimationIn(mFab);

        Log.d(TAG, "onActivityResult: " + mCurrentPhotoPath);
        if (resultCode == RESULT_OK) {
            mTracker.send(new HitBuilders.EventBuilder()
                    .setCategory(GoogleAnalytics.CAT_MAIN)
                    .setAction(GoogleAnalytics.ACTION_NEW_PHOTO)
                    .setLabel(GoogleAnalytics.LABEL_PHOTO_TAKEN)
                    .build());

            galleryAddPic();
            insertPicAtFrontOfGrid();
            Intent intent = new Intent(this, FaceActivity.class);

            Uri imageUri = new Uri.Builder()
                    .path(mCurrentPhotoPath)
                    .build();

            intent.putExtra("path", imageUri);
            startActivity(intent);
        } else {
            mTracker.send(new HitBuilders.EventBuilder()
                    .setCategory(GoogleAnalytics.CAT_MAIN)
                    .setAction(GoogleAnalytics.ACTION_NEW_PHOTO)
                    .setLabel(GoogleAnalytics.LABEL_PHOTO_CANCELED)
                    .build());
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
        }
        cursor.close();
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), getString(R.string.app_name));

        // Create the storage directory if it does not exist
        if (! storageDir.exists()){
            if (! storageDir.mkdirs()){
                return null;
            }
        }

        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        MenuInflater inflater = mode.getMenuInflater();
        inflater.inflate(R.menu.menu_delete, menu);

        mTracker.send(new HitBuilders.EventBuilder()
                .setCategory(GoogleAnalytics.CAT_MAIN)
                .setAction(GoogleAnalytics.ACTION_ACTION_MODE_TRIGGERED)
                .build());

        updateActionModeTitle(mode);

        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        if (item.getItemId() == R.id.action_delete) {
            deleteCheckedItems();
        }

        mode.finish();
        return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        Log.d(TAG, "Clearing all the choices");
        mGridview.clearChoices();
        mAdapter.clearSelections();
        mAdapter.notifyDataSetChanged();
    }

    private void deleteFileFromMediaStore(final ContentResolver contentResolver, final File file) {
        String canonicalPath;
        try {
            canonicalPath = file.getCanonicalPath();
        } catch (IOException e) {
            canonicalPath = file.getAbsolutePath();
        }
        final Uri uri = MediaStore.Files.getContentUri("external");
        final int result = contentResolver.delete(uri,
                MediaStore.Files.FileColumns.DATA + "=?", new String[] {canonicalPath});
        if (result == 0) {
            final String absolutePath = file.getAbsolutePath();
            if (!absolutePath.equals(canonicalPath)) {
                contentResolver.delete(uri,
                        MediaStore.Files.FileColumns.DATA + "=?", new String[]{absolutePath});
            }
        }
    }

    private void deleteCheckedItems() {
        int failedDelete = 0;
        int successDelete = 0;
        List<Integer> selectedItemPositions = mAdapter.getSelectedItems();
        for (int i = selectedItemPositions.size() - 1; i >= 0; i--) {
            String path = mImagePaths.get(selectedItemPositions.get(i));
            mAdapter.removeData(selectedItemPositions.get(i));
            File file = new File(path);
            if (!file.delete()) {
                Log.d(TAG, "failed to delete:" + path);
                failedDelete ++;
            } else {
                Log.d(TAG, "deleted:" + path);
                successDelete ++;
                deleteFileFromMediaStore(getContentResolver(), file);
            }

        }

        if (failedDelete > 0) {
            Log.d(TAG, "failed to delete " + failedDelete + " items.");
            mTracker.send(new HitBuilders.EventBuilder()
                    .setCategory(GoogleAnalytics.CAT_MAIN)
                    .setAction(GoogleAnalytics.ACTION_DELETE_FILE_FAILED)
                    .setLabel(String.valueOf(failedDelete))
                    .build());
        }

        if (successDelete > 0) {
            Log.d(TAG, "deleted " + successDelete + " items.");
            mTracker.send(new HitBuilders.EventBuilder()
                    .setCategory(GoogleAnalytics.CAT_MAIN)
                    .setAction(GoogleAnalytics.ACTION_DELETE_FILE)
                    .setLabel(String.valueOf(successDelete))
                    .build());
        }
    }

    @Override
    public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
        mAdapter.toggleSelection(position);
        updateActionModeTitle(mode);
    }

    private void updateActionModeTitle(ActionMode mode) {
        int selectCount = mGridview.getCheckedItemCount();
        switch (selectCount) {
            case 1:
                mode.setTitle(("1 " + getString(R.string.title_selected)));
                break;
            default:
                mode.setTitle((selectCount + " " + getString(R.string.title_selected)));

                break;
        }
    }

    @Override
    public void onRefresh() {
        mTracker.send(new HitBuilders.EventBuilder()
                .setCategory(GoogleAnalytics.CAT_MAIN)
                .setAction(GoogleAnalytics.ACTION_REFRESH)
                .build());

        refreshList();
    }

    private void refreshList() {
        fetchImagePaths();
        mAdapter = new GridAdapter(this, 0, mImagePaths);
        mGridview.setAdapter(mAdapter);
        mGridview.startLayoutAnimation();
        mSwipeContainer.setRefreshing(false);
    }
}
