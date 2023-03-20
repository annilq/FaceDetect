package com.yunqi.hospital.network;

import android.os.Environment;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import com.blankj.utilcode.util.Utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * 下载任务
 */
public class DownloadAsyncTask extends AsyncTask<String, Integer, Boolean> {

    public interface DownloadCallback {
        void onDownloadSuccess(String filePath);

        void onDownloadFailed(String errorInfo);
    }

    private Context context;
    private DownloadCallback callback;
    private ProgressDialog dialog;
    private String resultInfo;

    public DownloadAsyncTask(Context context, DownloadCallback callback) {
        this.context = context;
        this.callback = callback;
    }

    @Override
    protected void onPreExecute() {
        if (dialog == null) {
            dialog = new ProgressDialog(context);
            dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            dialog.setIndeterminate(false);
            dialog.setMax(100);
            dialog.setMessage("正在下载...");
            dialog.setProgress(0);
            dialog.setCancelable(false);
            dialog.setCanceledOnTouchOutside(false);
        }

        dialog.show();
    }
    public static String getFileDirPath(String fileDir) {
        String rootDir;
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            rootDir = Environment.getExternalStorageDirectory().getPath();
        } else {
            rootDir = Utils.getApp().getFilesDir().getPath();
        }

        rootDir = rootDir + File.separator + "Const.APP_NAME" + File.separator + fileDir;
        File dirFile = new File(rootDir);
        if (!dirFile.exists()) {
            dirFile.mkdirs();
        }

        return rootDir;
    }

    public static String getFileStoragePath(String fileDir, String fileName) {
        return getFileDirPath(fileDir) + File.separator + fileName;

    }
    @Override
    protected Boolean doInBackground(String... params) {
        HttpURLConnection conn = null;
        InputStream is = null;
        FileOutputStream fos = null;
        try {
            String fileUrl = params[0];
            String fileDir = params[1];
            String fileName = params[2];

            URL url = new URL(fileUrl);
            conn = (HttpURLConnection) url.openConnection();
//            conn.setRequestMethod("GET");
//            conn.setDoOutput(true);
//            conn.connect();

            int totalLength = conn.getContentLength();
            is = conn.getInputStream();

            resultInfo = getFileStoragePath(fileDir, fileName);
            File file = new File(resultInfo);
            fos = new FileOutputStream(file);

            long readLength = 0;
            int numRead;
            byte buffer[] = new byte[1024];
            while ((numRead = is.read(buffer)) > 0) {
                fos.write(buffer, 0, numRead);
                readLength += numRead;
                publishProgress((int) (readLength * 100 / totalLength));
            }

        } catch (Exception e) {
            resultInfo = e.getMessage();
            return false;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }

        return true;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        dialog.setProgress(values[0]);
    }

    @Override
    protected void onPostExecute(Boolean result) {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }

        if (this.callback != null) {
            if (result) {
                this.callback.onDownloadSuccess(resultInfo);
            } else {
                this.callback.onDownloadFailed(resultInfo);
            }
        }
    }

}
