package com.ronda.camerademo.view;

/**
 * Created by Ronda on 2017/12/13.
 */

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
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
 * 对于大多数Camera的应用, 方向好像是固定为 portrait 或 landscape, 是不支持随着屏幕的旋转而旋转的
 * <p>
 * Building a camera app  步骤
 * 一. Capturing pictures
 * 1. Detect and Access Camera
 * 2. Create a Preview Class (extends SurfaceView and implements the SurfaceHolder interface)
 * 3. Build a Preview Layout
 * 4. Setup Listeners for Capture
 * 5. Capture and Save Files
 * 6. Release the Camera
 * <p>
 * 二. Capturing video
 * Unlike taking pictures with a device camera, capturing video requires a very particular call order.
 * <p>
 * Starting with Android 4.0 (API level 14), the Camera.lock() and Camera.unlock() calls are managed for you automatically.
 * <p>
 * 1. Open Camera
 * 2. Connect Preview
 * 3. Start Preview
 * 4. Start Recording Video
 * - a. Unlock the Camera  - Unlock the camera for use by MediaRecorder by calling Camera.unlock().
 * - b. Configure MediaRecorder ( following are MediaRecorder methods)
 * -- 1. setCamera()
 * -- 2. setAudioSource(MediaRecorder.AudioSource.CAMCORDER)
 * -- 3. setVideoSource(MediaRecorder.VideoSource.CAMERA)
 * -- 4. Set the video output format and encoding. For Android 2.2 (API Level 8) and higher, use the MediaRecorder.setProfile method,
 * --    and get a profile instance using CamcorderProfile.get(). For versions of Android prior to 2.2, you must set the video output format and encoding parameters:
 * ---- 1) setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
 * ---- 2) setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT)
 * ---- 3) setVideoEncoder(MediaRecorder.VideoEncoder.DEFAULT)
 * -- 5. setOutputFile()
 * -- 6. setPreviewDisplay()
 * - c. Prepare MediaRecorder
 * - d. Start MediaRecorder
 * 5. Stop Recording Video
 * - a. Stop MediaRecorder
 * - b. Reset MediaRecorder
 * - c. Release MediaRecorder
 * - d. Lock the Camera - Lock the camera so that future MediaRecorder sessions can use it by calling Camera.lock().
 * -    Starting with Android 4.0 (API level 14), this call is not required unless the MediaRecorder.prepare() call fails.
 * 6. Stop the Preview - Camera.stopPreview().
 * 7. Release Camera - Camera.release().
 * <p>
 * <p>
 * 注意：它可以使用MediaRecorder 而不用首先创建一个相机预览，并且跳过这一过程中前几个步骤。然而，由于在开始录制之前用户通常更愿意看到一个预览，这个过程不再这里讨论。
 * 提示：如果您的应用程序通常用于录制视频，以开始预览之前，设置setRecordingHint(boolean)为true 。此设置可以帮助减少需要录制的时间。
 * <p>
 * <p>
 * Camera features:
 * <p>
 * Most camera features can be accessed(接近,获取) and set using the through Camera.Parameters object.
 * However, there are several important features that require more than simple settings in Camera.Parameters. These features are covered in the following sections:
 * * Metering and focus areas (测光和焦点区域)
 * * Face detection (面部检测)
 * * Time lapse video (时间推移视频, 即延迟摄影)
 * <p>
 * 所以, Camera features 分为两类:
 * simple settings (using Camera.Parameters object.)
 * complex settings (上面显示的三个特点)
 * <p>
 * Camera.Parameters对象提供了getSupported ...（） ， is...Supported()或GetMax的...（）方法，以确定是否（以及多大程度上）支持此功能。
 * <p>
 * <p>
 * // get Camera parameters
 * Camera.Parameters params= mCamera.getParameters();
 * // set the focus mode
 * params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
 * // set Camera parameters
 * mCamera.setParameters(params);
 * <p>
 * 像上面这种形式适用于几乎所有的拍照功能，而且在你获得Camera对象的实例以后大多数参数可以在任何时候改变(注意: 当给params设置好了参数后,一定不要忘了再把params设置给mCamera, 否则不会起效)。
 * 但是注意: 一些摄像功能不能随意更改。尤其是，改变相机预览的大小和方向，您需要先停止预览，再改变预览大小，然后重新启动预览。从Android 4.0（API等级14）开始，预览方向的改变不再需要重新启动预览。
 * <p>
 * 但是对于Metering and focus areas, Face detection, Time lapse video 这些复杂功能的实现, 就需要更多更多的代码, 不能是这么简单的了
 * <p>
 * Camera.Area对象包含两个数据参数：一个 Rect是为指定摄像机视图之内的区域和一个权重值.
 * 在Camera.Area对象中的Rect区域描述一个矩形被映射到一个2000×2000单元格上, 即preview区域, 而且是以中心点为坐标原点的, 向右为x正方向, 向下为y正方向.
 * 这个2000×2000单元格的坐标系统是固定不变的,不会随着图像的缩放级别或预览方向的变化而变化
 * <p>
 * 在您的相机应用程序使用脸部识别功能需要几个基本步骤：(MI4并不好使)
 * 1. 检查设备支持人脸检测（Check that face detection is supported on the device）
 * 2. 创建人脸检测监听器（Create a face detection listener
 * 3. 将人脸检测监听加入到你的Camera对象（Add the face detection listener to your camera object
 * 4. 预览后启动人脸检测（每次预览都要重新启动）（Start face detection after preview (and after every preview restart))
 * While the face detection feature is running, setWhiteBalance(String), setFocusAreas(List<Camera.Area>) and setMeteringAreas(List<Camera.Area>) have no effect.
 * <p>
 * <p>
 * Time lapse video 延迟摄影(其实就是每秒只抓取几帧而已)
 * // Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
 * mMediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_TIME_LAPSE_HIGH));
 * ...
 * // Step 5.5: Set the video capture rate to a low number
 * mMediaRecorder.setCaptureRate(0.1); // capture a frame every 10 seconds
 * <p>
 * Camera 一旦调用了release()方法后, 就不能再次使用了, 除非又重新调用 Camera.Open() 方法
 * <p>
 * =====================================================================================================
 * <p>
 * 预览、拍照、自动聚焦、触摸聚焦、连续拍照、照片存储, 预览拉伸问题解决
 * // width, height: [(240, 160),(320, 240),(480, 320),...,(1920, 1080)]
 * List<Camera.Size> supportedPreviewSizes = mCamera.getParameters().getSupportedPreviewSizes();
 * parameters.setPreviewSize(optimalSize.width, optimalSize.height); // 设置预览界面的大小. 经测试, 当SurfaceView大小一样的情况下, PreviewSize 越大预览界面越清晰
 */

