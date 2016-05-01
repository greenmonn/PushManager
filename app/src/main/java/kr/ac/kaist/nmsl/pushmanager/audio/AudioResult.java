package kr.ac.kaist.nmsl.pushmanager.audio;

import be.tarsos.dsp.AudioEvent;

/**
 * Created by cjpark on 2016-05-01.
 */
public class AudioResult {
    public AudioEvent audioEvent;
    public double spl;
    public double pitch;

    public AudioResult (AudioEvent audioEvent, double spl, double pitch) {
        this.audioEvent = audioEvent;
        this.spl = spl;
        this.pitch = pitch;
    }
}
