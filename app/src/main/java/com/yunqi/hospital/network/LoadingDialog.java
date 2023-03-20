package com.yunqi.hospital.network;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;

import com.yunqi.hospital.R;


/**
 * Loading Dialog
 */
public class LoadingDialog extends Dialog {

    public LoadingDialog(@NonNull Context context) {
        super(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_loading);
    }
}
