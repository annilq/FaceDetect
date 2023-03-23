package com.yunqi.hospital;
import static com.blankj.utilcode.util.ThreadUtils.runOnUiThread;

import android.webkit.JavascriptInterface;

import com.blankj.utilcode.util.AppUtils;
import com.blankj.utilcode.util.DeviceUtils;
import com.blankj.utilcode.util.NetworkUtils;
import com.google.gson.Gson;

import java.util.HashMap;


public class JSInterface {
    public WebViewActivity webViewActivity;

    public JSInterface(WebViewActivity activity) {
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
        webViewActivity.scanFace("javascript:NativeBridge.NativeCallback('" + callbackId + "')");
    }
}
