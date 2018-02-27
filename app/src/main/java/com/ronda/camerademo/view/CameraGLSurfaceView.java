package com.ronda.camerademo.view;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import java.io.IOException;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by Ronda on 2017/12/25.
 *
 * 参考
 * http://blog.csdn.net/yanzi1225627/article/details/33339965/
 *
 * 预览时只有 portrait 时 才是正确的. 还有待完善
 */

public class CameraGLSurfaceView extends BaseCameraView implements GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener {

    int mTextureID = -1;
    DirectDrawer mDirectDrawer;

    private GLSurfaceView mGLSurfaceView;
    private SurfaceTexture mSurfaceTexture;

    public CameraGLSurfaceView(Context context, int cameraId) {
        super(context, cameraId);
    }

    public CameraGLSurfaceView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected View onCreateCameraView(Context context) {
        mGLSurfaceView = new GLSurfaceView(context){
            @Override
            public void onPause() {// 测试发现该方法不会自动被回调, 只能手动调用 mGLSurfaceView.onPause()
                super.onPause();

                Log.d(TAG, "mGLSurfaceView --> onPause");

                stopPreviewAndReleaseCamera();
            }
        };
        //addView(mGLSurfaceView);// 抽取到父类中了

        // 设置GLSurfaceView的版本.
        // 如果没这个设置是啥都画不出来了，因为Android支持OpenGL ES1.1和2.0及最新的3.0，而且版本间差别很大。不告诉他版本他不知道用哪个版本的api渲染。
        mGLSurfaceView.setEGLContextClientVersion(2);
        mGLSurfaceView.setRenderer(this);

        /**
         * When renderMode is RENDERMODE_CONTINUOUSLY, the renderer is called repeatedly to re-render the scene. When renderMode is RENDERMODE_WHEN_DIRTY, the renderer only rendered when the surface is created, or when requestRender is called. Defaults to RENDERMODE_CONTINUOUSLY.
         *
         * Using RENDERMODE_WHEN_DIRTY can improve battery life and overall system performance by allowing the GPU and CPU to idle when the view does not need to be updated.
         */
        mGLSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        return mGLSurfaceView;
    }

    @Override
    public void startPreview() {
        // The Surface has been created, now tell the camera where to draw the preview.
        try {
            if (mCamera == null) {
                mCamera = getCameraInstance(mCameraId);
            }

            mCamera.setPreviewTexture(mSurfaceTexture);
            mCamera.startPreview();

        } catch (IOException e) {
            Log.d(TAG, "Error setting camera preview: " + e.getMessage());
            releaseCamera(); // 若预览失败,则释放Camera
        }
    }

    /**
     * 调用顺序: onSurfaceCreated() --> onSurfaceChanged() --> { onDrawFrame() --> onFrameAvailable()[这个回调方法是OnFrameAvailableListener中的] }(这两个方法循环调用)...
     * 当旋转屏幕时, 又从头开始来一遍;
     * 当从后台切换到前台时, 从 onSurfaceChanged() 开始走
     */
    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        Log.i(TAG, "onSurfaceCreated...");
        mTextureID = createTextureID();
        mSurfaceTexture = new SurfaceTexture(mTextureID);
        mSurfaceTexture.setOnFrameAvailableListener(this);
        mDirectDrawer = new DirectDrawer(mTextureID);

        startPreview();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        Log.i(TAG, "onSurfaceChanged...");
        GLES20.glViewport(0, 0, width, height);

        stopPreview();

        if (mCamera == null) {
            mCamera = getCameraInstance(mCameraId);
        }

        setOptimalPreviewSize(width, height);

        setCameraPictureSize();

        setCorrectCameraOrientation(mCamera);// 这个设置方向对于预览不管用, 对于图片时有用的. 而且预览时只有 portrait 时 才是正确的. 估计是在其他地方有修改

        startPreview();
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        Log.i(TAG, "onDrawFrame...");
        GLES20.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        mSurfaceTexture.updateTexImage();
        float[] mtx = new float[16];
        mSurfaceTexture.getTransformMatrix(mtx);
        /**
         * 如果不调用mDirectDrawer.draw(mtx);是啥都显示不出来的！！！这是GLSurfaceView的特别之处。为啥呢？
         * 因为GLSurfaceView不是Android亲生的，而Surfaceview和TextureView才是。所以得自己按照OpenGL ES的流程画
         */
        mDirectDrawer.draw(mtx);
    }

    /**
     * 正因是RENDERMODE_WHEN_DIRTY所以就要告诉GLSurfaceView什么时候Render，也就是啥时候进到onDrawFrame()这个函数里。
     * SurfaceTexture.OnFrameAvailableListener这个接口就干了这么一件事，当有数据上来后会进到onFrameAvailable(), 然后执行requestRender()
     *
     * @param surfaceTexture
     */
    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        Log.i(TAG, "onFrameAvailable...");
        mGLSurfaceView.requestRender();
    }


    /**
     * 与TextureView里对比可知，TextureView预览时因为实现了SurfaceTextureListener会自动创建SurfaceTexture。但在GLSurfaceView里则要手动创建同时绑定一个纹理ID
     *
     * @return
     */
    private int createTextureID() {
        int[] texture = new int[1];

        GLES20.glGenTextures(1, texture, 0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texture[0]);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);

        return texture[0];
    }

}