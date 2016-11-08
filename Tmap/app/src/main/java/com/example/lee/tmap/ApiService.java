package com.example.lee.tmap;

import javax.security.auth.callback.Callback;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.GsonConverterFactory;
import retrofit2.Retrofit;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Created by Lee on 2016-10-26.
 */
public interface ApiService {
    public static final String API_URL = "https://apis.skplanetx.com";

    @GET("comments")
    Call<ResponseBody> getComment(@Query("postId") int postId);

    @Headers({
            "Accept: application/json",
            "appKey: 483f055b-19f2-3a22-a3fb-935bc1684b0b"
    })
    @FormUrlEncoded
    @POST("/tmap/routes?version=1")
    Call<TmapDataModel> getGuidePath(
            @Field("startX") String startX,
            @Field("startY") String startY,
            @Field("endX") String endX,
            @Field("endY") String endY,
            @Field("reqCoordType") String reqCoordType);
}
