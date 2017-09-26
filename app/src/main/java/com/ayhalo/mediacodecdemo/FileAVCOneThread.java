package com.ayhalo.mediacodecdemo;


import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;
import android.view.SurfaceHolder;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * MediaCodecDemo
 * Created by Halo on 2017/9/20.
 */

public class FileAVCOneThread extends ReadThread {

    private final static String MIME_TYPE = "video/avc";
    private String path;
    private static final String TAG = "FileAVCOneThread";
    private MediaExtractor extractor;
    private MediaCodec mCodec;
    private SurfaceHolder holder;
    private boolean isFinish = false;
    private boolean isPause = false;
    private boolean isError = false;

    public FileAVCOneThread(SurfaceHolder holder, String path) {
        this.holder = holder;
        this.path = path;
    }

    @Override
    public void run() {
        initMediaCodec();
        long startMs = System.currentTimeMillis();
        while (!isFinish) {
            synchronized (this) {
                if (isPause) {
                    try {
                        Log.d(TAG, "run: wait!");
                        wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

            ByteBuffer[] inputBuffers = mCodec.getInputBuffers();
            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            int inIndex = mCodec.dequeueInputBuffer(10000);
            if (inIndex >= 0) {
                ByteBuffer buffer = inputBuffers[inIndex];
                int sampleSize = extractor.readSampleData(buffer, 0);
                if (sampleSize < 0) {
                    Log.d(TAG, "InputBuffer BUFFER_FLAG_END_OF_STREAM");
                    mCodec.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    isFinish = true;
                    break;
                } else {
                    mCodec.queueInputBuffer(inIndex, 0, sampleSize, extractor.getSampleTime(), 0);
                    extractor.advance();
                }
            }

            int outIndex = mCodec.dequeueOutputBuffer(info, 10000);
            if (outIndex >= 0) {
                while (info.presentationTimeUs / 1000 > System.currentTimeMillis() - startMs) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        break;
                    }
                }
                try {
                    mCodec.releaseOutputBuffer(outIndex, true);
                } catch (Exception e) {
                    Log.d(TAG, "run: " + e);
                }
            }
        }
        stopCodec();
    }

    private void initMediaCodec() {
        try {
            extractor = new MediaExtractor();
            extractor.setDataSource(path);
            for (int i = 0; i < extractor.getTrackCount(); i++) {
                MediaFormat format = extractor.getTrackFormat(i);
                Log.d(TAG, "initMediaCodec: "+format);
                String mime = format.getString(MediaFormat.KEY_MIME);
                Log.d(TAG, "run: " + mime);
                if (mime.startsWith("video/")) {
                    extractor.selectTrack(i);
                    mCodec = MediaCodec.createDecoderByType(mime);
                    mCodec.configure(format, holder.getSurface(), null, 0);
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "initMediaCodec: "+e);
            isError = true;
        }
        if (mCodec == null) {
            isFinish = true;
            return;
        }
        mCodec.start();
    }

    @Override
    public void stopCodec() {
        isFinish = true;
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (mCodec != null) {
            try {
                mCodec.stop();
                mCodec.release();
                extractor.release();
                mCodec = null;
            } catch (Exception e) {
                Log.e(TAG, "stopCodec: " + e);
            }
        }
    }

    @Override
    public int getPlayerState() {
        if (isError){
            return PLAYER_STATE_ERROR;
        }else if (isFinish){
            return PLAYER_STATE_FINISHED;
        }else if (isPause){
            return PLAYER_STATE_PAUSED;
        }else {
            return PLAYER_STATE_PLAYING;
        }
    }

    @Override
    public void startPlayer() {
        synchronized (this){
            if (isPause){
                isPause = false;
                notify();
            }
        }
    }

    @Override
    public void pausePlayer() {
        isPause = true;
    }

}
