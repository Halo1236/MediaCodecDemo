package com.ayhalo.mediacodecdemo;

import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

public class MxplayerActivity extends AppCompatActivity implements SurfaceHolder.Callback, View.OnClickListener {

    private static final String TAG = "Mxplayer";
    private String videoPath;
    private SurfaceView video;
    private SurfaceHolder surfaceHolder;
    private AudioManager mAudioManager;
    //读取文件解码线程
    private ReadThread mThread = null;
    private ImageView imageView;
    private TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mx_player);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        Intent mIntent = getIntent();
        videoPath = mIntent.getStringExtra("path");
        video = (SurfaceView) findViewById(R.id.video_view);
        imageView = (ImageView) findViewById(R.id.app_video_play);
        textView = (TextView)findViewById(R.id.app_video_status_text);
        surfaceHolder = video.getHolder();
        surfaceHolder.addCallback(this);
        imageView.setOnClickListener(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        initPlayer();
        imageView.setImageResource(R.drawable.ic_stop_white_24dp);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    @Override
    public void onBackPressed() {
        mThread.stopCodec();
        super.onBackPressed();
        finish();
    }

    public void initPlayer() {
        if (videoPath.endsWith(".h264")) {
            mThread = new FileStream(this,surfaceHolder, videoPath);
        } else if (videoPath.endsWith(".mp4")){
            mThread = new FileAVCOne(surfaceHolder, videoPath);
            //ReadData read = new ReadData(videoPath);
            //read.start();
        }else if (videoPath.endsWith(".aac")){
            mThread = new AudioThread(this,videoPath);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.app_video_play:
                switch (mThread.getPlayerState()){
                    case ReadThread.PLAYER_STATE_PLAYING:
                        mThread.pausePlayer();
                        imageView.setImageResource(R.drawable.ic_play_arrow_white_24dp);
                        break;
                    case ReadThread.PLAYER_STATE_FINISHED:
                        initPlayer();
                        imageView.setImageResource(R.drawable.ic_stop_white_24dp);
                        break;
                    case ReadThread.PLAYER_STATE_PAUSED:
                        mThread.startPlayer();
                        imageView.setImageResource(R.drawable.ic_stop_white_24dp);
                        break;
                    case ReadThread.PLAYER_STATE_ERROR:
                        textView.setText("解码器被其他应用占用!");
                        break;
                }
                Log.d(TAG, "onClick: "+mThread.getPlayerState());
                break;
        }
    }
}
