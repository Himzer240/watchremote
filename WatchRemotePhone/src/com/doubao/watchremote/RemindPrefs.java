package com.doubao.watchremote;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Calendar;
import java.util.Locale;

/** 全局提醒方式 + 勿扰时段 */
public class RemindPrefs {

    private static final String P = "app_prefs";

    public static final int MODE_FOLLOW = -1;  // 跟随每个时间单独设置
    public static final int MODE_ALARM = 0;    // 闹钟式：全屏页 + 循环铃声
    public static final int MODE_HEADS = 1;    // 悬浮弹窗：横幅通知，不弹页面
    public static final int MODE_SILENT = 2;   // 不提醒

    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences(P, Context.MODE_PRIVATE);
    }

    public static int globalMode(Context c) {
        return sp(c).getInt("global_mode", MODE_FOLLOW);
    }

    public static void setGlobalMode(Context c, int mode) {
        sp(c).edit().putInt("global_mode", mode).apply();
    }

    public static String modeName(int mode) {
        if (mode == MODE_ALARM) return "闹钟提醒";
        if (mode == MODE_HEADS) return "悬浮弹窗";
        if (mode == MODE_SILENT) return "不提醒";
        return "跟随全局";
    }

    public static void setDndOff(Context c) {
        sp(c).edit().putBoolean("dnd_on", false).apply();
    }

    public static void setDndAll(Context c) {
        sp(c).edit().putBoolean("dnd_on", true).putBoolean("dnd_all", true).apply();
    }

    /** 备份恢复用：一次写回整组勿扰设置 */
    public static void restoreDnd(Context c, boolean on, boolean all, int startMin, int endMin) {
        sp(c).edit().putBoolean("dnd_on", on).putBoolean("dnd_all", all)
                .putInt("dnd_start", startMin).putInt("dnd_end", endMin).apply();
    }

    public static void setDndWindow(Context c, int startMin, int endMin) {
        sp(c).edit().putBoolean("dnd_on", true).putBoolean("dnd_all", false)
                .putInt("dnd_start", startMin).putInt("dnd_end", endMin).apply();
    }

    public static boolean onboardDone(Context c) {
        return sp(c).getBoolean("onboard_done", false);
    }

    public static void setOnboardDone(Context c) {
        sp(c).edit().putBoolean("onboard_done", true).apply();
    }

    public static int repeatMinutes(Context c) {
        return sp(c).getInt("repeat_min", 5);
    }

    public static void setRepeatMinutes(Context c, int mins) {
        sp(c).edit().putInt("repeat_min", mins).apply();
    }

    public static boolean dndOn(Context c) {
        return sp(c).getBoolean("dnd_on", false);
    }

    public static boolean dndAllDay(Context c) {
        return sp(c).getBoolean("dnd_on", false) && sp(c).getBoolean("dnd_all", true);
    }

    public static int dndStart(Context c) {
        return sp(c).getInt("dnd_start", 22 * 60);
    }

    public static int dndEnd(Context c) {
        return sp(c).getInt("dnd_end", 7 * 60);
    }

    /** 此刻是否在勿扰时段 */
    public static boolean inDnd(Context c) {
        if (!dndOn(c)) return false;
        if (dndAllDay(c)) return true;
        Calendar now = Calendar.getInstance();
        int cur = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
        int s = dndStart(c);
        int e = dndEnd(c);
        if (s == e) return true;
        if (s < e) return cur >= s && cur < e;
        return cur >= s || cur < e;  // 跨零点
    }

    public static String dndText(Context c) {
        if (!dndOn(c)) return "勿扰：未开启";
        if (dndAllDay(c)) return "勿扰：全天开启（当前不提醒）";
        return String.format(Locale.CHINA, "勿扰：%02d:%02d – %02d:%02d%s",
                dndStart(c) / 60, dndStart(c) % 60, dndEnd(c) / 60, dndEnd(c) % 60,
                inDnd(c) ? "（正在勿扰）" : "");
    }

    /** 生效方式：先看勿扰，再看全局，最后看每条时间自己的设置 */
    public static int modeFor(Context c, Medicine m, int index) {
        if (inDnd(c)) return MODE_SILENT;
        return staticModeFor(c, m, index);
    }

    public static int staticModeFor(Context c, Medicine m, int index) {
        if (m.groupMode >= 0) return m.groupMode;
        return m.modeAt(index);
    }

    /** 高级功能：去掉确认弹窗里的倒计时 */
    public static boolean noCountdown(Context c) {
        return sp(c).getBoolean("no_countdown", false);
    }

    public static void setNoCountdown(Context c, boolean on) {
        sp(c).edit().putBoolean("no_countdown", on).apply();
    }

    /** 主页底部区域是否收起（只留「今日已服用」标题） */
    public static boolean logCollapsed(Context c) {
        return sp(c).getBoolean("log_collapsed", false);
    }

    public static void setLogCollapsed(Context c, boolean on) {
        sp(c).edit().putBoolean("log_collapsed", on).apply();
    }

    /** 提醒方式：0 铃铛（有声音） / 1 铃铛划线（静音） / 2 月亮（勿扰） */
    public static final int ALERT_RING = 0;
    public static final int ALERT_SILENT = 1;
    public static final int ALERT_DND = 2;

    public static boolean silentOn(Context c) {
        return sp(c).getBoolean("silent_on", false);
    }

    public static int alertMode(Context c) {
        if (dndAllDay(c)) return ALERT_DND;
        if (silentOn(c)) return ALERT_SILENT;
        return ALERT_RING;
    }

    public static void setAlertMode(Context c, int mode) {
        if (mode == ALERT_DND) {
            sp(c).edit().putBoolean("silent_on", false).apply();
            setDndAll(c);
        } else if (mode == ALERT_SILENT) {
            setDndOff(c);
            sp(c).edit().putBoolean("silent_on", true).apply();
        } else {
            setDndOff(c);
            sp(c).edit().putBoolean("silent_on", false).apply();
        }
    }

    public static String alertModeText(Context c) {
        int m = alertMode(c);
        if (m == ALERT_DND) return "已开启勿扰：所有提醒都不响";
        if (m == ALERT_SILENT) return "已设为静音提醒：到点会提醒，但没有声音";
        return "已设为正常提醒：到点响铃并震动";
    }
}
