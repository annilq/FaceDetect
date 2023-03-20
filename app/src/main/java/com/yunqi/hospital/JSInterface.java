package com.yunqi.hospital;


import android.app.AlertDialog;
import android.os.Build;
import android.text.TextUtils;
import android.webkit.JavascriptInterface;

import com.blankj.utilcode.util.SPUtils;
import com.yunqi.hospital.device.DeviceConstant;
import com.yunqi.hospital.network.ApiClient;

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
    public void readIDCard(String callbackId) {
        webViewActivity.readIdCard(result -> webViewActivity.loadCallback("javascript:NativeBridge.NativeCallback('" + callbackId + "','" + result + "')"));
    }

    /**
     * 关闭读取身份证
     */
    @JavascriptInterface
    public void closeIDCard(String callbackId) {
        webViewActivity.closeIdCard();
    }

    /**
     * 读取社保卡
     *
     * @param callbackId 回调Id
     */
    @JavascriptInterface
    public void readHealthCard(String callbackId) {
        webViewActivity.readHealthCard(result -> webViewActivity.loadCallback("javascript:NativeBridge.NativeCallback('" + callbackId + "','" + result + "')"));
    }

    /**
     * 关闭读取社保卡
     */
    @JavascriptInterface
    public void closeHealthCard(String callbackId) {
        webViewActivity.closeHealthCard();
    }

    /**
     * 打开扫码
     *
     * @param callbackId 回调Id
     */
    @JavascriptInterface
    public void openScanner(String callbackId) {
        webViewActivity.openScanner(result -> webViewActivity.loadCallback("javascript:NativeBridge.NativeCallback('" + callbackId + "','" + result + "')"));
    }

    /**
     * 关闭扫码
     */
    @JavascriptInterface
    public void closeScanner(String callbackId) {
        webViewActivity.closeScanner();
    }

    /**
     * 获取设备序列号
     */
    @JavascriptInterface
    public String getDeviceSN(String callbackId) {
        return Build.SERIAL;
    }

    /**
     * 获取设备id
     */
    @JavascriptInterface
    public String getDeviceId(String callbackId) {
        String deviceId = SPUtils.getInstance().getString(DeviceConstant.SpKey.device_id);
        // 为空将状态设置为【未登录】
        if (TextUtils.isEmpty(deviceId)) {
            SPUtils.getInstance().put(DeviceConstant.SpKey.device_state, DeviceConstant.State.STATE_NOT_LOGIN);
        }

        return deviceId;
    }

    /**
     * 获取设备状态 (默认未登录)
     */
    @JavascriptInterface
    public int getDeviceState(String callbackId) {
        return SPUtils.getInstance().getInt(DeviceConstant.SpKey.device_state, DeviceConstant.State.STATE_NOT_LOGIN);
    }

    /**
     * 设备登录
     *
     * @param callbackId 回调id
     * @param authCode   授权码
     */
    @JavascriptInterface
    public void login(String callbackId, String authCode) {
        webViewActivity.login(authCode);
    }

    /**
     * 获取token
     *
     * @param reFetch    是否重新获取
     * @param callbackId 回调id
     */
    @JavascriptInterface
    public void getToken(String callbackId, boolean reFetch) {
        String token = SPUtils.getInstance().getString(DeviceConstant.SpKey.token);
        if (reFetch || TextUtils.isEmpty(token)) {
            webViewActivity.getToken(result -> {
                webViewActivity.loadCallback("javascript:NativeBridge.NativeCallback('" + callbackId + "','" + result + "')");
            });
        } else {
            webViewActivity.loadCallback("javascript:NativeBridge.NativeCallback('" + callbackId + "','" + token + "')");
        }
    }

    /**
     * 切换运行环境
     */
    @JavascriptInterface
    public void switchServer(String callbackId) {
        new AlertDialog.Builder(webViewActivity)
                .setTitle("运行环境")
                .setSingleChoiceItems(new String[]{
                                "正式环境\n" + ApiClient.RELEASE_URL + "\n",
                                "测试环境\n" + ApiClient.TEST_URL + "\n",
                                "本地环境\n" + ApiClient.LOCAL_URL + "\n",
                        },
                        getServerIndex(), (dialogInterface, i) -> {
                            dialogInterface.dismiss();

                            String url;
                            if (i == 0) {
                                url = ApiClient.RELEASE_URL;
                            } else if (i == 1) {
                                url = ApiClient.TEST_URL;
                            } else {
                                url = ApiClient.LOCAL_URL;
                            }

                            if (!url.equals(ApiClient.BASE_URL)) {
                                ApiClient.BASE_URL = url;
                                webViewActivity.reLaunch();
                            }

                        }).show();
    }

    private int getServerIndex() {
        if (ApiClient.BASE_URL.equals(ApiClient.LOCAL_URL)) {
            return 2;
        } else if (ApiClient.BASE_URL.equals(ApiClient.TEST_URL)) {
            return 1;
        } else {
            return 0;
        }
    }

    /**
     * 获取当前运行环境
     */
    @JavascriptInterface
    public String getServer(String callbackId) {
        return ApiClient.BASE_URL;
    }
}
