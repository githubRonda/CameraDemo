package com.ronda.camerademo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.ronda.camerademo.view.CameraPreview;


/**
 * Created by Ronda on 2017/12/16.
 */

public class CameraActivity extends AppCompatActivity {
    private static final String TAG = CameraActivity.class.getSimpleName();

    private FrameLayout mFlContainer;
    private CameraPreview mCameraPreview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.camera_preview);

        if (!CameraPreview.checkCameraHardware(this)) {
            Log.d(TAG, "not support camera hardware");
            return;
        }

        mCameraPreview = new CameraPreview(this, 0);

        mFlContainer = (FrameLayout) findViewById(R.id.camera_preview);
        mFlContainer.addView(mCameraPreview, 0);
        findViewById(R.id.btn_image_capture).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCameraPreview.takePicture();
            }

        });


        findViewById(R.id.btn_focus).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCameraPreview.reAutoFocus();
            }
        });

        findViewById(R.id.btn2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCameraPreview.toggleVideoRecorder();
            }
        });


    }

}