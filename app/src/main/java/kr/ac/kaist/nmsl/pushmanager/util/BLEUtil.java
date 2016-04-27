package kr.ac.kaist.nmsl.pushmanager.util;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;

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
}
