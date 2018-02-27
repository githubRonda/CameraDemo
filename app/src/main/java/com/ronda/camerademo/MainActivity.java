package com.ronda.camerademo;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.ronda.camerademo.view.CameraTextureView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        findViewById(R.id.btn_exist_camera).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                startActivity(new Intent(getApplicationContext(), IntentPhotoActivity.class));
            }
        });


        findViewById(R.id.btn_build_camera1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                startActivity(new Intent(getApplicationContext(), CameraSurfaceViewActivity.class));
            }
        });

        findViewById(R.id.btn_build_camera2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                startActivity(new Intent(getApplicationContext(), CameraGLSurfaceViewActivity.class));
            }
        });

        findViewById(R.id.btn_build_camera3).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                startActivity(new Intent(getApplicationContext(), CameraTextureViewActivity.class));
            }
        });

        findViewById(R.id.btn_build_camera4).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                startActivity(new Intent(getApplicationContext(), CameraViewActivity.class));
            }
        });
    }

}
