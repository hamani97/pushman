package com.hamani.suntech.pushman.common;

import com.hamani.suntech.pushman.data.RestData;
import com.hamani.suntech.pushman.data.RestNotice;
import com.hamani.suntech.pushman.data.RestPush;
import com.hamani.suntech.pushman.data.RestReply;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface HttpSuntechService {
    @Headers({"Accept: application/json"})

    @GET("admin/index.php/api/{method}")
    Call<RestData> addDevice(
            @Path("method") String method,
            @Query("device_id") String device_id,
            @Query("token") String token,
            @Query("brand") String brand,
            @Query("model") String model,
            @Query("version") String version,
            @Query("name") String name,
            @Query("empno") String empno
    );

    @GET("admin/index.php/api/{method}")
    Call<RestPush> getList(
            @Path("method") String method,
            @Query("device_id") String device_id,
            @Query("page") String page,
            @Query("status") String status,
            @Query("filter") String filter
    );

    @GET("admin/index.php/api/{method}")
    Call<RestPush> getData(
            @Path("method") String method,
            @Query("device_id") String device_id,
            @Query("idx") String idx
    );

    @GET("admin/index.php/api/{method}")
    Call<RestPush> setData(
            @Path("method") String method,
            @Query("device_id") String device_id,
            @Query("idx") String idx,
            @Query("status") String status
    );

    @GET("admin/index.php/api/{method}")
    Call<RestReply> getReply(
            @Path("method") String method,
            @Query("device_id") String device_id,
            @Query("push_idx") String push_idx,
            @Query("page") String page
    );

    @GET("admin/index.php/api/{method}")
    Call<RestNotice> getNoticeList(
            @Path("method") String method,
            @Query("device_id") String device_id,
            @Query("page") String page
    );

    @GET("admin/index.php/api/{method}")
    Call<RestNotice> getNoticeData(
            @Path("method") String method,
            @Query("device_id") String device_id,
            @Query("idx") String idx
    );
}