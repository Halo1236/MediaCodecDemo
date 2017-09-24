package com.ayhalo.mediacodecdemo;

/**
 * @author Halo
 *         2017 2017/9/23 18:37.
 */

public abstract class ReadThread extends Thread {

    public abstract void startPlayer();

    public abstract void pausePlayer();

    public abstract void stopCodec();

    public abstract boolean getPauseState();
}
