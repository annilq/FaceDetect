package com.yunqi.hospital;

import static com.blankj.utilcode.util.ThreadUtils.runOnUiThread;

import android.app.Activity;
import android.webkit.JavascriptInterface;

import com.blankj.utilcode.util.AppUtils;
import com.blankj.utilcode.util.DeviceUtils;
import com.blankj.utilcode.util.NetworkUtils;
import com.google.gson.Gson;

import java.util.HashMap;
interface WebViewInterFace {
    void reload(String result);
    void scanFace(String result);
    void loadCallback(String result);
}
public class JSInterface1 {
    public Activity webViewActivity;

    public JSInterface1(Activity activity) {
        this.webViewActivity = activity;
    }

    @JavascriptInterface
    public void reload(String callbackId) {
        webViewActivity.reload();
    }

    /**
     * 读取身份证
     *
     * @param callbackId 回调Id
     */
    @JavascriptInterface
    public void getDeviceInfo(String callbackId) {
        HashMap<String, String> params = new HashMap<>();
        params.put("appVersion", AppUtils.getAppVersionName());
        params.put("serialNo", webViewActivity.getDeviceSN());
        params.put("ipAddress", NetworkUtils.getIPAddress(true));
        params.put("macAddress", DeviceUtils.getMacAddress());
        Gson gson = new Gson();
        String jsonStr = gson.toJson(params);
        runOnUiThread(() -> webViewActivity.loadCallback("javascript:NativeBridge.NativeCallback('" + callbackId + "','" + jsonStr + "')"));
    }

    @JavascriptInterface
    public void scanFace(String callbackId) {
        runOnUiThread(()->webViewActivity.scanFace(callbackId));
    }
}
