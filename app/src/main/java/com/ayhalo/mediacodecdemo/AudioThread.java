package com.ayhalo.mediacodecdemo;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;


/**
 * MediaCodecDemo
 * Created by Halo on 2017/9/27.
 */

public class AudioThread extends ReadThread {

    public static final String TAG = "halo";
    private static final String MIME_TYPE = "audio/mp4a-latm";
    //文件路径
    private String filePath;
    private int mCount = 0;
    //文件读取完成标识
    private boolean isFinish = false;
    private boolean isPause = false;
    private boolean isError = false;
    //这个值用于找到第一个帧头后，继续寻找第二个帧头，如果解码失败可以尝试缩小这个值
    private int FRAME_MIN_LEN = 50;
    //一般AAC帧大小不超过200k,如果解码失败可以尝试增大这个值
    private static int FRAME_MAX_LEN = 200 * 1024;
    //根据帧率获取的解码每帧需要休眠的时间,根据实际帧率进行操作
    private int PRE_FRAME_TIME = 1000 / 50;
    private FileInputStream fis = null;
    //声道数
    private static final int KEY_CHANNEL_COUNT = 2;
    //采样率
    private static final int KEY_SAMPLE_RATE = 48000;

    private AudioManager audioManager;
    private Context context;

    private AudioTrack audioTrack;
    private MediaCodec mCodec;

    public AudioThread(Context context,String path) {
        this.context = context;
        this.filePath = path;
        audioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
        this.start();
    }

