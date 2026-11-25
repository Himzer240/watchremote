package com.doubao.watchremote;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class Scheduler {

    public static final String ACTION_REMIND = "com.doubao.watchremote.REMIND";
    public static final String ACTION_TAKEN = "com.doubao.watchremote.TAKEN";
    public static final String ACTION_SNOOZE = "com.doubao.watchremote.SNOOZE";
    public static final long TEST_ID = 900001L;

    public static int requestCode(long medId, int index) {
        return (int) (medId % 1000000L) * 10 + index;
    }

    public static PendingIntent alarmIntent(Context c, long medId, int index) {
        Intent i = new Intent(c, ReminderReceiver.class);
        i.setAction(ACTION_REMIND);
        i.putExtra("medId", medId);
        i.putExtra("index", index);
        return PendingIntent.getBroadcast(c, requestCode(medId, index), i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    public static void schedule(Context c, Medicine m, int index) {
        if (index < 0 || index >= m.times.size()) return;
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        int[] t = m.times.get(index);
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, t[0]);
        cal.set(Calendar.MINUTE, t[1]);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        // 规则：定的时间已经过了就从明天开始；还没到就从今天开始
        if (cal.getTimeInMillis() <= System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_YEAR, 1);
            EventLog.add(c, "这个时间今天已经过了，从明天开始：" + m.name
                    + " #" + (index + 1));
        }
        long when = cal.getTimeInMillis();
        PendingIntent pi = alarmIntent(c, m.id, index);
        Intent show = new Intent(c, AlarmActivity.class);
        show.putExtra("medId", m.id);
        show.putExtra("index", index);
        show.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent showPi = PendingIntent.getActivity(c, 900000 + requestCode(m.id, index), show,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        int mode = RemindPrefs.staticModeFor(c, m, index);
        EventLog.add(c, "排定 " + m.name + " #" + (index + 1) + " → "
                + new java.text.SimpleDateFormat("MM-dd HH:mm", Locale.CHINA)
                        .format(new java.util.Date(when))
                + (am.canScheduleExactAlarms() ? " · 精确" : " · 非精确（未允许准时提醒）")
                + " · " + (mode == RemindPrefs.MODE_ALARM ? "全屏闹钟"
                        : mode == RemindPrefs.MODE_HEADS ? "悬浮" : "不提醒"));
        try {
            if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, pi);
            } else if (mode == RemindPrefs.MODE_ALARM) {
                // 闹钟式：系统闹钟通道，Doze 下也能准时唤醒，状态栏出现闹钟标记
                am.setAlarmClock(new AlarmManager.AlarmClockInfo(when, showPi), pi);
            } else {
                // 悬浮弹窗 / 不提醒：用精确闹钟即可，不当系统闹钟
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, pi);
            }
        } catch (Exception e) {
            EventLog.add(c, "排定失败(" + m.name + " #" + (index + 1) + ")：" + e);
            try {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, pi);
            } catch (Exception e2) {
                EventLog.add(c, "退化为非精确闹钟也失败：" + e2);
                try {
                    am.set(AlarmManager.RTC_WAKEUP, when, pi);
                } catch (Exception e3) {
                    EventLog.add(c, "普通闹钟也失败：" + e3);
                }
            }
        }
    }

    public static PendingIntent repeatIntent(Context c, long medId, int index, long since, int attempt) {
        Intent i = new Intent(c, ReminderReceiver.class);
        i.setAction(ACTION_REMIND);
        i.putExtra("medId", medId);
        i.putExtra("index", index);
        i.putExtra("repeat", attempt);
        i.putExtra("since", since);
        return PendingIntent.getBroadcast(c, 700000 + requestCode(medId, index), i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    /** 超时没吃：隔 N 分钟再提醒一次，最多 24 次 */
    public static void scheduleRepeat(Context c, Medicine m, int index, long since, int attempt) {
        int mins = RemindPrefs.repeatMinutes(c);
        if (mins <= 0 || attempt > 24) return;
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        long when = System.currentTimeMillis() + mins * 60000L;
        PendingIntent pi = repeatIntent(c, m.id, index, since, attempt);
        try {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, pi);
        } catch (Exception e) {
            try {
                am.set(AlarmManager.RTC_WAKEUP, when, pi);
            } catch (Exception ignored) {
            }
        }
    }

    public static void cancelRepeat(Context c, long medId, int index) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        try {
            am.cancel(repeatIntent(c, medId, index, 0, 0));
        } catch (Exception ignored) {
        }
    }

    public static void cancelMedicine(Context c, Medicine m) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        for (int i = 0; i < m.times.size(); i++) {
            am.cancel(alarmIntent(c, m.id, i));
            cancelRepeat(c, m.id, i);
        }
    }

    public static void rescheduleAll(Context c) {
        List<Medicine> list = MedStore.load(c);
        for (Medicine m : list) {
            boolean on = m.enabled && WatchFlags.stateOf(c, m.id) == WatchFlags.NORMAL;
            for (int i = 0; i < m.times.size(); i++) {
                if (on) {
                    schedule(c, m, i);
                } else {
                    AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
                    if (am != null) am.cancel(alarmIntent(c, m.id, i));
                }
            }
        }
    }

    public static void snooze(Context c, long medId, int index) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        long when = System.currentTimeMillis() + 10 * 60 * 1000L;
        Intent i = new Intent(c, ReminderReceiver.class);
        i.setAction(ACTION_REMIND);
        i.putExtra("medId", medId);
        i.putExtra("index", index);
        PendingIntent pi = PendingIntent.getBroadcast(c, 777000 + requestCode(medId, index), i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        try {
            if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, pi);
            } else if (Build.VERSION.SDK_INT >= 23) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, pi);
            } else {
                am.setExact(AlarmManager.RTC_WAKEUP, when, pi);
            }
        } catch (Exception e) {
            am.set(AlarmManager.RTC_WAKEUP, when, pi);
        }
    }

    /** 5 秒后触发一次测试提醒：用虚拟药品，不会写入任何服药记录 */
    public static void testNow(Context c, long delayMs) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        long when = System.currentTimeMillis() + delayMs;
        Intent i = new Intent(c, ReminderReceiver.class);
        i.setAction(ACTION_REMIND);
        i.putExtra("medId", TEST_ID);
        i.putExtra("index", 0);
        i.putExtra("test", true);
        PendingIntent pi = PendingIntent.getBroadcast(c, 888001, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Intent show = new Intent(c, AlarmActivity.class);
        show.putExtra("medId", TEST_ID);
        show.putExtra("index", 0);
        show.putExtra("test", true);
        show.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent showPi = PendingIntent.getActivity(c, 888002, show,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        try {
            // 测试也走系统闹钟通道，和正式提醒同一条链路
            am.setAlarmClock(new AlarmManager.AlarmClockInfo(when, showPi), pi);
        } catch (Exception e) {
            try {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, pi);
            } catch (Exception e2) {
                am.set(AlarmManager.RTC_WAKEUP, when, pi);
            }
        }
    }

    /** 返回下一次提醒的毫秒时间戳和描述，没有则返回 null */
    public static long[] nextUpcoming(Context c) {
        long best = Long.MAX_VALUE;
        long bestMed = -1;
        int bestIndex = -1;
        List<Medicine> list = MedStore.load(c);
        for (Medicine m : list) {
            if (!m.enabled) continue;
            for (int i = 0; i < m.times.size(); i++) {
                if (RemindPrefs.staticModeFor(c, m, i) == RemindPrefs.MODE_SILENT) continue;
                int[] t = m.times.get(i);
                Calendar cal = Calendar.getInstance();
                cal.set(Calendar.HOUR_OF_DAY, t[0]);
                cal.set(Calendar.MINUTE, t[1]);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                if (cal.getTimeInMillis() <= System.currentTimeMillis()) {
                    cal.add(Calendar.DAY_OF_YEAR, 1);
                }
                if (cal.getTimeInMillis() < best) {
                    best = cal.getTimeInMillis();
                    bestMed = m.id;
                    bestIndex = i;
                }
            }
        }
        if (bestMed < 0) return null;
        return new long[]{best, bestMed, bestIndex};
    }

    public static String formatWhen(long millis) {
        Calendar now = Calendar.getInstance();
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(millis);
        Calendar today = Calendar.getInstance();
        String hm = String.format(Locale.CHINA, "%02d:%02d",
                cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE));
        Calendar dayStart = (Calendar) today.clone();
        dayStart.set(Calendar.HOUR_OF_DAY, 0);
        dayStart.set(Calendar.MINUTE, 0);
        dayStart.set(Calendar.SECOND, 0);
        dayStart.set(Calendar.MILLISECOND, 0);
        if (cal.getTimeInMillis() < dayStart.getTimeInMillis() + 86400000L) {
            return "今天 " + hm;
        }
        if (cal.getTimeInMillis() < dayStart.getTimeInMillis() + 2 * 86400000L) {
            return "明天 " + hm;
        }
        return "后天 " + hm;
    }
}
