package kr.ac.kaist.nmsl.pushmanager.warning;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class WarningService extends Service {
    public WarningService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
