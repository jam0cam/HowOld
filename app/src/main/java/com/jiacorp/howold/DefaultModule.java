package com.jiacorp.howold;

import com.squareup.okhttp.OkHttpClient;

import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.client.OkClient;

/**
 * Created by jitse on 5/5/15.
 */
@Module(
        injects = FaceActivity.class
)
public class DefaultModule {

    @Provides
    @Singleton
    public RestAdapter provideRestAdapter(RequestInterceptor interceptor, OkHttpClient client) {
        return new RestAdapter.Builder()
                .setEndpoint("https://apius.faceplusplus.com/v2")
                .setRequestInterceptor(interceptor)
                .setClient(new OkClient(client))
                .build();
    }

    @Provides
    @Singleton
    public OkHttpClient provideClient() {
        OkHttpClient okHttpClient = new OkHttpClient();
        okHttpClient.setReadTimeout(60 * 1000, TimeUnit.MILLISECONDS);
        return okHttpClient;
    }

    @Provides
    @Singleton
    public RequestInterceptor provideRequestInterceptor() {
        return request -> {
            request.addQueryParam("api_key", "d4b08a16bb000c885be7481d9df44b95");
            request.addQueryParam("api_secret", "TRmpqYhFCzM0P23U2axLDYVTdSfZGh3K");
        };
    }

    @Provides
    @Singleton
    public FaceService provideFaceService(RestAdapter restAdapter) {
        return restAdapter.create(FaceService.class);
    }
}
