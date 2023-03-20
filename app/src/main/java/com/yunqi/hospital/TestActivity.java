package com.yunqi.hospital;

import android.os.Bundle;
import android.os.RemoteException;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.yunqi.hospital.databinding.ActivityMainBinding;
import com.yunqi.hospital.device.DeviceService;
import com.yunqi.hospital.device.SDKExecutors;

import cn.hsa.ctp.device.sdk.managerService.aidl.HealthCardReader;
import cn.hsa.ctp.device.sdk.managerService.aidl.IDCardReader;
import cn.hsa.ctp.device.sdk.managerService.aidl.OnScanListener;
import cn.hsa.ctp.device.sdk.managerService.aidl.Scanner;


public class TestActivity extends AppCompatActivity implements View.OnClickListener {
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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnIdCardStart.setOnClickListener(this);
        binding.btnIdCardEnd.setOnClickListener(this);

        binding.btnHealthCardStart.setOnClickListener(this);
        binding.btnHealthCardEnd.setOnClickListener(this);

        binding.btnScanStart.setOnClickListener(this);
        binding.btnScanEnd.setOnClickListener(this);

        DeviceService.getInstance(getApplicationContext()).connect();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn_idCard_start) {
            readIdCard();
        } else if (v.getId() == R.id.btn_idCard_end) {
            closeIdCard();
        } else if (v.getId() == R.id.btn_healthCard_start) {
            readHealthCard();
        } else if (v.getId() == R.id.btn_healthCard_end) {
            closeHealthCard();
        } else if (v.getId() == R.id.btn_scan_start) {
            openScanner();
        } else if (v.getId() == R.id.btn_scan_end) {
            closeScanner();
        }
    }


    /**
     * 读取身份证
     */
    private void readIdCard() {
        SDKExecutors.getThreadPoolInstance().submit(() -> {
            try {
                if (idCardReader == null) {
                    idCardReader = DeviceService.getInstance(getApplicationContext()).getIDCard();
                    idCardReader.setFindCardTimeOut(5000); // 寻卡超时时间
                    idCardReader.setTimeOut(5000); // 读卡超时时间
                }

//                // 打开设备
//                int openResult = idCardReader.openDevice();
//                if (openResult != 0) {
//                    Log.e("moon", openResult + "");
//                    toast("打开读卡设备失败");
//                    return;
//                }
//
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
                    StringBuilder builder = new StringBuilder();
                    builder.append("用户信息：")
                            .append("姓名：" + bundle.getString("name"))
                            .append("\r\n")
                            .append("身份证号：" + bundle.getString("id_number"))
                            .append("\r\n")
                            .append("性别：" + bundle.getString("sex"))
                            .append("\r\n")
                            .append("住址：" + bundle.getString("address"))
                            .append("\r\n")
                            .append("出生日期：" +
                                    bundle.getString("birth_year") +
                                    bundle.getString("birth_moth") +
                                    bundle.getString("birth_day"));
                    String data = builder.toString();
                    toast(data);
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
    private void closeIdCard() {
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

    private void readHealthCard() {
        SDKExecutors.getThreadPoolInstance().submit(() -> {
            try {
                if (healthCardReader == null) {
                    healthCardReader = DeviceService.getInstance(getApplicationContext()).getHealthCardReader();
                }
                Bundle bundle = healthCardReader.readCard(5000);
                if (bundle != null && bundle.getInt("errorCode") == 0x90) {
                    StringBuilder builder = new StringBuilder();
                    builder.append("用户信息：\r\n")
                            .append("姓名：" + bundle.getString("name"))
                            .append("\r\n")
                            .append("社保卡号：" + bundle.getString("cardNo"))
                            .append("\r\n")
                            .append("性别：" + bundle.getString("sex"))
                            .append("\r\n")
                            .append("身份证号：" + bundle.getString("idCardNo"))
                            .append("\r\n")
                            .append("区域码：" + bundle.getString("districtCode"))
                            .append("\r\n")
                            .append("规范版本：" + bundle.getString("cardVersion"));
                    String data = builder.toString();
                    toast(data);
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
    private void closeHealthCard() {
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
    private void openScanner() {
        SDKExecutors.getThreadPoolInstance().submit(() -> {
            try {
                if (scanner == null) {
                    scanner = DeviceService.getInstance(getApplicationContext()).getScanner();
                }
                scanner.startScan(5, new OnScanListener.Stub() {
                    @Override
                    public void onSuccess(String s) throws RemoteException {
                        toast("扫码成功 " + s);
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
    private void closeScanner() {
        try {
            if (scanner != null) {
                scanner.stopScan();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void toast(final String msg) {
        runOnUiThread(() -> Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show());
    }
}