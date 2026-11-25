package com.doubao.watchremote;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * 记录“最后一次本机改动计划”的时间，用来判断两端操作谁更新。
 * 谁的时间更晚，就以谁的为准。
 */
public class SyncStamp {
    private static final String PREF = "sync_stamp";
    private static final String KEY = "at";

    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public static void touch(Context c) {
        sp(c).edit().putLong(KEY, System.currentTimeMillis()).apply();
    }

    public static long last(Context c) {
        return sp(c).getLong(KEY, 0L);
    }

    public static void set(Context c, long at) {
        if (at > 0) sp(c).edit().putLong(KEY, at).apply();
    }
}
