package com.yunqi.hospital;

import android.Manifest;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;

import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;

import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import com.blankj.utilcode.util.AppUtils;
import com.blankj.utilcode.util.NetworkUtils;
import com.blankj.utilcode.util.SPUtils;
import com.yunqi.hospital.databinding.ActivityWebviewBinding;
import com.yunqi.hospital.device.DeviceConstant;
import com.yunqi.hospital.network.ApiClient;
import com.yunqi.hospital.network.ApiObserver;
import com.yunqi.hospital.network.ApiService;
import com.zhanyun.cameraface.camera.CameraView;
import com.zhanyun.cameraface.camera.FaceSDK;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class WebViewActivity extends AppCompatActivity implements WebViewInterFace {

    private ActivityWebviewBinding binding;

    private static final String FRAGMENT_DIALOG = "dialog";
    private static final String TAG = "MainActivity";

    private static final int REQUEST_CAMERA_PERMISSION = 1;
    private String faceCallBackId;
    private String base64Str;
    private Handler mBackgroundHandler;
    long lastModirTime;
    private CameraView.Callback mCallback = new CameraView.Callback() {

        @Override
        public void onCameraOpened(CameraView cameraView) {
            Log.d(TAG, "onCameraOpened");
        }

        @Override
        public void onCameraClosed(CameraView cameraView) {
            Log.d(TAG, "onCameraClosed");
        }

//        @Override
//        public void onPictureTaken(CameraView cameraView, final byte[] data) {
//            Log.i("take photo", "take photo-------------");
//            base64Str = Base64.encodeToString(data, Base64.DEFAULT);
//
//            Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
//            Bitmap finalBitmap = bitmap;
//            runOnUiThread(() -> binding.ivFacePic.setImageBitmap(finalBitmap));
//        }
//        拍照消课一起进行
        @Override
        public void onPictureTaken(CameraView cameraView, final byte[] data) {
            Log.i("take photo", "take photo-------------");
            base64Str = Base64.encodeToString(data, Base64.DEFAULT);
//            runOnUiThread(()->endDetect(binding.confirmButton));
            endDetect(binding.camera);
        }

//        @Override
//        public void onPreviewFrame(final byte[] data, final Camera camera) {
//            if (System.currentTimeMillis() - lastModirTime <= 200 || data == null || data.length == 0) {
//                return;
//            }
//            Log.i(TAG, "onPreviewFrame " + (data == null ? null : data.length));
//            getBackgroundHandler().post(new FaceThread(data, camera));
//            lastModirTime = System.currentTimeMillis();
//        }
    };

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

        if (binding.camera != null) {
            binding.camera.addCallback(mCallback);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideStatusBar();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
//            申请成功
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

    public void reLaunch(View view) {
        binding.webView.post(this::recreate);
    }

    /**
     * 结果回调
     */
    public void loadCallback(final String url) {
        runOnUiThread(() -> {
            binding.webView.loadUrl(url);
        });
//        binding.webView.post(() -> binding.webView.loadUrl(url));
    }

    // ============ network request ==================


    public String getDeviceSN() {
        if (TextUtils.isEmpty(Build.SERIAL) || "unknown".equals(Build.SERIAL)) {
            return "h123456";
        }

//        return Build.SERIAL;
        return Build.SERIAL + System.currentTimeMillis();
    }


    private void toast(final String msg) {
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
        NetworkUtils.unregisterNetworkStatusChangedListener(mOnNetworkStatusChangedListener);
//        人脸识别
        if (mBackgroundHandler != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                mBackgroundHandler.getLooper().quitSafely();
            } else {
                mBackgroundHandler.getLooper().quit();
            }
            mBackgroundHandler = null;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

    }

    @Override
    protected void onPause() {
        binding.camera.stop();
        super.onPause();
    }

    //图像预览
//    private Handler getBackgroundHandler() {
//        if (mBackgroundHandler == null) {
//            HandlerThread thread = new HandlerThread("background");
//            thread.start();
//            mBackgroundHandler = new Handler(thread.getLooper());
//        }
//        return mBackgroundHandler;
//    }

    //    private class FaceThread implements Runnable {
//        private byte[] mData;
//        private ByteArrayOutputStream mBitmapOutput;
//        private Matrix mMatrix;
//        private Camera mCamera;
//
//        public FaceThread(byte[] data, Camera camera) {
//            mData = data;
//            mBitmapOutput = new ByteArrayOutputStream();
//            mMatrix = new Matrix();
//            int mOrienta = binding.camera.getCameraDisplayOrientation();
//            mMatrix.postRotate(mOrienta * -1);
//            mMatrix.postScale(-1, 1);//默认是前置摄像头，直接写死 -1 。
//            mCamera = camera;
//        }
//
//        @Override
//        public void run() {
//            Log.i(TAG, "thread is run");
//            Bitmap bitmap = null;
//            Bitmap roteBitmap = null;
//            try {
//                Camera.Parameters parameters = mCamera.getParameters();
//                int width = parameters.getPreviewSize().width;
//                int height = parameters.getPreviewSize().height;
//
//                YuvImage yuv = new YuvImage(mData, parameters.getPreviewFormat(), width, height, null);
//                mData = null;
//                yuv.compressToJpeg(new Rect(0, 0, width, height), 100, mBitmapOutput);
//
//                byte[] bytes = mBitmapOutput.toByteArray();
//                BitmapFactory.Options options = new BitmapFactory.Options();
//                options.inPreferredConfig = Bitmap.Config.RGB_565;//必须设置为565，否则无法检测
//                bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
//
//                mBitmapOutput.reset();
//                roteBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), mMatrix, false);
//                List<Rect> rects = FaceSDK.detectionBitmap(bitmap, getResources().getDisplayMetrics().widthPixels, getResources().getDisplayMetrics().heightPixels);
//
//                if (null == rects || rects.size() == 0) {
//                    Log.i("janecer", "没有检测到人脸哦");
//                } else {
//                    Log.i("janecer", "检测到有" + rects.size() + "人脸");
////                    可以在识别人脸时候马上结束，也可以用户拍照
////                    Bitmap finalBitmap = bitmap;
////                    runOnUiThread(() -> binding.ivFacePic.setImageBitmap(finalBitmap));
////                    binding.camera.stop();
//                    for (int i = 0; i < rects.size(); i++) {//返回的rect就是在TexutView上面的人脸对应的实际坐标
//                        Log.i("janecer", "rect : left " + rects.get(i).left + " top " + rects.get(i).top + "  right " + rects.get(i).right + "  bottom " + rects.get(i).bottom);
//                    }
//                }
//
//            } catch (Exception e) {
//                e.printStackTrace();
//            } finally {
//                mMatrix = null;
//                if (bitmap != null) {
//                    bitmap.recycle();
//                }
//                if (roteBitmap != null) {
//                    roteBitmap.recycle();
//                }
//
//                if (mBitmapOutput != null) {
//                    try {
//                        mBitmapOutput.close();
//                        mBitmapOutput = null;
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                }
//            }
//        }
//    }
    public void scanFace(String callbackId) {
        faceCallBackId = callbackId;
        binding.cameralayer.setVisibility(View.VISIBLE);

        binding.camera.setCameraDistance(100);
        binding.camera.start();
    }
    public void scanFace(View view) {
        binding.cameralayer.setVisibility(View.VISIBLE);

        binding.camera.setCameraDistance(1000);
        binding.camera.start();
    }

    public void cancelDetect(View view) {
        base64Str = "";
        endDetect(view);
    }

    public void endDetect(View view) {
        binding.cameralayer.setVisibility(View.GONE);
        binding.ivFacePic.setImageDrawable(null);
        loadCallback("javascript:NativeBridge.NativeCallback('" + faceCallBackId + "','" + base64Str + "')");
//        binding.camera.stop();
    }

//
    public void confirmDetect(View view) {
        binding.camera.takePicture();
    }

    public void takePhoto(View view) {
        binding.camera.takePicture();
    }

    public void reload(View view) {
//        loadHomePage();
        reload();
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
                }
                // No need to start camera here; it is handled by onResume
                break;
        }
    }
}
