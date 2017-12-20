package com.ronda.camerademo.utils;



import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by ronda on 17-8-24/24.
 */

public class MusicUtil {
    private static SoundPool mSoundPool;
    private static Map<Integer, Integer> mMap = new HashMap<>();

    static {
        mSoundPool = new SoundPool(5, AudioManager.STREAM_MUSIC, 0);  //Android 5.0 Lollipop及以后使用Builder来创建 SoundPool

    }

    public static void playMusic(Context c, final int resourceId) {
        AudioManager manager = (AudioManager) c.getSystemService(Context.AUDIO_SERVICE);
//        manager.adjustStreamVolume (AudioManager.STREAM_MUSIC, AudioManager.ADJUST_SAME, AudioManager.FLAG_SHOW_UI);
//        float streamVolumeCurrent = manager.getStreamVolume(AudioManager.STREAM_MUSIC);
//        float streamVolumeMax = manager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
//        final float volume = streamVolumeCurrent / streamVolumeMax;

        Integer integer = mMap.get(resourceId);
        if (null == integer){
            mMap.put(resourceId, mSoundPool.load(c, resourceId, 1));
        }
        else {// 若之前有load()过，则直接play(), 否则就load(), 当load() 完成后，就会触发下面的 setOnLoadCompleteListener 监听器
            mSoundPool.play(mMap.get(resourceId), 1f, 1f, 1, 0, 1f);
        }

        mSoundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
            @Override
            public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
                mSoundPool.play(mMap.get(resourceId), 1f, 1f, 1, 0, 1f);
            }
        });
    }


    public static void playWithMediaPlayer(final Context context, final int resid){

        new Thread(){
            @Override
            public void run() {
                try{
                    MediaPlayer mp = null;
                    mp = MediaPlayer.create(context, resid);//重新设置要播放的音频
                    mp.setAudioStreamType(AudioManager.STREAM_MUSIC);//AudioManager.STREAM_MUSIC
                    //requestAudioFocus(OnAudioFocusChangeListener l, int streamType, int durationHint)；
                    mp.start();//开始播放
                }catch(Exception e){
                    e.printStackTrace();//输出异常信息
                }
            }
        }.start();

    }


    public static void  palyStatic(Context context, String fileName) {
        InputStream mFile = null;
        AudioTrack mTrack = null;
        try {
            mFile = context.getAssets().open(fileName);
        } catch (Exception e) {
        }
        byte[] mBuffer = null;
        short mLen = 0;
        if (mTrack == null && mFile != null) {
            try {
                readHeader(mFile);
                mBuffer = new byte[1024 * 1024];
                mLen = (short) mFile.read(mBuffer);
                // AudioTrack.MODE_STREAM(不能重复播放) ;AudioTrack.MODE_STATIC(可以重复播放)
                mTrack = new AudioTrack(AudioManager.STREAM_MUSIC
                        , 8000,  //每秒8K个点(采样率)
                        AudioFormat.CHANNEL_OUT_STEREO, //双声道
                        AudioFormat.ENCODING_PCM_16BIT, //一个采样点16比特-2个字节(设置音频数据块是8位还是16位-采样精度)
                        mLen,
                        AudioTrack.MODE_STREAM);
            } catch (Exception e) {
            }
        }
        if (mTrack != null) {
            short written = (short) mTrack.write(mBuffer, 0, mLen);
            mTrack.play();
        }
    }

    /**
     * -- 获取音频wav文件头信息
     * @param wavStream
     * @throws IOException
     */
    public static void readHeader(InputStream wavStream) throws IOException {

        ByteBuffer buffer = ByteBuffer.allocate(44);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        wavStream.read(buffer.array(), buffer.arrayOffset(), buffer.capacity());
        buffer.rewind();
        buffer.position(buffer.position() + 20);
        short format = buffer.getShort();  // --格式
        short channels = buffer.getShort();  // --声道
       int  rate = buffer.getInt(); // --采样率
        buffer.position(buffer.position() + 6);
        short bits = buffer.getShort(); // --采样精度
        while (buffer.getInt() != 0x61746164) { // "data" marker
            int size = buffer.getInt();
            wavStream.skip(size);
            buffer.rewind();
            wavStream.read(buffer.array(), buffer.arrayOffset(), 8);
            buffer.rewind();
        }
        int dataSize = buffer.getInt();  // -- 数据长度
        Log.d("TAG", "readHeader: format=" + format + "--channels=" + channels + "--rate=" + rate + "--bits=" + bits + "--dataSize=" + dataSize);
    }
}
