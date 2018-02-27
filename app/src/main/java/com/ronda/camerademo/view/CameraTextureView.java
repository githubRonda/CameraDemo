package com.ronda.camerademo.view;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;

import java.io.IOException;

/**
 * Created by Ronda on 2017/12/25.
 *
 * 1. 给 TextureView 设置监听器
 * 2. 使用 setPreviewTexture(SurfaceTexture)设置预览时, 参数是 SurfaceTexture
 * 3. SurfaceTexture 的获取方法:
 * -- 1) 从 TextureView 中获取. mSurfaceTexture = mTextureView.getSurfaceTexture();
 * -- 2) 四个回调方法中都有 SurfaceTexture 形参
 */

public class CameraTextureView extends BaseCameraView implements TextureView.SurfaceTextureListener {


    private TextureView mTextureView;
    private SurfaceTexture mSurfaceTexture;

    public CameraTextureView(Context context, int cameraId) {
        super(context, cameraId);
        Log.d(TAG, "CameraTextureView");
    }

    public CameraTextureView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected View onCreateCameraView(Context context) {
        mTextureView = new TextureView(context);
        // addView(mTextureView);// 抽取到父类中了

        mTextureView.setSurfaceTextureListener(this);

        mSurfaceTexture = mTextureView.getSurfaceTexture(); // 初始化时getSurfaceTexture()返回null, 直到 onSurfaceTextureAvailable 被回调为止

        Log.d(TAG, "mSurfaceTexture: "+mSurfaceTexture);
        return mTextureView;
    }


    /**
     * 调用顺序
     * CameraTextureView构造器 --> onSurfaceTextureAvailable() --> onSurfaceTextureUpdated()... --> onSurfaceTextureDestroyed()
     *
     * onSurfaceTextureSizeChanged() 在第一次创建时不会被调用, 而且在华为mate7上测试发现及时旋转屏幕(全屏显示)这个方法也会不被调用.
     */
    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        Log.d(TAG, "onSurfaceTextureAvailable");

        mSurfaceTexture = surface;

        // stop preview before making changes
        stopPreview();

        /**
         * 程序从后台切换到前台时, 会回调 onSurfaceTextureAvailable() 方法,
         * 但由于切换到后台时回调了 onSurfaceTextureDestroyed() 方法导致 mCamera 被释放和置空了, 所以若是不进行判断的话会空指针异常
         */
        if (mCamera == null) {
            mCamera = getCameraInstance(mCameraId);
        }

        setOptimalPreviewSize(width, height);

        setCameraPictureSize();

        setCorrectCameraOrientation(mCamera);

        startPreview();
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        Log.d(TAG, "onSurfaceTextureSizeChanged");
        mSurfaceTexture = surface;

        // stop preview before making changes
        stopPreview();


        if (mCamera == null) {
            mCamera = getCameraInstance(mCameraId);
        }

        setOptimalPreviewSize(width, height);

        setCameraPictureSize();

        setCorrectCameraOrientation(mCamera);

        requestLayout();

        // start preview with new settings
        startPreview();
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        Log.d(TAG, "onSurfaceTextureDestroyed");
        stopPreviewAndReleaseCamera();
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        Log.d(TAG, "onSurfaceTextureUpdated");
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

            mCamera.setPreviewTexture(mSurfaceTexture);
            mCamera.startPreview();

            //startFaceDetection();// start face detection feature
        } catch (IOException e) {
            Log.d(TAG, "Error setting camera preview: " + e.getMessage());
            releaseCamera(); // 若预览失败,则释放Camera
        }
    }
}
