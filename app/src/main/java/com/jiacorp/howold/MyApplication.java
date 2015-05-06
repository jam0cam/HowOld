package com.jiacorp.howold;

import android.app.Application;

import dagger.ObjectGraph;

/**
 * Created by jitse on 5/5/15.
 */
public class MyApplication extends Application {

    private static ObjectGraph sObjectGraph;

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

}
