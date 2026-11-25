package com.doubao.watchremote;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.view.animation.DecelerateInterpolator;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.util.List;

public class MainActivity extends Activity {

    private static final int REQ_NOTIF = 101;
    private static final int ACT_NOTIF = 1;
    private static final int ACT_EXACT = 2;
    private static final int ACT_BATTERY = 3;
    private static final int ACT_FSI = 6;

    private LinearLayout listBox;
    private TextView todayText;
    private TextView statusText;
    private ImageView btnDnd;
    private TextView btnReconnect;
    private boolean onboardActive = false;
    private boolean onboardShowing = false;
    private TextView emptyText;
    private TextView logText;
    private TextView permText;
    private View permBox;
    private Button btnPerm;
    private int permAction = 0;
    private boolean watchPending = false;
    private boolean watchIntroShown = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        listBox = findViewById(R.id.listBox);
        todayText = findViewById(R.id.todayText);
        statusText = findViewById(R.id.statusText);
        btnDnd = findViewById(R.id.btnDnd);
        emptyText = findViewById(R.id.emptyText);
        logText = findViewById(R.id.logText);
        permText = findViewById(R.id.permText);
        permBox = findViewById(R.id.permBox);
        btnPerm = findViewById(R.id.btnPerm);

        try {
            ImageView watchBtn = findViewById(R.id.btnRefresh);
            watchBtn.setColorFilter(0xFFE5484D); // 手表按钮固定红色，不跟主题色
        } catch (Throwable ignored) {
        }
        btnReconnect = findViewById(R.id.btnReconnect);
        if (btnReconnect != null) {
            btnReconnect.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    WatchBridge.reconnect();
                    WatchSync.pull(MainActivity.this);
                    Loc.toast(MainActivity.this, "正在重新连接手表…", Toast.LENGTH_SHORT).show();
                    v.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            applyWatchTopButtons();
                        }
                    }, 1500);
                }
            });
        }

        findViewById(R.id.btnRefresh).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Chaos.actions() && Chaos.chance(0.8)) {
                    chaosHeader();
                    return;
                }
                startActivity(new Intent(MainActivity.this, WatchActivity.class));
            }
        });

        findViewById(R.id.btnAdd).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Chaos.actions() && Chaos.chance(0.75)) {
                    chaosBottom();
                    return;
                }
                startActivity(new Intent(MainActivity.this, AddActivity.class));
            }
        });

        findViewById(R.id.btnTest).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Scheduler.testNow(MainActivity.this, 5000L);
                Loc.toast(MainActivity.this,
                        Tone.s(MainActivity.this,
                                "5 秒后会按系统闹钟通道发出「测试提醒」通知，不弹应用页面、不计入记录，可以先锁屏验证",
                                "5 秒后用系统闹钟通道发一条测试喵，不弹页面、不记账，可以先锁屏试试～",
                                "A test alert will arrive in 5 s via the system alarm channel (no page, no logging). Lock the screen to check."),
                        Toast.LENGTH_LONG).show();
            }
        });

        btnPerm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openPermission(permAction);
            }
        });

        findViewById(R.id.btnAbout).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Chaos.actions() && Chaos.chance(0.8)) {
                    chaosHeader();
                    return;
                }
                showAbout();
            }
        });

        findViewById(R.id.btnSettings).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Chaos.actions() && Chaos.chance(0.8)) {
                    chaosHeader();
                    return;
                }
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            }
        });

        AppTheme.apply(this);
        if (Tone.elder(this)) {
            findViewById(R.id.btnRefresh).setVisibility(View.GONE);
        }
        MedStore.migrateLegacy(this);
        migrateOldGlobalMode();
        final View bottomContent = findViewById(R.id.bottomContent);
        final TextView todayTitle = findViewById(R.id.todayTitle);
        final TextView btnToggleLog = findViewById(R.id.btnToggleLog);
        if (bottomContent != null && todayTitle != null && btnToggleLog != null) {
            applyLogCollapsed(bottomContent, todayTitle, btnToggleLog, RemindPrefs.logCollapsed(this), false);
            btnToggleLog.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    boolean collapsed = bottomContent.getVisibility() != View.VISIBLE;
                    RemindPrefs.setLogCollapsed(MainActivity.this, !collapsed);
                    applyLogCollapsed(bottomContent, todayTitle, btnToggleLog, !collapsed, true);
                }
            });
        }

        btnDnd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int next = (RemindPrefs.alertMode(MainActivity.this) + 1) % 3;
                RemindPrefs.setAlertMode(MainActivity.this, next);
                applyAlertIcon();
                Loc.toast(MainActivity.this, RemindPrefs.alertModeText(MainActivity.this),
                        Toast.LENGTH_SHORT).show();
            }
        });
        // 长按仍然可以设勿扰时段
        btnDnd.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                showDndDialog();
                return true;
            }
        });

        onboardActive = !RemindPrefs.onboardDone(this);

        NotificationHelper.ensureChannel(this);
        Scheduler.rescheduleAll(this);

        PullRefreshScrollView puller = (PullRefreshScrollView) findViewById(R.id.pageBg);
        if (puller != null) {
            puller.setOnRefreshListener(new PullRefreshScrollView.OnRefreshListener() {
                @Override
                public void onRefresh(Runnable done) {
                    WatchSync.pushPlans(MainActivity.this);
                    refresh();
                    WatchBridge.reconnect();
                    WatchSync.pull(MainActivity.this);
                    done.run();
                }
            });
        }

        WatchBridge.setOnChanged(new Runnable() {
            @Override
            public void run() {
                refresh();
                if (watchPending && !watchIntroShown) {
                    String st = WatchBridge.stateText(MainActivity.this);
                    if (!st.startsWith("连接中") && !st.startsWith("重连中") && !st.startsWith("未启动")) {
                        watchPending = false;
                        showWatchIntroDialog();
                    }
                }
            }
        });
        WatchBridge.start(this);
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                WatchSync.openWatchApp(MainActivity.this, "/pages/index");
            }
        }, 1500);
    }

    @Override
    protected void onResume() {
        super.onResume();
        AppTheme.apply(this);
        refresh();
        if (onboardActive) showOnboardStep();
    }

    /** ⋯ 菜单里的「更改今日服用次数」：只改今天的记录，不碰计划总次数。确认要等 10 秒。 */
    private void showEditTakenDialog(final Medicine m) {
        final int[] value = {MedStore.takenCount(this, m.id)};
        final android.widget.LinearLayout box = new android.widget.LinearLayout(this);
        box.setOrientation(android.widget.LinearLayout.VERTICAL);
        int pad = (int) (18 * getResources().getDisplayMetrics().density);
        box.setPadding(pad, pad, pad, pad);

        final android.widget.TextView tip = new android.widget.TextView(this);
        final boolean enTaken = enMode();
        tip.setText(enTaken
                ? ("Only today's log changes; the plan stays as is\nNow: " + value[0])
                : ("只改你今天的记录，不会改这个计划要吃几次\n现在：" + value[0] + " 次"));
        tip.setTextSize(13);
        tip.setTextColor(0xFF6B7A86);
        box.addView(tip);

        android.widget.LinearLayout row = new android.widget.LinearLayout(this);
        row.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER);
        row.setPadding(0, pad, 0, 0);

        final android.widget.TextView num = new android.widget.TextView(this);
        num.setText(String.valueOf(value[0]));
        num.setTextSize(24);
        num.setGravity(android.view.Gravity.CENTER);
        num.setPadding(pad, 0, pad, 0);

        android.widget.Button minus = new android.widget.Button(this);
        minus.setText("-");
        minus.setTextSize(20);
        minus.setBackgroundResource(R.drawable.bg_btn_ghost);
        android.widget.Button plus = new android.widget.Button(this);
        plus.setText("+");
        plus.setTextSize(20);
        plus.setBackgroundResource(R.drawable.bg_btn_ghost);

        android.widget.LinearLayout.LayoutParams lp =
                new android.widget.LinearLayout.LayoutParams((int) (56 * getResources().getDisplayMetrics().density),
                        (int) (48 * getResources().getDisplayMetrics().density));
        minus.setLayoutParams(lp);
        plus.setLayoutParams(lp);
        row.addView(minus);
        row.addView(num);
        row.addView(plus);
        box.addView(row);

        minus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (value[0] > 0) value[0]--;
                num.setText(String.valueOf(value[0]));
                tip.setText(enTaken
                        ? ("Only today's log changes; the plan stays as is\nChange to: " + value[0])
                        : ("只改你今天的记录，不会改这个计划要吃几次\n改成：" + value[0] + " 次"));
            }
        });
        plus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (value[0] < 24) value[0]++;
                num.setText(String.valueOf(value[0]));
                tip.setText(enTaken
                        ? ("Only today's log changes; the plan stays as is\nChange to: " + value[0])
                        : ("只改你今天的记录，不会改这个计划要吃几次\n改成：" + value[0] + " 次"));
            }
        });

        final android.app.AlertDialog dlg = new Loc(this)
                .setTitle(m.name)
                .setView(box)
                .setPositiveButton(Lang.s(this, "保存"), null)
                .setNegativeButton("取消", null)
                .create();
        dlg.setOnShowListener(new android.content.DialogInterface.OnShowListener() {
            @Override
            public void onShow(android.content.DialogInterface d) {
                final android.widget.Button ok = dlg.getButton(android.app.AlertDialog.BUTTON_POSITIVE);
                final android.os.Handler h = new android.os.Handler(android.os.Looper.getMainLooper());
                if (!RemindPrefs.noCountdown(MainActivity.this)) {
                    ok.setEnabled(false);
                    h.post(new Runnable() {
                        int left = 10;

                        @Override
                        public void run() {
                            if (left > 0) {
                                ok.setText(cnt("保存", "Save", left));
                                ok.setEnabled(false);
                                left--;
                                h.postDelayed(this, 1000);
                            } else {
                                ok.setText(Lang.s(MainActivity.this, "保存"));
                                ok.setEnabled(true);
                            }
                        }
                    });
                } else {
                    ok.setEnabled(true);
                    ok.setText(Lang.s(MainActivity.this, "保存"));
                }
                ok.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        MedStore.setTakenCount(MainActivity.this, m.id, value[0]);
                        WatchSync.pushPlans(MainActivity.this);
                        dlg.dismiss();
                        refresh();
                        Loc.toast(MainActivity.this,
                                Tone.s(MainActivity.this,
                                        "已改成今天吃 " + value[0] + " 次",
                                        "改好啦喵，今天吃掉 " + value[0] + " 次～",
                                        "Today's count set to " + value[0]), android.widget.Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
        dlg.show();
    }

    /** 先检测、后提示：等连接尝试有结果了再决定弹不弹 */
    private void maybeShowWatchIntro() {
        // 从来没成功连过：不打扰，等用户自己去手表页连
        if (!WatchBridge.everConnected(this)) return;
        if (watchIntroShown) return;
        String st = WatchBridge.stateText(this);
        if (st.startsWith("已连接")) return;
        if (st.startsWith("连接中") || st.startsWith("重连中") || st.startsWith("未启动")) {
            // 还在检测，等结果
            watchPending = true;
            return;
        }
        watchPending = false;
        showWatchIntroDialog();
    }

    private void showWatchIntroDialog() {
        if (isFinishing()) return;
        watchIntroShown = true;
        new Loc(this)
                .setTitle("手表未连接")
                .setMessage("手机与手表之间的数据通道当前不可用。\n\n"
                        + "请确认：\n"
                        + "1. 手表与手机已完成配对，蓝牙已开启\n"
                        + "2. 打开小米运动健康，设备显示为在线\n"
                        + "3. 手表上已安装「吃药提醒」\n\n"
                        + "随后可在手表页点击「连接到手表」重新建立连接。")
                .setPositiveButton("前往手表页", new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface d, int w) {
                        startActivity(new Intent(MainActivity.this, WatchActivity.class));
                    }
                })
                .setNegativeButton("稍后", null)
                .show();
    }

    /** 顶部刷新按钮：整个应用状态重来一遍 */
    private void fullRefresh() {
        NotificationHelper.ensureChannel(this);
        Scheduler.rescheduleAll(this);
        MedStore.migrateLegacy(this);
        WatchBridge.reconnect();
        WatchSync.pull(this);
        refresh();
        Loc.toast(this, "已刷新：计划、提醒、手表连接", android.widget.Toast.LENGTH_SHORT).show();
    }

    /** 没连上就露个「↻」按钮，连上了就藏起来 */
    private void applyWatchTopButtons() {
        if (btnReconnect == null) return;
        String st = WatchBridge.stateText(this);
        boolean connected = st.startsWith("已连接");
        btnReconnect.setVisibility(connected ? View.GONE : View.VISIBLE);
    }

    private void refresh() {
        List<Medicine> list = MedStore.load(this);
        listBox.removeAllViews();

        int total = 0;
        int takenTotal = 0;
        for (final Medicine m : list) {
            View v = LayoutInflater.from(this).inflate(R.layout.item_medicine, listBox, false);
            TextView name = v.findViewById(R.id.itemName);
            TextView detail = v.findViewById(R.id.itemDetail);
            TextView status = v.findViewById(R.id.itemStatus);
            TextView del = v.findViewById(R.id.itemDelete);
            TextView take = v.findViewById(R.id.itemTake);
            TextView more = v.findViewById(R.id.itemMore);

            name.setText(m.name);
            boolean enNow = enMode();
            String d = enNow
                    ? ("Each " + m.dose + " · " + Lang.s(this, m.meal) + "\n" + m.timesTextWithModes(true))
                    : ("每次 " + m.dose + " · " + m.meal + "服用\n" + m.timesTextWithModes());
            if (m.purpose != null && !m.purpose.isEmpty()) {
                d += (enNow ? "\nFor: " : "\n用于：") + m.purpose;
            }
            detail.setText(d);
            int taken = MedStore.takenCount(this, m.id);
            int wState = WatchFlags.stateOf(this, m.id);
            // 手表上暂停的计划，跟手机自己关掉提醒是一回事
            boolean off = !m.enabled || wState != WatchFlags.NORMAL;
            status.setText(off
                    ? Lang.s(this, "已关闭提醒")
                    : (enNow ? ("Taken " + taken + " / " + m.doseCount() + " today")
                             : ("今天已服 " + taken + " / " + m.doseCount() + " 次")));
            if (off) {
                // 提醒关掉了，或者在手表上暂停/禁用了：按钮置灰不可点，跟手机自己关掉时一样
                take.setBackgroundResource(R.drawable.bg_btn_grey);
                take.setTextColor(0xFF9AA3B2);
                take.setText(Tone.disabledTake(this));
            } else if (taken >= m.doseCount()) {
                // 今天已经吃满，按钮变灰但仍可点（再点会走“过量二次确认 + 5 秒倒计时”）
                take.setBackgroundResource(R.drawable.bg_btn_grey);
                take.setTextColor(0xFF9AA3B2);
                take.setText(Tone.takeBtn(this, true));
            } else {
                take.setBackground(AppTheme.filled(MainActivity.this));
                take.setTextColor(0xFFFFFFFF);
                take.setText(Tone.takeBtn(this, false));
            }
            if (!off) {
                total += m.doseCount();
                takenTotal += Math.min(taken, m.doseCount());
            }

            take.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (Chaos.actions() && Chaos.chance(0.75)) {
                        chaosCard(m);
                        return;
                    }
                    final int now = MedStore.takenCount(MainActivity.this, m.id);
                    boolean over = now >= m.doseCount();
                    String msg = remindMsg(m, now, over);

                    final AlertDialog dlg = new Loc(MainActivity.this)
                            .setTitle(over ? "确认过量记录" : "吃过了")
                            .setMessage(msg)
                            .setNegativeButton("取消", null)
                            .setPositiveButton(over
                            ? (RemindPrefs.noCountdown(MainActivity.this)
                                    ? Lang.s(MainActivity.this, "确认吃过")
                                    : cnt("确认吃过", "Confirm taken", 5))
                            : Lang.s(MainActivity.this, "确认吃过"),
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            MedStore.markTaken(MainActivity.this, m.id);
                                            for (int i = 0; i < m.times.size(); i++) {
                                                Scheduler.cancelRepeat(MainActivity.this, m.id, i);
                                            }
                                            WatchSync.pushPlans(MainActivity.this);
                                            int after = MedStore.takenCount(MainActivity.this, m.id);
                                            Loc.toast(MainActivity.this,
                                                    takenLine(m.name, after, m.doseCount()),
                                                    Toast.LENGTH_SHORT).show();
                                            refresh();
                                        }
                                    })
                            .create();

                    final boolean overNow = over;
                    if (overNow && RemindPrefs.noCountdown(MainActivity.this)) {
                        Button ok = dlg.getButton(AlertDialog.BUTTON_POSITIVE);
                        if (ok != null) {
                            ok.setEnabled(true);
                            ok.setText(Lang.s(MainActivity.this, "确认吃过"));
                        }
                    }
                    if (overNow && !RemindPrefs.noCountdown(MainActivity.this)) {
                        // 过量记录：按钮先禁用，5 秒倒计时后才允许点，留足反悔时间
                        dlg.setOnShowListener(new DialogInterface.OnShowListener() {
                            @Override
                            public void onShow(DialogInterface dialog) {
                                final Button ok = dlg.getButton(AlertDialog.BUTTON_POSITIVE);
                                ok.setEnabled(false);
                                final Handler h = new Handler(Looper.getMainLooper());
                                final int[] left = {5};
                                final Runnable[] tick = new Runnable[1];
                                tick[0] = new Runnable() {
                                    @Override
                                    public void run() {
                                        if (left[0] > 0) {
                                            ok.setText(cnt("确认吃过", "Confirm taken", left[0]));
                                            left[0]--;
                                            h.postDelayed(tick[0], 1000);
                                        } else {
                                            ok.setText(Lang.s(MainActivity.this, "确认吃过"));
                                            ok.setEnabled(true);
                                        }
                                    }
                                };
                                ok.setText(cnt("确认吃过", "Confirm taken", 5));
                                h.postDelayed(tick[0], 1000);
                                dlg.setOnDismissListener(new DialogInterface.OnDismissListener() {
                                    @Override
                                    public void onDismiss(DialogInterface d) {
                                        h.removeCallbacks(tick[0]);
                                    }
                                });
                            }
                        });
                    }
                    dlg.show();
                }
            });

            if (!m.enabled) {
                if (Chaos.on()) {
                    // 混乱模式：暂停的计划点下去也得有反应
                    take.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            chaosCard(m);
                        }
                    });
                } else {
                    take.setOnClickListener(null);
                    take.setClickable(false);
                }
            }

            more.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    PopupMenu pm = new PopupMenu(MainActivity.this, view);
                    pm.getMenu().add(0, 1, 0, Lang.s(MainActivity.this, "我按错了（撤销一次）"));
                    pm.getMenu().add(0, 4, 1, Tone.s(MainActivity.this,
                            "更改今日服用次数", "改今天的次数喵", "Change today's taken count"));
                    pm.getMenu().add(0, 2, 2, Lang.s(MainActivity.this, "编辑这个计划"));
                    pm.getMenu().add(0, 3, 3, Lang.s(MainActivity.this,
                            m.enabled ? "暂停该计划" : "恢复该计划"));
                    pm.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            if (Chaos.actions() && Chaos.chance(0.85)) {
                                switch (Chaos.pick(4)) {
                                    case 0: askUndo(m); return true;
                                    case 1: showEditTakenDialog(m); return true;
                                    case 2: setPlanEnabled(m, !m.enabled); return true;
                                    default:
                                        startActivity(new Intent(MainActivity.this, WatchActivity.class));
                                        return true;
                                }
                            }
                            if (item.getItemId() == 1) {
                                askUndo(m);
                                return true;
                            }
                            if (item.getItemId() == 2) {
                                Intent i = new Intent(MainActivity.this, AddActivity.class);
                                i.putExtra("medId", m.id);
                                startActivity(i);
                                return true;
                            }
                            if (item.getItemId() == 3) {
                                setPlanEnabled(m, !m.enabled);
                                return true;
                            }
                            if (item.getItemId() == 4) {
                                showEditTakenDialog(m);
                                return true;
                            }
                            return false;
                        }
                    });
                    pm.show();
                }
            });

            del.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    final AlertDialog delDlg = new Loc(MainActivity.this)
                            .setTitle("删除提醒")
                            .setMessage(Tone.s(MainActivity.this,
                                    "确定删除「" + m.name + "」的吃药提醒吗？",
                                    "真要删掉「" + m.name + "」的提醒喵？",
                                    "Delete the reminder for “" + m.name + "”?"))
                            .setNegativeButton("取消", null)
                            .setPositiveButton("删除", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Scheduler.cancelMedicine(MainActivity.this, m);
                                    MedStore.remove(MainActivity.this, m.id);
                                    WatchSync.pushPlans(MainActivity.this);
                                    refresh();
                                }
                            })
                            .create();
                    delDlg.setOnShowListener(new DialogInterface.OnShowListener() {
                        @Override
                        public void onShow(DialogInterface dialog) {
                            AppTheme.styleDialogButtons(delDlg, true);
                        }
                    });
                    delDlg.show();
                }
            });

            listBox.addView(v);
        }

        emptyText.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
        findViewById(R.id.btnTest).setVisibility(View.VISIBLE);
        logText.setText(MedStore.todayLogText(this));
        applyAlertIcon();

        long[] next = Scheduler.nextUpcoming(this);
        if (total == 0) {
            todayText.setText(Tone.homeEmpty(this));
            emptyText.setText(Tone.homeEmpty(this));
            if (statusText != null) {
                String extra = WatchBridge.lastRecvText();
                statusText.setText(watchLine(extra));
            }
        } else {
            String s = Tone.homeSummary(this, total, takenTotal);
            if (next != null) s += " · 下次 " + Scheduler.formatWhen(next[0]);
            todayText.setText(s);
            if (statusText != null) {
                String extra = WatchBridge.lastRecvText();
                statusText.setText(watchLine(extra));
            }
        }

        applyWatchTopButtons();
        checkPermissions();
        AppTheme.localize(getWindow().getDecorView(), this);
        if (Tone.elder(this)) {
            AppTheme.scaleText(getWindow().getDecorView(), 1.22f);
        }
    }

    /** 「我按错了」：撤销今天最后一次服药记录，二次确认 */
    private void askUndo(final Medicine m) {
        final int now = MedStore.takenCount(this, m.id);
        if (now <= 0) {
            Loc.toast(this, Tone.s(this, "「" + m.name + "」今天还没有服药记录，不用撤销",
                    "「" + m.name + "」今天还没记录喵，不用撤～",
                    "“" + m.name + "” has no dose logged today; nothing to undo"),
                    Toast.LENGTH_SHORT).show();
            return;
        }
        new Loc(this)
                .setTitle("我按错了")
                            .setMessage(undoMsg(m, now))
                .setNegativeButton("取消", null)
                .setPositiveButton("确认撤销", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        boolean ok = MedStore.removeLastTaken(MainActivity.this, m.id);
                        if (ok) {
                            WatchSync.pushPlans(MainActivity.this);
                            int after = MedStore.takenCount(MainActivity.this, m.id);
                            Loc.toast(MainActivity.this,
                                    takenLine(m.name, after, m.doseCount()),
                                    Toast.LENGTH_SHORT).show();
                            refresh();
                        } else {
                            Loc.toast(MainActivity.this, "没找到可撤销的记录", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .show();
    }

    /** 老版本的“全局提醒方式”搬到每个计划里 */
    private void migrateOldGlobalMode() {
        int g = RemindPrefs.globalMode(this);
        if (g < 0) return;
        for (Medicine m : MedStore.load(this)) {
            if (m.groupMode < 0) {
                m.groupMode = g;
                MedStore.upsert(this, m);
            }
        }
        RemindPrefs.setGlobalMode(this, RemindPrefs.MODE_FOLLOW);
    }

    private static final String BILLI_URL = "https://b23.tv/ic5GAoB";

    /** 直接读安装包里的版本号，改版本时不用再改文案 */
    private String versionName() {
        try {
            android.content.pm.PackageInfo pi = getPackageManager()
                    .getPackageInfo(getPackageName(), 0);
            String vn = pi.versionName;
            long vc = android.os.Build.VERSION.SDK_INT >= 28
                    ? pi.getLongVersionCode() : pi.versionCode;
            return (vn == null ? "?" : vn) + "（" + vc + "）";
        } catch (Throwable t) {
            return "未知";
        }
    }

    private void showAbout() {
        String msgZh = "一个纯本地运行的吃药提醒工具，给需要长期按时吃药的人用。\n\n"
                + "能做的事：\n"
                + "· 添加多个药品计划，每个计划可分 1–6 次，分别设时间、用量、饭前/饭后/随餐\n"
                + "· 拍说明书 / 处方 / 药盒，在线识别后自动填好药名、用量、次数、时间、饭前饭后、用处\n"
                + "· 每个时间点单独选提醒方式：闹钟（全屏 + 铃声 + 震动）、悬浮弹窗、不提醒\n"
                + "· 到点没吃药会按设置间隔重复提醒（默认 5 分钟，可在设置改）\n"
                + "· 首页可一键「吃过了」补记，吃满变「再吃一遍」；「⋯ → 我按错了」可撤销\n"
                + "· 勿扰（全天 / 时间段），勿扰期间所有提醒都不响\n"
                + "· 今日已服记录、数据备份与恢复、主题颜色自定义\n\n"
                + "数据全部存在本机，只有拍照识别时会上传那一张图。\n\n"
                + "制作者：萧茂蜜呀\n"
                + "哔哩哔哩：萧茂蜜呀\n"
                + "https://b23.tv/ic5GAoB\n\n"
                + "版本 " + versionName();
        String msgEn = "A fully offline medicine reminder for people who take medicine on a schedule.\n\n"
                + "What it does:\n"
                + "· Several plans; each plan can have 1–6 doses with its own times, dose and meal relation\n"
                + "· Photograph a leaflet / prescription / box to fill in name, dose, frequency, times, meal relation and purpose\n"
                + "· Per-dose alert style: alarm (full screen + sound + vibration), pop-up, or silent\n"
                + "· Repeats at a set interval if a dose is missed (5 min by default, changeable in Settings)\n"
                + "· One-tap “Taken” logging on the home page; “Take again” when the day is complete; undo via “⋯ → Undo last dose”\n"
                + "· Do not disturb (all day or a time window); nothing sounds during DND\n"
                + "· Today's log, backup and restore, custom theme colour\n\n"
                + "All data stays on this phone; only the photo you scan is uploaded.\n\n"
                + "By: 萧茂蜜呀\n"
                + "bilibili: 萧茂蜜呀\n"
                + "https://b23.tv/ic5GAoB\n\n"
                + "Version " + versionName();
        String msg = Tone.s(this, msgZh, null, msgEn);
        new Loc(this)
                .setTitle("关于 · 吃药提醒")
                .setMessage(msg)
                .setNegativeButton("关闭", null)
                .setNeutralButton("更新日志", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        showChangelog();
                    }
                })
                .setPositiveButton("打开作者主页", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(BILLI_URL));
                            startActivity(i);
                        } catch (Throwable t) {
                            try {
                                Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(BILLI_URL));
                                i.setPackage("tv.danmaku.bili");
                                startActivity(i);
                            } catch (Throwable t2) {
                                Loc.toast(MainActivity.this,
                                        "没装 B 站也没浏览器，浏览器打开 b23.tv/ic5GAoB 就行",
                                        Toast.LENGTH_LONG).show();
                            }
                        }
                    }
                })
                .show();
    }

    private void showDndDialog() {
        String[] items = {Lang.s(this, "关闭勿扰"), Lang.s(this, "全天勿扰"),
                Lang.s(this, "只在一段时间内勿扰")};
        new Loc(this)
                .setTitle("勿扰")
                .setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            RemindPrefs.setDndOff(MainActivity.this);
                            afterDndChange("已关闭勿扰");
                        } else if (which == 1) {
                            RemindPrefs.setDndAll(MainActivity.this);
                            afterDndChange("已开启全天勿扰，勿扰期间所有提醒都不响");
                        } else {
                            pickDndWindow();
                        }
                    }
                })
                .show();
    }

    private void pickDndWindow() {
        final LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        box.setPadding(pad, pad / 2, pad, 0);

        TextView t1 = new TextView(this);
        t1.setText("开始时间");
        t1.setTextColor(0xFF77808F);
        t1.setTextSize(12);
        box.addView(t1);

        final TimePicker s1 = new TimePicker(this);
        s1.setIs24HourView(Boolean.TRUE);
        s1.setHour(RemindPrefs.dndStart(this) / 60);
        s1.setMinute(RemindPrefs.dndStart(this) % 60);
        box.addView(s1);

        TextView t2 = new TextView(this);
        t2.setText("结束时间（跨零点也可以，比如 22:00 到 07:00）");
        t2.setTextColor(0xFF77808F);
        t2.setTextSize(12);
        box.addView(t2);

        final TimePicker s2 = new TimePicker(this);
        s2.setIs24HourView(Boolean.TRUE);
        s2.setHour(RemindPrefs.dndEnd(this) / 60);
        s2.setMinute(RemindPrefs.dndEnd(this) % 60);
        box.addView(s2);

        new Loc(this)
                .setTitle("勿扰时间段")
                .setView(box)
                .setNegativeButton("取消", null)
                .setPositiveButton("保存", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        int st = s1.getHour() * 60 + s1.getMinute();
                        int en = s2.getHour() * 60 + s2.getMinute();
                        RemindPrefs.setDndWindow(MainActivity.this, st, en);
                        afterDndChange("勿扰时段已保存");
                    }
                })
                .show();
    }

    private void afterDndChange(String msg) {
        applyAlertIcon();
        Loc.toast(this, msg, Toast.LENGTH_SHORT).show();
    }

    /** 首次进入：弹窗引导必开项，期间其它按钮都被挡住；可跳过 */
    private void showOnboardStep() {
        if (!onboardActive || onboardShowing) return;

        int action = 0;
        String title = "";
        String desc = "";
        String titleEn = "";
        String descEn = "";
        int total = 0;

        NotificationManager nm =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        boolean notifOk = true;
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            notifOk = false;
        }
        if (nm != null && !nm.areNotificationsEnabled()) notifOk = false;
        total++;

        boolean exactOk = true;
        if (Build.VERSION.SDK_INT >= 31) {
            android.app.AlarmManager am =
                    (android.app.AlarmManager) getSystemService(Context.ALARM_SERVICE);
            exactOk = am == null || am.canScheduleExactAlarms();
            total++;
        }
        boolean fsiOk = true;
        if (Build.VERSION.SDK_INT >= 34 && nm != null) {
            fsiOk = nm.canUseFullScreenIntent();
            total++;
        }
        boolean batteryOk = true;
        if (Build.VERSION.SDK_INT >= 23) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            batteryOk = pm != null && pm.isIgnoringBatteryOptimizations(getPackageName());
            total++;
        }

        if (!notifOk) {
            action = ACT_NOTIF;
            title = "开启通知权限";
            desc = "不开通知，到点完全不会有提醒。点「去开启」后在系统弹窗里允许通知。";
            titleEn = "Enable notifications";
            descEn = "Without notifications there will be no alerts at all. Tap “Turn on” and allow notifications in the system prompt.";
        } else if (!exactOk) {
            action = ACT_EXACT;
            title = "允许准时提醒";
            desc = "允许后提醒才能卡着点响，否则可能延迟几分钟。点「去开启」后在列表里允许本应用。";
            titleEn = "Allow exact alarms";
            descEn = "Only with this permission can alerts fire on time; otherwise they may be a few minutes late. Tap “Turn on” and allow this app in the list.";
        } else if (!fsiOk) {
            action = ACT_FSI;
            title = "打开全屏通知";
            desc = "安卓 14 起锁屏弹出提醒页需要单独允许全屏通知，不开锁屏时只会出横幅。";
            titleEn = "Enable full-screen alerts";
            descEn = "On Android 14+ a full-screen alert on the lock screen needs this permission; without it you only get a banner.";
        } else if (!batteryOk) {
            action = ACT_BATTERY;
            title = "加入电池优化白名单";
            desc = "不加白名单，后台可能被系统清掉，提醒会哑火。";
            titleEn = "Add to battery whitelist";
            descEn = "Without the whitelist the app may be killed in the background and alerts will stop.";
        }

        if (action == 0) {
            onboardActive = false;
            RemindPrefs.setOnboardDone(this);
            Loc.toast(this, "必开项都开好了，可以正常用", Toast.LENGTH_SHORT).show();
            return;
        }

        final int act = action;
        onboardShowing = true;
        AlertDialog d = new Loc(this)
                .setTitle(Tone.s(this, "首次使用引导（" + total + " 项必开）", null,
                        "First-run setup (" + total + " required)"))
                .setMessage(Tone.s(this,
                        title + "\n\n" + desc + "\n\n开完会自动进入下一项，也可以直接跳过。",
                        title + "\n\n" + desc + "\n\n开完自动进下一项喵，也可以直接跳过～",
                        titleEn + "\n\n" + descEn + "\n\nEnabled items move on automatically; you can also skip."))
                .setCancelable(false)
                .setNegativeButton("跳过引导", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        onboardActive = false;
                        RemindPrefs.setOnboardDone(MainActivity.this);
                        Loc.toast(MainActivity.this, "已跳过，之后可以在设置里再开", Toast.LENGTH_SHORT).show();
                    }
                })
                .setPositiveButton("去开启", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        openPermission(act);
                    }
                })
                .create();
        d.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                onboardShowing = false;
            }
        });
        d.show();
    }

    private void checkPermissions() {
        StringBuilder sb = new StringBuilder();
        permAction = 0;

        boolean needNotif = false;
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            needNotif = true;
        }
        NotificationManager nm =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null && nm.areNotificationsEnabled() == false) {
            needNotif = true;
        }
        if (needNotif) {
            sb.append("· 还没有开启通知权限，到点不会弹出提醒");
            permAction = ACT_NOTIF;
        }

        if (Build.VERSION.SDK_INT >= 31) {
            android.app.AlarmManager am =
                    (android.app.AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (am != null && !am.canScheduleExactAlarms()) {
                if (sb.length() > 0) sb.append("\n");
                sb.append("· 未允许“准时提醒”，提醒可能延迟几分钟");
                if (permAction == 0) permAction = ACT_EXACT;
            }
        }

        // Android 14 起全屏通知默认关闭，锁屏时只会显示横幅
        if (Build.VERSION.SDK_INT >= 34 && nm != null && !nm.canUseFullScreenIntent()) {
            if (sb.length() > 0) sb.append("\n");
            sb.append("· 【必开】系统未允许“全屏通知”，锁屏时可能只弹出横幅，请打开全屏通知");
            if (permAction == 0) permAction = ACT_FSI;
        }

        if (Build.VERSION.SDK_INT >= 23) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (pm != null && !pm.isIgnoringBatteryOptimizations(getPackageName())) {
                if (sb.length() > 0) sb.append("\n");
                sb.append("· 建议把本应用加入电池优化白名单，避免后台被清理");
                if (permAction == 0) permAction = ACT_BATTERY;
            }
        }

        if (sb.length() > 0) {
            permBox.setVisibility(View.VISIBLE);
            permText.setText(sb.toString());
            String label;
            if (permAction == ACT_NOTIF) label = Lang.s(this, "开启通知");
            else if (permAction == ACT_FSI) label = Lang.s(this, "打开全屏通知");
            else if (permAction == ACT_EXACT) label = Lang.s(this, "允许准时提醒");
            else label = Lang.s(this, "加入白名单");
            btnPerm.setText(label);
        } else {
            permBox.setVisibility(View.GONE);
        }
    }

    private void openPermission(int action) {
        try {
            if (action == ACT_NOTIF) {
                if (Build.VERSION.SDK_INT >= 33) {
                    requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_NOTIF);
                } else {
                    Intent i = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                    i.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
                    startActivity(i);
                }
            } else if (action == ACT_EXACT) {
                if (Build.VERSION.SDK_INT >= 31) {
                    Intent i = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                    i.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(i);
                }
            } else if (action == ACT_FSI) {
                SettingsPages.openFullScreenIntent(this);
            } else if (action == ACT_BATTERY) {
                Intent i = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                i.setData(Uri.parse("package:" + getPackageName()));
                startActivity(i);
            }
        } catch (Exception e) {
            try {
                Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                i.setData(Uri.parse("package:" + getPackageName()));
                startActivity(i);
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_NOTIF) {
            refresh();
        }
    }

    // ---------------- 文案辅助（随表达风格切换） ----------------

    private boolean enMode() {
        return Tone.style(this) == Tone.EN;
    }

    /** 倒计时按钮文案：中文用全角括号，英文用半角 */
    private String cnt(String zhBase, String enBase, int n) {
        return enMode() ? (enBase + " (" + n + ")") : (zhBase + "（" + n + "）");
    }

    private String watchLine(String extra) {
        boolean en = enMode();
        String st = WatchBridge.stateText(this);
        String line = en
                ? ("Watch: " + st + " · taken today " + MedStore.takenTotalToday(this))
                : ("手表：" + st + " · 今天共吃 " + MedStore.takenTotalToday(this) + " 次");
        if (extra != null && !extra.isEmpty()) {
            line += en ? (" · last: " + extra) : (" · 最近 " + extra);
        }
        return line;
    }

    private String remindMsg(Medicine m, int now, boolean over) {
        if (enMode()) {
            String s = "“" + m.name + "” — " + m.dose + " · " + Lang.s(this, m.meal)
                    + ", plan " + m.doseCount() + " doses today, " + now + " logged."
                    + "\n\nDid you just take one?";
            if (over) {
                s += "\n\nAll " + m.doseCount() + " doses are already logged; one more counts as an overdose. Please confirm you really took it.";
            }
            return s;
        }
        String s = "「" + m.name + "」每次 " + m.dose + " · " + m.meal + "服用，今天计划 "
                + m.doseCount() + " 次，已记录 " + now + " 次。\n\n确认刚刚已经吃过一次吗？";
        if (over) {
            s += "\n\n今天已经吃满 " + m.doseCount() + " 次了，再记就是过量，请确认真的吃了。";
        }
        return s;
    }

    private String undoMsg(Medicine m, int now) {
        if (enMode()) {
            return "Undo the latest dose of “" + m.name + "” today?\n\nAfter undo: "
                    + (now - 1) + " / " + m.doseCount() + " taken today.";
        }
        return "撤销「" + m.name + "」今天最近的一次服药记录？\n\n撤销后今天已服 "
                + (now - 1) + " / " + m.doseCount() + " 次。";
    }

    private String takenLine(String name, int taken, int total) {
        if (enMode()) return "“" + name + "” taken " + taken + " / " + total + " today";
        return "「" + name + "」今天已服 " + taken + " / " + total + " 次";
    }

    /** 暂停 / 恢复一个计划的全部提醒（不动计划内容与今天的记录） */
    private void setPlanEnabled(Medicine m, boolean on) {
        m.enabled = on;
        MedStore.upsert(this, m);
        // 手机端暂停/恢复也记进名单，带时间戳推给手表
        WatchFlags.setPaused(this, m.id, !on);
        if (on) {
            Scheduler.rescheduleAll(this);
        } else {
            Scheduler.cancelMedicine(this, m);
            for (int i = 0; i < m.times.size(); i++) {
                Scheduler.cancelRepeat(this, m.id, i);
            }
        }
        WatchSync.pushPlans(this);
        refresh();
        Loc.toast(this, Tone.s(this,
                on ? "已恢复该计划的提醒" : "已暂停该计划的提醒",
                on ? "恢复好啦喵，又提醒你了～" : "先不吵你啦喵，暂停这个计划～",
                on ? "Reminders resumed for this plan" : "Reminders paused for this plan"),
                Toast.LENGTH_SHORT).show();
    }

    /** 更新日志 */
    private void showChangelog() {
        String zh = "v8.25.25\n"
                + "· 手表新增「遥控」页：滑动/点按控制手机界面翻页、滚动、返回\n\n"
                + "v8.25.24\n"
                + "· 暂停/恢复改成两端同一个开关：表上暂停=手机关闭，表上恢复=手机开启，主页不再出现空列表\n\n"
                + "v8.25.23\n"
                + "· 设置页新增「反馈 / 举报 Bug」：填写后直接提交到仓库，走代理通道国内也能发\n\n"
                + "v8.25.22\n"
                + "· 喵语升级成完整独立语言包：设置页等全部界面文字都有喵化文案，不再残留书面语\n\n"
                + "v8.25.21\n"
                + "· 表端 1.32：不管停在哪个页面都能收同步（全局接收先存再刷新），按钮背景固定只换字\n\n"
                + "v8.25.20\n"
                + "· 表端 1.31：暂停按钮背景固定不动，只换文字「暂停这个计划／恢复这个计划」\n\n"
                + "v8.25.19\n"
                + "· 表端 1.30：按钮颜色写死在样式里不再动态切，进次数页每次都重新拉计划，发送失败会自动重试一次\n\n"
                + "v8.25.18\n"
                + "· 手机端暂停/恢复也会写进同步名单并立刻推表，两端彻底一致\n"
                + "· 表端 1.29：暂停和恢复分成两个独立按钮，不会再出现纯色块没字\n\n"
                + "v8.25.17\n"
                + "· 手机端任何改动立刻推给手表，手表任何操作立刻回推，谁的操作新就按谁\n\n"
                + "v8.25.16\n"
                + "· 手表上关掉提醒的计划在次数页显示「恢复这个计划」，点了直接恢复并同步\n\n"
                + "v8.25.15\n"
                + "· 手表连着就自动来要一次计划，不用手动点同步也不会脱节\n"
                + "· 表端 1.26：暂停按钮修好，恢复后不会再变成一整块主题色没字\n\n"
                + "v8.25.14\n"
                + "· 表端 1.25：手机端关掉的计划不显示在手表主页，但在「管理每个计划的次数」页里能看到，标着「（已关闭）」\n\n"
                + "v8.25.13\n"
                + "· 手机端关掉提醒的计划，同步时不再发给手表；手表端会顺带清掉残留的那些，主页不再显示\n\n"
                + "v8.25.12\n"
                + "· 手表端 1.23：只管暂停/恢复，不再有禁用；暂停按钮做大了一整行\n"
                + "· 手表设置页删掉「已暂停的计划」那块，暂停后在计划页再点一次就恢复\n"
                + "· 手机端遇到手表暂停的计划，直接按「已关闭提醒」处理：灰掉、不排提醒\n\n"
                + "v8.25.11\n"
                + "· 表上暂停/禁用的计划，手机端按“关闭提醒”一样处理：那一行按钮灰掉、状态写「手表端已暂停/已禁用」，也不再排手机提醒；表上恢复后自动恢复\n\n"
                + "v8.25.10\n"
                + "· 不重复下载：手机端和表端包本地已经是最新（或更新）就不再下，直接提示并有现成的包可装\n"
                + "· 自动清理：只留最新的一份安装包，旧的删掉，免得越攒越多占空间\n"
                + "· 表端下载也改成跟手机端一样，多个镜像里取最新版本\n\n"
                + "v8.25.9\n"
                + "· 手表端 1.22：在表上暂停或禁用某个计划后，会告诉手机，手机的计划那一行后面直接标「手表端已暂停」/「手表端已禁用」\n\n"
                + "v8.25.8\n"
                + "· 检查更新把几个镜像全问一遍，取版本号最大的那个，单个镜像缓存报旧版本也不会再把新版本盖掉\n\n"
                + "v8.25.7\n"
                + "· 手表端页面的「打开小米运动健康」不再跟着灰掉：没连上手表时它照样能点，不打开它就连不上\n\n"
                + "v8.25.6\n"
                + "· 版本信息改走 gh-proxy 代理（jsDelivr 缓存太久，发新版后 App 半天看不到），下载地址也自动换代理重试\n\n"
                + "v8.25.5\n"
                + "· 加了日志：闪退、进不去都能留证据。设置页最下面「日志」能看到位置，点「分享日志」直接发出去\n\n"
                + "v8.25.4\n"
                + "· 修掉闪退：以前桌面版本和仓库里的版本不一样时，回到前台弹更新窗用了 Application 的上下文，安卓直接崩；现在改用当前页面弹\n\n"
                + "v8.25.3\n"
                + "· 手表端 1.21：计划可以暂停，暂停后主页不显示，手表设置里能选恢复或禁用；手表设置页颜色也会跟着手机变了\n\n"
                + "v8.25.2\n"
                + "· 更新改走 jsDelivr 镜像（国内能连），连不上会自动换备用的两个地址，下载也一样\n"
                + "· 更新包地址直接从仓库里取，不再走豆包网盘\n\n"
                + "v8.25.1\n"
                + "· 没网的时候，设置页点「检查更新」会在按钮下面提示「还没有连接到网络」；手表页下载表端也会先提示没网\n\n"
                + "v8.25.0\n"
                + "· 手表页新增「下载表端软件」：从 GitHub 取最新表端包，下完存到手机里，「安装手表端」就装这份最新的\n"
                + "· 没连上手表时，手表页只留「连接手表」和「同步计划」，其它按钮变灰\n\n"
                + "v8.24.7\n"
                + "· 去掉进应用的「手表未连接」弹窗；顶栏手表图标固定红色；没连上时旁边多一个「↻」重连按钮\n\n"
                + "v8.24.6\n"
                + "· 手机端「手表端设置」页背景跟主题色\n\n"
                + "v8.24.5\n"
                + "· 手表端背景色真正同步：手表存颜色、连上主动索取\n\n"
                + "v8.24.2\n"
                + "· 主题色同步到手表端\n\n"
                + "v8.17.0\n"
                + "· 「更改今日服用次数」从长按计划卡移到卡片「⋯」菜单里，长按不再触发\n\n"
                + "v8.16.0\n"
                + "· 下拉刷新加阈值：要下拉超过 76dp 松手才刷新，过程中提示「下拉刷新 → 再往下拉一点 → 松手刷新」；没松手又滑回去就取消\n\n"
                + "v8.15.1\n"
                + "· 手表图标重画：上下表带改成带圆角卡扣、表冠移到 3 点位置，更像真表\n"
                + "· 关于图标重画：圆圈里的 i 收紧字距，点和竖分开更清楚\n\n"
                + "v8.15.0\n"
                + "· 顶部按钮换成镂空线条图标（手表 / 铃铛 / 铃铛划线 / 月亮 / 齿轮 / i），统一线宽\n"
                + "· 修手表页「同步计划到手表」按钮颜色不跟主题走的问题\n\n"
                + "v8.14.0\n"
                + "· 顶部四个按钮统一用 emoji：⌚ 手表、🔔 正常提醒、🔕 静音提醒、⚙️ 设置、ℹ️ 关于；勿扰时铃铛变🌙并加蓝底\n\n"
                + "v8.13.0\n"
                + "· 顶部图标统一成同一套线稿：手表不再用 emoji，改成绘制的表带表壳指针；铃铛改成细线条空心，与齿轮、i 同粗细\n\n"
                + "v8.12.1\n"
                + "· 重画铃铛图标，形状更规整（钟身、钟口横边、钟锤都重做）\n\n"
                + "v8.12.0\n"
                + "· 勿扰改成图标：铃铛＝正常响铃，铃铛划线＝提醒但静音，蓝底月亮＝勿扰；按钮移到手表与设置之间，点一下切换，长按设时段\n\n"
                + "v8.11.0\n"
                + "· 底部标题旁加独立上下按钮：点▼收起（标题变「更多」），点▲展开\n\n"
                + "v8.10.0\n"
                + "· 底部标题可点：收起后标题变为「更多」，点「更多」展开\n\n"
                + "v8.9.0\n"
                + "· 底部收起按钮改为「更多 / 收起」，收起与展开带高度渐变动画\n\n"
                + "v8.8.0\n"
                + "· 主页底部可收起：点▼只留「今日已服用」标题，点▲恢复显示记录与按钮\n\n"
                + "v8.7.0\n"
                + "· 主页改版：药品计划放进可滑动区域（下拉刷新只刷新计划），今日已服用、添加按钮与提示固定贴在屏幕底部\n\n"
                + "v8.6.0\n"
                + "· 设置页底部新增「高级功能」，可开启「去掉确认倒计时」：确认已服用、再吃一遍、保存今日次数都立即生效，不再等 5 / 10 秒\n\n"
                + "v8.4.0\n"
                + "· 删除确认框的两个按钮分开，删除改为红色\n\n"
                + "v8.3.0\n"
                + "· 计划卡片去掉右侧开关，暂停/恢复统一放到「⋯」菜单\n"
                + "· 计划暂停后，卡片按钮显示「已关闭」\n\n"
                + "v8.2.0\n"
                + "· 「⋯」更多操作新增「暂停该计划 / 恢复该计划」，暂停后不再响铃，计划内容保留\n\n"
                + "v8.1.0\n"
                + "· English 全量翻译：弹窗、Toast、输入提示、通知、诊断页与事件记录\n\n"
                + "v8.0.0\n"
                + "· 设置页回归纵向单列；计划次数移到独立页面，「＋ / －」按钮各占半行，不再点不到\n\n"
                + "v7.9.0\n"
                + "· 手机与手表双向同步：新增/编辑/删除/打卡/撤销/下拉刷新都会推给手表\n"
                + "· 手表到点提醒由手机推送";
        String en = "v8.25.25\n"
                + "· Watch remote page: swipe/tap to scroll, page and go back on the phone\n\n"
                + "v8.25.24\n"
                + "· Pause/resume is now one shared switch across phone and watch; main list no longer goes empty\n\n"
                + "v8.25.23\n"
                + "· Settings: Feedback / report a bug, submitted to the repo via the proxy channel\n\n"
                + "v8.25.22\n"
                + "· Cat-speak is now a complete standalone language pack covering every UI string\n\n"
                + "v8.25.21\n"
                + "· Watch 1.32: sync is received on any page via a global handler; pause button keeps a fixed style\n\n"
                + "v8.25.20\n"
                + "· Watch 1.31: the pause button keeps one fixed style, only its label changes\n\n"
                + "v8.25.19\n"
                + "· Watch 1.30: button colors fixed in CSS, plans page reloads every time, send auto-retries\n\n"
                + "v8.25.18\n"
                + "· Phone pause/resume is now broadcast to the watch instantly\n"
                + "· Watch 1.29: pause and resume are separate buttons, no blank colored block\n\n"
                + "v8.25.17\n"
                + "· Any change on either side is pushed instantly; the newer edit wins\n\n"
                + "v8.25.16\n"
                + "· Plans turned off on the phone can be restored straight from the watch\n\n"
                + "v8.25.15\n"
                + "· Watch asks for the latest plans as soon as it connects\n"
                + "· Watch 1.26: pause button fixed (no more solid accent block without text after resuming)\n\n"
                + "v8.25.14\n"
                + "· Watch 1.25: plans switched off on the phone stay off on the watch home but remain visible in the per-plan page, marked as off\n\n"
                + "v8.25.13\n"
                + "· Plans disabled on the phone are no longer synced to the watch, and leftovers are cleared\n\n"
                + "v8.25.12\n"
                + "· Watch 1.23: pause/resume only (no disable), bigger pause button, no paused list on the settings page\n"
                + "· A plan paused on the watch is treated as disabled on the phone\n\n"
                + "v8.25.11\n"
                + "· Plans paused or disabled on the watch now look and behave like disabled on the phone, and their alarms are cancelled\n\n"
                + "v8.25.10\n"
                + "· No re-download when the newest package is already stored; old ones are pruned\n\n"
                + "v8.25.9\n"
                + "· Watch 1.22 reports paused/disabled plans, so the phone list shows the state\n\n"
                + "v8.25.8\n"
                + "· Update check now queries every mirror and takes the highest version code\n\n"
                + "v8.25.7\n"
                + "· The Xiaomi Health button stays clickable even when the watch is disconnected\n\n"
                + "v8.25.6\n"
                + "· Version checks now go through a raw-GitHub proxy; jsDelivr cached too long\n\n"
                + "v8.25.5\n"
                + "· Added crash and runtime logging. Settings → Log shows the folder and can share the file\n\n"
                + "v8.25.4\n"
                + "· Fixed the startup crash: the update prompt was built with the Application context and Android rejects window tokens from it\n\n"
                + "v8.25.3\n"
                + "· Watch 1.21: plans can be paused (hidden on home, restore or disable in watch settings); watch settings page now follows the theme color\n\n"
                + "v8.25.2\n"
                + "· Update checks now go through the jsDelivr mirror, with two fallback hosts\n"
                + "· Update packages are served straight from the repo\n\n"
                + "v8.25.1\n"
                + "· Offline: settings shows “not connected to network” under the check button\n\n"
                + "v8.25.0\n"
                + "· Watch page: download the latest watch build from GitHub; install uses it\n"
                + "· When disconnected, only connect and sync stay enabled\n\n"
                + "v8.17.0\n"
                + "· “Change today's taken count” moved from long-press into the card's ⋯ menu\n\n"
                + "v8.16.0\n"
                + "· Pull-to-refresh now needs a real threshold (76dp); the header hints “Pull to refresh → A bit more → Release to refresh”, and sliding back cancels it\n\n"
                + "v8.15.1\n"
                + "· Redrew the watch: rounded strap lugs and a crown at 3 o'clock so it reads as a real watch\n"
                + "· Redrew the info icon: the dot and stem are clearly separated\n\n"
                + "v8.15.0\n"
                + "· Top buttons use hollow line icons (watch / bell / bell-off / moon / gear / info) with one stroke weight\n"
                + "· Fixed the watch page sync button ignoring the theme color\n\n"
                + "v8.14.0\n"
                + "· Top buttons all use emoji: ⌚ watch, 🔔 normal, 🔕 silent, ⚙️ settings, ℹ️ about; DND turns the bell into 🌙 on a blue tile\n\n"
                + "v8.13.0\n"
                + "· Top icons now share one line style: the watch is drawn instead of an emoji, and the bell is a thin outline matching the gear\n\n"
                + "v8.12.1\n"
                + "· Redrew the bell icon so it reads like a normal bell\n\n"
                + "v8.12.0\n"
                + "· DND became an icon: bell = ring, slashed bell = silent alert, blue moon = DND; it sits between the watch and settings buttons, tap to switch, long-press for a time window\n\n"
                + "v8.11.0\n"
                + "· Separate fold button beside the bottom title: ▼ folds it (title becomes More), ▲ expands\n\n"
                + "v8.10.0\n"
                + "· Tap the bottom title to fold; it turns into “More”, tap again to expand\n\n"
                + "v8.9.0\n"
                + "· The collapse button now reads More / Hide, with an animated expand and collapse\n\n"
                + "v8.8.0\n"
                + "· The bottom area collapses: ▼ keeps only the “Taken today” title, ▲ restores the log and buttons\n\n"
                + "v8.7.0\n"
                + "· Home redesign: plans live in a scrollable area (pull to refresh refreshes them); today's log, the add button and the tip are pinned to the bottom\n\n"
                + "v8.6.0\n"
                + "· New “Advanced” section at the bottom of Settings with “No confirm countdown”: confirming a dose, an extra dose or saving today's count applies immediately instead of waiting 5 / 10 seconds\n\n"
                + "v8.4.0\n"
                + "· Delete dialog: buttons separated, Delete shown in red\n\n"
                + "v8.3.0\n"
                + "· Removed the switch on plan cards; pause/resume now lives in the “⋯” menu\n"
                + "· A paused plan shows “Paused” on its button\n\n"
                + "v8.2.0\n"
                + "· Added “Pause this plan / Resume this plan” to the “⋯” menu; pausing stops alerts but keeps the plan\n\n"
                + "v8.1.0\n"
                + "· Full English translation: dialogs, toasts, input hints, notifications, diagnostics and event log\n\n"
                + "v8.0.0\n"
                + "· Settings back to a single column; dose counts moved to their own page with large +/- buttons\n\n"
                + "v7.9.0\n"
                + "· Two-way phone/watch sync: add, edit, delete, log, undo and pull-to-refresh all push to the watch\n"
                + "· Scheduled watch alerts are pushed from the phone";
        new Loc(this)
                .setTitle(Tone.s(this, "更新日志", "更新日志喵", "Changelog"))
                .setMessage(Tone.s(this, zh, null, en))
                .setPositiveButton(Tone.s(this, "好", "好喵", "OK"), null)
                .show();
    }

    /** 底部区域收起 / 展开：收起后只剩「今日已服用」标题，按钮变成「更多」 */
    private void applyLogCollapsed(final View content, final TextView title, final TextView btn,
                                   final boolean collapsed, boolean animate) {
        title.setText(collapsed
                ? Tone.s(this, "更多", "更多喵", "More")
                : Tone.s(this, "今日已服用", "今日服用喵", "Taken today"));
        btn.setText(collapsed ? "▲" : "▼");
        final int full;
        if (collapsed) {
            full = content.getHeight();
        } else {
            content.setVisibility(View.VISIBLE);
            content.setAlpha(0f);
            full = measureFullHeight(content);
        }
        if (!animate || full <= 0) {
            content.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
            content.setAlpha(1f);
            content.setVisibility(collapsed ? View.GONE : View.VISIBLE);
            content.requestLayout();
            return;
        }
        final ViewGroup.LayoutParams lp = content.getLayoutParams();
        final int target = collapsed ? 0 : full;
        ValueAnimator va = ValueAnimator.ofInt(collapsed ? full : 0, target);
        va.setDuration(220);
        va.setInterpolator(new DecelerateInterpolator());
        va.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator a) {
                int h = (Integer) a.getAnimatedValue();
                lp.height = h;
                content.getLayoutParams().height = h;
                content.setAlpha(full <= 0 ? 1f : Math.min(1f, h * 1.4f / full));
                content.requestLayout();
            }
        });
        va.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator a) {
                if (collapsed) {
                    content.setVisibility(View.GONE);
                    lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                    content.setAlpha(1f);
                } else {
                    lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                    content.setAlpha(1f);
                }
                content.requestLayout();
            }
        });
        va.start();
    }

    private int measureFullHeight(View v) {
        int w = v.getWidth();
        if (w <= 0) w = getResources().getDisplayMetrics().widthPixels;
        v.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        return v.getMeasuredHeight();
    }

    /** 顶部按钮都用 emoji：🔔 正常、🔕 静音、🌙 勿扰（月亮带蓝底） */
    private void applyAlertIcon() {
        if (btnDnd == null) return;
        int m = RemindPrefs.alertMode(this);
        btnDnd.setImageResource(m == RemindPrefs.ALERT_DND ? R.drawable.ic_moon_line
                : m == RemindPrefs.ALERT_SILENT ? R.drawable.ic_bell_off_line
                : R.drawable.ic_bell_line);
        if (m == RemindPrefs.ALERT_DND) {
            // 月亮：白线条 + 主题色圆角底
            btnDnd.setColorFilter(0xFFFFFFFF);
            btnDnd.setImageAlpha(255);
            btnDnd.setBackground(AppTheme.round(AppTheme.accent(this), 11, this));
        } else {
            btnDnd.setColorFilter(getResources().getColor(R.color.text));
            btnDnd.setBackgroundResource(R.drawable.bg_btn_ghost);
        }
    }

    /** 混乱模式：顶部三个按钮随机跳到别的页面 */
    private void chaosHeader() {
        switch (Chaos.pick(3)) {
            case 0:
                startActivity(new Intent(this, WatchActivity.class));
                break;
            case 1:
                startActivity(new Intent(this, SettingsActivity.class));
                break;
            default:
                showAbout();
                break;
        }
    }

    /** 混乱模式：底部按钮随机做别的事 */
    private void chaosBottom() {
        switch (Chaos.pick(4)) {
            case 0:
                startActivity(new Intent(this, SettingsActivity.class));
                break;
            case 1:
                startActivity(new Intent(this, WatchActivity.class));
                break;
            case 2:
                Scheduler.testNow(this, 5000L);
                Loc.toast(this, Tone.s(this, "混乱模式：先发一条测试提醒", "混乱模式喵：先发个测试提醒",
                        "Chaos: hello, here's a test alert"), Toast.LENGTH_SHORT).show();
                break;
            default:
                Loc.toast(this, Tone.s(this, "混乱模式：这次什么也没做", "混乱模式喵：这次什么都没做",
                        "Chaos: nothing happened this time"), Toast.LENGTH_SHORT).show();
                break;
        }
    }

    /** 混乱模式：卡片上的「吃过了」随机变成别的事 */
    private void chaosCard(final Medicine m) {
        switch (Chaos.pick(6)) {
            case 0:
                askUndo(m);
                break;
            case 1:
                showEditTakenDialog(m);
                break;
            case 2:
                setPlanEnabled(m, !m.enabled);
                break;
            case 3:
                startActivity(new Intent(this, SettingsActivity.class));
                break;
            case 4:
                startActivity(new Intent(this, WatchActivity.class));
                break;
            default:
                Loc.toast(this, Tone.s(this, "混乱模式：这次没吃，先算了", "混乱模式喵：这次先不吃啦",
                        "Chaos: skipped it this time"), Toast.LENGTH_SHORT).show();
                break;
        }
    }
}
