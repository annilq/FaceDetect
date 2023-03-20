package com.yunqi.hospital.network;

import com.yunqi.hospital.BuildConfig;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * 网络请求
 */
public class ApiClient {
    public static final String LOCAL_URL = "http://192.168.0.97:8000/";
    public static final String TEST_URL = "http://ybzd.ncyunqi.com/ybzd-zzfw/";
    public static final String RELEASE_URL = "https://school-test.ncyunqi.com/classpad/";
    public static String BASE_URL = RELEASE_URL;
    private static final int TIME_OUT = 3; // 超时时间

    private final Retrofit mRetrofit;

    private static class SingletonHolder {
        private static final ApiClient INSTANCE = new ApiClient();
    }

    public static ApiClient getInstance() {
        return SingletonHolder.INSTANCE;
    }

    private ApiClient() {
        // 创建OKHttpClient
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(BuildConfig.DEBUG ? HttpLoggingInterceptor.Level.BODY
                : HttpLoggingInterceptor.Level.NONE);

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor) // 日志拦截器
                .addInterceptor(chain -> { // 请求拦截器
                    Request originRequest = chain.request();
                    Request.Builder builder = originRequest.newBuilder();
                    if (!BASE_URL.equals(RELEASE_URL)) {
                        builder.url(originRequest.url().toString().replace(RELEASE_URL, TEST_URL));
                    }
                    return chain.proceed(builder.build());
                })
                .connectTimeout(TIME_OUT, TimeUnit.SECONDS) // 连接超时时间
                .writeTimeout(TIME_OUT, TimeUnit.SECONDS) // 写操作超时时间
                .readTimeout(TIME_OUT, TimeUnit.SECONDS) // 读操作超时时间
                .build();

        // 创建Retrofit
        mRetrofit = new Retrofit.Builder()
                .client(okHttpClient) // 使用okhttp网络请求
                .addCallAdapterFactory(RxJava3CallAdapterFactory.create()) // Rxjava回调
                .addConverterFactory(GsonConverterFactory.create()) // Gson转换数据
                .baseUrl(BASE_URL)
                .build();
    }

    public <T> T create(Class<T> service) {
        return mRetrofit.create(service);
    }
}
