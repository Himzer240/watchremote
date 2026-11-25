package com.doubao.watchremote;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/** 运行时主题色：按钮填充色、强调文字、页面底色、状态栏 */
public class AppTheme {

    private static final String P = "app_prefs";
    public static final int DEFAULT = 0xFF1098AD;

    public static int[] PRESETS = {
            0xFF1098AD, 0xFF2F6FED, 0xFF12B886, 0xFFE8590C, 0xFFE5484D,
            0xFF7048E8, 0xFF0CA678, 0xFFF08C00, 0xFF495057, 0xFFC2255C,
            0xFF1971C2, 0xFF0B7285, 0xFF5F3DC4, 0xFF9C36B5, 0xFFD6336C,
            0xFFF76707, 0xFF5C940D, 0xFF2B8A3E, 0xFF0C8599, 0xFF3B5BDB,
            0xFF845EF7, 0xFFA15C00, 0xFF343A40, 0xFF2C3E50
    };

    public static int accent(Context c) {
        return c.getSharedPreferences(P, Context.MODE_PRIVATE).getInt("accent_color", DEFAULT);
    }

    public static void setAccent(Context c, int color) {
        c.getSharedPreferences(P, Context.MODE_PRIVATE).edit()
                .putInt("accent_color", 0xFF000000 | color).apply();
    }

    public static int darken(int color, float f) {
        int r = (int) (Color.red(color) * (1 - f));
        int g = (int) (Color.green(color) * (1 - f));
        int b = (int) (Color.blue(color) * (1 - f));
        return Color.rgb(r, g, b);
    }

    public static int mixWhite(int color, float whiteRatio) {
        float k = 1 - whiteRatio;
        int r = (int) (Color.red(color) * k + 255 * whiteRatio);
        int g = (int) (Color.green(color) * k + 255 * whiteRatio);
        int b = (int) (Color.blue(color) * k + 255 * whiteRatio);
        return Color.rgb(r, g, b);
    }

    public static int pageBg(Context c) {
        return mixWhite(accent(c), 0.93f);
    }

    public static GradientDrawable round(int color, float radiusDp, Context c) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radiusDp * c.getResources().getDisplayMetrics().density);
        return d;
    }

    /** 浅主题色底 + 同色描边，用在问答卡片、次级按钮上 */
    public static GradientDrawable softBordered(Context c, float radiusDp) {
        int acc = accent(c);
        GradientDrawable d = round(mixWhite(acc, 0.90f), radiusDp, c);
        d.setStroke((int) (1.2f * c.getResources().getDisplayMetrics().density),
                0x33000000 | (acc & 0x00FFFFFF));
        return d;
    }

    /** 白底 + 主题色细描边，用在答案卡片上 */
    public static GradientDrawable cardBordered(Context c, float radiusDp) {
        int acc = accent(c);
        GradientDrawable d = round(0xFFFFFFFF, radiusDp, c);
        d.setStroke((int) (1.2f * c.getResources().getDisplayMetrics().density),
                (0x4D << 24) | (acc & 0x00FFFFFF));
        return d;
    }

    public static Drawable filled(Context c) {
        int acc = accent(c);
        StateListDrawable sl = new StateListDrawable();
        sl.addState(new int[]{android.R.attr.state_pressed}, round(darken(acc, 0.14f), 14, c));
        sl.addState(new int[]{}, round(acc, 14, c));
        return sl;
    }

    /** 每次进页面调一次：状态栏、页面底色、强调色按钮和文字 */
    /** 按当前表达风格，把界面里写死的中文文案换掉（喵语 / English） */
    public static void localize(View v, Context c) {
        if (v == null || c == null) return;
        if (Tone.style(c) == Tone.FORMAL) return;
        if (v instanceof android.widget.TextView) {
            android.widget.TextView t = (android.widget.TextView) v;
            CharSequence cur = t.getText();
            if (cur != null && Lang.hasHan(cur.toString())) {
                String out = Lang.s(c, cur.toString());
                if (!out.contentEquals(cur)) t.setText(out);
            }
            if (t instanceof android.widget.EditText) {
                CharSequence hint = t.getHint();
                if (hint != null && Lang.hasHan(hint.toString())) {
                    String out = Lang.s(c, hint.toString());
                    if (!out.contentEquals(hint)) t.setHint(out);
                }
            }
        }
        if (v instanceof android.view.ViewGroup) {
            android.view.ViewGroup g = (android.view.ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) localize(g.getChildAt(i), c);
        }
    }

    /** 老年人模式：把界面里的字统一放大 */
    public static void scaleText(View v, float f) {
        if (v == null) return;
        if ("scaled".equals(v.getTag())) return;
        v.setTag("scaled");
        if (v instanceof android.widget.TextView) {
            android.widget.TextView t = (android.widget.TextView) v;
            t.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,
                    t.getTextSize() * f);
        }
        if (v instanceof android.view.ViewGroup) {
            android.view.ViewGroup g = (android.view.ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) {
                View c = g.getChildAt(i);
                if (c.getTag() != null && "scaled".equals(c.getTag())) {
                    // 已经处理过的子控件跳过
                }
                scaleText(c, f);
            }
        }
    }

    public static void apply(Activity a) {
        int acc = accent(a);
        int bg = pageBg(a);
        try {
            a.getWindow().setStatusBarColor(bg);
            a.getWindow().setNavigationBarColor(bg);
        } catch (Throwable ignored) {
        }
        View root = a.findViewById(android.R.id.content);
        paint(root, a, acc, bg);
        if (Chaos.moves()) Chaos.startMove(a);
    }

    private static void paint(View v, Context c, int acc, int bg) {
        Object tag = v.getTag();
        if (tag instanceof String) {
            String t = (String) tag;
            if ("accentBg".equals(t)) {
                v.setBackgroundDrawable(filled(c));
            } else if ("accentText".equals(t) && v instanceof TextView) {
                ((TextView) v).setTextColor(acc);
            } else if ("accentSoft".equals(t)) {
                v.setBackgroundDrawable(round(mixWhite(acc, 0.88f), 14, c));
            } else if ("pageBg".equals(t)) {
                v.setBackgroundColor(bg);
            }
        }
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) paint(g.getChildAt(i), c, acc, bg);
        }
    }

    public static String hex(int color) {
        return String.format("#%06X", 0xFFFFFF & color);
    }

    /** 弹窗按钮：往两边拉开一点距离；dangerPositive=true 时确认按钮用红色 */
    public static void styleDialogButtons(android.app.AlertDialog d, boolean dangerPositive) {
        if (d == null) return;
        try {
            android.widget.Button pos = d.getButton(android.app.AlertDialog.BUTTON_POSITIVE);
            android.widget.Button neg = d.getButton(android.app.AlertDialog.BUTTON_NEGATIVE);
            float dp = d.getContext().getResources().getDisplayMetrics().density;
            if (pos != null) {
                int gap = (int) (20 * dp);
                android.view.ViewGroup.LayoutParams lp = pos.getLayoutParams();
                if (lp instanceof android.view.ViewGroup.MarginLayoutParams) {
                    android.view.ViewGroup.MarginLayoutParams m =
                            (android.view.ViewGroup.MarginLayoutParams) lp;
                    m.setMarginStart(gap);
                }
                pos.setMinWidth((int) (72 * dp));
                if (dangerPositive) {
                    pos.setTextColor(0xFFD93F3F);
                } else {
                    pos.setTextColor(accent(d.getContext()));
                }
                pos.requestLayout();
            }
            if (neg != null) {
                neg.setTextColor(0xFF6B7A86);
            }
        } catch (Throwable ignored) {
        }
    }
}
