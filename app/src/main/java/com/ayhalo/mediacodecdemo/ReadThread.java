package com.ayhalo.mediacodecdemo;

/**
 * @author Halo
 *         2017 2017/9/23 18:37.
 */

public abstract class ReadThread extends Thread {

    public static final int PLAYER_STATE_PLAYING = 1;

    public static final int PLAYER_STATE_FINISHED = 2;

    public static final int PLAYER_STATE_PAUSED = 3;

    public static final int PLAYER_STATE_ERROR = 4;

    abstract void startPlayer();

    abstract void pausePlayer();

    abstract void stopCodec();

    abstract int getPlayerState();
}
