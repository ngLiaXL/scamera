package com.sc.camera;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Window;
import android.view.WindowManager;

import java.io.FileOutputStream;

public class MainActivity extends AppCompatActivity {

    /**
     * INTERVAL 间隔多少秒拍照一次
     * 拍照后的文件存储到SD卡根目录a/b 文件夹下面
     * 如果需要上传到服务器，取a/b 下面的文件直接上传
     * 使用UploadTask这个类上传
     */
    public static final long INTERVAL = 15000;

    com.sc.camera.camera.CameraView cameraView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        cameraView = new com.sc.camera.camera.CameraView(this);
        setContentView(cameraView);
        SService.runIntentInService(this, new Intent(this, SService.class));

    }


    public void startCapture() {
        try {
            if (cameraView != null) {
                cameraView.takePicture(new Camera.PictureCallback() {
                    @Override
                    public void onPictureTaken(byte[] data, Camera camera) {
                        try {
                            String path = ExternalStorageUtils.getDiskCacheDir(String.valueOf(System
                                    .currentTimeMillis())).getAbsolutePath();
                            data2file(data, path);
                        } catch (Exception e) {
                        }
                        camera.startPreview();
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void data2file(byte[] w, String fileName) throws Exception {
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(fileName);
            out.write(w);
            out.close();
        } catch (Exception e) {
            if (out != null)
                out.close();
            throw e;
        }
    }

    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            startCapture();
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(mReceiver);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
