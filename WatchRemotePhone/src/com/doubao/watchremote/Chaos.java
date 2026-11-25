package com.doubao.watchremote;

import android.app.Activity;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;

import java.util.Random;

/**
 * 混乱模式：只存在这一次运行里。图标不变，但点进去做的事会随机；页面背景颜色一直变。
 * 把应用从后台删掉（进程被杀）后这个开关就自动回到关闭。
 */
public class Chaos {

    private static boolean on = false;
    /** 三种混乱方式：0 只乱功能 / 1 只乱动 / 2 又乱功能又乱动 */
    public static final int STYLE_ACTIONS = 0;
    public static final int STYLE_MOVE = 1;
    public static final int STYLE_BOTH = 2;
    private static int style = STYLE_ACTIONS;
    private static final java.util.ArrayList<android.animation.ValueAnimator> ANIMS = new java.util.ArrayList<>();
    private static final Random R = new Random();
    private static final Handler H = new Handler(Looper.getMainLooper());
    private static Runnable ticker;
    private static float hue = 0.55f;

    public static boolean on() {
        return on;
    }

    public static void set(boolean v) {
        on = v;
        if (v) {
            style = R.nextInt(3);   // 每次开都不一样
        } else {
            stopBg();
            stopMove();
        }
    }

    public static int style() {
        return style;
    }

    /** 功能随机 */
    public static boolean actions() {
        return on && (style == STYLE_ACTIONS || style == STYLE_BOTH);
    }

    /** 元素乱动 */
    public static boolean moves() {
        return on && (style == STYLE_MOVE || style == STYLE_BOTH);
    }

    public static int pick(int n) {
        return n <= 0 ? 0 : R.nextInt(n);
    }

    public static boolean chance(double p) {
        return R.nextDouble() < p;
    }

    private static float nextHue() {
        hue = (hue + 0.07f) % 1f;
        return hue;
    }

    /** 页面底色一直变色（已停用：用户觉得太花） */
    public static void startBg(Activity a) {
        if (true) return;
        if (!on || a == null) return;
        stopBg();
        final Activity act = a;
        ticker = new Runnable() {
            @Override
            public void run() {
                try {
                    View root = act.findViewById(android.R.id.content);
                    if (root instanceof ViewGroup) {
                        View child = ((ViewGroup) root).getChildAt(0);
                        if (child != null) {
                            int c = Color.HSVToColor(new float[]{nextHue(), 0.30f, 1f});
                            child.setBackgroundColor(c);
                        }
                    }
                } catch (Throwable ignored) {
                }
                H.postDelayed(this, 820);
            }
        };
        H.post(ticker);
    }

    public static void stopBg() {
        if (ticker != null) H.removeCallbacks(ticker);
        ticker = null;
    }

    /** 所有元素轻轻乱动：按钮照样能点，内容不变形 */
    public static void startMove(final Activity a) {
        if (!moves() || a == null) return;
        stopMove();
        final View root = a.findViewById(android.R.id.content);
        if (root == null) return;
        root.post(new Runnable() {
            @Override
            public void run() {
                walk(root, 0);
            }
        });
    }

    private static void walk(View v, int depth) {
        if (v.getVisibility() != View.VISIBLE) return;
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            // 根容器不动，避免布局跳动
            if (depth > 0) maybeShake(v);
            for (int i = 0; i < g.getChildCount(); i++) walk(g.getChildAt(i), depth + 1);
        } else {
            maybeShake(v);
        }
    }

    private static void maybeShake(View v) {
        if (!moves()) return;
        float d = v.getResources().getDisplayMetrics().density;
        final float ampX = (2.5f + R.nextInt(4)) * d;
        final float ampY = (1.5f + R.nextInt(3)) * d;
        final float rot = 1.2f + R.nextInt(3);
        final float phase = (float) (R.nextDouble() * Math.PI * 2);
        final float speed = 1 + R.nextInt(2);
        android.animation.ValueAnimator va =
                android.animation.ValueAnimator.ofFloat(0f, (float) (Math.PI * 4));
        va.setDuration(1100 + R.nextInt(900));
        va.setRepeatCount(android.animation.ValueAnimator.INFINITE);
        va.setInterpolator(new android.view.animation.LinearInterpolator());
        va.addUpdateListener(new android.animation.ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(android.animation.ValueAnimator a) {
                float t = (Float) a.getAnimatedValue();
                v.setTranslationX(ampX * (float) Math.sin(t + phase));
                v.setTranslationY(ampY * (float) Math.cos(t * speed + phase));
                v.setRotation(rot * (float) Math.sin(t * 1.4f + phase));
            }
        });
        va.start();
        ANIMS.add(va);
    }

    public static void stopMove() {
        for (android.animation.ValueAnimator va : ANIMS) {
            try {
                va.cancel();
            } catch (Throwable ignored) {
            }
        }
        ANIMS.clear();
    }

    /** 这次开的是哪种混乱方式（给提示用） */
    public static String styleName(android.content.Context c) {
        if (style == STYLE_MOVE) return "满屏乱动";
        if (style == STYLE_BOTH) return "又乱跳又乱动";
        return "功能乱跳";
    }
}
