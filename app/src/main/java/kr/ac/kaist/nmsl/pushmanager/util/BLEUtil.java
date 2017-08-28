package kr.ac.kaist.nmsl.pushmanager.util;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconTransmitter;

public class BLEUtil {

    public static BluetoothAdapter getBluetoothAdapter() {
        return BluetoothAdapter.getDefaultAdapter();
    }

    public static String getMacAddress() {
        return BluetoothAdapter.getDefaultAdapter().getAddress();
    }

    public static boolean isAdvertisingSupportedDevice(Context context) {
        if (context == null) {
            return false;
        }
        int isSupported = BeaconTransmitter.checkTransmissionSupported(context);
        switch (isSupported) {
            case BeaconTransmitter.SUPPORTED:
                return true;
            case BeaconTransmitter.NOT_SUPPORTED_BLE:
            case BeaconTransmitter.NOT_SUPPORTED_CANNOT_GET_ADVERTISER:
            case BeaconTransmitter.NOT_SUPPORTED_CANNOT_GET_ADVERTISER_MULTIPLE_ADVERTISEMENTS:
            case BeaconTransmitter.NOT_SUPPORTED_MIN_SDK:
            default:
                return false;
        }
    }

    public static long[] getMyPhoneNumber(Context context) {

        //TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        //String phoneNumber = telephonyManager.getLine1Number();
        String phoneNumber = "010-9117-5805";

        if (phoneNumber == null || phoneNumber.isEmpty()) {
            return new long[]{0L, 0L};
        }

        // if phone number exists
        phoneNumber = phoneNumber.replaceAll("\\D+", "");    // extract number only
        long myNumber = Long.parseLong(phoneNumber);
        long[] myPhoneNumber = new long[2];
        if (myNumber >= Integer.MAX_VALUE) {
            myPhoneNumber[0] = (Integer.MAX_VALUE - 1);
            myPhoneNumber[1] = myNumber - myPhoneNumber[0];
        } else {
            myPhoneNumber[0] = 0;
            myPhoneNumber[1] = myNumber;
        }

        return myPhoneNumber;

    }


    public static long getBLEPhoneNumber(Beacon detectedBeacon) {
        if (detectedBeacon == null || detectedBeacon.getDataFields().size() < 2) {
            return 0;
        } else {
            return detectedBeacon.getDataFields().get(0) + detectedBeacon.getDataFields().get(1);
        }
    }
}
