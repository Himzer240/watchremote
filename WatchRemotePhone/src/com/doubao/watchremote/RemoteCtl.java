package com.doubao.watchremote;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;

/** 手表遥控手机界面：滑动/点按映射为滚动、翻页、返回 */
public class RemoteCtl {

    public static void apply(final Activity a, final String act) {
        if (a == null || a.isFinishing()) return;
        a.runOnUiThread(new Runnable() {
            @Override public void run() {
                String msg;
                if ("up".equals(act) || "down".equals(act)) {
                    ScrollView sv = findScroll(a.getWindow().getDecorView());
                    if (sv != null) {
                        sv.smoothScrollBy(0, "up".equals(act) ? 420 : -420);
                    }
                    msg = "up".equals(act) ? "手表遥控：往下翻" : "手表遥控：往上翻";
                } else if ("left".equals(act)) {
                    if (!(a instanceof MainActivity)) {
                        a.finish();
                        msg = "手表遥控：返回";
                    } else {
                        msg = "手表遥控：已在主页";
                    }
                } else if ("right".equals(act)) {
                    Intent i;
                    if (a instanceof MainActivity) i = new Intent(a, SettingsActivity.class);
                    else if (a instanceof SettingsActivity) i = new Intent(a, WatchActivity.class);
                    else i = new Intent(a, MainActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    a.startActivity(i);
                    msg = "手表遥控：下一页";
                } else if ("top".equals(act)) {
                    ScrollView sv = findScroll(a.getWindow().getDecorView());
                    if (sv != null) sv.smoothScrollTo(0, 0);
                    msg = "手表遥控：回顶部";
                } else {
                    msg = "手表遥控：" + act;
                }
                Loc.toast(a, msg, android.widget.Toast.LENGTH_SHORT).show();
            }
        });
    }

    private static ScrollView findScroll(View v) {
        if (v instanceof ScrollView) return (ScrollView) v;
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) {
                ScrollView r = findScroll(g.getChildAt(i));
                if (r != null) return r;
            }
        }
        return null;
    }
}
