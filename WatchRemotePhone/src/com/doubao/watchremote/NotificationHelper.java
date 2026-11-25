package com.doubao.watchremote;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

public class NotificationHelper {

    /** 全新安装时使用的默认类别 */
    public static final String DEFAULT_CHANNEL_ID = "med_remind_v3";
    private static final String PREF = "app_prefs";
    private static final String KEY_CHANNEL = "channel_id";

    /** 当前实际使用的类别：升级时沿用旧类别，不重置用户已经调好的设置 */
    public static String channelId(Context c) {
        String id = c.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .getString(KEY_CHANNEL, "");
        return (id == null || id.isEmpty()) ? DEFAULT_CHANNEL_ID : id;
    }

    private static void saveChannelId(Context c, String id) {
        c.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .edit().putString(KEY_CHANNEL, id).apply();
    }

    public static int notifId(long medId, int index) {
        return Scheduler.requestCode(medId, index);
    }

    public static void ensureChannel(Context c) {
        if (Build.VERSION.SDK_INT < 26) return;
        NotificationManager nm =
                (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;

        String saved = channelId(c);
        NotificationChannel cur = saved.isEmpty() ? null : nm.getNotificationChannel(saved);

        // 用户手动删掉的类别，安卓不允许应用用同一个 ID 再建回来，
        // 所以只要当前类别不在、或被系统关门（重要级=无），就换一个新 ID 全开重建。
        if (cur != null && cur.getImportance() != NotificationManager.IMPORTANCE_NONE) {
            cur.setName(Tone.s(c, "吃药提醒", "吃药提醒喵～", "Medicine Reminder"));
            cur.setDescription(Tone.s(c, "到点提醒你按时吃药", "到点叫你吃药喵", "Reminds you when it's time"));
            nm.createNotificationChannel(cur);
            return;
        }

        if (cur != null) {
            // 旧类别被关成“无”，改名标注一下，方便用户自己删
            try {
                cur.setName(Tone.s(c, "吃药提醒（已关闭，可删除）", "吃药提醒喵（已关闭）",
                        "Medicine Reminder (off, safe to delete)"));
                nm.createNotificationChannel(cur);
            } catch (Throwable ignored) {
            }
        }

        // 找一个没用过的 ID：med_remind_vN，N 只增不减
        int seq = c.getSharedPreferences(PREF, Context.MODE_PRIVATE).getInt("channel_seq", 3);
        String id;
        while (true) {
            seq++;
            id = "med_remind_v" + seq;
            if (nm.getNotificationChannel(id) == null) break;
        }
        c.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .edit().putInt("channel_seq", seq).apply();

        // 新建即全部默认开启：重要级（可悬浮/横幅）、闹钟铃声、震动、锁屏显示全部内容、角标、呼吸灯
        NotificationChannel ch = new NotificationChannel(
                id, Tone.s(c, "吃药提醒", "吃药提醒喵～", "Medicine Reminder"),
                NotificationManager.IMPORTANCE_HIGH);
        ch.setDescription(Tone.s(c, "到点提醒你按时吃药", "到点叫你吃药喵", "Reminds you when it's time"));
        Uri sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if (sound == null) sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        AudioAttributes attrs = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        ch.setSound(sound, attrs);
        ch.enableVibration(true);
        ch.setVibrationPattern(new long[]{0, 600, 300, 600, 300, 900});
        ch.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        ch.setShowBadge(true);
        ch.enableLights(true);
        ch.setBypassDnd(false);
        nm.createNotificationChannel(ch);
        saveChannelId(c, id);
    }


    private static final String SILENT_CHANNEL = "med_silent_v1";

    /** 静音通道：能弹通知、有横幅，但没有铃声和震动 */
    static String silentChannel(Context c) {
        if (Build.VERSION.SDK_INT < 26) return null;
        NotificationManager nm =
                (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return null;
        if (nm.getNotificationChannel(SILENT_CHANNEL) != null) return SILENT_CHANNEL;
        NotificationChannel ch = new NotificationChannel(SILENT_CHANNEL,
                Tone.s(c, "吃药提醒（静音）", "吃药提醒喵（静音）", "Medicine Reminder (silent)"),
                NotificationManager.IMPORTANCE_HIGH);
        ch.setDescription(Tone.s(c, "到点弹出提醒，但不响铃不震动", "到点弹出来，不响喵",
                "Shows the reminder without sound or vibration"));
        ch.setSound(null, null);
        ch.enableVibration(false);
        ch.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        ch.setShowBadge(true);
        ch.enableLights(true);
        nm.createNotificationChannel(ch);
        return SILENT_CHANNEL;
    }

    public static void show(Context c, Medicine m, int index) {
        show(c, m, index, false);
    }

    public static void show(Context c, Medicine m, int index, boolean test) {
        show(c, m, index, test, RemindPrefs.MODE_ALARM, 0);
    }

    public static void show(Context c, Medicine m, int index, boolean test, int mode) {
        show(c, m, index, test, mode, 0);
    }

    public static void show(Context c, Medicine m, int index, boolean test, int mode, int repeatNo) {
        if (Build.VERSION.SDK_INT >= 33
                && c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            EventLog.add(c, "没发通知：没有通知权限");
            return;
        }
        ensureChannel(c);
        NotificationManager nm =
                (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;

        String time = m.timeAt(index);
        int id = notifId(m.id, index);

        Intent full = new Intent(c, AlarmActivity.class);
        full.putExtra("medId", m.id);
        full.putExtra("index", index);
        full.putExtra("test", test);
        full.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent fullPi = PendingIntent.getActivity(c, id, full,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent taken = new Intent(c, TakenReceiver.class);
        taken.setAction(Scheduler.ACTION_TAKEN);
        taken.putExtra("medId", m.id);
        taken.putExtra("index", index);
        PendingIntent takenPi = PendingIntent.getBroadcast(c, 200000 + id, taken,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent snooze = new Intent(c, SnoozeReceiver.class);
        snooze.setAction(Scheduler.ACTION_SNOOZE);
        snooze.putExtra("medId", m.id);
        snooze.putExtra("index", index);
        PendingIntent snoozePi = PendingIntent.getBroadcast(c, 300000 + id, snooze,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String title = test ? Tone.s(c, "测试提醒（不会记账）", "测试提醒喵（不记账）", "Test alert (no logging)")
                : Tone.remindTitle(c, m.name, m.dose);
        String text;
        if (test) {
            text = Tone.s(c, "测试提醒：走系统闹钟通道发通知（不弹应用页面、不记账），可锁屏验证铃声和亮屏",
                    "测试提醒喵：走系统闹钟通道发通知（不弹页面、不记账），可以锁屏试试铃声～",
                    "Test alert via the system alarm channel (no page, no logging); lock the screen to check the sound");
        } else if (repeatNo > 0) {
            text = Tone.s(c,
                    "还没吃？第 " + repeatNo + " 次提醒（" + m.meal + "）  计划时间 " + time,
                    "还没吃喵？第 " + repeatNo + " 次提醒（" + m.meal + "）  计划 " + time,
                    "Not taken yet? Reminder #" + repeatNo + " (" + Lang.s(c, m.meal) + ")  scheduled " + time);
        } else {
            text = Tone.s(c,
                    "该吃药了（" + m.meal + "）  计划时间 " + time,
                    "该吃药啦喵（" + m.meal + "）  计划 " + time,
                    "Time for medicine (" + Lang.s(c, m.meal) + ")  scheduled " + time);
        }

        boolean silent = RemindPrefs.silentOn(c);
        Notification.Builder b;
        if (Build.VERSION.SDK_INT >= 26) {
            String ch = silent ? silentChannel(c) : channelId(c);
            b = new Notification.Builder(c, ch);
        } else {
            b = new Notification.Builder(c);
            if (!silent) {
                Uri sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
                b.setSound(sound);
                b.setVibrate(new long[]{0, 600, 300, 600, 300, 900});
            } else {
                b.setSound(null);
                b.setVibrate(null);
            }
            b.setPriority(Notification.PRIORITY_MAX);
        }
        b.setSmallIcon(R.drawable.ic_notify)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new Notification.BigTextStyle().bigText(title + "\n" + text))
                .setContentIntent(fullPi)
                .setAutoCancel(true)
                .setCategory(mode == RemindPrefs.MODE_ALARM
                        ? Notification.CATEGORY_ALARM : Notification.CATEGORY_REMINDER)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setShowWhen(true)
                .addAction(R.drawable.ic_notify, Tone.takenAction(c), takenPi)
                .addAction(R.drawable.ic_notify, Tone.snoozeAction(c), snoozePi);

        if (mode == RemindPrefs.MODE_ALARM) {
            b.setFullScreenIntent(fullPi, true);
            b.setVisibility(Notification.VISIBILITY_PUBLIC);
        }
        nm.notify(id, b.build());
        EventLog.add(c, "已发通知：" + m.name + "（" + (mode == RemindPrefs.MODE_ALARM
                ? "全屏闹钟" : mode == RemindPrefs.MODE_HEADS ? "悬浮" : "静音") + "）");
        if (!test && mode == RemindPrefs.MODE_ALARM) {
            // 闹钟式才自动弹全屏页；悬浮弹窗只出横幅
            AlarmActivity.scheduleAutoPopup(c, m.id, index, false);
        }
    }

    public static void cancel(Context c, long medId, int index) {
        NotificationManager nm =
                (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) nm.cancel(notifId(medId, index));
    }
}
