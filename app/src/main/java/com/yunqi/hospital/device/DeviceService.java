package com.yunqi.hospital.device;

import static android.content.Context.BIND_AUTO_CREATE;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import cn.hsa.ctp.device.sdk.managerService.aidl.Beeper;
import cn.hsa.ctp.device.sdk.managerService.aidl.HealthCardReader;
import cn.hsa.ctp.device.sdk.managerService.aidl.HospitalCardReader;
import cn.hsa.ctp.device.sdk.managerService.aidl.HybridReader;
import cn.hsa.ctp.device.sdk.managerService.aidl.IDCardReader;
import cn.hsa.ctp.device.sdk.managerService.aidl.ManagerServiceInterface;
import cn.hsa.ctp.device.sdk.managerService.aidl.Scanner;

public class DeviceService {
    private static ManagerServiceInterface deviceService;
    private static DeviceService device;
//    private final Object waitObj = new Object();

    public synchronized static DeviceService getInstance(Context context) {
        if (device == null) {
            device = new DeviceService(context);
        }
        return device;
    }

    private final Context context;

    private DeviceService(Context context) {
        this.context = context;
    }

    public void connect() {
        Log.d("DeviceService", "---------------->connect time:" + System.currentTimeMillis());
        if (deviceService == null) {
            Log.d("DeviceService", "---------------->run time:" + System.currentTimeMillis());
            Intent intent = new Intent();
            intent.setPackage("cn.hsa.ctp.device.sdk.managerService.aidl"); // 使用服务apk的时候使用
//            intent.setPackage("com.spdb.hlj.sdk.managerService.aidl"); // 使用服务apk的时候使用
            intent.setAction("cn.hsa.ctp.device.sdk.managerService.start.aidl");
            context.bindService(intent, serviceConnection, BIND_AUTO_CREATE);
//            try {
//                synchronized (waitObj) {
//                    waitObj.wait();
//                }
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
        }
    }

    public void disconnect() {
        if (deviceService != null) {
            try {
                context.unbindService(serviceConnection);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                deviceService = null;
            }
        }
    }

    private final ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceDisconnected(ComponentName name) {
            deviceService = null;
//            connect();
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d("DeviceService", "---------------->connect success time:" + System.currentTimeMillis());
            deviceService = ManagerServiceInterface.Stub.asInterface(service);
//            synchronized (waitObj) {
//                waitObj.notifyAll();
//            }
        }
    };

    public Scanner getScanner() {
        try {
            connect();
            return Scanner.Stub.asInterface(deviceService.getScanner(0));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public IDCardReader getIDCard() {
        try {
            connect();
            return IDCardReader.Stub.asInterface(deviceService.getIDCardReader());
        } catch (RemoteException e) {
            e.printStackTrace();
            return null;
        }
    }

    public HealthCardReader getHealthCardReader() {
        try {
            connect();
            return HealthCardReader.Stub.asInterface(deviceService.getHealthCardReader());
        } catch (RemoteException e) {
            e.printStackTrace();
            return null;
        }
    }


    public Beeper getBeeper() {
        try {
            connect();
            return Beeper.Stub.asInterface(deviceService.getBeeper());
        } catch (RemoteException e) {
            e.printStackTrace();
            return null;
        }
    }

    public HospitalCardReader getHospitalCardReader() {
        try {
            connect();
            return HospitalCardReader.Stub.asInterface(deviceService.getHospitalCardReader());
        } catch (RemoteException e) {
            e.printStackTrace();
            return null;
        }
    }


    public HybridReader getHybridReader() {
        try {
            connect();
            return HybridReader.Stub.asInterface(deviceService.getHybridReader());
        } catch (RemoteException e) {
            e.printStackTrace();
            return null;
        }
    }
}
