package com.doubao.watchremote;

import android.app.AlertDialog;
import android.content.Context;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Toast;

/** 弹窗自动本地化：创建时给窗口挂一个布局监听，文字一变/一显示就按当前风格翻译 */
public class Loc extends AlertDialog.Builder {

    private final Context ctx;

    public Loc(Context c) {
        super(c);
        ctx = c;
    }

    private void hook(final AlertDialog d) {
        if (d == null) return;
        try {
            final View root = d.getWindow().getDecorView();
            AppTheme.localize(root, ctx);
            root.getViewTreeObserver().addOnGlobalLayoutListener(
                    new ViewTreeObserver.OnGlobalLayoutListener() {
                        @Override
                        public void onGlobalLayout() {
                            AppTheme.localize(root, ctx);
                        }
                    });
        } catch (Throwable ignored) {
        }
    }

    @Override
    public AlertDialog create() {
        AlertDialog d = super.create();
        hook(d);
        return d;
    }

    @Override
    public AlertDialog show() {
        AlertDialog d = super.show();
        try {
            AppTheme.localize(d.getWindow().getDecorView(), ctx);
        } catch (Throwable ignored) {
        }
        return d;
    }

    /** Toast 统一走这里，中文原文按当前风格翻译 */
    public static Toast toast(Context c, CharSequence text, int duration) {
        String zh = text == null ? "" : text.toString();
        return Toast.makeText(c, Lang.s(c, zh), duration);
    }

    private static final String[][] FRAG = {
            {"没找到手表。先打开小米运动健康，确认手表已连接（蓝牙开着）。",
                    "Watch not found. Open Mi Fitness and check the watch is connected (Bluetooth on)."},
            {"权限已通过，正在打开手表应用…", "Permission granted; opening the watch app…"},
            {"请求手表权限（手机会弹授权）…", "Requesting watch permission (your phone will ask)…"},
            {"手表应用已打开，正在发送计划…", "Watch app opened; sending plans…"},
            {"手表上找不到「吃药提醒」（包名 com.doubao.watchremote）。用表盘自定义工具装 v1.2.1 的 .bin 到手表，装好再点同步。\n如果表上还留着旧版（旧包名），先在手表应用列表里把它删掉。",
                    "“Medicine Reminder” is not found on the watch (package com.doubao.watchremote). Install the .bin with the watch-face tool, then sync again.\nIf an old version with the old package name is still on the watch, remove it first."},
            {"已发到手表。手表上打开「吃药提醒」就能看到新计划。",
                    "Sent. Open “Medicine Reminder” on the watch to see the plan."},
            {"签名不匹配：手表端安装包和这个 App 不是同一个证书签的，",
                    "Signature mismatch: the watch package is signed with a different certificate than this app. "},
            {"手表上没装「吃药提醒」应用，先用表盘自定义工具把 .bin 装到手表上。",
                    "The watch app is not installed. Install the .bin with the watch-face tool first."},
            {"穿戴服务不可用：", "Wearable service unavailable: "},
            {"已找到设备：", "Device found: "},
            {"，正在校验权限…", ", checking permission…"},
            {"获取设备列表失败：", "Could not get device list: "},
            {"手表权限被拒绝或失败：", "Watch permission denied or failed: "},
            {"权限校验失败：", "Permission check failed: "},
            {"权限接口异常：", "Permission API error: "},
            {"打开手表应用失败（", "Could not open the watch app ("},
            {"），直接尝试发送…", "), sending anyway…"},
            {"生成计划数据失败：", "Could not build plan data: "},
            {"发送失败：", "Send failed: "},
            {"消息接口异常：", "Message API error: "},
            {"正在查找手表…", "Looking for the watch…"},
            {"这个时间今天已经过了，从明天开始：", "This time has passed today; starting tomorrow: "},
            {"排定失败(", "Scheduling failed ("},
            {"排定 ", "Scheduled "},
            {"闹钟触发 medId=", "Alarm fired medId="},
            {"已发通知：", "Notification sent: "},
            {"没发通知：没有通知权限", "No notification: notifications are off"},
            {"找不到这个计划（可能已被删除），跳过", "Plan not found (maybe deleted); skipped"},
            {"这个计划已关闭或时间已改，跳过", "Plan is off or the time changed; skipped"},
            {"今天次数已吃满，本次不提醒", "Daily doses complete; no alert this time"},
            {"打开了全屏提醒页", "Full-screen alert page opened"},
            {"手动重排了全部提醒", "Manually rescheduled all alerts"},
            {"手动测试：10 秒后触发", "Manual test: fires in 10 s"},
            {"退化为非精确闹钟也失败：", "Inexact alarm fallback failed: "},
            {"普通闹钟也失败：", "Plain alarm failed: "},
            {"计划新增 ", "Plans added "},
            {"计划覆盖为 ", "Plans replaced: "},
            {"记录新增 ", "Logs added: "},
            {"，跳过重复 ", ", skipped duplicates: "},
            {"，现在共 ", ", now "},
            {"服药记录 ", "Dose logs "},
            {"主题颜色", "theme colour"},
            {"勿扰设置", "DND settings"},
            {"提醒设置", "reminder settings"},
            {"（全屏闹钟）", " (full-screen alarm)"},
            {"（悬浮）", " (pop-up)"},
            {"（静音）", " (silent)"},


            // 杂项片段（日志 / 状态行）
            {"表上打卡", "logged on watch"},
            {"表上撤销", "undone on watch"},
            {"表上次数（校正 ", "counts from watch (fixed "},
            {"个）", ")"},
            {"（没记录可撤）", " (nothing to undo)"},
            {"收到手表：打卡 id=", "From watch: dose id="},
            {"收到手表：撤销 id=", "From watch: undo id="},
            {"收到手表：全量次数，校正 ", "From watch: full counts, reconciled "},
            {"个计划", " plans"},
            {"手表消息解析失败 ", "Could not parse watch message "},
    };

    /** 把已知的中文片段替换成英文，用于拼出来的动态文本 */
    public static String frag(Context c, String zh) {
        if (zh == null || !Lang.hasHan(zh)) return zh;
        if (Tone.style(c) != Tone.EN) return zh;
        String out = zh;
        for (String[] pair : FRAG) {
            if (out.contains(pair[0])) out = out.replace(pair[0], pair[1]);
        }
        String exact = Lang.s(c, out);
        return exact;
    }
}
