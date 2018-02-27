package com.ronda.camerademo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.ronda.camerademo.view.BaseCameraView;
import com.ronda.camerademo.view.CameraPreview;
import com.ronda.camerademo.view.CameraTextureView;


/**
 * Created by Ronda on 2017/12/16.
 *
 * CameraSurfaceView, CameraTextureView, CameraGLSurfaceView 都可以直接在布局文件中显示,通过camera_id 属性指定Camera
 */

public class CameraViewActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.camera_preview2);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
