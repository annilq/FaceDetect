package com.yunqi.hospital;

public interface WebViewInterFace {
    void reload();

    void scanFace(String result);

    void loadCallback(String result);

    String getDeviceSN();
}
