package com.ronda.camerademo;

import android.os.Environment;
import android.os.StatFs;
import android.widget.Toast;

import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() throws Exception {
        assertEquals(4, 2 + 2);
    }

    private void delete(){
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
            File path = Environment.getExternalStorageDirectory();
            StatFs statFs = new StatFs(path.getPath());
            long blockSize = statFs.getBlockSize(); // 获取block的SIZE
            long tatalBlock = statFs.getBlockCount(); // 获取block数量
            long availableBlock = statFs.getAvailableBlocks(); // 获取已使用的block数量
            long sd = availableBlock * blockSize /1024 /1024; // 获取当前可用内容容量, 单位: MB
            if (sd < 300){
                String filePath = Environment.getExternalStorageDirectory().getAbsolutePath()+"/video/";
                File file = new File(filePath);
                if (!file.exists()){
                    file.mkdirs();
                }


                File[] files = file.listFiles();
                if (files.length > 0){
                    String[] childFileName = file.list();
                    String deleteFileName = filePath + childFileName[0];
                    File file1 = new File(deleteFileName);
                    file1.delete();
                }
            }else if(Environment.getExternalStorageState().equals(Environment.MEDIA_REMOVED)){
//                Toast.makeText(this, "请插入TF卡", Toast.LENGTH_SHORT).show();
            }
        }
    }






}