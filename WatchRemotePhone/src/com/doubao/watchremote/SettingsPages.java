package com.doubao.watchremote;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.widget.Toast;

/** 三个系统设置入口的公共跳转逻辑 */
public class SettingsPages {

    public static void openAppInfo(Activity a) {
        try {
            Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            i.setData(Uri.parse("package:" + a.getPackageName()));
            a.startActivity(i);
        } catch (Exception e) {
            try {
                a.startActivity(new Intent(Settings.ACTION_SETTINGS));
            } catch (Exception ignored) {
            }
        }
    }

    public static void openChannel(Activity a) {
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                Intent i = new Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS);
                i.putExtra(Settings.EXTRA_APP_PACKAGE, a.getPackageName());
                i.putExtra(Settings.EXTRA_CHANNEL_ID, NotificationHelper.channelId(a));
                a.startActivity(i);
            } else {
                Intent i = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                i.putExtra(Settings.EXTRA_APP_PACKAGE, a.getPackageName());
                a.startActivity(i);
            }
        } catch (Exception e) {
            openAppInfo(a);
        }
    }

    public static void openFullScreenIntent(Activity a) {
        if (Build.VERSION.SDK_INT < 34) {
            Loc.toast(a, "当前系统（Android " + Build.VERSION.SDK_INT
                    + "）不需要单独开全屏通知，锁屏提醒由通知类别里的悬浮/横幅控制", Toast.LENGTH_LONG).show();
            return;
        }
        android.app.NotificationManager nm =
                (android.app.NotificationManager) a.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null && nm.canUseFullScreenIntent()) {
            Loc.toast(a, "全屏通知已经允许，锁屏时会直接弹出提醒页", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            Intent i = new Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT);
            i.setData(Uri.parse("package:" + a.getPackageName()));
            if (a.getPackageManager().resolveActivity(i, 0) == null) {
                throw new Exception("no activity");
            }
            a.startActivity(i);
        } catch (Exception e) {
            Loc.toast(a, "这台手机的系统里没有“全屏通知”开关，一般可以直接用；"
                    + "锁屏能不能弹出，取决于通知类别和系统的锁屏通知设置", Toast.LENGTH_LONG).show();
        }
    }
}
