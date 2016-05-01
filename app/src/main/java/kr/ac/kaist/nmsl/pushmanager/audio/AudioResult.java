package kr.ac.kaist.nmsl.pushmanager.audio;

import be.tarsos.dsp.AudioEvent;

/**
 * Created by cjpark on 2016-05-01.
 */
public class AudioResult {
    public double spl;
    public double pitch;
    
    public AudioResult (double spl, double pitch) {
        this.spl = spl;
        this.pitch = pitch;
    }
}
