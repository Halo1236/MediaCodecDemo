package com.ayhalo.mediacodecdemo;


import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Environment;
import android.util.Log;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * MediaCodecDemo
 * Created by Halo on 2017/9/20.
 */

public class ReadData extends Thread {

    private final static String MIME_TYPE = "video/avc";
    private String path;
    private static final String TAG = "FileAVCOne";
    private MediaExtractor mediaExtractor;
    private MediaFormat format;
    private boolean isFinish = false;
    private static final int FRAME_MAX_LEN = 200 * 1024;
    private ByteBuffer buffer;
    private LinkedBlockingQueue<ByteBuffer[]> videoData = new LinkedBlockingQueue<>();
    private LinkedBlockingQueue<ByteBuffer[]> audioData = new LinkedBlockingQueue<>();

    public ReadData(String path) {
        this.path = path;
        initMediaCodec();
    }

    public void run() {
        while (!isFinish) {
            buffer = ByteBuffer.allocate(FRAME_MAX_LEN);
            int sampleSize = 0;
            sampleSize = mediaExtractor.readSampleData(buffer, 0);

            if (sampleSize < 0) {
                Log.d(TAG, "InputBuffer BUFFER_FLAG_END_OF_STREAM");
                isFinish = true;
                break;
            } else {
                FileOutputStream fos = null;
                Log.d(TAG, "run: "+sampleSize+" "+buffer.array().length+" "+mediaExtractor.getSampleTime());
                try {
                    fos = new FileOutputStream(Environment.getExternalStorageDirectory().getAbsoluteFile()+"/1.h264",true);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                try {
                    ObjectOutputStream obj = new ObjectOutputStream(fos);
                    obj.write(buffer.array());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                mediaExtractor.advance();
            }
        }
    }

    private void initMediaCodec() {
        try {
            mediaExtractor = new MediaExtractor();
            mediaExtractor.setDataSource(path);
            for (int i = 0; i < mediaExtractor.getTrackCount(); i++) {
                format = mediaExtractor.getTrackFormat(i);
                Log.d(TAG, "initMediaCodec: " + format);
                String mime = format.getString(MediaFormat.KEY_MIME);
                Log.d(TAG, "run: " + mime);
                if (mime.startsWith("video/")) {
                    mediaExtractor.selectTrack(i);
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "initMediaCodec: " + e);
        }
    }

}
