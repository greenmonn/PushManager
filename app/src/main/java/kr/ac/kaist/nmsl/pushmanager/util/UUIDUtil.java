package kr.ac.kaist.nmsl.pushmanager.util;

import java.util.UUID;

public class UUIDUtil {
	public static UUID toUUID(String data) {
		if (data == null || data.isEmpty()) {
			throw new RuntimeException(
					"Input parameter cannot be an empty string.");
		}

		return UUID.nameUUIDFromBytes(data.getBytes());
	}
}
