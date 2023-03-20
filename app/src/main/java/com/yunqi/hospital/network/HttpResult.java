package com.yunqi.hospital.network;

/**
 * 请求响应基类
 */
public class HttpResult<T> {
    private int code;
    private String info;
    private T resp;

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public T getResp() {
        return resp;
    }

    public void setResp(T resp) {
        this.resp = resp;
    }
}
