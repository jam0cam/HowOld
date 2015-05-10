package com.jiacorp.howold;

import android.os.AsyncTask;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

/**
 * Created by jitse on 5/10/15.
 */
public class CleanupAsynTask extends AsyncTask<String, Void, Void> {
    @Override
    protected Void doInBackground(String... params) {

        File mediaStorageDir = new File(params[0]);

        if (mediaStorageDir.exists()) {
            try {
                FileUtils.cleanDirectory(mediaStorageDir);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return null;
    }
}
