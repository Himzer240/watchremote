package com.doubao.watchremote;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** 提醒诊断：把“能不能提醒”的每一道关卡列出来，并给出测试与重排 */
public class DiagnosticsActivity extends Activity {

    private TextView status;
    private TextView log;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_diag);
        AppTheme.apply(this);
        status = findViewById(R.id.diagStatus);
        log = findViewById(R.id.diagLog);

        findViewById(R.id.btnBack).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        findViewById(R.id.btnReschedule).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Scheduler.rescheduleAll(DiagnosticsActivity.this);
                EventLog.add(DiagnosticsActivity.this, "手动重排了全部提醒");
                Loc.toast(DiagnosticsActivity.this, "已重新排定", Toast.LENGTH_SHORT).show();
                refresh();
            }
        });
        findViewById(R.id.btnTest).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Scheduler.testNow(DiagnosticsActivity.this, 10000);
                EventLog.add(DiagnosticsActivity.this, "手动测试：10 秒后触发");
                Loc.toast(DiagnosticsActivity.this, "10 秒后测试，等一下", Toast.LENGTH_SHORT).show();
                refresh();
            }
        });
        findViewById(R.id.btnClearLog).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EventLog.clear(DiagnosticsActivity.this);
                refresh();
            }
        });
        findViewById(R.id.btnNotifySettings).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SettingsPages.openAppInfo(DiagnosticsActivity.this);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private String L(String zh, String en) {
        return Tone.style(this) == Tone.EN ? en : zh;
    }

    private void refresh() {
        StringBuilder sb = new StringBuilder();
        {
            boolean perm = true;
            if (Build.VERSION.SDK_INT >= 33) {
                perm = checkSelfPermission("android.permission.POST_NOTIFICATIONS")
                        == PackageManager.PERMISSION_GRANTED;
            }
            android.app.NotificationManager nm =
                    (android.app.NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            boolean on = nm != null && nm.areNotificationsEnabled();
            sb.append(perm && on ? "✅" : "❌").append(L(" 通知权限：", " Notifications: "))
                    .append(L(perm ? "已允许" : "未允许", perm ? "allowed" : "not allowed"))
                    .append(L("，系统通知开关：", ", system toggle: "))
                    .append(L(on ? "开" : "关", on ? "on" : "off")).append("\n");
            String ch = NotificationHelper.channelId(this);
            sb.append(L("ℹ️ 正在使用的通知类别：", "ℹ️ Channel in use: ")).append(ch).append("\n");
            if (Build.VERSION.SDK_INT >= 26 && nm != null) {
                android.app.NotificationChannel c = nm.getNotificationChannel(ch);
                if (c == null) {
                    sb.append(L("❌ 这个类别在系统里不存在（可能被用户删了）\n",
                            "❌ This channel does not exist (maybe deleted)\n"));
                } else {
                    int imp = c.getImportance();
                    boolean none = imp == android.app.NotificationManager.IMPORTANCE_NONE;
                    String impText = L(none ? "关闭（不会响）"
                            : imp >= android.app.NotificationManager.IMPORTANCE_HIGH ? "高（可横幅/悬浮）"
                            : "中/低（可能没有横幅）",
                            none ? "off (silent)"
                            : imp >= android.app.NotificationManager.IMPORTANCE_HIGH ? "high (banner/pop-up)"
                            : "medium/low (banner may be missing)");
                    sb.append(none ? "❌" : "✅").append(L(" 类别重要级：", " Channel importance: "))
                            .append(impText).append("\n");
                    sb.append(c.getSound() != null ? "✅" : "⚠️").append(L(" 类别铃声：", " Channel sound: "))
                            .append(L(c.getSound() != null ? "有" : "无", c.getSound() != null ? "yes" : "none"))
                            .append("\n");
                    sb.append(c.shouldVibrate() ? "✅" : "⚠️").append(L(" 类别震动：", " Channel vibration: "))
                            .append(L(c.shouldVibrate() ? "开" : "关", c.shouldVibrate() ? "on" : "off"))
                            .append("\n");
                }
            }
        }
        if (Build.VERSION.SDK_INT >= 31) {
            android.app.AlarmManager am =
                    (android.app.AlarmManager) getSystemService(ALARM_SERVICE);
            boolean ok = am != null && am.canScheduleExactAlarms();
            sb.append(ok ? "✅" : "❌").append(L(" 准时提醒（精确闹钟）：", " Exact alarms: "))
                    .append(L(ok ? "已允许" : "未允许，会被系统延迟甚至不触发",
                            ok ? "allowed" : "not allowed; may be delayed or not fire")).append("\n");
        }
        if (Build.VERSION.SDK_INT >= 34) {
            android.app.NotificationManager nm =
                    (android.app.NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            boolean ok = nm != null && nm.canUseFullScreenIntent();
            sb.append(ok ? "✅" : "⚠️").append(L(" 全屏通知：", " Full-screen alerts: "))
                    .append(L(ok ? "已允许" : "未允许，锁屏只会出横幅",
                            ok ? "allowed" : "not allowed; only a banner on the lock screen")).append("\n");
        }
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        boolean bat = pm != null && pm.isIgnoringBatteryOptimizations(getPackageName());
        sb.append(bat ? "✅" : "⚠️").append(L(" 电池优化白名单：", " Battery whitelist: "))
                .append(L(bat ? "已加入" : "未加入，后台可能被清",
                        bat ? "added" : "not added; may be killed in background")).append("\n");

        java.util.List<Medicine> list = MedStore.load(this);
        sb.append("\n").append(L("计划 ", "Plans: ")).append(list.size()).append(L(" 个：\n", "\n"));
        for (Medicine m : list) {
            sb.append("· ").append(m.name).append(m.enabled ? "" : L("（提醒已关闭）", " (reminders off)"))
                    .append("  ");
            sb.append(m.times.size()).append(L(" 次，已服 ", " doses, taken "))
                    .append(MedStore.takenCount(this, m.id)).append("/").append(m.doseCount());
            sb.append(L("\n  时间：", "\n  Times: "));
            for (int i = 0; i < m.times.size(); i++) {
                int[] t = m.times.get(i);
                sb.append(String.format(Locale.CHINA, "%02d:%02d", t[0], t[1]));
                int mode = RemindPrefs.modeFor(this, m, i);
                sb.append("(").append(L(mode == 0 ? "全屏闹钟" : mode == 1 ? "悬浮" : "不提醒",
                        mode == 0 ? "alarm" : mode == 1 ? "pop-up" : "silent")).append(")");
                if (i < m.times.size() - 1) sb.append(L("、", ", "));
            }
            sb.append("\n");
        }
        long[] next = Scheduler.nextUpcoming(this);
        if (next != null && next[0] > 0) {
            Medicine nm2 = MedStore.find(this, next[1]);
            sb.append(L("\n下一次：", "\nNext: ")).append(new SimpleDateFormat("MM-dd HH:mm", Locale.CHINA)
                    .format(new Date(next[0])));
            if (nm2 != null) sb.append("  ").append(nm2.name);
        } else {
            sb.append(L("\n没有排上任何提醒", "\nNothing scheduled"));
        }
        if (RemindPrefs.inDnd(this)) {
            sb.append(L("\n⚠️ 现在正处在勿扰时段，所有提醒都会被静音",
                    "\n⚠️ DND is active now; all alerts are silenced"));
        }
        status.setText(sb.toString());
        log.setText(Loc.frag(this, EventLog.dump(this)));
    }
}
