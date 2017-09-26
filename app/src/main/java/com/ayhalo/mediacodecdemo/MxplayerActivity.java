package com.ayhalo.mediacodecdemo;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
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
        Intent mIntent = getIntent();
        videoPath = mIntent.getStringExtra("path");
        video = (SurfaceView) findViewById(R.id.video_view);
        imageView = (ImageView) findViewById(R.id.app_video_play);
        textView = (TextView)findViewById(R.id.app_video_status_text);
        surfaceHolder = video.getHolder();
        surfaceHolder.addCallback(this);
        imageView.setOnClickListener(this);
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
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
        mThread.stopCodec();
    }

    @Override
    public void onBackPressed() {
        mThread.stopCodec();
        super.onBackPressed();
    }

    public void initPlayer() {
        if (videoPath.endsWith(".h264")) {
            mThread = new FileStreamThread(surfaceHolder, videoPath);
        } else {
            mThread = new FileAVCOneThread(surfaceHolder, videoPath);
        }
        mThread.start();
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

    private AudioManager.OnAudioFocusChangeListener mAudioFocusChange = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int focusChange) {
            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_LOSS:
                    //长时间丢失焦点
                    Log.d(TAG, "AUDIOFOCUS_LOSS");
                    //stop();
                    //释放焦点
                    mAudioManager.abandonAudioFocus(mAudioFocusChange);
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                    //短暂性失去焦点
                    Log.d(TAG, "AUDIOFOCUS_LOSS_TRANSIENT");
                    //stop();
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    //短暂性丢失焦点并作降音处理
                    Log.d(TAG, "AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK");
                    //mMediaPlayer.setVolume(0.1f,0.1f);
                    break;
                case AudioManager.AUDIOFOCUS_GAIN:
                    //重新获得焦点
                    Log.d(TAG, "AUDIOFOCUS_GAIN");
                    //mMediaPlayer.setVolume(1.0f,1.0f);
                    //start();
                    break;
            }
        }
    };
}
