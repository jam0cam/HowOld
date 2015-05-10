package com.jiacorp.howold;

import android.app.Application;
import android.os.Environment;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;

import dagger.ObjectGraph;

/**
 * Created by jitse on 5/5/15.
 */
public class MyApplication extends Application {

    private static ObjectGraph sObjectGraph;
    private Tracker mTracker = null;

    @Override
    public void onCreate() {
        super.onCreate();

        sObjectGraph = ObjectGraph.create(new DefaultModule());
        sObjectGraph.injectStatics();
    }

    /**
     * Inject the supplied object using the application's object graph.
     */
    public void inject(Object o) {
        sObjectGraph.inject(o);
    }

    public String getPrivateAppDirectory() {
        return Environment.getExternalStorageDirectory()
                + "/Android/data/"
                + getApplicationContext().getPackageName()
                + "/Files";
    }

    public synchronized Tracker getTracker() {
        if ( null == mTracker ) {
            GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
            mTracker = analytics.newTracker(R.xml.analytics);
        }

        return mTracker;
    }
}
