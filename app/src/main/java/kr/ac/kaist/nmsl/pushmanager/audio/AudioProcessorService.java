package kr.ac.kaist.nmsl.pushmanager.audio;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import java.util.Timer;
import java.util.TimerTask;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.SilenceDetector;
import be.tarsos.dsp.StopAudioProcessor;
import be.tarsos.dsp.io.android.AudioDispatcherFactory;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchProcessor;
import kr.ac.kaist.nmsl.pushmanager.Constants;

public class AudioProcessorService extends Service {

    public AudioProcessorService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        final AudioDispatcher dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(22050,1024,0);
        dispatcher.addAudioProcessor(new PauseAudioProcessor(0.1, 1, new PauseAudioProcessor.SilenceHandler() {
            @Override
            public void handleSilence(boolean isSilence, double currentSPL, AudioEvent audioEvent) {
                Log.d(Constants.DEBUG_TAG, isSilence + ", " + currentSPL + ", " + audioEvent.getTimeStamp());
            }
        }));

        new Thread(dispatcher).start();

        Log.d(Constants.DEBUG_TAG, "dispatcher started");

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
