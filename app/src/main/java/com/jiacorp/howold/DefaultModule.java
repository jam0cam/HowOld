package com.jiacorp.howold;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import retrofit.RestAdapter;

/**
 * Created by jitse on 5/5/15.
 */
@Module(
        injects = FaceActivity.class
)
public class DefaultModule {

    @Provides
    @Singleton
    public RestAdapter provideRestAdapter() {
        return new RestAdapter.Builder()
                .setEndpoint("http://apius.faceplusplus.com")
                .build();
    }

    @Provides
    @Singleton
    public FaceService provideFaceService(RestAdapter restAdapter) {
        return restAdapter.create(FaceService.class);
    }
}
