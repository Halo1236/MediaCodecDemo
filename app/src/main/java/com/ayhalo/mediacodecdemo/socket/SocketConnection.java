package com.ayhalo.mediacodecdemo.socket;

import java.net.Socket;

/**
 * MediaCodecDemo
 * Created by Halo on 2017/9/27.
 */

public class SocketConnection {
    public boolean isRun = false;
    public Socket socket;
    public SocketConnection(){
        socket = new Socket();
    }

//    public ByteBuffer getFrame(){
//        return ;
//    }
    public boolean getIsRun() {
        return isRun;
    }

    public Socket getSocket() {
        return socket;
    }


}
