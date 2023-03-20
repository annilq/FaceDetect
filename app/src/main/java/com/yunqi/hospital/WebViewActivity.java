package com.yunqi.hospital;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.blankj.utilcode.util.AppUtils;
import com.blankj.utilcode.util.DeviceUtils;
import com.blankj.utilcode.util.NetworkUtils;
import com.blankj.utilcode.util.SPUtils;
import com.google.gson.Gson;
import com.yunqi.hospital.databinding.ActivityWebviewBinding;
import com.yunqi.hospital.device.DeviceConstant;
import com.yunqi.hospital.device.DeviceService;
import com.yunqi.hospital.device.HealthCard;
import com.yunqi.hospital.device.IDCard;
import com.yunqi.hospital.device.SDKExecutors;
import com.yunqi.hospital.network.ApiClient;
import com.yunqi.hospital.network.ApiObserver;
import com.yunqi.hospital.network.ApiService;
import com.yunqi.hospital.network.DownloadAsyncTask;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

import cn.hsa.ctp.device.sdk.managerService.aidl.HealthCardReader;
import cn.hsa.ctp.device.sdk.managerService.aidl.IDCardReader;
import cn.hsa.ctp.device.sdk.managerService.aidl.OnScanListener;
import cn.hsa.ctp.device.sdk.managerService.aidl.Scanner;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class WebViewActivity extends AppCompatActivity implements View.OnClickListener {
    /**
     * 读身份证
     */
    private IDCardReader idCardReader;
    /**
     * 扫码
     */
    private Scanner scanner;
    /**
     * 社保（医保）卡
     */
    private HealthCardReader healthCardReader;

    private ActivityWebviewBinding binding;

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.hardware:
                loadHomePage();
                break;
        }
    }

    public interface OnResultListener {
        void onResult(String result);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityWebviewBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        WebSettings webSettings = binding.webView.getSettings();
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setLoadsImagesAutomatically(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webSettings.setJavaScriptEnabled(true); //支持js
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        webSettings.setDomStorageEnabled(true);

        JSInterface jsInterface = new JSInterface(this);
        binding.webView.addJavascriptInterface(jsInterface, "jsInterface");
        binding.hardware.setOnClickListener(this);
        // 处理跨域
//        webSettings.setAllowUniversalAccessFromFileURLs(true);

        // 支持chrome远程调试
//        if (BuildConfig.DEBUG) {
//            WebView.setWebContentsDebuggingEnabled(true);
//        }
//
//        registerNetworkListener();
//
//        startTestAlive();
//
//        DeviceService.getInstance(getApplicationContext()).connect();

        loadHomePage();
    }

    // ===============  Network Listener ==============

    private AlertDialog networkDialog;

    private NetworkUtils.OnNetworkStatusChangedListener mOnNetworkStatusChangedListener = new NetworkUtils.OnNetworkStatusChangedListener() {
        @Override
        public void onDisconnected() {
            if (networkDialog == null) {
                networkDialog = new AlertDialog.Builder(WebViewActivity.this)
                        .setTitle("温馨提示")
                        .setMessage("\n网络暂不可用\n\n请等待...")
                        .setCancelable(false)
                        .create();
            }
            if (!networkDialog.isShowing()) {
                networkDialog.show();
            }
        }

        @Override
        public void onConnected(NetworkUtils.NetworkType networkType) {
            if (networkDialog != null && networkDialog.isShowing()) {
                networkDialog.dismiss();
                loadHomePage();
            }
        }
    };

    private void registerNetworkListener() {
        NetworkUtils.registerNetworkStatusChangedListener(mOnNetworkStatusChangedListener);
    }

    // ===============  load webView ==============
    private final String HOME_URL = ApiClient.BASE_URL + "index.html";

    private void loadHomePage() {
        binding.webView.post(() -> binding.webView.loadUrl(HOME_URL));
    }

    private void loadLoginPage() {
        binding.webView.post(() -> binding.webView.loadUrl(HOME_URL + "#/device"));
    }

    /**
     * 页面刷新
     */
    public void reload() {
        binding.webView.post(() -> binding.webView.reload());
    }

    /**
     * 结果回调
     */
    public void loadCallback(final String url) {
        binding.webView.post(() -> binding.webView.loadUrl(url));
    }

    public void reLaunch() {
        binding.webView.post(this::recreate);
    }

    // ===============  Device ==============

    /**
     * 读取身份证
     */
    public void readIdCard(OnResultListener onResultListener) {
        SDKExecutors.getThreadPoolInstance().submit(() -> {
            try {
                if (idCardReader == null) {
                    idCardReader = DeviceService.getInstance(getApplicationContext()).getIDCard();
                    idCardReader.setFindCardTimeOut(5000); // 寻卡超时时间
                    idCardReader.setTimeOut(5000); // 读卡超时时间
                }

                // 打开设备
                int openResult = idCardReader.openDevice();
                if (openResult != 0) {
                    toast("打开读取设备失败");
                    return;
                }

//                // 寻卡
//                int result = idCardReader.findCard();
//                if (result == 0x9f) {
//                    toast("寻身份证成功");
//                } else {
//                    toast("寻身份证失败");
//                    return;
//                }

                // 读卡
                Bundle bundle = idCardReader.readBaseMsg();
                if (bundle != null && bundle.getInt("errorCode") == 0x90) {
                    IDCard idCard = new IDCard(bundle.getString("name"), bundle.getString("id_number"), bundle.getString("sex")
                            , bundle.getString("address"), bundle.getString("birth_year") +
                            bundle.getString("birth_moth") +
                            bundle.getString("birth_day"));

                    runOnUiThread(() -> onResultListener.onResult(new Gson().toJson(idCard)));

                } else {
                    toast("读身份证失败");
                }
            } catch (Exception e) {
                toast(e.getMessage());
                e.printStackTrace();
            }
        });
    }

    /**
     * 关闭身份证设备
     */
    public void closeIdCard() {
        try {
            if (idCardReader != null) {
                idCardReader.closeDevice();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * 读取社保卡
     */

    public void readHealthCard(OnResultListener onResultListener) {
        SDKExecutors.getThreadPoolInstance().submit(() -> {
            try {
                if (healthCardReader == null) {
                    healthCardReader = DeviceService.getInstance(getApplicationContext()).getHealthCardReader();
                }

                Bundle bundle = healthCardReader.readCard(5000);
                if (bundle != null && bundle.getInt("errorCode") == 0x90) {
                    HealthCard healthCard = new HealthCard(bundle.getString("name"), bundle.getString("cardNo"), bundle.getString("sex"),
                            bundle.getString("idCardNo"), bundle.getString("districtCode"), bundle.getString("cardVersion"),
                            bundle.getString("cardSN"));

                    runOnUiThread(() -> {
//                        Toast.makeText(WebViewActivity.this, new Gson().toJson(healthCard), Toast.LENGTH_LONG).show();
                        onResultListener.onResult(new Gson().toJson(healthCard));
                    });
                } else {
                    toast("读社保卡失败");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * 关闭社保卡
     */
    public void closeHealthCard() {
        try {
            if (healthCardReader != null) {
                healthCardReader.closeDevice();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 打开扫码设备
     */
    public void openScanner(OnResultListener onResultListener) {
        SDKExecutors.getThreadPoolInstance().submit(() -> {
            try {
                if (scanner == null) {
                    scanner = DeviceService.getInstance(getApplicationContext()).getScanner();
                }
                scanner.startScan(5, new OnScanListener.Stub() {
                    @Override
                    public void onSuccess(String s) throws RemoteException {
                        runOnUiThread(() -> onResultListener.onResult(s));

                    }

                    @Override
                    public void onError(int i) throws RemoteException {
                        toast("扫码错误 " + i);
                    }

                    @Override
                    public void onTimeout() throws RemoteException {
                        toast("扫码超时");
                    }

                    @Override
                    public void onCancel() throws RemoteException {
                        toast("扫码取消");
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                toast("扫码异常 " + e.getMessage());
            }
        });
    }

    /**
     * 关闭扫码设备
     */
    public void closeScanner() {
        try {
            if (scanner != null) {
                scanner.stopScan();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ============ network request ==================

    private final int MESSAGE_HEART_BEAT = 101; // 心跳
    private final int MESSAGE_HEART_BEAT_INTERVAL = 3 * 60 * 1000; // 心跳间隔

    private final int MESSAGE_TEST_ALIVE = 102; // 测试服务
    private final int MESSAGE_TEST_ALIVE_INTERVAL = 10 * 1000; // 测试服务间隔

    @SuppressLint("HandlerLeak")
    private final Handler mHandler = new Handler() {

        @Override
        public void handleMessage(@NonNull Message message) {
            if (message.what == MESSAGE_HEART_BEAT) {
                sendHeartBeat();
                sendEmptyMessageDelayed(MESSAGE_HEART_BEAT, MESSAGE_HEART_BEAT_INTERVAL);
            } else if (message.what == MESSAGE_TEST_ALIVE) {
                testDeviceAlive();
                sendEmptyMessageDelayed(MESSAGE_TEST_ALIVE, MESSAGE_TEST_ALIVE_INTERVAL);
            }
        }
    };

    private void startTestAlive() {
        mHandler.removeMessages(MESSAGE_TEST_ALIVE);
        mHandler.sendEmptyMessage(MESSAGE_TEST_ALIVE);
    }

    private void startHeartBeat() {
        mHandler.removeMessages(MESSAGE_HEART_BEAT);
        mHandler.sendEmptyMessage(MESSAGE_HEART_BEAT);
    }

    /**
     * 发送心跳
     */
    private void sendHeartBeat() {
        if (!NetworkUtils.isConnected())
            return;

        HashMap<String, String> params = new HashMap<>();
        params.put("appVersion", AppUtils.getAppVersionName());
        params.put("breakdown", "0");
        params.put("token", SPUtils.getInstance().getString(DeviceConstant.SpKey.token));

        ApiClient.getInstance().create(ApiService.class)
                .heartbeat(params)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new ApiObserver<String>(this) {

                    @Override
                    public void onSuccess(String resp) {
                        // do nothing
                    }

                    @Override
                    public void onTokenInvalid(String message) {
                        mHandler.removeMessages(MESSAGE_HEART_BEAT);
                        getToken(result -> mHandler.sendEmptyMessage(MESSAGE_HEART_BEAT));
                    }
                });
    }

    /**
     * 测试服务是否可用
     */

    private AlertDialog testDialog;

    private void showTestDialog() {
        if (testDialog == null) {
            testDialog = new AlertDialog.Builder(WebViewActivity.this)
                    .setTitle("温馨提示")
                    .setMessage("\n服务暂不可用\n\n请稍后...")
                    .setCancelable(false)
                    .create();
        }
        if (!testDialog.isShowing()) {
            testDialog.show();
        }
    }

    private void testDeviceAlive() {
        if (!NetworkUtils.isConnected())
            return;

        ApiClient.getInstance().create(ApiService.class)
                .isAlive()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new ApiObserver<Boolean>(this) {

                    @Override
                    public void onSuccess(Boolean resp) {
                        if (resp) {
                            if (testDialog != null && testDialog.isShowing()) {
                                testDialog.dismiss();
                                loadHomePage();
                            }
                        } else {
                            showTestDialog();
                        }
                    }

                    @Override
                    public void onTokenInvalid(String message) {
                        showTestDialog();
                    }

                    @Override
                    public void onFailure(String message) {
                        showTestDialog();
                    }
                });
    }


    private String getDeviceSN() {
        if (TextUtils.isEmpty(Build.SERIAL) || "unknown".equals(Build.SERIAL)) {
            return "h123456";
        }

        return Build.SERIAL;
    }

    /**
     * 设备登录
     */
    public void login(String authCode) {
        HashMap<String, String> params = new HashMap<>();
        params.put("serialNo", getDeviceSN());
        params.put("authCode", authCode);

        ApiClient.getInstance().create(ApiService.class)
                .login(params)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new ApiObserver<String>(this, true) {

                    @Override
                    public void onSuccess(String resp) {
                        // 1. 保存authCode 和 deviceId
                        SPUtils.getInstance().put(DeviceConstant.SpKey.auth_code, authCode);
                        SPUtils.getInstance().put(DeviceConstant.SpKey.device_id, resp);

                        // 2. 开始发送心跳
                        if (TextUtils.isEmpty(SPUtils.getInstance().getString(DeviceConstant.SpKey.token))) {
                            getToken(result -> startHeartBeat());
                        } else {
                            startHeartBeat();
                        }

                        // 3. 跳转到首页
                        loadHomePage();
                    }
                });
    }

    /**
     * 获取token
     */
    public void getToken(OnResultListener onResultListener) {
        HashMap<String, String> params = new HashMap<>();
        params.put("id", SPUtils.getInstance().getString(DeviceConstant.SpKey.device_id));
        params.put("ipAddress", NetworkUtils.getIPAddress(true));
        params.put("macAddress", DeviceUtils.getMacAddress());
        try {
            // sha256(授权码+ 设备序列号)
            params.put("refreshToken", sha256(SPUtils.getInstance().getString(DeviceConstant.SpKey.auth_code)
                    + getDeviceSN()));
        } catch (Exception e) {
            e.printStackTrace();
        }

        ApiClient.getInstance().create(ApiService.class)
                .getToken(params)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new ApiObserver<String>(this) {

                    @Override
                    public void onSuccess(String resp) {
                        // 保存token
                        SPUtils.getInstance().put(DeviceConstant.SpKey.token, resp);

                        if (onResultListener != null) {
                            onResultListener.onResult(resp);
                        }
                    }

                    @Override
                    public void onTokenInvalid(String message) {
                        new AlertDialog.Builder(WebViewActivity.this)
                                .setTitle("温馨提示")
                                .setMessage("\n" + message)
                                .setCancelable(false)
                                .setPositiveButton("确认", (dialogInterface, i) -> {
                                    // 跳转到登录页面
                                    loadLoginPage();
                                }).show();
                    }

                    @Override
                    public void onFailure(String message) {
                        new AlertDialog.Builder(WebViewActivity.this)
                                .setTitle("温馨提示")
                                .setMessage("\n" + message)
                                .setCancelable(false)
                                .setPositiveButton("重试", (dialogInterface, i) -> {
                                    getToken(onResultListener);
                                }).show();
                    }
                });
    }

    private String sha256(String s) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(s.getBytes(StandardCharsets.UTF_8));
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }


    private void toast(final String msg) {
        runOnUiThread(() -> Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show());
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideStatusBar();
    }

    public void hideStatusBar() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }


    /**
     * 禁用home
     */
//    @Override
//    public void onAttachedToWindow() {
//        this.getWindow().setType(WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG);
//        super.onAttachedToWindow();
//    }

    /**
     * 禁用回退键
     */
    @Override
    public void onBackPressed() {
//        if (binding.webView.canGoBack()) {
//            binding.webView.goBack();
//        } else {
//            super.onBackPressed();
//        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding.webView.clearHistory();
        mHandler.removeCallbacksAndMessages(null);
        NetworkUtils.unregisterNetworkStatusChangedListener(mOnNetworkStatusChangedListener);
        DeviceService.getInstance(getApplicationContext()).disconnect();
    }
}
