package com.jiacorp.howold;

import com.google.gson.JsonObject;

import retrofit.http.Body;
import retrofit.http.POST;

/**
 * Created by jitse on 5/4/15.
 */
public interface FaceService {

    @POST("/v1/reviews")
    JsonObject detectFace(@Body byte[] array);

}