    @Override
    public void run() {
        File file = new File(filePath);
        //判断文件是否存在
        if (file.exists()) {
            try {
                fis = new FileInputStream(file);
                //保存完整数据帧
                byte[] frame = new byte[FRAME_MAX_LEN];
                //当前帧长度
                int frameLen = 0;
                //每次从文件读取的数据
                byte[] readData = new byte[10 * 1024];
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
                                //寻找第二个帧头
                                int headSecondIndex = findHead(frame, headFirstIndex + FRAME_MIN_LEN, frameLen);
                                //如果第二个帧头存在，则两个帧头之间的就是一帧完整的数据
                                if (headSecondIndex > 0 && isHead(frame, headSecondIndex)) {
                                    mCount++;
                                    decode(frame, headFirstIndex, headSecondIndex - headFirstIndex);
                                    //截取headSecondIndex之后到frame的有效数据,并放到frame最前面
                                    byte[] temp = Arrays.copyOfRange(frame, headSecondIndex, frameLen);
                                    System.arraycopy(temp, 0, frame, 0, temp.length);
                                    //修改frameLen的值
                                    frameLen = temp.length;
                                    //线程休眠
                                    sleepThread(startTime, System.currentTimeMillis());
                                    //重置开始时间
                                    startTime = System.currentTimeMillis();
                                    //继续寻找数据帧
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
                        //文件读取结束
                        isFinish = true;
                        stopCodec();
                        break;
                    }
                }
            } catch (Exception e) {
                Log.d(TAG, "run: " + e);
            }
        }
    }

    /**
     * 初始化解码器
     *
     * @return 初始化失败返回false，成功返回true
     */
    public void initAudioCodec() {
        audioManager.requestAudioFocus(listener,AudioManager.STREAM_MUSIC,AudioManager.AUDIOFOCUS_GAIN);
        int minSize = AudioTrack.getMinBufferSize(KEY_SAMPLE_RATE, KEY_CHANNEL_COUNT, AudioFormat.ENCODING_PCM_16BIT);
        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, KEY_SAMPLE_RATE, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT, minSize, AudioTrack.MODE_STREAM);
        audioTrack.play();
        try {
            //初始化解码器
            mCodec = MediaCodec.createDecoderByType(MIME_TYPE);
            //MediaFormat用于描述音视频数据的相关参数
            MediaFormat mediaFormat = new MediaFormat();
            mediaFormat.setString(MediaFormat.KEY_MIME, MIME_TYPE);
            //声道个数
            mediaFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, KEY_CHANNEL_COUNT);
            //采样率
            mediaFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, KEY_SAMPLE_RATE);
            //比特率
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 128000);
            //用来标记AAC是否有adts头，1->有
            mediaFormat.setInteger(MediaFormat.KEY_IS_ADTS, 1);
            //用来标记aac的类型
            mediaFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
            //ByteBuffer key（暂时不了解该参数的含义，但必须设置）
            byte[] data = new byte[]{(byte) 0x11, (byte) 0x90};
            ByteBuffer csd_0 = ByteBuffer.wrap(data);
            mediaFormat.setByteBuffer("csd-0", csd_0);
            mCodec.configure(mediaFormat, null, null, 0);
        } catch (Exception e) {
            Log.e("HALO", "initMediaCodec: " + e);
            isError = true;
        }
        if (mCodec == null) {
            isFinish = true;
            return;
        }
        mCodec.start();
    }

    /**
     * aac解码+播放
     */
    public void decode(byte[] buf, int offset, int length) {
        if (mCodec == null){
            initAudioCodec();
        }
        //输入ByteBuffer
        ByteBuffer[] codecInputBuffers = mCodec.getInputBuffers();
        //输出ByteBuffer
        ByteBuffer[] codecOutputBuffers = mCodec.getOutputBuffers();
        //等待时间，0->不等待，-1->一直等待
        long kTimeOutUs = 0;
        try {
            //返回一个包含有效数据的input buffer的index,-1->不存在
            int inputBufIndex = mCodec.dequeueInputBuffer(kTimeOutUs);
            if (inputBufIndex >= 0) {
                //获取当前的ByteBuffer
                ByteBuffer dstBuf = codecInputBuffers[inputBufIndex];
                dstBuf.clear();
                dstBuf.put(buf, offset, length);
                //将指定index的input buffer提交给解码器
                mCodec.queueInputBuffer(inputBufIndex, 0, length, mCount * 10000, 0);
            }
            //编解码器缓冲区
            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            //返回一个output buffer的index，-1->不存在
            int outputBufferIndex = mCodec.dequeueOutputBuffer(info, kTimeOutUs);

            ByteBuffer outputBuffer;
            while (outputBufferIndex >= 0) {
                //获取解码后的ByteBuffer
                outputBuffer = codecOutputBuffers[outputBufferIndex];
                //用来保存解码后的数据
                byte[] outData = new byte[info.size];
                outputBuffer.get(outData);
                //清空缓存
                outputBuffer.clear();
                //播放解码后的数据
                audioTrack.write(outData, 0, info.size);
                //释放已经解码的buffer
                mCodec.releaseOutputBuffer(outputBufferIndex, false);
                //解码未解完的数据
                outputBufferIndex = mCodec.dequeueOutputBuffer(info, 0);
            }
        } catch (Exception e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
        }
    }

    private int findHead(byte[] data, int startIndex, int max) {
        int i;
        for (i = startIndex; i <= max; i++) {
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

    /**
     * 判断aac帧头
     */
    private boolean isHead(byte[] data, int offset) {
        boolean result = false;
        if (data[offset] == (byte) 0xFF && data[offset + 1] == (byte) 0xF1 && data[offset + 2] == (byte) 0x50 && data[offset + 3] == (byte) 0x80) {
            result = true;
        }
        return result;
    }

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
    public void startPlayer() {
        synchronized (this) {
            if (isPause) {
                audioTrack.play();
                isPause = false;
                notify();
            }
        }
    }

    @Override
    public void pausePlayer() {
        audioTrack.pause();
        isPause = true;
    }

    @Override
    void stopCodec() {
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
                audioTrack.stop();
                audioTrack.release();
                fis.close();
                mCodec = null;
                audioTrack = null;
            } catch (Exception e) {
                Log.d(TAG, "stopCodec: " + e);
            }

        }

    }

    @Override
    int getPlayerState() {
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
    private AudioManager.OnAudioFocusChangeListener listener = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int focusChange) {
            switch (focusChange){
                case AudioManager.AUDIOFOCUS_LOSS:
                    //长时间丢失焦点
                    Log.d(TAG,"AUDIOFOCUS_LOSS");
                    stopCodec();
                    //释放焦点
                    audioManager.abandonAudioFocus(listener);
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                    //短暂性失去焦点
                    Log.d(TAG,"AUDIOFOCUS_LOSS_TRANSIENT");
                    pausePlayer();
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    //短暂性丢失焦点并作降音处理
                    Log.d(TAG, "AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK");
                    audioTrack.setStereoVolume(0.3f,0.3f);
                    break;
                case AudioManager.AUDIOFOCUS_GAIN:
                    //重新获得焦点
                    Log.d(TAG, "AUDIOFOCUS_GAIN");
                    audioTrack.setStereoVolume(1.0f,1.0f);
                    break;
            }
        }
    };
}
