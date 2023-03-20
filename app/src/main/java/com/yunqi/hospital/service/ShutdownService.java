package com.yunqi.hospital.service;

import android.app.IntentService;
import android.content.Intent;

import androidx.annotation.Nullable;

import com.blankj.utilcode.util.SPUtils;
import com.yunqi.hospital.device.DeviceConstant;
import com.yunqi.hospital.network.ApiClient;
import com.yunqi.hospital.network.ApiObserver;
import com.yunqi.hospital.network.ApiService;

import java.util.HashMap;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class ShutdownService extends IntentService {
    public ShutdownService() {
        super("shutdown");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        // 发送关机通知
        HashMap<String, String> params = new HashMap<>();
        params.put("token", SPUtils.getInstance().getString(DeviceConstant.SpKey.token));

        ApiClient.getInstance().create(ApiService.class)
                .shutdown(params)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new ApiObserver<String>(this) {

                    @Override
                    public void onSuccess(String resp) {
                        // do nothing
                    }
                });
    }
}
