package com.doubao.watchremote;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;

/** 手表端上报的计划状态：哪些被暂停、哪些被禁用，手机端照着显示 */
public class WatchFlags {

    private static final String PREF = "watch_flags";
    private static final String KEY_PAUSED = "paused";
    private static final String KEY_DISABLED = "disabled";

    public static final int NORMAL = 0;
    public static final int PAUSED = 1;
    public static final int DISABLED = 2;

    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public static void save(Context c, JSONArray paused, JSONArray disabled) {
        sp(c).edit()
                .putString(KEY_PAUSED, paused == null ? "[]" : paused.toString())
                .putString(KEY_DISABLED, disabled == null ? "[]" : disabled.toString())
                .apply();
    }

    private static boolean in(Context c, String key, long id) {
        try {
            JSONArray arr = new JSONArray(sp(c).getString(key, "[]"));
            for (int i = 0; i < arr.length(); i++) {
                String s = arr.optString(i, "");
                if (s.equals(String.valueOf(id))) return true;
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    /** 手机这边记录的暂停名单，推给手表时带上 */
    /** 手机自己暂停/恢复某个计划时同步维护名单 */
    public static void setPaused(Context c, long id, boolean paused) {
        try {
            JSONArray src = new JSONArray(sp(c).getString(KEY_PAUSED, "[]"));
            JSONArray out = new JSONArray();
            boolean had = false;
            for (int i = 0; i < src.length(); i++) {
                String s = src.optString(i, "");
                if (s.equals(String.valueOf(id))) { had = true; continue; }
                out.put(s);
            }
            if (paused) out.put(String.valueOf(id));
            sp(c).edit().putString(KEY_PAUSED, out.toString()).apply();
            SyncStamp.touch(c);
        } catch (Throwable ignored) {
        }
    }

    public static String pausedJson(Context c) {
        return sp(c).getString(KEY_PAUSED, "[]");
    }

    /** 从暂停名单里去掉某个 id（手表上重新打开计划时用） */
    public static void clearPaused(Context c, long id) {
        remove(c, KEY_PAUSED, id);
        remove(c, KEY_DISABLED, id);
    }

    private static void remove(Context c, String key, long id) {
        try {
            JSONArray src = new JSONArray(sp(c).getString(key, "[]"));
            JSONArray out = new JSONArray();
            for (int i = 0; i < src.length(); i++) {
                String s = src.optString(i, "");
                if (!s.equals(String.valueOf(id))) out.put(s);
            }
            sp(c).edit().putString(key, out.toString()).apply();
        } catch (Throwable ignored) {
        }
    }

    public static int stateOf(Context c, long id) {
        if (in(c, KEY_DISABLED, id)) return DISABLED;
        if (in(c, KEY_PAUSED, id)) return PAUSED;
        return NORMAL;
    }

    public static String label(Context c, long id) {
        int st = stateOf(c, id);
        if (st == DISABLED) return Tone.s(c, "（手表端已禁用）", "（表上禁用了）", " (disabled on watch)");
        if (st == PAUSED) return Tone.s(c, "（手表端已暂停）", "（表上暂停了）", " (paused on watch)");
        return "";
    }
}
