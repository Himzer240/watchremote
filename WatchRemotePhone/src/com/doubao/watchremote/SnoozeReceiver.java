package com.doubao.watchremote;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class SnoozeReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        long medId = intent.getLongExtra("medId", -1);
        int index = intent.getIntExtra("index", 0);
        NotificationHelper.cancel(context, medId, index);
        for (int i = 0; i <= 6; i++) Scheduler.cancelRepeat(context, medId, i);
        Scheduler.snooze(context, medId, index);
    }
}
