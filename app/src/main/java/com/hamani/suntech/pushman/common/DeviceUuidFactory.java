package com.hamani.suntech.pushman.common;

import android.content.Context;
import android.provider.Settings;

import java.io.UnsupportedEncodingException;
import java.util.UUID;

public class DeviceUuidFactory {

    protected volatile static UUID uuid;
    protected SharedData sd;

    public DeviceUuidFactory(Context context) {
        if (uuid == null) {
            synchronized (DeviceUuidFactory.class) {
                if (uuid == null) {
                    sd = new SharedData(context);
                    final String id = sd.getString(Globals.PREFS_DEVICE_ID);
                    if (id != null) {
                        uuid = UUID.fromString(id);
                    } else {
                        final String android_id = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
                        try {
                            if (!"9774d56d682e549c".equals(android_id)) {
                                uuid = UUID.nameUUIDFromBytes(android_id.getBytes("utf8"));
                            } else {
                                uuid = UUID.randomUUID();
                            }
                        } catch (UnsupportedEncodingException e) {
                            throw  new RuntimeException(e);
                        }
                        sd.setString(Globals.PREFS_DEVICE_ID, uuid.toString());
                    }
                }
            }
        }
    }

    public UUID getDeviceUuid() {
        return uuid;
    }
}
