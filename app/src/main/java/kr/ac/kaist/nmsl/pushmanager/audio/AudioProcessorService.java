package kr.ac.kaist.nmsl.pushmanager.audio;

import android.app.Service;
import android.content.Intent;
import android.media.MediaRecorder;
import android.os.IBinder;
import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.filters.HighPass;
import be.tarsos.dsp.filters.LowPassFS;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchProcessor;
import kr.ac.kaist.nmsl.pushmanager.Constants;

public class AudioProcessorService extends Service {
    private Timer mTimer;

    public AudioProcessorService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mTimer = new Timer();
        mTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                final AudioDispatcher dispatcher = SilenceAudioDispatcherFactory.fromMicrophone(MediaRecorder.AudioSource.MIC, 22050,1024,0);
                dispatcher.addAudioProcessor(new SilenceAudioProcessor(Constants.AUDIO_SILENCE_SPL, Constants.AUDIO_SAMPLING_DURATION, new SilenceAudioProcessor.ResultHandler() {
                    @Override
                    public void handleResult(boolean isSilence, double currentSPL, double pitch, AudioEvent audioEvent) {
                        //if (!isSilence) {
                            Log.d(Constants.DEBUG_TAG, "AudioProcessor: " + isSilence + ", " + currentSPL + ", " + pitch + ", " + audioEvent.getTimeStamp());
                        //}
                        Intent i = new Intent(Constants.INTENT_FILTER_AUDIO);

                        i.putExtra("spl", currentSPL);
                        i.putExtra("pitch", pitch);

                        sendBroadcast(i);
                    }
                }, new SilenceAudioProcessor.TimeoutHandler() {
                    @Override
                    public void handleTimeout() {
                        dispatcher.stop();
                    }
                }));

                dispatcher.run();
            }
        }, Constants.AUDIO_SAMPLING_PERIOD * 1000, Constants.AUDIO_SAMPLING_PERIOD * 1000);

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        mTimer.cancel();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
