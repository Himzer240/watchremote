package com.doubao.watchremote;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;

import java.util.Calendar;

public class ReminderReceiver extends BroadcastReceiver {

    /** 息屏时强制点亮屏幕，保证用户能看到提醒 */
    public static void wakeScreen(Context c) {
        try {
            PowerManager pm = (PowerManager) c.getSystemService(Context.POWER_SERVICE);
            if (pm == null || pm.isInteractive()) return;
            @SuppressWarnings("deprecation")
            PowerManager.WakeLock wl = pm.newWakeLock(
                    PowerManager.SCREEN_BRIGHT_WAKE_LOCK
                            | PowerManager.ACQUIRE_CAUSES_WAKEUP
                            | PowerManager.ON_AFTER_RELEASE,
                    "MedReminder:wake");
            wl.setReferenceCounted(false);
            // 15 秒后自动释放；期间若提醒页弹出，会由提醒页保持亮屏
            wl.acquire(15000L);
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        long medId = intent.getLongExtra("medId", -1);
        int index = intent.getIntExtra("index", 0);
        long since = intent.getLongExtra("since", 0);
        int attempt = intent.getIntExtra("repeat", 0);
        EventLog.add(context, "闹钟触发 medId=" + medId + " #" + (index + 1)
                + (attempt > 0 ? (" 第" + (attempt + 1) + "次") : ""));

        if (medId == Scheduler.TEST_ID) {
            Medicine fake = new Medicine();
            fake.id = Scheduler.TEST_ID;
            fake.name = "测试提醒";
            fake.dose = "1 粒";
            fake.meal = "测试";
            Calendar now = Calendar.getInstance();
            fake.times.add(new int[]{now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE)});
            wakeScreen(context);
            NotificationHelper.show(context, fake, 0, true);
            return;
        }

        Medicine m = MedStore.find(context, medId);
        if (m == null) {
            EventLog.add(context, "找不到这个计划（可能已被删除），跳过");
            return;
        }
        if (!m.enabled || index < 0 || index >= m.times.size()) {
            EventLog.add(context, "这个计划已关闭或时间已改，跳过");
            return;
        }

        try {
            String hm = String.format(java.util.Locale.CHINA, "%02d:%02d", m.times.get(index)[0], m.times.get(index)[1]);
            WatchSync.remind(context, m.name, hm);
        } catch (Throwable ignored) {
        }

        // 超时重复提醒（第 2 次起）
        if (since > 0 && attempt > 0) {
            if (MedStore.takenAfter(context, m.id, since)) return;   // 已经吃了，链条停
            int rmode = RemindPrefs.modeFor(context, m, index);
            if (rmode == RemindPrefs.MODE_SILENT) return;
            wakeScreen(context);
            NotificationHelper.show(context, m, index, false, rmode, attempt);
            Scheduler.scheduleRepeat(context, m, index, since, attempt + 1);
            return;
        }

        // 今天已经吃满计划次数（比如提前用「吃过药了」补记了），这次就不提醒，只排明天
        if (MedStore.takenCount(context, m.id) >= m.doseCount()) {
            EventLog.add(context, "今天次数已吃满，本次不提醒");
            Scheduler.schedule(context, m, index);
            return;
        }

        int mode = RemindPrefs.modeFor(context, m, index);
        if (mode == RemindPrefs.MODE_SILENT) {
            // 勿扰 / 全部不提醒 / 这条时间设成不提醒：不出声、不亮屏，只排明天
            Scheduler.schedule(context, m, index);
            return;
        }

        wakeScreen(context);
        NotificationHelper.show(context, m, index, false, mode);
        // 排下一次（明天同一时间）
        Scheduler.schedule(context, m, index);
        // 超时没吃就按设置的间隔重复提醒
        Scheduler.scheduleRepeat(context, m, index, System.currentTimeMillis(), 1);
    }
}
