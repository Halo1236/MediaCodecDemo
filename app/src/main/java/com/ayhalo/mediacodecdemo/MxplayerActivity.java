package com.ayhalo.mediacodecdemo;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;

public class MxplayerActivity extends AppCompatActivity implements SurfaceHolder.Callback, View.OnClickListener {

    public static final int H264_WITH_STARTCODE = 1;
    public static final int H264_WITHOUTH_STARTCODE = 2;
    private static final String TAG = "Mxplayer";
    private String videoPath;
    private SurfaceView video;
    private SurfaceHolder surfaceHolder;

    //读取文件解码线程
    private FileAVC1Thread thread;
    private ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mx_player);
        Intent mIntent = getIntent();
        videoPath = mIntent.getStringExtra("path");
        video = (SurfaceView) findViewById(R.id.video_view);
        imageView = (ImageView)findViewById(R.id.app_video_play);
        surfaceHolder = video.getHolder();
        surfaceHolder.addCallback(this);
        imageView.setOnClickListener(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (thread == null) {
            //解码线程第一次初始化
            thread = new FileAVC1Thread(holder,videoPath);
            thread.start();
            imageView.setImageResource(R.drawable.ic_stop_white_24dp);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    @Override
    public void onBackPressed() {
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.app_video_play:
                switch (thread.state){
                    case 1:
                        imageView.setImageResource(R.drawable.ic_stop_white_24dp);
                        thread.pausePlayer();
                        break;
                    case 0:
                        imageView.setImageResource(R.drawable.ic_play_arrow_white_24dp);
                        thread.startPlayer();
                        break;
                }
                break;
        }
    }
}
