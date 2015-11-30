package kr.ac.kaist.nmsl.pushmanager.defer;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class DeferService extends Service {
    public DeferService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
