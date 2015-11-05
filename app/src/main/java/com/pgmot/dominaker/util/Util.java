package com.pgmot.dominaker.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.UUID;

/**
 * Created by mot on 11/4/15.
 */
public class Util {
    static public String getDeviceUUID(Context context) {
        final String uuidKey = "uuid";
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String uuid = sharedPreferences.getString(uuidKey, "");

        if (uuid.isEmpty()) {
            uuid = getUUID();
            sharedPreferences.edit().putString(uuidKey, uuid).apply();
        }
        return uuid;
    }

    static public String getUUID(){
        return UUID.randomUUID().toString();
    }
}
