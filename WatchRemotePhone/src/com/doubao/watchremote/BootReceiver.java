package com.doubao.watchremote;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        NotificationHelper.ensureChannel(context);
        Scheduler.rescheduleAll(context);
    }
}
