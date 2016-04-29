package kr.ac.kaist.nmsl.pushmanager.audio;

import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.SilenceDetector;

/**
 * Created by cjpark on 2016-04-28.
 */
public class PauseAudioProcessor implements AudioProcessor {
    private double periodStartTime = -1;
    private double periodEndTime = -1;

    private double pulseDuration;
    private double period;

    private SilenceDetector mSilenceDetector;

private SilenceHandler handler;

    public PauseAudioProcessor(double pulseDuration, double period, SilenceHandler handler) {
        this.pulseDuration = pulseDuration;
        this.period = period;
        this.handler = handler;

        mSilenceDetector = new SilenceDetector(-70.0, false);
    }

    @Override
    public boolean process(AudioEvent audioEvent) {
        if (periodStartTime < 0 && periodEndTime < 0) {
            initPeriod(audioEvent.getTimeStamp());
        }

        if (audioEvent.getTimeStamp() - periodStartTime >= period) {
            initPeriod(audioEvent.getTimeStamp());
        }

        if (audioEvent.getTimeStamp() - periodStartTime < pulseDuration) {
            if(this.handler != null){
                this.handler.handleSilence(mSilenceDetector.isSilence(audioEvent.getFloatBuffer()), mSilenceDetector.currentSPL(), audioEvent);
            }
            return true;
        } else if (audioEvent.getTimeStamp() - periodStartTime >= pulseDuration) {
            // Cool down
            return false;
        } else {
            return false;
        }
    }

    private void initPeriod (double time) {
        periodStartTime = time;
        periodEndTime = time + period;
    }

    @Override
    public void processingFinished() {

    }

    public interface SilenceHandler {
        public void handleSilence(boolean isSilence, double currentSPL, AudioEvent audioEvent);
    }
}
