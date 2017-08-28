package kr.ac.kaist.nmsl.pushmanager.audio;

import java.util.Date;

import be.tarsos.dsp.AudioEvent;
import kr.ac.kaist.nmsl.pushmanager.Constants;
import kr.ac.kaist.nmsl.pushmanager.util.Util;

/**
 * Created by cjpark on 2016-05-01.
 */
public class AudioResult {
    public boolean isTalking;
    public boolean isMyVoice;
    public Date updatedAt;
    
    public AudioResult (boolean isTalking, boolean isMyVoice) {
        this.isTalking = isTalking;
        this.isMyVoice = isMyVoice;
        updatedAt = new Date();
    }
}