public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {

    private static final String TAG = CameraPreview.class.getSimpleName();

    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;

    private SurfaceHolder mHolder;
    private Camera mCamera;
    private int mCameraId;
    private Context mContext;
    private MediaRecorder mMediaRecorder;

    public CameraPreview(Context context, int cameraId) {
        super(context);
        this.mContext = context;
        this.mCameraId = cameraId;

        mCamera = getCameraInstance(cameraId);

        isSupportAutoFocus = context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_AUTOFOCUS);

        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = getHolder();
        mHolder.addCallback(this);

        //兼容android 3.0以下的API
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            // deprecated setting, but required on Android versions prior to 3.0
            mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

        Log.d(TAG, "surfaceCreated");

        // The Surface has been created, now tell the camera where to draw the preview.
        try {
            //Camera.Parameters parameters = mCamera.getParameters();
            holder.setKeepScreenOn(true);
            if (mCamera == null) {
                mCamera = getCameraInstance(mCameraId);
            }

            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();

            //startFaceDetection();// start face detection feature
        } catch (IOException e) {
            Log.d(TAG, "Error setting camera preview: " + e.getMessage());
            releaseCamera(); // 若预览失败,则释放Camera
        }
    }

    /**
     * 当要改变sufaceView 的size 或者 rotation 或者 Image or Video的格式时, 必须要先停止预览, 设置好这些参数后, 再启动预览
     */
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        Log.d(TAG, "surfaceChanged --> format: " + format + ", w: " + w + ", h:" + h);

        // If your preview can change or rotate, take care of those events here.
        // Make sure to stop the preview before resizing or reformatting it.

        if (mHolder.getSurface() == null) {
            // preview surface does not exist
            Log.d(TAG, "mHolder.getSurface() == null");
            return;
        }

        // stop preview before making changes
        try {
            mCamera.stopPreview();
        } catch (Exception e) {
            // ignore: tried to stop a non-existent preview
            Log.d(TAG, "Error stopping camera preview: " + e.getMessage());
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
        try {
            mCamera.setPreviewDisplay(mHolder); // 这句一般可以不要, 因为 mCamera 和 mHolder 一般不会改变

            // Important: Call startPreview() to start updating the preview surface.
            // Preview must be started before you can take a picture.
            mCamera.startPreview();

            //startFaceDetection();// re-start face detection feature

        } catch (Exception e) {
            // ignore: tried to stop a non-existent preview
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
        }


    }


    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d(TAG, "surfaceDestroyed");

        // Surface will be destroyed when we return, so stop the preview.
        stopPreviewAndReleaseCamera();
    }


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

            try {
                mCamera.setPreviewDisplay(mHolder);
            } catch (IOException e) {
                e.printStackTrace();
            }

            // Important: Call startPreview() to start updating the preview
            // surface. Preview must be started before you can take a picture.
            mCamera.startPreview();
        }
    }


    /**
     * 设置拍照的图片的大小和质量. 在surfaceCreated 和 surfaceChanged 时调用
     */
    private void setCameraPictureSize() {
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
    private void setOptimalPreviewSize(int w, int h) {

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

        this.setLayoutParams(new FrameLayout.LayoutParams(optimalSize.width, optimalSize.height)); // 当size改变的时候, 会重新回调 surfaceChanage() 方法

        Camera.Size previewSize = mCamera.getParameters().getPreviewSize();
        Log.e(TAG, "当前 previewSize 预览界面的大小: width: " + previewSize.width + ", height: " + previewSize.height);//横竖屏均为:width: 640, height: 480, 感觉一直都不变
    }

    /**
     * 给Camera设置正确的预览方向和图片保存时的Camera方向
     *
     * @param camera
     */
    private void setCorrectCameraOrientation(Camera camera) {

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
    private int getBackFacingCameraId() {
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
    private Camera.ShutterCallback shutterCallback = new Camera.ShutterCallback() {
        @Override
        public void onShutter() {
            // 可以在这里播放一个拍照的音频
            MusicUtil.playMusic(getContext(), R.raw.photo);
        }
    };

    private Camera.PictureCallback rawPictureCallback = new Camera.PictureCallback() {

        /**
         * @param data 这个data是未经过压缩的原始的图片数据, 比mPictureCallback回调中的data要大
         * @param camera
         */
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {

        }
    };
    private Camera.PictureCallback mPictureCallback = new Camera.PictureCallback() {

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

            if (pictureFile == null) {
                Toast.makeText(mContext, "请插入存储卡！", Toast.LENGTH_SHORT).show();
                return null;
            }
            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(params[0]);
                fos.flush();
                fos.close();
            } catch (FileNotFoundException e) {
                Log.d(TAG, "File not found: " + e.getMessage());
            } catch (IOException e) {
                Log.d(TAG, "Error accessing file: " + e.getMessage());
            }
            return null;
        }
    }

    /**
     * Create a File for saving an image or video
     */
    private File getOutputMediaFile(int type) {
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
    private void stopPreviewAndReleaseCamera() {

        stopPreview();

        // Important: Call release() to release the camera for use by other
        // applications. Applications should release the camera immediately
        // during onPause() and re-open() it during onResume()).
        releaseCamera();

    }

    private void stopPreview() {
        if (mCamera != null) {
            // Call stopPreview() to stop updating the preview surface.
            mCamera.stopPreview();
        }
    }

    /**
     * 释放Camera
     */
    private void releaseCamera() {
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
        mMediaRecorder.setPreviewDisplay(getHolder().getSurface());

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