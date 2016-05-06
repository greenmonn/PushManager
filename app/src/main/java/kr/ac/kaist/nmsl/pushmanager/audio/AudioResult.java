package kr.ac.kaist.nmsl.pushmanager.audio;

import be.tarsos.dsp.AudioEvent;
import kr.ac.kaist.nmsl.pushmanager.Constants;
import kr.ac.kaist.nmsl.pushmanager.util.Util;

/**
 * Created by cjpark on 2016-05-01.
 */
public class AudioResult {
    public boolean isTalking;
    
    public AudioResult (boolean isTalking) {
        this.isTalking = isTalking;
    }
}
