package com.doubao.watchremote;

import android.content.Context;
import android.util.AttributeSet;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 主页下拉刷新。直接在 dispatchTouchEvent 里拦，不依赖子控件是否消费事件，
 * 所以页面内容变长、连上手表以后也不会失灵。
 */
public class PullRefreshScrollView extends ScrollView {

    public interface OnRefreshListener {
        void onRefresh(Runnable done);
    }

    private TextView header;
    private OnRefreshListener listener;
    private boolean refreshing;
    private boolean intercepting;
    private boolean candidate;
    private float downY;
    private int slop;
    /** 刷新中/完成时提示条高度 */
    private final int headerPx = (int) (46 * getResources().getDisplayMetrics().density);
    /** 提示条最多能拉多高（拉到头才算过阈值） */
    private final int maxDragPx = (int) (94 * getResources().getDisplayMetrics().density);
    /** 手指下拉距离跟提示条 1:1，拉到底就等于过阈值 */
    private final int triggerPx = maxDragPx;
    /** 过阈值后一直震：0 延迟、80ms 震、60ms 停，无限循环 */
    private final long[] buzzPattern = new long[]{0, 80, 60};
    private Vibrator vibrator;
    private boolean buzzing;

    public PullRefreshScrollView(Context c) {
        super(c);
        init();
    }

    public PullRefreshScrollView(Context c, AttributeSet a) {
        super(c, a);
        init();
    }

    public PullRefreshScrollView(Context c, AttributeSet a, int def) {
        super(c, a, def);
        init();
    }

    private void init() {
        slop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
        try {
            vibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
        } catch (Throwable ignored) {
        }
    }

    /** 过阈值：开始持续震动 */
    private void buzzStart() {
        if (buzzing) return;
        if (vibrator == null) {
            try {
                vibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
            } catch (Throwable ignored) {
            }
        }
        if (vibrator == null) {
            EventLog.add(getContext(), "震动不可用：取不到 Vibrator");
            return;
        }
        try {
            if (!vibrator.hasVibrator()) {
                EventLog.add(getContext(), "震动不可用：设备没有马达");
                return;
            }
            buzzing = true;
            EventLog.add(getContext(), "下拉过阈值：开始持续震动");
            if (android.os.Build.VERSION.SDK_INT >= 26) {
                vibrator.vibrate(VibrationEffect.createWaveform(buzzPattern, 0));
            } else {
                vibrator.vibrate(buzzPattern, 0);
            }
        } catch (Throwable e) {
            EventLog.add(getContext(), "震动失败：" + e);
        }
    }

    private void buzzStop() {
        if (!buzzing) return;
        buzzing = false;
        try {
            if (vibrator != null) vibrator.cancel();
        } catch (Throwable ignored) {
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        buzzStop();
        super.onDetachedFromWindow();
    }

    public void setOnRefreshListener(OnRefreshListener l) {
        listener = l;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        if (getChildCount() != 1) return;
        View content = getChildAt(0);
        if ("refreshWrapper".equals(content.getTag())) return;
        removeViewAt(0);
        LinearLayout wrap = new LinearLayout(getContext());
        wrap.setTag("refreshWrapper");
        wrap.setOrientation(LinearLayout.VERTICAL);
        header = new TextView(getContext());
        header.setText(Lang.s(getContext(), "下拉刷新"));
        header.setTextSize(12);
        header.setGravity(android.view.Gravity.CENTER);
        wrap.addView(header, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0));
        wrap.addView(content, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        addView(wrap);
    }

    private void setHeaderHeight(int px) {
        if (header == null) return;
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) header.getLayoutParams();
        lp.height = px;
        header.setLayoutParams(lp);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (refreshing) return super.dispatchTouchEvent(ev);
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downY = ev.getY();
                intercepting = false;
                candidate = getScrollY() <= 0;
                break;
            case MotionEvent.ACTION_MOVE:
                if (candidate && !intercepting && ev.getY() - downY > slop) {
                    intercepting = true;
                }
                if (intercepting) {
                    float dy = ev.getY() - downY;
                    // 越往下越接近阈值，提示条高度按 0.7 倍跟随
                    int h = (int) Math.max(0, Math.min(maxDragPx, dy));
                    setHeaderHeight(h);
                    boolean passed = dy >= triggerPx || h >= maxDragPx;
                    if (passed) buzzStart();
                    else buzzStop();
                    if (header != null) {
                        if (passed) {
                            header.setText(Lang.s(getContext(), "松手刷新"));
                        } else if (dy > triggerPx * 0.4f) {
                            header.setText(Lang.s(getContext(), "再往下拉到底"));
                        } else {
                            header.setText(Lang.s(getContext(), "下拉刷新"));
                        }
                    }
                    return true;
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                if (!intercepting) break;
                intercepting = false;
                // 松手时的位置还够不够阈值；没松手又滑回去，就等于取消
                buzzStop();
                boolean doRefresh = ev.getActionMasked() == MotionEvent.ACTION_UP
                        && ((ev.getY() - downY) >= triggerPx
                        || (header != null && header.getHeight() >= maxDragPx));
                if (doRefresh) startRefresh();
                else setHeaderHeight(0);
                return true;
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    private void startRefresh() {
        if (listener == null) {
            setHeaderHeight(0);
            return;
        }
        buzzStop();
        refreshing = true;
        setHeaderHeight(headerPx);
        if (header != null) header.setText(Lang.s(getContext(), "刷新中…"));
        final long t0 = System.currentTimeMillis();
        final boolean[] finished = {false};
        listener.onRefresh(new Runnable() {
            @Override
            public void run() {
                if (finished[0]) return;
                finished[0] = true;
                postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        refreshing = false;
                        String hm = new SimpleDateFormat("HH:mm:ss", Locale.CHINA).format(new Date());
                        if (header != null) header.setText(Lang.s(getContext(), "已刷新 ") + hm);
                        postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                setHeaderHeight(0);
                            }
                        }, 900);
                    }
                }, Math.max(300, 700 - (System.currentTimeMillis() - t0)));
            }
        });
        // 保险：2.5 秒后无论如何收起
        postDelayed(new Runnable() {
            @Override
            public void run() {
                if (refreshing) {
                    refreshing = false;
                    setHeaderHeight(0);
                }
            }
        }, 2500);
    }
}
