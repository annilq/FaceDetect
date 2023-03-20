package com.yunqi.hospital.network;

import android.content.Context;
import android.text.TextUtils;

import com.blankj.utilcode.util.ToastUtils;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.Disposable;


/**
 * 网络请求响应
 */
public abstract class ApiObserver<T> implements Observer<HttpResult<T>> {
    private LoadingDialog loadingDialog;

    public ApiObserver(Context context) {
        this(context, false);
    }

    public ApiObserver(Context context, boolean needLoading) {
        super();
        if (needLoading && context != null) {
            loadingDialog = new LoadingDialog(context);
        }
    }

    @Override
    public void onSubscribe(@NonNull Disposable d) {
        onStart();
    }

    @Override
    public void onNext(@NonNull HttpResult<T> tHttpResult) {
        switch (tHttpResult.getCode()) {
            case HttpResultCode.SUCCESS:
                onSuccess(tHttpResult.getResp());
                break;
            case HttpResultCode.TOKEN_INVALID:
                onTokenInvalid(tHttpResult.getInfo());
                break;
            default:
                onFailure(tHttpResult.getInfo());
                break;

        }
        onFinish();
    }

    @Override
    public void onError(@NonNull Throwable e) {
        onFailure(e.getMessage());
        onFinish();
    }

    @Override
    public void onComplete() {
    }

    // 请求开始
    public void onStart() {
        if (loadingDialog != null) {
            loadingDialog.show();
        }
    }

    // 请求成功
    public abstract void onSuccess(T response);

    // Token失效
    public void onTokenInvalid(String message) {
        if (!TextUtils.isEmpty(message)) {
            ToastUtils.showLong(message);
        }
    }

    // 请求失败
    public void onFailure(String message) {
        if (!TextUtils.isEmpty(message)) {
            ToastUtils.showLong(message);
        }
    }

    // 请求结束
    public void onFinish() {
        if (loadingDialog != null) {
            loadingDialog.dismiss();
        }
    }
}
