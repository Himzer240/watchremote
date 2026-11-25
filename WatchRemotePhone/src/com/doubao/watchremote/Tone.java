package com.doubao.watchremote;

import android.content.Context;

/** 表达风格：0 书面语、1 喵语、2 English；以及老年人模式开关 */
public class Tone {

    public static final int FORMAL = 0;
    public static final int CAT = 1;
    public static final int EN = 2;

    private static final String P = "tone_pref";
    private static final String K_STYLE = "style";
    private static final String K_ELDER = "elder";

    public static int style(Context c) {
        int v = c.getSharedPreferences(P, Context.MODE_PRIVATE).getInt(K_STYLE, FORMAL);
        return (v < 0 || v > 2) ? FORMAL : v;
    }

    public static void setStyle(Context c, int v) {
        c.getSharedPreferences(P, Context.MODE_PRIVATE).edit().putInt(K_STYLE, v).apply();
    }

    public static boolean elder(Context c) {
        return c.getSharedPreferences(P, Context.MODE_PRIVATE).getBoolean(K_ELDER, false);
    }

    public static void setElder(Context c, boolean on) {
        c.getSharedPreferences(P, Context.MODE_PRIVATE).edit().putBoolean(K_ELDER, on).apply();
    }

    public static String styleName(Context c) {
        int s = style(c);
        return s == CAT ? "喵语" : (s == EN ? "English" : "书面语");
    }

    /** 按当前风格取一句话 */
    public static String s(Context c, String formal, String cat, String en) {
        int st = style(c);
        if (st == CAT) return cat == null ? Lang.s(c, formal) : cat;
        if (st == EN) return en == null ? formal : en;
        return formal;
    }

    public static String remindTitle(Context c, String name, String dose) {
        return s(c, name + " · " + dose, name + " 该吃啦喵～", name + " · " + dose);
    }

    public static String remindBody(Context c, String meal, String time, int repeatNo) {
        String en = (repeatNo > 0 ? "Reminder #" + repeatNo + ". " : "")
                + "It's time for your medicine (" + meal + "). Scheduled " + time + ".";
        String cat = (repeatNo > 0 ? "第 " + repeatNo + " 次啦，别忘了哦～ " : "")
                + "该吃药咯～ " + meal + " · 计划 " + time + " (｡•ω•｡)♡";
        String formal = (repeatNo > 0 ? "第 " + repeatNo + " 次提醒：" : "")
                + "请按时服药（" + meal + "），计划时间 " + time;
        return s(c, formal, cat, en);
    }

    public static String takenAction(Context c) {
        return s(c, "已服用", "吃掉啦喵", "Taken");
    }

    public static String snoozeAction(Context c) {
        return s(c, "10 分钟后", "10 分钟后再提醒", "Snooze 10m");
    }

    public static String homeEmpty(Context c) {
        return s(c, "今日暂无用药计划", "今天还没有吃药安排喵～", "No medicine scheduled today");
    }

    public static String homeSummary(Context c, int total, int taken) {
        return s(c, "今日计划 " + total + " 次，已服用 " + taken + " 次",
                "今天要吃药 " + total + " 次，已经吃掉 " + taken + " 次啦喵 (｡•ᴗ•｡)",
                "Plan " + total + " doses, taken " + taken);
    }

    public static String takeBtn(Context c, boolean overflow) {
        if (overflow) {
            return s(c, "追加一次", "再吃一次喵", "Take again");
        }
        return s(c, "已服用", "吃掉啦喵", "Taken");
    }

    public static String disabledTake(Context c) {
        return s(c, "已关闭", "关掉啦喵", "Paused");
    }
}
