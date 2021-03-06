package com.example.lee.tmap;

import com.example.lee.tmap.ValueObject.GuideDataVO;
import com.example.lee.tmap.ValueObject.RecentPathVO;
import com.example.lee.tmap.ValueObject.ReverseGeocodingVO;
import com.example.lee.tmap.ValueObject.TmapDataVO;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Query;

/**
 * Created by Lee on 2016-10-26.
 */
public interface ApiService {
    public static final String API_URL = "https://apis.skplanetx.com";
    public static final String SERVER_URL = "http://52.78.14.6:3000";

    @Headers({
        "accept: application/json",
        "appKey: 483f055b-19f2-3a22-a3fb-935bc1684b0b"
    })
    @GET("/tmap/geo/reversegeocoding?version=1")
    Call<ReverseGeocodingVO> getAddress(
      @Query("lat") String lat,
      @Query("lon") String lon,
      @Query("coordType") String coordType,
      @Query("addressType") String addressType
    );


    @Headers({
        "accept: application/json",
        "appKey: 483f055b-19f2-3a22-a3fb-935bc1684b0b"
    })
    @FormUrlEncoded
    @POST("/tmap/routes?version=1")
    Call<TmapDataVO> getGuidePath(
        @Field("endX") String endX,
        @Field("endY") String endY,
        @Field("reqCoordType") String reqCoordType,
        @Field("startX") String startX,
        @Field("startY") String startY
    );

    @Headers({
            "accept: application/json",
            "appKey: 483f055b-19f2-3a22-a3fb-935bc1684b0b"
    })
    @FormUrlEncoded
    @POST("/tmap/routes?version=1")
    Call<GuideDataVO> getGuidePathResult(
            @Field("endX") String endX,
            @Field("endY") String endY,
            @Field("reqCoordType") String reqCoordType,
            @Field("startX") String startX,
            @Field("startY") String startY
    );



    /*==========
        [ Server Database ]
        [ 1. 최근 검색어 가져오기. ]
        [ 2. 최근 검색어 저장 ]
    ========== */

    // [ 1. 최근 검색어 가져오기. ]
    @Headers({
        "Accept: application/json",
    })
    @GET("/routerecord/")
    Call<RecentPathVO> getRecentPath(
        // @Query("USER_UUID") String USER_UUID
    );

    // [ 2. 최근 검색어 저장. ]
    @FormUrlEncoded
    @POST("/routerecord/saveroute")
    Call<ResponseBody> saveRoute(
            @Field("userUUID") String userUUID,
            @Field("arrivalName") String arrivalName,
            @Field("searchDate") String searchDate,
            @Field("latitudeValue") String latitudeValue,
            @Field("longitudeValue") String longitudeValue
    );
}
