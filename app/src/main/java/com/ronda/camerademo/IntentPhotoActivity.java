package com.ronda.camerademo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.VideoView;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * 以Intent方式启动系统Camera进行拍照和录像
 */
public class IntentPhotoActivity extends Activity implements View.OnClickListener {


    private static final String TAG = IntentPhotoActivity.class.getSimpleName();


    /**
     * onSaveInstanceState() 和 onRestoreInstanceState() 中需要保持和恢复的数据
     */
    private static final String BITMAP_STORAGE_KEY = "viewbitmap";
    private static final String IMAGEVIEW_VISIBILITY_STORAGE_KEY = "imageviewvisibility";
    private static final String VIDEO_STORAGE_KEY = "viewvideo";
    private static final String VIDEOVIEW_VISIBILITY_STORAGE_KEY = "videoviewvisibility";

    static final int REQUEST_TAKE_PHOTO_S = 1;
    static final int REQUEST_TAKE_PHOTO_B = 2;
    static final int REQUEST_VIDEO_CAPTURE = 3;

    private ImageView mImageView;
    private Bitmap mImageBitmap; // 抓取Small Pic 时, 从onActivityForResult() 中获取的Bitmap

    private VideoView mVideoView;
    private Uri mVideoUri;

    private String mCurrentPhotoPath; // 保存大图片时的路径

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo);

        findViewById(R.id.btn_pic_small).setOnClickListener(this);
        findViewById(R.id.btn_pic_big).setOnClickListener(this);
        findViewById(R.id.btn_take_video).setOnClickListener(this);

        mImageView = (ImageView) findViewById(R.id.iv_show);
        mVideoView = (VideoView) findViewById(R.id.video_view);
    }


    // Some lifecycle callbacks so that the image can survive orientation change
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        Log.d(TAG, "onSaveInstanceState --> "+(mImageBitmap == null));
        outState.putParcelable(BITMAP_STORAGE_KEY, mImageBitmap);
        outState.putParcelable(VIDEO_STORAGE_KEY, mVideoUri);
        outState.putBoolean(IMAGEVIEW_VISIBILITY_STORAGE_KEY, (mImageBitmap != null));
        outState.putBoolean(VIDEOVIEW_VISIBILITY_STORAGE_KEY, (mVideoUri != null));
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        Log.d(TAG, "onRestoreInstanceState "+savedInstanceState.getBoolean(IMAGEVIEW_VISIBILITY_STORAGE_KEY));
        mImageBitmap = savedInstanceState.getParcelable(BITMAP_STORAGE_KEY);
        mVideoUri = savedInstanceState.getParcelable(VIDEO_STORAGE_KEY);
        mImageView.setImageBitmap(mImageBitmap);
        mImageView.setVisibility(savedInstanceState.getBoolean(IMAGEVIEW_VISIBILITY_STORAGE_KEY) ? View.VISIBLE : View.GONE);
        mVideoView.setVideoURI(mVideoUri);
        mVideoView.setVisibility(savedInstanceState.getBoolean(VIDEOVIEW_VISIBILITY_STORAGE_KEY) ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_pic_small:
                takeSmallPicture();
                break;
            case R.id.btn_pic_big:
                takeBigPicture();
                break;
            case R.id.btn_take_video:
                takeVideo();
                break;
        }
    }

    /**
     * 使用系统Camera App 拍一张小图片
     */
    private void takeSmallPicture() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO_S);
        }
    }

    /**
     * 使用系统Camera App 拍一张大图片(无压缩), 其实本质上就是
     */
    private void takeBigPicture() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
                ex.printStackTrace();
            }
            // Continue only if the File was successfully created
            /**
             * Note: We are using getUriForFile(Context, String, File) which returns a content:// URI.
             * For more recent apps targeting Android 7.0 (API level 24) and higher, passing a file:// URI across a package boundary(边界) causes
             * a FileUriExposedException. Therefore, we now present a more generic way of storing images using a FileProvider.
             *
             * 在之前的版本中,一直使用的是:Uri.fromFile(photoFile);返回一个Uri:  file:///storage/emulated/0/Android/data/com.ronda.camerademo/files/Pictures/JPEG_20171219_220551_-2060021981.jpg
             * 而使用下面的 FileProvider.getUriForFile() 返回的Uri为: content://com.example.android.fileprovider/my_images/JPEG_20171219_220551_-2060021981.jpg
             * 可以发现: 协议有 file 协议变成了 content 协议
             *
             * 如何理解 Uri? 就是用于标识资源的, 和 Url 类似. 但是 Url 更具体, 不仅可以唯一标识资源, 而且可以定位资源的位置. 所以 Uri 和 id 作用类似, 就相当于给某一个路径(或其他资源) 关联了一个标识符
             */
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this, "com.example.android.fileprovider", photoFile);// 第二个参数:authority是一个字符串, 没有什么实际意义,只要和Manifest中配置的provider一致即可.
                Log.w(TAG, "uri1: " + photoURI.toString()); // content://com.example.android.fileprovider/my_images/JPEG_20171219_220551_-2060021981.jpg
                Log.w(TAG, "uri2: " + Uri.fromFile(photoFile).toString());// file:///storage/emulated/0/Android/data/com.ronda.camerademo/files/Pictures/JPEG_20171219_220551_-2060021981.jpg

                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI); // 兼容api24及以上, 使用 content://URI
                //takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(file)); // api24之前的版本可以直接使用 file://URI,
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO_B);
            }
        }
    }

    /**
     * 启动系统 Camera 来录像
     */
    private void takeVideo() {
        Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        startActivityForResult(takeVideoIntent, REQUEST_VIDEO_CAPTURE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_TAKE_PHOTO_S:
                if (resultCode == RESULT_OK) {
                    handleSmallCameraPhoto(data);
                }
                break;
            case REQUEST_TAKE_PHOTO_B:
                if (resultCode == RESULT_OK) {
                    handleBigCameraPhoto();
                }
                break;
            case REQUEST_VIDEO_CAPTURE:
                if (resultCode == RESULT_OK) {
                    handleCameraVideo(data);
                }
                break;
        }
    }


    private void handleSmallCameraPhoto(Intent data) {
        Bundle extras = data.getExtras();
//        Bitmap imageBitmap = (Bitmap) extras.get("data");
        mImageBitmap = (Bitmap) extras.get("data");//data 是一个很小的图片数据, 通常用于获取一个icon
        mImageView.setImageBitmap(mImageBitmap);

        mImageView.setVisibility(View.VISIBLE);
        mVideoUri = null;
        mVideoView.setVisibility(View.GONE);
    }

    private void handleBigCameraPhoto() {
        if (mCurrentPhotoPath != null) {
            setPic();
            galleryAddPic();

            mCurrentPhotoPath = null;
            mImageView.setVisibility(View.VISIBLE);

            mVideoUri = null;
            mVideoView.setVisibility(View.GONE);
        }
    }

    private void handleCameraVideo(Intent intent) {
        mVideoUri = intent.getData();
        Log.d(TAG, "mVideoUri: " + mVideoUri);

        mVideoView.setVideoURI(mVideoUri);

        mVideoView.setVisibility(View.VISIBLE);
        mImageBitmap = null;
        mImageView.setVisibility(View.GONE);
    }


    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        //JPEG_20171219_215745_935801472.jpg --> 935801472 是 createTempFile() 方法中随机生成的
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        Log.d(TAG, "mCurrentPhotoPath: " + mCurrentPhotoPath);
        return image;
    }

    /**
     * 把图片文件的 Uri 添加到 system's Media Provider 中, 以便通过系统图库APP 可以直接查看到
     * 注意:(均是在华为mate7上测试的结果)
     * 1. 该 Uri 必须是图片文件的Uri, 不能是所在目录的 Uri. 而且同一个目录下的多个图片文件只有Uri添加到 Media Provider 中的才可以在图库中看到, 没有添加的是看不到的,即使是在同一个目录下
     * 2. 属于APP私有目录下的图片文件 (eg: /storage/emulated/0/Android/data/<package name>/ 或 /data/data/<package name>/)的Uri, 即使添加到 Media Provider 也是查看不到的
     * <p>
     * If you saved your photo to the directory provided by getExternalFilesDir(), the media scanner cannot access the files because they are private to your app.
     * <p>
     * The following method demonstrates(展示) how to invoke the system's media scanner to add your photo to the Media Provider's database,
     * making it available in the Android Gallery application and to other apps.
     */
    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(mCurrentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }


    /**
     * Decode a scaled image
     * <p>
     * Managing multiple full-sized images can be tricky with limited memory.
     * If you find your application running out of memory after displaying just a few images,
     * you can dramatically(戏剧地, 显著地) reduce the amount of dynamic(动态的) heap used by expanding the JPEG into
     * a memory array that's already scaled to match the size of the destination(目的地) view.
     * The following example method demonstrates this technique.
     * <p>
     * 由于拍出来的全尺寸的照片其实是比较大的, 又加上内存的限制, 所以比较容易内存溢出
     */
    private void setPic() {
        /* There isn't enough memory to open up more than a couple camera photos */
        /* So pre-scale the target bitmap into which the file is decoded */

        // Get the size of the ImageView
        int targetW = mImageView.getWidth();
        int targetH = mImageView.getHeight();

        // Get the size of the image
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        // Figure out which way needs to be reduced less
        int scaleFactor = 1;
        if ((targetW > 0) || (targetH > 0)) {
            scaleFactor = Math.min(photoW / targetW, photoH / targetH);
        }

        // Set bitmap options to scale the image to fill the ImageView
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

        // Decode the JPEG file into a Bitmap
        Bitmap bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);

        // Associate the Bitmap to the ImageView
        mImageView.setImageBitmap(bitmap);
        mVideoUri = null;
        mImageView.setVisibility(View.VISIBLE);
        mVideoView.setVisibility(View.INVISIBLE);
    }


    private void recordvideo() {
        Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        if (takeVideoIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takeVideoIntent, REQUEST_VIDEO_CAPTURE);
        }
    }


    public static boolean isIntentAvailable(Context context, String action) {
        final PackageManager packageManager = context.getPackageManager();
        final Intent intent = new Intent(action);
        List<ResolveInfo> list = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);

        return list.size() > 0;
    }

    /**
     * resolveActivity() returns the first activity component that can handle the intent.
     * <p>
     * 这两个isIntentAvailable()方法的作用是一样的.
     * 当我们调用 startActivity()/startActivityForResult()方法时, 最好先检查一下有没有能够处理该Intent的Activity, 避免程序崩溃
     */
    public boolean isIntentAvailable(Intent intent) {
        return intent.resolveActivity(getPackageManager()) != null;
    }
}
