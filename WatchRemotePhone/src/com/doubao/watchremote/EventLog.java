package com.doubao.watchremote;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** 提醒链路的事件记录：排定 / 触发 / 发通知 / 异常，方便排查“不提醒” */
public class EventLog {

    private static final String P = "diag_log";
    private static final String K = "lines";
    private static final int MAX = 60;

    public static void add(Context c, String s) {
        try {
            String line = new SimpleDateFormat("MM-dd HH:mm:ss", Locale.CHINA)
                    .format(new Date()) + "  " + s;
            SharedPreferences sp = c.getSharedPreferences(P, Context.MODE_PRIVATE);
            String old = sp.getString(K, "");
            String[] parts = old.isEmpty() ? new String[0] : old.split("\n");
            StringBuilder sb = new StringBuilder(line);
            int start = Math.max(0, parts.length - (MAX - 1));
            for (int i = start; i < parts.length; i++) {
                if (parts[i].isEmpty()) continue;
                sb.append("\n").append(parts[i]);
            }
            sp.edit().putString(K, sb.toString()).apply();
        } catch (Throwable ignored) {
        }
    }

    public static String dump(Context c) {
        String s = c.getSharedPreferences(P, Context.MODE_PRIVATE).getString(K, "");
        return s == null || s.isEmpty() ? "（还没有记录）" : s;
    }

    public static void clear(Context c) {
        c.getSharedPreferences(P, Context.MODE_PRIVATE).edit().clear().apply();
    }
}
