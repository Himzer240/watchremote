package com.doubao.watchremote;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class TakenReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        long medId = intent.getLongExtra("medId", -1);
        int index = intent.getIntExtra("index", 0);
        if (medId != Scheduler.TEST_ID) {
            MedStore.markTaken(context, medId);
            for (int i = 0; i <= 6; i++) Scheduler.cancelRepeat(context, medId, i);
        }
        NotificationHelper.cancel(context, medId, index);
    }
}
