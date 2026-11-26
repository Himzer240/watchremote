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
        // 所有手势都从屏幕正中间起手，不碰屏幕边缘，免得被系统当成返回手势直接退出
        float cx = w / 2f;
        float cy = h / 2f;
        float dy = h * 0.30f;
        float dx = w * 0.32f;
        switch (act) {
            case "up":      // 手指往上滑 = 内容往下翻
                swipe(cx, cy, cx, cy - dy);
                break;
            case "down":    // 手指往下滑 = 内容往上翻
                swipe(cx, cy, cx, cy + dy);
                break;
            case "left":    // 从中间往左滑
                swipe(cx, cy, cx - dx, cy);
                break;
            case "right":   // 从中间往右滑
                swipe(cx, cy, cx + dx, cy);
                break;
            case "top":     // 从中间连续两次上滑到顶
                swipe(cx, cy, cx, cy - dy);
                main.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        swipe(cx, cy, cx, cy - dy);
                    }
                }, 220);
                break;
            case "back":
                performGlobalAction(GLOBAL_ACTION_BACK);
                break;
            default:
                break;
        }
    }

    /** 手表手势：方向 + 长度（0.35~1），一律从手机屏幕正中间起手 */
    public static void gesture(String dir, float len) {
        final RemoteService s = inst;
        if (s == null || dir == null) return;
        if (len < 0.3f) len = 0.3f;
        if (len > 1f) len = 1f;
        final float L = len;
        s.main.post(new Runnable() {
            @Override
            public void run() {
                DisplayMetrics dm = s.getResources().getDisplayMetrics();
                float w = dm.widthPixels;
                float h = dm.heightPixels;
                float cx = w / 2f, cy = h / 2f;
                float dy = h * 0.45f * L;
                float dx = w * 0.45f * L;
                switch (dir) {
                    case "up":    s.swipe(cx, cy, cx, cy - dy, 300); break;
                    case "down":  s.swipe(cx, cy, cx, cy + dy, 300); break;
                    case "left":  s.swipe(cx, cy, cx - dx, cy, 300); break;
                    case "right": s.swipe(cx, cy, cx + dx, cy, 300); break;
                    default: break;
                }
            }
        });
    }

    /** 点按 = 手机屏幕正中间点一下 */
    public static void tap() {
        final RemoteService s = inst;
        if (s == null) return;
        s.main.post(new Runnable() {
            @Override
            public void run() {
                DisplayMetrics dm = s.getResources().getDisplayMetrics();
                float cx = dm.widthPixels / 2f;
                float cy = dm.heightPixels / 2f;
                s.swipe(cx, cy, cx + 1f, cy, 70);
            }
        });
    }

    /** 手表原样发来的坐标：nx/ny 是 0~1 比例，按手机屏幕尺寸还原 */
    public static void point(String act, float nx1, float ny1, float nx2, float ny2) {
        final RemoteService s = inst;
        if (s == null) return;
        s.main.post(new Runnable() {
            @Override
            public void run() {
                DisplayMetrics dm = s.getResources().getDisplayMetrics();
                float w = dm.widthPixels;
                float h = dm.heightPixels;
                float x1 = clamp(nx1, w), y1 = clamp(ny1, h);
                float x2 = clamp(nx2, w), y2 = clamp(ny2, h);
                if ("tap".equals(act)) {
                    s.swipe(x1, y1, x1 + 1f, y1, 70);
                } else {
                    s.swipe(x1, y1, x2, y2, 300);
                }
            }
        });
    }

    private static float clamp(float ratio, float max) {
        if (ratio < 0.06f) ratio = 0.06f;
        if (ratio > 0.94f) ratio = 0.94f;
        return ratio * max;
    }

    private void swipe(float x1, float y1, float x2, float y2) {
        Path p = new Path();
        p.moveTo(x1, y1);
        p.lineTo(x2, y2);
        GestureDescription.Builder b = new GestureDescription.Builder();
        b.addStroke(new GestureDescription.StrokeDescription(p, 0, 260));
        dispatch(b);
    }

    private void swipe(float x1, float y1, float x2, float y2, int ms) {
        Path p = new Path();
        p.moveTo(x1, y1);
        p.lineTo(x2, y2);
        GestureDescription.Builder b = new GestureDescription.Builder();
        b.addStroke(new GestureDescription.StrokeDescription(p, 0, ms));
        dispatch(b);
    }

    private void dispatch(GestureDescription.Builder b) {
        try {
            dispatchGesture(b.build(), null, null);
        } catch (Throwable ignored) {
        }
    }
}
