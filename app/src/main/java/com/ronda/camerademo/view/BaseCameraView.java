package com.ronda.camerademo.view;

/**
 * Created by Ronda on 2017/12/13.
 */

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.ronda.camerademo.R;
import com.ronda.camerademo.utils.MusicUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * 这个类是 CameraSurfaceView, CameraTextureView, CameraGLSurfaceView 的基类.
 * 封装的是对 Camera 的一些常用操作(设置方向, 图片大小, 拍照,录像等),
 * 但是由于预览时 SurfaceView 和 TextureView 的设置方法是不一样的, 而且两者的相关回调方法也是不一样的, 所以这些东西就交由子类来实现
 *
 * 实际开发中一般只会使用到其中一种, 所以封装成类似 CameraPreview 这种形式即可.
 */
public abstract class BaseCameraView extends FrameLayout {

    public static final String TAG = BaseCameraView.class.getSimpleName();

    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;

    protected Camera mCamera;
    protected int mCameraId;
    protected Context mContext;
    protected MediaRecorder mMediaRecorder;

    public BaseCameraView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.BaseCameraView);
        int cameraId = ta.getInt(R.styleable.BaseCameraView_camera_id, 0);
        ta.recycle();

        Log.d("Liu", "cameraId = " + cameraId);

        init(context, cameraId);
    }

    public BaseCameraView(Context context, int cameraId) {
        super(context);
        init(context, cameraId);
    }

    private void init(Context context, int cameraId) {
        this.mContext = context;
        this.mCameraId = cameraId;

        mCamera = getCameraInstance(cameraId);

        isSupportAutoFocus = context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_AUTOFOCUS);

        addView(onCreateCameraView(context));
    }


    /**
     * 因为子类可能使用 SurfaceView, GLSurfaceView, TextureView等, 所以交由子类来创建对应的View
     */
    protected abstract View onCreateCameraView(Context context) ;

    /**
     * 开始预览功能需由子类实现, 因为: SurfaceView 中是使用 mCamera.setPreviewDisplay(), 而 TextureView 和 GLSurfaceView中使用 mCamera.setPreviewTexture()
     */
    public abstract void startPreview();


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        reAutoFocus();
        return false;
    }


    /**
     * setting or change current camera in preview
     *
     * @param cameraId
     */
    public void setCameraId(int cameraId) {

        if (mCameraId == cameraId) {
            return;
        }

        stopPreviewAndReleaseCamera();

        mCameraId = cameraId;
        mCamera = getCameraInstance(cameraId);

        if (mCamera != null) {
            List<Camera.Size> mSupportedPreviewSizes = mCamera.getParameters().getSupportedPreviewSizes();
            requestLayout();

            startPreview(); // set camera id
        }
    }


    /**
     * 设置拍照的图片的大小和质量. 在surfaceCreated 和 surfaceChanged 时调用
     */
    protected void setCameraPictureSize() {
        int setFixPictureWidth = 0; // 设置最适合当前手机的图片宽度
        int setFixPictureHeight = 0; // 设置当前最适合的图片高度
        int maxPictureSize = 8000000; // 设置一个固定的最大尺寸

        Camera.Parameters param = mCamera.getParameters();
        List<Camera.Size> mSupportedsizeList = param.getSupportedPictureSizes();

        if (mSupportedsizeList != null && mSupportedsizeList.size() > 1) {
            Iterator<Camera.Size> itos = mSupportedsizeList.iterator();
            while (itos.hasNext()) {
                Camera.Size curSize = itos.next();
                int curSupporSize = curSize.width * curSize.height;
                int fixPictrueSize = setFixPictureWidth * setFixPictureHeight;
                if (curSupporSize > fixPictrueSize && curSupporSize <= maxPictureSize) {
                    setFixPictureWidth = curSize.width;
                    setFixPictureHeight = curSize.height;
                }
            }
        }
        if (setFixPictureWidth != 0 && setFixPictureHeight != 0) {
            param.setPictureSize(setFixPictureWidth, setFixPictureHeight);

            Log.d(TAG, "PictureWidth: " + setFixPictureWidth + ", PictureHeight: " + setFixPictureHeight);
        }
        param.setJpegQuality(100);
        param.setPictureFormat(ImageFormat.JPEG);
        mCamera.setParameters(param);
    }


    /**
     * 根据SurfaceView的宽高从 SupportedPreviewSizes 中选择一个宽高比比较合适的预览尺寸. 所以在surfaceChanged() 中调用并设置
     * parameters.setPreviewSize(optimalSize.width, optimalSize.height);
     * <p>
     * 若是不设置 previewSize 的话, 会自动选择一个默认值, 华为手机中测试默认值: 横竖屏均为:width: 640, height: 480.
     */
    protected void setOptimalPreviewSize(int w, int h) {

        Camera.Parameters parameters = mCamera.getParameters();
        List<Camera.Size> sizes = parameters.getSupportedPreviewSizes();

        final double ASPECT_TOLERANCE = 0.2;
        double targetRatio = (double) w / h;

        if (sizes == null)
            return;

        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        // Try to find an size match aspect ratio and size
        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE)
                continue;
            //若宽高比在允许的范围内, 则选择一个高度相差最小的
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        // Cannot find the one match the aspect ratio, ignore the requirement
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }

        Log.d(TAG, "optimalSize --> width: " + optimalSize.width + ", height: " + optimalSize.height);//width: 1440, height: 1080

        /**
         * 同时设置preview预览界面和SurfaceView的大小,的确是可以解决预览界面拉伸变长的问题
         *
         * 但是由于使用getSupportedPreviewSizes()方法获取到的Size均是width大于height的,
         * 所以我们选择的这个 optimalSize 不一定能够使得当前这个Surface的大小刚好匹配屏幕, 可能会有外边距
         * 当手机为横屏时, 可以获取到一个宽高比比较合适的值, 外边距可能还不明显; 但是当手机为竖屏时上下的外边距就十分明显了~
         *
         * 后来发现, 即使是竖屏的时候, 把 PreviewSize 设为 1920*1080, 把 SurfaceView 设为 1080*1920 也是可以的, 画面虽然是有一点缩放效果, 但是还过得去. 看自己选择
         */
        parameters.setPreviewSize(optimalSize.width, optimalSize.height); // 设置预览界面的大小. 经测试, 当SurfaceView大小一样的情况下, PreviewSize 越大预览界面越清晰
        mCamera.setParameters(parameters); // 要重新设置一下, 否者上面的parameters的参数设置是不会生效的

        final LayoutParams layoutParams = new LayoutParams(optimalSize.width, optimalSize.height);
        // 刚开始测试时没有问题的, 后来报了一个错误ViewRootImpl$CalledFromWrongThreadException后, 就一直报这个错, 退出重进也不行. 所以只好把setLayoutParams() 放到View.post()中保证一定在主线程中执行
        post(new Runnable() {
            @Override
            public void run() {
                Log.d("Liu", "ThreadId: " + Thread.currentThread().getId()); //ThreadId: 1
                // android.view.ViewRootImpl$CalledFromWrongThreadException: Only the original thread that created a view hierarchy can touch its views.
                BaseCameraView.this.setLayoutParams(layoutParams); // 当size改变的时候, 会重新回调 surfaceChanage() 方法
            }
        });
        Log.d("Liu", "ThreadId1: " + Thread.currentThread().getId()); //ThreadId1: 12319


        Camera.Size previewSize = mCamera.getParameters().getPreviewSize();
        Log.e(TAG, "当前 previewSize 预览界面的大小: width: " + previewSize.width + ", height: " + previewSize.height);//横竖屏均为:width: 640, height: 480, 感觉一直都不变
    }

    /**
     * 给Camera设置正确的预览方向和图片保存时的Camera方向
     *
     * @param camera
     */
    protected void setCorrectCameraOrientation(Camera camera) {

        Camera.CameraInfo camInfo = new Camera.CameraInfo();
        Camera.getCameraInfo(getBackFacingCameraId(), camInfo);

        // 获取到的值为0, 1, 2, 3 这四个值中的其中一个
        Display display = ((WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        int rotation = display.getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int result;
        if (camInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (camInfo.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (camInfo.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result); // 设置预览的旋转角度, 顺时针旋转

        Camera.Parameters parameters = camera.getParameters();
        parameters.setRotation(result);// 设置保存图片时的旋转角度
        camera.setParameters(parameters);
    }


    /**
     * 获取后置摄像头的id
     *
     * @return
     */
    public int getBackFacingCameraId() {
        int cameraId = -1;
        // Search for the front facing camera
        int numberOfCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                cameraId = i;
                break;
            }
        }
        return cameraId;
    }

    /**
     * 是否支持自动聚焦，默认不支持
     */
    private boolean isSupportAutoFocus = false;

    /**
     * Call the camera to Auto Focus
     */
    public void reAutoFocus() {
        if (isSupportAutoFocus) {
            mCamera.autoFocus(new Camera.AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean success, Camera camera) {
                    Log.d(TAG, "onAutoFocus --> success: " + success);
                }
            });
        }
    }


    /**
     * 自动聚焦，然后拍照
     */
    public void takePicture() {
        if (mCamera != null) {
            mCamera.autoFocus(autoFocusCallback);
        }
    }

    private Camera.AutoFocusCallback autoFocusCallback = new Camera.AutoFocusCallback() {

        public void onAutoFocus(boolean success, Camera camera) {
            // TODO Auto-generated method stub
            if (success) {
                Log.i(TAG, "autoFocusCallback: success...");
                doTakePicture();
            } else {
                Log.i(TAG, "autoFocusCallback: fail...");
                if (isSupportAutoFocus) {
                    doTakePicture();
                }
            }
        }
    };


    /**
     * 调整照相的方向，执行拍照
     */
    private void doTakePicture() {

        if (mCamera != null) {
            /**
             * @param shutter   the callback for image capture moment, or null
             * @param raw       the callback for raw (uncompressed) image data, or null
             * @param postview  callback with postview image data, may be null (not all hardware supports )
             * @param jpeg      the callback for JPEG image data, or null
             */
            mCamera.takePicture(shutterCallback, rawPictureCallback, null, mPictureCallback);
        }
    }

    //shutter: 快门,百叶窗
    protected Camera.ShutterCallback shutterCallback = new Camera.ShutterCallback() {
        @Override
        public void onShutter() {
            // 可以在这里播放一个拍照的音频
            MusicUtil.playMusic(getContext(), R.raw.photo);
        }
    };

    protected Camera.PictureCallback rawPictureCallback = new Camera.PictureCallback() {

        /**
         * @param data 这个data是未经过压缩的原始的图片数据, 比mPictureCallback回调中的data要大
         * @param camera
         */
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {

        }
    };

    protected Camera.PictureCallback mPictureCallback = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            new SavePictureTask().execute(data);
            mCamera.startPreview();//重新开始预览
        }
    };


    public class SavePictureTask extends AsyncTask<byte[], Void, Void> {

        private File pictureFile;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
        }

        @Override
        protected Void doInBackground(byte[]... params) {

            if (params == null || params[0] == null) {

                Log.e(TAG, "picture data is null");
                return null;
            }

            if (pictureFile == null) {
                Toast.makeText(mContext, "请插入存储卡！", Toast.LENGTH_SHORT).show();
                return null;
            }

            FileOutputStream fos = null;

            try {
                fos = new FileOutputStream(pictureFile);
                fos.write(params[0]);
                fos.flush();

                /**
                 * 还有一种保存图片的方式: 即先把 byte[] 转成 bitmap 对象, 然后调用 Bitmap.compress() 把数据压缩至文件中.
                 * 好处就是: 可以对bitmap进行处理, eg: 旋转, 缩放等各种矩阵操作
                 */
                // Bitmap bitmap = BitmapFactory.decodeByteArray(params[0], 0, params[0].length);
                // Matrix matrix = new Matrix();
                // matrix.postRotate(90f);
                // Bitmap rotaBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false);
                // bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);

            } catch (FileNotFoundException e) {
                Log.d(TAG, "File not found: " + e.getMessage());
            } catch (IOException e) {
                Log.d(TAG, "Error accessing file: " + e.getMessage());
            } finally {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }
    }

    /**
     * Create a File for saving an image or video
     */
    protected File getOutputMediaFile(int type) {
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.


        Log.d(TAG, "getExternalStorageState: " + Environment.getExternalStorageState());
        Log.d(TAG, "===" + Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString());
        Log.d(TAG, "+++" + mContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES).toString());

        // File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "MyCameraApp");
        File mediaStorageDir = new File(mContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "MyCameraApp"); // 在华为手机上上面这句会失败, 这句是可以成功
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator + "IMG_" + timeStamp + ".jpg");
        } else if (type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator + "VID_" + timeStamp + ".mp4");
        } else {
            return null;
        }

        return mediaFile;
    }


    /**
     * When this function returns, mCamera will be null.
     */
    public void stopPreviewAndReleaseCamera() {

        stopPreview();

        // Important: Call release() to release the camera for use by other
        // applications. Applications should release the camera immediately
        // during onPause() and re-open() it during onResume()).
        releaseCamera();

    }

    public void stopPreview() {
        if (mCamera != null) {
            // Call stopPreview() to stop updating the preview surface.
            mCamera.stopPreview();
        }
    }

    /**
     * 释放Camera
     */
    public void releaseCamera() {
        if (mCamera != null) {
            mCamera.release();        // release the camera for other applications
            mCamera = null;
        }
    }


    /**
     * 启动人脸识别, 在surfaceCreated 和 surfaceChanged 中调用
     * mCamera.setFaceDetectionListener(new MyFaceDetectionListener()); --> 先给Camera设置人脸检测监听器
     * <p>
     * Note: Remember to call this method after calling startPreview(). Do not attempt to start face
     * detection in the onCreate() method of your camera app's main activity,
     * as the preview is not available by this point in your application's the execution.
     */
    public void startFaceDetection() {

        // Try starting Face Detection
        Camera.Parameters params = mCamera.getParameters();

        // start face detection only *after* preview has started
        if (params.getMaxNumDetectedFaces() > 0) {

            mCamera.setFaceDetectionListener(new MyFaceDetectionListener());

            // camera supports face detection, so can start it:
            mCamera.startFaceDetection();
        }
    }

    /**
     * Check if this device has a camera
     */
    public static boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            // this device has a camera
            return true;
        } else {
            // no camera on this device
            return false;
        }

        //Android设备可以有多个摄像头, 可以使用下面方法获取摄像头数量
        //Camera.getNumberOfCameras();
    }


    /**
     * A safe way to get an instance of the Camera object.
     * <p>
     * 在有些博客中有看到过, 把 Camera.open() 放到子线程中执行. 可能是认为打开一个耗时相对较长, 会阻塞其他操作
     */
    public static Camera getCameraInstance(int cameraId) {
        Camera c = null;
        try {
            //若 cameraId 为0, 则表示访问的是第一个Camera-后置Camera
            c = Camera.open(cameraId); // attempt to get a Camera instance
        } catch (Exception e) {
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }


    //======================================视屏录制相关=============================================

    private boolean isRecording = false; // 是否正在录制视屏

    /**
     * 开始或停止录制
     * todo 这个方法更应该写在调用者中
     */
    public void toggleVideoRecorder() {
        //注：当完成视频录制，不要释放camera，否则预览将被停止。
        if (isRecording) {
            // stop recording and release camera
            mMediaRecorder.stop();  // stop the recording
            releaseMediaRecorder(); // release the MediaRecorder object
            mCamera.lock();         // take camera access back from MediaRecorder

            // inform the user that recording has stopped
            Toast.makeText(mContext, "停止录制", Toast.LENGTH_SHORT).show();

            isRecording = false;
        } else {
            // initialize video camera
            if (prepareVideoRecorder()) {
                // Camera is available and unlocked, MediaRecorder is prepared,
                // now you can start recording
                mMediaRecorder.start();

                // inform the user that recording has started
                Toast.makeText(mContext, "开始录制", Toast.LENGTH_SHORT).show();

                isRecording = true;
            } else {
                // prepare didn't work, release the camera
                releaseMediaRecorder();
                // inform user
            }
        }
    }


    /**
     * MediaRecorder 录制前的准备配置
     *
     * @return
     */
    public boolean prepareVideoRecorder() {

        mMediaRecorder = new MediaRecorder();

        // Step 1: Unlock and set camera to MediaRecorder
        mCamera.unlock();
        mMediaRecorder.setCamera(mCamera);

        // Step 2: Set sources
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

        // Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
            CamcorderProfile camcorderProfile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);// 这里可以配置camcorderProfile中的各个成员变量
            mMediaRecorder.setProfile(camcorderProfile);
        } else { //兼容android 2.2以下的API
            // Step 3: Set output format and encoding (for versions prior to API Level 8)
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
            mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.DEFAULT);

            /*
            The following video recording parameters for MediaRecorder are given default settings
            setVideoEncodingBitRate()
            setVideoSize()
            setVideoFrameRate()
            setAudioEncodingBitRate()
            setAudioChannels()
            setAudioSamplingRate()
            */
        }


        // Step 4: Set output file
        mMediaRecorder.setOutputFile(getOutputMediaFile(MEDIA_TYPE_VIDEO).toString());
        // Step 4.1: Set recording length
        // mMediaRecorder.setMaxDuration(10000);
        // mMediaRecorder.setOrientationHint(270);

        // Step 5: Set the preview output
        // 测试时,发现无论是对于SurfaceView 还是 TextureView 亦或者 GLSurfaceView 都可以不用设置,录像时也是可以预览的. 估计是录像时没有关闭先前的预览的缘故
        // mMediaRecorder.setPreviewDisplay(getHolder().getSurface());

        // Step 6: Prepare configured MediaRecorder
        try {
            mMediaRecorder.prepare();
        } catch (IllegalStateException e) {
            Log.d(TAG, "IllegalStateException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        } catch (IOException e) {
            Log.d(TAG, "IOException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        }
        return true;
    }


    /**
     * 释放MediaRecorder
     */
    private void releaseMediaRecorder() {
        if (mMediaRecorder != null) {
            mMediaRecorder.reset();   // clear recorder configuration
            mMediaRecorder.release(); // release the recorder object
            mMediaRecorder = null;
            mCamera.lock();           // lock camera for later use
        }
    }


    //======================================= Camera features ======================================
    // 在使用 Camera 特性时, 一定要先检测该 Camera 是否支持这个 feature, 然后再决定使用与否

    /**
     * Checking feature.检查Camera所支持的功能 eg: auto_focus_mode
     * <p>
     * 还有很多 Camera features 都可以使用 Camera.Parameters中提供的 getSupported ...(),
     * is...Supported()或getMax的...()方法，以确定是否（以及多大程度上）支持此功能。 可以根据代码提示查看这类features
     */
    private boolean checkCameraFeature() {
        // get Camera parameters
        Camera.Parameters params = mCamera.getParameters();

        List<String> supportFocusModes = params.getSupportedFocusModes();
        if (supportFocusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
            // Autofocus mode is supported
            return true;
        }

        //When using API Level 9 or higher, use the Camera.getCameraInfo() to determine if a camera is on the front or back of the device, and the orientation of the image
        /*
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        Camera.getCameraInfo(0, cameraInfo);
        cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK; // 可以判断前后视
        cameraInfo.orientation == 270; // 判断图片的orientation. 有时还需要配合 setDisplayOrientation(90) 方法, 图片显示才会正确
        */

        return false;
    }

    /**
     * Using camera features. eg: auto focus mode
     */
    private void useCameraFeatures() {
        if (!checkCameraFeature()) {
            Log.d(TAG, "camera 不支持这个功能");
            return;
        }

        // get Camera parameters
        Camera.Parameters params = mCamera.getParameters();

        // set the focus mode
        // Applications should call {@link autoFocus(AutoFocusCallback)} to start the focus in this mode.
        params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);

        // set Camera parameters
        mCamera.setParameters(params);
    }


    /**
     * 使用 测光和聚焦区域 feature
     * 但是我发现在小米4上会报错
     */
    private void meteringAndFocusAreas() {
        // set Camera parameters
        Camera.Parameters params = mCamera.getParameters();

        if (params.getMaxNumMeteringAreas() > 0) { // check that metering areas are supported
            List<Camera.Area> meteringAreas = new ArrayList<Camera.Area>();

            Rect areaRect1 = new Rect(-100, -100, 100, 100);    // specify an area in center of image
            meteringAreas.add(new Camera.Area(areaRect1, 600)); // set weight to 60%
            Rect areaRect2 = new Rect(800, -1000, 1000, -800);  // specify an area in upper right of image
            meteringAreas.add(new Camera.Area(areaRect2, 400)); // set weight to 40%
            params.setMeteringAreas(meteringAreas); // 设置测光区域
        }

        if (params.getMaxNumFocusAreas() > 0) { // check that focus areas are supported
            List<Camera.Area> focusAreas = new ArrayList<Camera.Area>();

            Rect areaRect1 = new Rect(-100, -100, 100, 100);    // specify an area in center of image
            focusAreas.add(new Camera.Area(areaRect1, 600)); // set weight to 60%
            Rect areaRect2 = new Rect(800, -1000, 1000, -800);  // specify an area in upper right of image
            focusAreas.add(new Camera.Area(areaRect2, 400)); // set weight to 40%
            params.setFocusAreas(focusAreas); // 设置聚焦区域
        }

        mCamera.setParameters(params);
    }


    /**
     * 方向变化监听器，监听传感器方向的改变
     * 注意，监听的开启的关闭，是由该类的父类的 enable() 和 disable() 方法实现的。所以可以在 surfaceCreated 和 surfaceDestroyed 时启动和关闭监听器
     * <p>
     * int orientation = orientationDetector.getOrientation();
     * Log.d(TAG, "doTakePicture --> orientation: " + orientation);
     * Camera.Parameters cameraParameter = mCamera.getParameters();
     * cameraParameter.setRotation(90);
     * if ((orientation >= 45) && (orientation < 135)) {
     * cameraParameter.setRotation(180);
     * }
     * if ((orientation >= 135) && (orientation < 225)) {
     * cameraParameter.setRotation(270);
     * }
     * if ((orientation >= 225) && (orientation < 315)) {
     * cameraParameter.setRotation(0);
     * }
     * mCamera.setParameters(cameraParameter);
     */
    public class MyOrientationDetector extends OrientationEventListener {

        private final String TAG = MyOrientationDetector.class.getSimpleName();

        private int mOrientation;

        public MyOrientationDetector(Context context) {
            super(context);
        }

        public MyOrientationDetector(Context context, int rate) {
            super(context, rate);
        }

        /**
         * orientation 的范围是[0, 359], 当手机水平放置在桌面时, 该orientation的值为-1,即ORIENTATION_UNKNOWN
         *
         * @param orientation
         */
        @Override
        public void onOrientationChanged(int orientation) {
            this.mOrientation = orientation;
            Log.d(TAG, "当前的传感器方向为" + orientation);
        }

        public int getOrientation() {
            return mOrientation;
        }
    }


    public class MyFaceDetectionListener implements Camera.FaceDetectionListener {

        @Override
        public void onFaceDetection(Camera.Face[] faces, Camera camera) {
            if (faces.length > 0) {
                Log.d("FaceDetection", "face detected: " + faces.length +
                        " Face 1 Location X: " + faces[0].rect.centerX() +
                        "Y: " + faces[0].rect.centerY());
            }
        }
    }
}