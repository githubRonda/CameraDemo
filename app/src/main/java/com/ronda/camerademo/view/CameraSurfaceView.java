package com.ronda.camerademo.view;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;

import java.io.IOException;

/**
 * Created by Ronda on 2017/12/25.
 *
 * SurfaceView has a SurfaceHolder, SurfaceHolder has a Surface
 *
 * 1. 给 SurfaceHolder 添加回调方法
 * 2. 使用setPreviewDisplay(SurfaceHolder)设置预览时, 参数也是SurfaceHolder
 * 3. SurfaceHolder的获取方法:
 * -- 1) 从SurfaceView中获取.mSurfaceHolder = mSurfaceView.getHolder();
 * -- 2) 三个回调方法中都有 SurfaceHolder 形参
 */

public class CameraSurfaceView extends BaseCameraView implements SurfaceHolder.Callback {

    private SurfaceView mSurfaceView;
    private SurfaceHolder mHolder;

    public CameraSurfaceView(Context context, int cameraId) {
        super(context, cameraId);
        Log.d(TAG, "CameraSurfaceView");
    }

    public CameraSurfaceView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected View onCreateCameraView(Context context) {
        mSurfaceView = new SurfaceView(context);
        // addView(mSurfaceView); // 抽取到父类中了

        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = mSurfaceView.getHolder();
        mHolder.addCallback(this);


        //兼容android 3.0以下的API
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            // deprecated setting, but required on Android versions prior to 3.0
            mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }
        return mSurfaceView;
    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {

        Log.d(TAG, "surfaceCreated");

        mHolder = holder;

        holder.setKeepScreenOn(true);
        startPreview();
    }


    /**
     * 当要改变sufaceView 的size 或者 rotation 或者 Image or Video的格式时, 必须要先停止预览, 设置好这些参数后, 再启动预览
     */
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        Log.d(TAG, "surfaceChanged --> format: " + format + ", w: " + w + ", h:" + h);

        mHolder = holder;

        // If your preview can change or rotate, take care of those events here.
        // Make sure to stop the preview before resizing or reformatting it.

        if (mHolder.getSurface() == null) {
            // preview surface does not exist
            Log.d(TAG, "mHolder.getSurface() == null");
            return;
        }


        // stop preview before making changes
        stopPreview();

        // 否则下面的参数设置可能会报空指针异常
        if (mCamera == null) {
            mCamera = getCameraInstance(mCameraId);
        }

        // set preview size and make any resize, rotate or reformatting(格式变化) changes here
        /**
         * When setting preview size, you must use values from Camera.Parameters.getSupportedPreviewSizes().Do
         * not set arbitrary values in the Camera.Parameters.setPreviewSize() method.
         *
         * you can also use the setDisplayOrientation() method to set the rotation of the preview image
         */
        //...
        setOptimalPreviewSize(w, h);

        setCameraPictureSize();

        setCorrectCameraOrientation(mCamera);

        requestLayout();

        // start preview with new settings
        startPreview();
    }


    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d(TAG, "surfaceDestroyed");

        // Surface will be destroyed when we return, so stop the preview.
        stopPreviewAndReleaseCamera();
    }


    /**
     * 开始预览. surfaceCreated() 和 surfaceChanged() 以及 setCameraId() 时调用
     */
    @Override
    public void startPreview() {
        // The Surface has been created, now tell the camera where to draw the preview.
        try {
            if (mCamera == null) {
                mCamera = getCameraInstance(mCameraId);
            }

            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();

            //startFaceDetection();// start face detection feature
        } catch (IOException e) {
            Log.d(TAG, "Error setting camera preview: " + e.getMessage());
            releaseCamera(); // 若预览失败,则释放Camera
        }
    }
}
