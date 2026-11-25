package com.doubao.watchremote;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

public class AlarmActivity extends Activity {

    private long medId = -1;
    private int index = 0;
    private boolean testMode = false;
    private MediaPlayer player;
    private Vibrator vibrator;
    public static boolean visible = false;

    public static void scheduleAutoPopup(final Context c, final long medId, final int index,
                                         final boolean test) {
        Handler h = new Handler(Looper.getMainLooper());
        Runnable r = new Runnable() {
            @Override
            public void run() {
                if (visible) return;
                try {
                    Intent i = new Intent(c, AlarmActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    i.putExtra("medId", medId);
                    i.putExtra("index", index);
                    i.putExtra("test", test);
                    c.startActivity(i);
                } catch (Exception ignored) {
                }
            }
        };
        h.post(r);
        h.postDelayed(r, 800);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        }
        setContentView(R.layout.activity_alarm);
        AppTheme.apply(this);
        EventLog.add(this, "打开了全屏提醒页");

        Button btnTaken = findViewById(R.id.btnTaken);
        Button btnSnooze = findViewById(R.id.btnSnooze);
        Button btnClose = findViewById(R.id.btnClose);

        btnTaken.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!testMode) {
                    MedStore.markTaken(AlarmActivity.this, medId);
                }
                NotificationHelper.cancel(AlarmActivity.this, medId, index);
                finish();
            }
        });
        btnSnooze.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Scheduler.snooze(AlarmActivity.this, medId, index);
                NotificationHelper.cancel(AlarmActivity.this, medId, index);
                finish();
            }
        });
        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        bind(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        bind(intent);
    }

    private void bind(Intent intent) {
        medId = intent.getLongExtra("medId", -1);
        index = intent.getIntExtra("index", 0);
        testMode = intent.getBooleanExtra("test", false) || medId == Scheduler.TEST_ID;

        Medicine m;
        if (testMode) {
            m = new Medicine();
            m.id = Scheduler.TEST_ID;
            m.name = "测试提醒";
            m.dose = "1 粒";
            m.meal = "测试";
            java.util.Calendar c = java.util.Calendar.getInstance();
            m.times.add(new int[]{c.get(java.util.Calendar.HOUR_OF_DAY),
                    c.get(java.util.Calendar.MINUTE)});
        } else {
            m = MedStore.find(this, medId);
        }
        if (m == null) {
            finish();
            return;
        }
        NotificationHelper.cancel(this, medId, index);

        ((TextView) findViewById(R.id.alarmName)).setText(m.name);
        if (testMode) {
            ((TextView) findViewById(R.id.alarmDetail))
                    .setText(Tone.s(this, "这是一条测试提醒，点“已服用”不会增加服药记录",
                            "这是一条测试提醒喵，点“已服用”不会记账～",
                            "This is a test alert; tapping “Taken” does not log a dose"));
            ((TextView) findViewById(R.id.alarmTime)).setText(Tone.s(this,
                    "测试时间 " + m.timeAt(0), "测试时间喵 " + m.timeAt(0),
                    "Test time " + m.timeAt(0)));
        } else {
            boolean en = Tone.style(this) == Tone.EN;
            String d = en ? ("Each " + m.dose + " · " + Lang.s(this, m.meal))
                    : ("每次 " + m.dose + " · " + m.meal + "服用");
            if (m.purpose != null && !m.purpose.isEmpty()) d += "\n" + m.purpose;
            ((TextView) findViewById(R.id.alarmDetail)).setText(d);
            ((TextView) findViewById(R.id.alarmTime))
                    .setText(Tone.s(this, "计划时间 " + m.timeAt(index),
                            "计划时间喵 " + m.timeAt(index), "Plan time " + m.timeAt(index)));
        }

        startAlarm();
    }

    private void startAlarm() {
        stopAlarm();
        boolean quiet = RemindPrefs.silentOn(this);
        if (quiet) return;   // 静音模式：页面照常弹出，但不响铃不震动
        try {
            Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (uri == null) uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            player = new MediaPlayer();
            player.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build());
            player.setDataSource(this, uri);
            player.setLooping(true);
            player.prepare();
            player.start();
        } catch (Exception e) {
            player = null;
        }
        try {
            vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                long[] pattern = new long[]{0, 600, 300, 600, 300, 900};
                if (Build.VERSION.SDK_INT >= 26) {
                    vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0));
                } else {
                    vibrator.vibrate(pattern, 0);
                }
            }
        } catch (Exception ignored) {
        }
    }

    private void stopAlarm() {
        if (player != null) {
            try {
                player.stop();
                player.release();
            } catch (Exception ignored) {
            }
            player = null;
        }
        if (vibrator != null) {
            try {
                vibrator.cancel();
            } catch (Exception ignored) {
            }
            vibrator = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        visible = true;
        if (Build.VERSION.SDK_INT >= 27) {
            try {
                android.app.KeyguardManager km = (android.app.KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
                if (km != null && km.isKeyguardLocked()) {
                    km.requestDismissKeyguard(this, null);
                }
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        visible = false;
        stopAlarm();
    }

    @Override
    protected void onDestroy() {
        stopAlarm();
        super.onDestroy();
    }
}
