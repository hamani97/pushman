package com.hamani.suntech.pushman.common;

import android.content.Context;

public class CheckCertification {

    public static final int USING_OK = 10;
    public static final int NO_DEVICE_ID = 11;
    public static final int NO_SERVER_ADDR = 12;

    private SharedData sd;

    public CheckCertification(Context context) {
        sd = new SharedData(context);
    }

    public int check() {
        int status = USING_OK;

        String device_id = sd.getString(Globals.PREFS_DEVICE_ID);
        String server_addr = sd.getString(Globals.PREFS_SERVER_ADDR);

        if (device_id==null || device_id.equals("")) {
            status = NO_DEVICE_ID;
        } else if (server_addr==null || server_addr.equals("")) {
            status = NO_SERVER_ADDR;
        }
        return status;
    }
}
