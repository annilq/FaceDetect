package com.yunqi.hospital.device;

public class DeviceConstant {
    public static class State {
        public static final int STATE_NORMAL = 1; // 正常
        public static final int STATE_NOT_LOGIN = 2; // 未登录
        public static final int STATE_NOT_AVAILABLE = 3; // 不可用
    }

    public static class SpKey {
        public static final String device_state = "device_state";
        public static final String device_id = "device_id";
        public static final String auth_code = "auth_code";
        public static final String token = "token";
    }
}
