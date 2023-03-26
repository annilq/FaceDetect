package com.yunqi.hospital;

import android.Manifest;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;

import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import com.blankj.utilcode.util.NetworkUtils;
import com.yunqi.hospital.databinding.ActivityMainBinding;
import com.yunqi.hospital.databinding.ActivityWebviewBinding;
import com.yunqi.hospital.network.ApiClient;


import java.io.ByteArrayOutputStream;
import java.io.IOException;


public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback,WebViewInterFace {

    private ActivityMainBinding binding;

    private static final String TAG = "MainActivity";
    private Camera camera;
    private SurfaceHolder surfaceHolder;
    private boolean previewing = false;

    private static final int REQUEST_CAMERA_PERMISSION = 1;
    private String faceCallBackId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
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

        // 处理跨域


//        webSettings.setAllowUniversalAccessFromFileURLs(true);

//         支持chrome远程调试
        if (BuildConfig.DEBUG) {
            WebView.setWebContentsDebuggingEnabled(true);
        }

        registerNetworkListener();


//        DeviceService.getInstance(getApplicationContext()).connect();

        loadHomePage();
//        人脸识别部分

    }

    private Camera.PictureCallback pictureCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
//            Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
//            Matrix matrix = new Matrix();
//            matrix.postRotate(180);
//            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
//            Bitmap finalBitmap = bitmap;
            String encoded = Base64.encodeToString(data, Base64.DEFAULT);
            refreshCamera();
            runOnUiThread(() -> endDetect(encoded));
            Log.d(TAG, "onPictureTaken - wrote bytes: " + data.length);
        }
    };

    public void surfaceCreated(SurfaceHolder holder) {
        try {
            camera.setPreviewDisplay(holder);
            camera.startPreview();
            previewing = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (previewing) {
            camera.stopPreview();
            previewing = false;
        }
        try {
            camera.setPreviewDisplay(holder);
            camera.startPreview();
            previewing = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        camera.stopPreview();
        camera.release();
        camera = null;
        previewing = false;
    }

    public void refreshCamera() {
        if (surfaceHolder.getSurface() == null) {
            return;
        }
        try {
            camera.stopPreview();
        } catch (Exception e) {
        }
        try {
            camera.setPreviewDisplay(surfaceHolder);
            camera.startPreview();
        } catch (Exception e) {
        }
    }

    public void initCamera() {
        camera = Camera.open();
        surfaceHolder = binding.camerapreview.getHolder();
        surfaceHolder.addCallback(this);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideStatusBar();
        requestCameraPermissions();
    }


    public static class ConfirmationDialogFragment extends DialogFragment {
        private static final String ARG_MESSAGE = "message";
        private static final String ARG_PERMISSIONS = "permissions";
        private static final String ARG_REQUEST_CODE = "request_code";
        private static final String ARG_NOT_GRANTED_MESSAGE = "not_granted_message";

        public static ConfirmationDialogFragment newInstance(String message,
                                                             String[] permissions, int requestCode, String notGrantedMessage) {
            ConfirmationDialogFragment fragment = new ConfirmationDialogFragment();
            Bundle args = new Bundle();
            args.putString(ARG_MESSAGE, message);
            args.putStringArray(ARG_PERMISSIONS, permissions);
            args.putInt(ARG_REQUEST_CODE, requestCode);
            args.putString(ARG_NOT_GRANTED_MESSAGE, notGrantedMessage);
            fragment.setArguments(args);
            return fragment;
        }
    }
    // ===============  Network Listener ==============

    private AlertDialog networkDialog;

    private NetworkUtils.OnNetworkStatusChangedListener mOnNetworkStatusChangedListener = new NetworkUtils.OnNetworkStatusChangedListener() {
        @Override
        public void onDisconnected() {
            if (networkDialog == null) {
                networkDialog = new AlertDialog.Builder(MainActivity.this)
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

    // ============ network request ==================

    public void toast(final String msg) {
        runOnUiThread(() -> Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show());
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


    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding.webView.clearHistory();
        NetworkUtils.unregisterNetworkStatusChangedListener(mOnNetworkStatusChangedListener);
    }

    @Override
    protected void onStop() {
        super.onStop();

    }

    @Override
    protected void onPause() {

        super.onPause();
    }

    public String getDeviceSN() {
        if (TextUtils.isEmpty(Build.SERIAL) || "unknown".equals(Build.SERIAL)) {
            return "h123456";
        }

        return Build.SERIAL;
    }

    public void scanFace(String callbackId) {
        faceCallBackId = callbackId;
        previewing=false;
        binding.cameralayer.setVisibility(View.VISIBLE);
    }

    public void cancelDetect(View view) {
        binding.cameralayer.setVisibility(View.GONE);
        binding.ivFacePic.setImageDrawable(null);
        this.loadCallback("javascript:NativeBridge.NativeCallback('" + faceCallBackId + "','')");
    }

    public void endDetect(View view) {
        Bitmap bitmap = ((BitmapDrawable) binding.ivFacePic.getDrawable()).getBitmap();
// 将Bitmap对象转换为字节数组，并使用Base64编码将其转换为字符串
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 60, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        String encoded = Base64.encodeToString(byteArray, Base64.DEFAULT);

        this.loadCallback("javascript:NativeBridge.NativeCallback('" + faceCallBackId + "','" + encoded + "')");

//        binding.cameralayer.setVisibility(View.GONE);
//        mIvFace.setImageDrawable(null);
//        camera.stopPreview();
//        camera.release();
    }

    public void endDetect(String base64) {
        this.loadCallback("javascript:NativeBridge.NativeCallback('" + faceCallBackId + "','" + base64 + "')");
        binding.cameralayer.post(() -> binding.cameralayer.setVisibility(View.GONE));
        binding.ivFacePic.post(() -> binding.ivFacePic.setImageDrawable(null));
    }

    public void takePhoto(View view) {
        camera.takePicture(null, null, pictureCallback);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_CAMERA_PERMISSION:
                if (permissions.length != 1 || grantResults.length != 1) {
//                    throw new RuntimeException("Error on requesting camera permission.");
                    Toast.makeText(this, "没有取到拍照权限",
                            Toast.LENGTH_SHORT).show();
                }
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "获取到拍照权限",
                            Toast.LENGTH_SHORT).show();
                    initCamera();
                }
                // No need to start camera here; it is handled by onResume
                break;
        }

    }

    private static final int REQUEST_CAMERA_PERMISSIONS = 1;

    // 请求相机权限
    private void requestCameraPermissions() {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
//            申请成功
            initCamera();
        } else if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.CAMERA)) {
            ConfirmationDialogFragment
                    .newInstance("获取相机权限失败",
                            new String[]{Manifest.permission.CAMERA},
                            REQUEST_CAMERA_PERMISSION,
                            "没有相机权限，app不能为您进行脸部检测")
                    .show(getSupportFragmentManager(), "");
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA_PERMISSION);
        }
    }

}
