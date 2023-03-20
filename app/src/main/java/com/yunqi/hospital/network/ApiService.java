package com.yunqi.hospital.network;

import java.util.Map;

import io.reactivex.rxjava3.core.Observable;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface ApiService {

    @POST("api/device/shutdown")
    Observable<HttpResult<String>> shutdown(@Body Map<String, String> params);

    @POST("api/device/heartbeat")
    Observable<HttpResult<String>> heartbeat(@Body Map<String, String> params);

    @POST("api/device/getToken")
    Observable<HttpResult<String>> getToken(@Body Map<String, String> params);

    @POST("api/device/login")
    Observable<HttpResult<String>> login(@Body Map<String, String> params);

    @POST("api/device/isAlive")
    Observable<HttpResult<Boolean>> isAlive();


}
