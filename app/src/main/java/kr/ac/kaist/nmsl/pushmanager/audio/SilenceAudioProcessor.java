package kr.ac.kaist.nmsl.pushmanager.audio;

import android.util.Log;

import java.util.ArrayList;

import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.SilenceDetector;
import be.tarsos.dsp.filters.HighPass;
import be.tarsos.dsp.filters.LowPassFS;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchProcessor;
import kr.ac.kaist.nmsl.pushmanager.Constants;
import kr.ac.kaist.nmsl.pushmanager.util.Util;

/**
 * Created by cjpark on 2016-04-28.
 */
public class SilenceAudioProcessor implements AudioProcessor {
    private double samplingDuration;

    private SilenceDetector silenceDetector;
    private HighPass highPass;
    private LowPassFS lowPass;
    private PitchProcessor pitchProcessor;
    private double pitch;
    private ArrayList<Boolean> isVoiceList;

    private ResultHandler resultHandler;
    private TimeoutHandler timeoutHandler;

    public SilenceAudioProcessor(double silenceSPL, double samplingDuration, ResultHandler resultHandler, TimeoutHandler timeoutHandler) {
        this.samplingDuration = samplingDuration;

        this.resultHandler = resultHandler;
        this.timeoutHandler = timeoutHandler;

        silenceDetector = new SilenceDetector(silenceSPL, false);
        highPass = new HighPass(50, 22050);
        lowPass= new LowPassFS(300, 22050);
        pitchProcessor = new PitchProcessor(PitchProcessor.PitchEstimationAlgorithm.FFT_YIN, 22050, 1024, new PitchDetectionHandler() {
            @Override
            public void handlePitch(PitchDetectionResult pitchDetectionResult, AudioEvent audioEvent) {
                pitch = pitchDetectionResult.getPitch();
                Log.d(Constants.DEBUG_TAG, "detected pitch: " + pitch);
            }
        });

        isVoiceList = new ArrayList<>();
    }

    @Override
    public boolean process(AudioEvent audioEvent) {
        if (audioEvent.getTimeStamp() <= samplingDuration) {
            highPass.process(audioEvent);
            lowPass.process(audioEvent);
            pitchProcessor.process(audioEvent);

            boolean isSilence = silenceDetector.isSilence(audioEvent.getFloatBuffer());
            double spl = silenceDetector.currentSPL();

            isVoiceList.add(Util.isVoiceDetected(spl, pitch));

            this.resultHandler.handleResult(isSilence, spl, pitch, audioEvent);
            return true;
        } else {
            this.timeoutHandler.handleTimeout(isVoiceList);
            return false;
        }
    }

    @Override
    public void processingFinished() {
        Log.d(Constants.DEBUG_TAG, "processingFinished, "+this.toString());
    }

    public interface ResultHandler {
        public void handleResult(boolean isSilence, double currentSPL, double pitch, AudioEvent audioEvent);
    }

    public interface TimeoutHandler {
        public void handleTimeout(ArrayList<Boolean> isVoiceList);
    }
}
