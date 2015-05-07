package com.jiacorp.howold;

import com.google.gson.JsonObject;

import retrofit.http.Body;
import retrofit.http.POST;
import retrofit.http.Query;
import rx.Observable;

/**
 * Created by jitse on 5/4/15.
 */
public interface FaceService {

    @POST("/detection/detect")
    Observable<JsonObject> detectFaceUrl(@Query("attribute") String attrs, @Query("url") String url);

    @POST("/detection/detect")
    Observable<JsonObject> detectFaceImage(@Body byte[] img, @Query("attribute") String attrs);

}
