package kr.ac.kaist.nmsl.pushmanager.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Date;

import kr.ac.kaist.nmsl.pushmanager.Constants;

/**
 * Created by cjpark on 2015-11-30.
 */
public class Util {
    private static final String FILE_UTIL_DATA_SEPARATOR = "\n";

    public static void writeLogToFile(Context context, String filename, String tag, String log) {
        File uuidDir = new File(Environment.getExternalStoragePublicDirectory("").getAbsolutePath());

        try {
            File dataFile = new File(uuidDir.getAbsolutePath() + "/" + filename + ".txt");
            FileOutputStream outputStream = new FileOutputStream(dataFile, true);

            outputStream.write(("["+ tag+ "]" + "\t").getBytes());
            outputStream.write((Long.toString(new Date().getTime()) + ", ").getBytes());
            outputStream.write((log + FILE_UTIL_DATA_SEPARATOR).getBytes());

            outputStream.close();

            refreshDataFile(context, dataFile);
        } catch (Exception e) {
            Log.e(Constants.DEBUG_TAG, e.getMessage());
        }
    }

    public static void refreshDataFile (Context context, File file) {
        context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + file.getAbsolutePath())));
    }

    public static boolean isVoiceDetected(double spl, double pitch) {
        return spl > Constants.AUDIO_SILENCE_SPL && pitch > 50 && pitch < 300;
    }

    public static boolean isTalking(ArrayList<Boolean> isVoiceList) {
        int count = 0;
        for (boolean isVoice: isVoiceList) {
            if (isVoice)
                count++;
        }

        //return count >= isVoiceList.size()/2;
        return count >= Constants.IS_TALKING_SAMPLE_COUNT;
    }
}
