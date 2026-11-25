package com.doubao.watchremote;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Intent;
import android.graphics.Path;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.accessibility.AccessibilityEvent;

/**
 * 无障碍服务：手表发来的滑动/返回指令，在这里变成手机上的真实操作。
 * 只做手势，不读屏幕内容。
 */
public class RemoteService extends AccessibilityService {

    private static volatile RemoteService inst;
    private final Handler main = new Handler(Looper.getMainLooper());

    public static boolean alive() {
        return inst != null;
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        inst = this;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
    }

    @Override
    public void onInterrupt() {
    }

    @Override
    public void onDestroy() {
        if (inst == this) inst = null;
        super.onDestroy();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        if (inst == this) inst = null;
        return super.onUnbind(intent);
    }

    /** 手表端调用：up 下翻 / down 上翻 / left 返回 / right 下一页 / top 回顶部 */
    public static void perform(String act) {
        final RemoteService s = inst;
        if (s == null || act == null) return;
        s.main.post(new Runnable() {
            @Override
            public void run() {
                s.exec(act);
            }
        });
    }

    private void exec(String act) {
        DisplayMetrics dm = getResources().getDisplayMetrics();
        int w = dm.widthPixels;
        int h = dm.heightPixels;
        switch (act) {
            case "up":      // 手指往上滑 = 内容往下翻
                swipe(w / 2, h * 0.72f, w / 2, h * 0.28f);
                break;
            case "down":    // 手指往下滑 = 内容往上翻
                swipe(w / 2, h * 0.28f, w / 2, h * 0.72f);
                break;
            case "right":   // 往左滑 = 翻到下一页
                swipe(w * 0.84f, h / 2f, w * 0.16f, h / 2f);
                break;
            case "left":    // 返回上一页
                performGlobalAction(GLOBAL_ACTION_BACK);
                break;
            case "top":     // 连滑两次到顶
                swipe(w / 2, h * 0.7f, w / 2, h * 0.2f);
                main.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        swipe(w / 2, h * 0.7f, w / 2, h * 0.2f);
                    }
                }, 220);
                break;
            default:
                break;
        }
    }

    private void swipe(float x1, float y1, float x2, float y2) {
        Path p = new Path();
        p.moveTo(x1, y1);
        p.lineTo(x2, y2);
        GestureDescription.Builder b = new GestureDescription.Builder();
        b.addStroke(new GestureDescription.StrokeDescription(p, 0, 260));
        try {
            dispatchGesture(b.build(), null, null);
        } catch (Throwable ignored) {
        }
    }
}
