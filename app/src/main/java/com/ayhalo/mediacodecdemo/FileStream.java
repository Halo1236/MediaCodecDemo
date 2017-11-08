package com.ayhalo.mediacodecdemo;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;
import android.view.SurfaceHolder;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * MediaCodecDemo
 * Created by Halo on 2017/9/21.
 */

public class FileStream extends ReadThread {
    private static final String TAG = "FileStream";

    //文件路径
    private String path;
    private FileInputStream fis = null;
    //文件读取完成标识
    private boolean isFinish = false;
    private boolean isPause = false;
    private boolean isError = false;

    //这个值用于找到第一个帧头后，继续寻找第二个帧头，如果解码失败可以尝试缩小这个值
    private int FRAME_MIN_LEN = 1024;
    //一般H264帧大小不超过200k,如果解码失败可以尝试增大这个值
    private static int FRAME_MAX_LEN = 300 * 1024;
    //根据帧率获取的解码每帧需要休眠的时间,根据实际帧率进行操作
    private int PRE_FRAME_TIME = 1000 / 25;

    private final static String MIME_TYPE = "video/avc"; // H.264 Advanced Video
    private int mCount = 0;
    private SurfaceHolder holder;
    private int width, height;
    private MediaCodec mCodec;
    private int FrameRate = 15;
    private MediaFormat mediaFormat;
    private Context context;

    public FileStream(Context context,SurfaceHolder holder, String path) {
        this.path = path;
        this.holder = holder;
        this.width = holder.getSurfaceFrame().width();
        this.height = holder.getSurfaceFrame().height();
        this.context = context;
        this.start();
    }

    @Override
    public void run() {
        File file = new File(path);
        //判断文件是否存在
        if (file.exists()) {
            try {
                fis = new FileInputStream(file);
                //保存完整数据帧
                byte[] frame = new byte[FRAME_MAX_LEN];
                //当前帧长度
                int frameLen = 0;
                //每次从文件读取的数据
                byte[] readData = new byte[100 * 1024];
                //开始时间
                long startTime = System.currentTimeMillis();
                //循环读取数据
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
                    if (fis.available() > 0) {
                        int readLen = fis.read(readData);
                        //当前长度小于最大值
                        if (frameLen + readLen < FRAME_MAX_LEN) {
                            //将readData拷贝到frame
                            System.arraycopy(readData, 0, frame, frameLen, readLen);
                            //修改frameLen
                            frameLen += readLen;
                            //寻找第一个帧头
                            int headFirstIndex = findHead(frame, 0, frameLen);
                            while (headFirstIndex >= 0 && isHead(frame, headFirstIndex)) {
                                int headSecondIndex = findHead(frame, headFirstIndex + FRAME_MIN_LEN, frameLen);
                                //如果第二个帧头存在，则两个帧头之间的就是一帧完整的数据
                                if (headSecondIndex > 0 && isHead(frame, headSecondIndex)) {
                                    onFrame(frame, headFirstIndex, headSecondIndex - headFirstIndex);
                                    //截取headSecondIndex之后到frame的有效数据,并放到frame最前面
                                    byte[] temp = Arrays.copyOfRange(frame, headSecondIndex, frameLen);
                                    System.arraycopy(temp, 0, frame, 0, temp.length);
                                    //修改frameLen的值
                                    frameLen = temp.length;
                                    sleepThread(startTime, System.currentTimeMillis());
                                    startTime = System.currentTimeMillis();
                                    headFirstIndex = findHead(frame, 0, frameLen);
                                } else {
                                    //找不到第二个帧头
                                    headFirstIndex = -1;
                                }
                            }
                        } else {
                            //如果长度超过最大值，frameLen置0
                            frameLen = 0;
                        }
                    } else {
                        isFinish = true;
                        break;
                    }
                }
            } catch (Exception e) {
                Log.d(TAG, "run: " + e);
            }
        }
    }

    private int findHead(byte[] data, int offset, int max) {
        int i;
        for (i = offset; i <= max; i++) {
            //发现帧头
            if (isHead(data, i))
                break;
        }
        //检测到最大值，未发现帧头
        if (i == max) {
            i = -1;
        }
        return i;
    }

    private boolean isHead(byte[] data, int offset) {
        boolean result = false;
        // 00 00 00 01 x
        if (data[offset] == 0x00 && data[offset + 1] == 0x00
                && data[offset + 2] == 0x00 && data[3] == 0x01) {
            result = true;
        }
        // 00 00 01 x
        if (data[offset] == 0x00 && data[offset + 1] == 0x00
                && data[offset + 2] == 0x01) {
            result = true;
        }
        return result;
    }

    private void initMediaCodec() {
        //根据需要解码的类型创建解码器
        try {
            mCodec = MediaCodec.createDecoderByType(MIME_TYPE);
        } catch (IOException e) {
            Log.e(TAG, "initMediaCodec: " + e);
            isError = true;
        }
        mediaFormat = MediaFormat.createVideoFormat(MIME_TYPE, width, height);

        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, FrameRate);
        mCodec.configure(mediaFormat, holder.getSurface(), null, 0);
        if (mCodec == null) {
            isFinish = true;
            return;
        }
        mCodec.start();
    }

    //视频解码
    private void onFrame(byte[] buf, int offset, int length) {
        if (mCodec == null){
            initMediaCodec();
        }
        // 获取输入buffer index
        ByteBuffer[] inputBuffers = mCodec.getInputBuffers();
        // 获取输出buffer index
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        int inputBufferIndex = mCodec.dequeueInputBuffer(10000);
        if (inputBufferIndex >= 0) {
            ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
            inputBuffer.clear();
            //put需要解码的数据
            inputBuffer.put(buf, offset, length);
            //解码
            mCodec.queueInputBuffer(inputBufferIndex, 0, length, mCount * 100000 / FrameRate, 0);
            mCount++;
        }
        int outputBufferIndex = mCodec.dequeueOutputBuffer(bufferInfo, 10000);
        for (; ; ) {
            //循环解码，直到数据全部解码完成
            if (outputBufferIndex >= 0) {
                try {
                    mCodec.releaseOutputBuffer(outputBufferIndex, true);
                    outputBufferIndex = mCodec.dequeueOutputBuffer(bufferInfo, 0);
                } catch (Exception e) {
                    Log.d(TAG, "run: " + e);
                }
            } else {
                break;
            }
        }
    }

    //修眠
    private void sleepThread(long startTime, long endTime) {
        //根据读文件和解码耗时，计算需要休眠的时间
        long time = PRE_FRAME_TIME - (endTime - startTime);
        if (time > 0) {
            try {
                Thread.sleep(time);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
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
                fis.close();
                mCodec = null;
            } catch (Exception e) {
                Log.d(TAG, "stopCodec: " + e);
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
        synchronized (this) {
            if (isPause) {
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