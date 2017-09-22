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

public class FileAVC1Thread extends Thread {

    private final static String MIME_TYPE = "video/avc";
    private String path;
    private static final String TAG = "FileAVC1Thread";
    private MediaExtractor extractor;
    private MediaCodec mCodec;
    private SurfaceHolder holder;
    public int state = 1;

    public FileAVC1Thread(SurfaceHolder holder,String path) {
        this.holder = holder;
        this.path = path;
    }

    @Override
    public void run() {
        try {
            extractor = new MediaExtractor();
            extractor.setDataSource(path);
            for (int i = 0; i < extractor.getTrackCount(); i++) {
                MediaFormat format = extractor.getTrackFormat(i);
                String mime = format.getString(MediaFormat.KEY_MIME);
                if (mime.startsWith("video/")) {
                    extractor.selectTrack(i);
                    mCodec = MediaCodec.createDecoderByType(mime);
                    mCodec.configure(format, holder.getSurface(), null, 0);
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (mCodec == null) {
            Log.e("DecodeActivity", "Can't find video info!");
            return;
        }

        mCodec.start();

        ByteBuffer[] inputBuffers = mCodec.getInputBuffers();
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        boolean isFinish = false;
        long startMs = System.currentTimeMillis();

        while (!isFinish) {
            synchronized (this){
                if (state == 0){
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            int inIndex = mCodec.dequeueInputBuffer(10000);
            if (inIndex >= 0) {
                ByteBuffer buffer = inputBuffers[inIndex];
                int sampleSize = extractor.readSampleData(buffer, 0);
                if (sampleSize < 0) {
                    Log.d("DecodeActivity", "InputBuffer BUFFER_FLAG_END_OF_STREAM");
                    mCodec.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    isFinish = true;
                } else {
                    mCodec.queueInputBuffer(inIndex, 0, sampleSize, extractor.getSampleTime(), 0);
                    extractor.advance();
                }
            }
            int outIndex = mCodec.dequeueOutputBuffer(info, 10000);
            if (outIndex >= 0) {
                while (info.presentationTimeUs / 1000 > System.currentTimeMillis() - startMs) {
                    try {
                        sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        break;
                    }
                }
                mCodec.releaseOutputBuffer(outIndex, true);
            }
            // All decoded frames have been rendered, we can stop playing now
            if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                Log.d("DecodeActivity", "OutputBuffer BUFFER_FLAG_END_OF_STREAM");
                break;
            }
        }
        stopCodec();
    }

    public void stopCodec() {
        try {
            mCodec.stop();
            mCodec.release();
            extractor.release();
            mCodec = null;
        } catch (Exception e) {
            e.printStackTrace();
            mCodec = null;
        }
    }

    public void startPlayer(){
        state = 1;
        notify();
    }

    public void pausePlayer(){
        state = 0;
    }
}