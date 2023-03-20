package com.yunqi.hospital;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.yunqi.hospital.service.ShutdownService;

public class BootBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) { // 开机 (4.0需要应用先打开一次)
            Intent thisIntent = new Intent(context, WebViewActivity.class);
            thisIntent.setAction("android.intent.action.MAIN");
            thisIntent.addCategory("android.intent.category.LAUNCHER");
            thisIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(thisIntent);
        } else if (Intent.ACTION_SHUTDOWN.equals(intent.getAction())) { // 关机
            Log.e("moon", "shutdown");
            context.startService(new Intent(context, ShutdownService.class));
        }
    }
}