package com.hamani.suntech.pushman.common;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

public class SharedData {
    private Context context;
    private SharedPreferences pref;
    private SharedPreferences.Editor edit;

    public SharedData(Context context) {
        this.context = context;
        pref = context.getSharedPreferences(context.getPackageName(), Activity.MODE_PRIVATE);
        edit = pref.edit();
    }

    public void ClearData() {
        if (pref != null) {
            edit.clear();
            edit.commit();
        }
    }

    public void setBoolean(String name, Boolean value) {
        edit.putBoolean(name, value);
        edit.commit();
    }
    public Boolean getBoolean(String name) {
        return pref.getBoolean(name, false);
    }

    public void setString(String name, String value) {
        edit.putString(name, value);
        edit.commit();
    }
    public String getString(String name) {
        return pref.getString(name, null);
    }
}
