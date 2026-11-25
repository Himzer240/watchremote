package com.doubao.watchremote;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Scanner;

public class SettingsActivity extends Activity {

    private TextView tvCheck;
    private boolean checking = false;
    private int dotStep = 0;
    private final android.os.Handler ui = new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable dots = new Runnable() {
        @Override
        public void run() {
            if (!checking || tvCheck == null) return;
            dotStep = (dotStep + 1) % 3;
            String d = dotStep == 0 ? "." : (dotStep == 1 ? ".." : "...");
            tvCheck.setVisibility(View.VISIBLE);
            tvCheck.setText("正在检查" + d);
            ui.postDelayed(this, 350);
        }
    };

    private static final int REQ_EXPORT = 2001;
    private static final int REQ_IMPORT = 2002;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        AppTheme.apply(this);

        findViewById(R.id.btnBack).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        findViewById(R.id.rowQa).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(SettingsActivity.this, DocsActivity.class));
            }
        });

        tvCheck = findViewById(R.id.tvCheckStatus);

        findViewById(R.id.rowUpdate).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checking) return;
                startCheck();
            }
        });

        findViewById(R.id.rowAppInfo).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SettingsPages.openAppInfo(SettingsActivity.this);
            }
        });

        findViewById(R.id.rowChannel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SettingsPages.openChannel(SettingsActivity.this);
            }
        });

        findViewById(R.id.rowFsi).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SettingsPages.openFullScreenIntent(SettingsActivity.this);
            }
        });

        findViewById(R.id.rowColor).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickColor();
            }
        });

        findViewById(R.id.rowRepeat).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickRepeat();
            }
        });

        findViewById(R.id.rowWatch).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(SettingsActivity.this, WatchActivity.class));
            }
        });

        findViewById(R.id.rowTone).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String[] items = {
                        Lang.s(SettingsActivity.this, "书面语（简洁，适合所有人）"),
                        Lang.s(SettingsActivity.this, "喵语（带语气词和颜文字）"),
                        "English"};
                new Loc(SettingsActivity.this)
                        .setTitle("表达风格")
                        .setSingleChoiceItems(items, Tone.style(SettingsActivity.this),
                                new android.content.DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(android.content.DialogInterface d, int which) {
                                        Tone.setStyle(SettingsActivity.this, which);
                                        d.dismiss();
                                        recreate();
                                    }
                                })
                        .show();
            }
        });

        findViewById(R.id.rowElder).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean on = !Tone.elder(SettingsActivity.this);
                Tone.setElder(SettingsActivity.this, on);
                Loc.toast(SettingsActivity.this,
                        on ? "已开启老年人模式：字体放大、界面精简、隐藏手表功能"
                                : "已关闭老年人模式", android.widget.Toast.LENGTH_SHORT).show();
                recreate();
            }
        });

        findViewById(R.id.rowNoCountdown).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean on = !RemindPrefs.noCountdown(SettingsActivity.this);
                RemindPrefs.setNoCountdown(SettingsActivity.this, on);
                updateRowLabels();
                Loc.toast(SettingsActivity.this,
                        Tone.s(SettingsActivity.this,
                                on ? "已开启：确认不再倒计时" : "已关闭：确认恢复倒计时",
                                on ? "开好啦喵：确认不用再等啦～" : "关掉啦喵：确认又要等几秒～",
                                on ? "On: no more countdown" : "Off: countdown restored"),
                        Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.rowLog).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLog();
            }
        });
        findViewById(R.id.rowFeedback).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showFeedback();
            }
        });
        findViewById(R.id.rowDiag).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(SettingsActivity.this, DiagnosticsActivity.class));
            }
        });

        findViewById(R.id.btnClear).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmClear();
            }
        });

        findViewById(R.id.btnExport).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exportBackup();
            }
        });

        final android.widget.Button rowChaos = findViewById(R.id.rowChaos);
        if (rowChaos != null) {
            rowChaos.setText(Chaos.on() ? "混乱模式：已开启（点一下关闭）" : "开启混乱模式");
            rowChaos.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (Chaos.on()) {
                        Chaos.set(false);
                        rowChaos.setText("开启混乱模式");
                        Loc.toast(SettingsActivity.this, Tone.s(SettingsActivity.this,
                                "混乱模式已关闭", "混乱模式关掉啦喵", "Chaos mode off"),
                                Toast.LENGTH_SHORT).show();
                        recreate();
                        return;
                    }
                    new Loc(SettingsActivity.this)
                            .setTitle("开启混乱模式")
                            .setMessage("开启后图标不变，但点进去做的事每次都不一样，页面底色也会一直变色。\n把应用从后台删掉、重新打开就会恢复正常。")
                            .setNegativeButton("取消", null)
                            .setPositiveButton("开启", new android.content.DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(android.content.DialogInterface d, int w) {
                                    Chaos.set(true);
                                    Loc.toast(SettingsActivity.this,
                                            "混乱模式已开启（" + Chaos.styleName(SettingsActivity.this)
                                                    + "）：删掉后台就能恢复",
                                            Toast.LENGTH_LONG).show();
                                    recreate();
                                }
                            })
                            .show();
                }
            });
            rowChaos.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (!Chaos.on()) return false;
                    Chaos.set(false);
                    rowChaos.setText("开启混乱模式");
                    Loc.toast(SettingsActivity.this, "混乱模式已关闭", Toast.LENGTH_SHORT).show();
                    recreate();
                    return true;
                }
            });
        }

        // 混乱模式：每一行点下去做的是别的事
        if (Chaos.actions()) {
            int[] rows = {R.id.rowAppInfo, R.id.rowChannel, R.id.rowFsi, R.id.rowColor,
                    R.id.rowRepeat, R.id.rowWatch, R.id.rowTone, R.id.rowElder,
                    R.id.rowNoCountdown, R.id.rowDiag};
            for (int id : rows) {
                View row = findViewById(id);
                if (row == null) continue;
                row.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        chaosAction(Chaos.pick(10));
                    }
                });
            }
        }

        findViewById(R.id.btnImport).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                importBackup();
            }
        });
    }

    private void updateRowLabels() {
        boolean en = Tone.style(this) == Tone.EN;
        try {
            android.widget.Button tone = findViewById(R.id.rowTone);
            if (tone != null) {
                tone.setText((en ? "Tone: " : "表达风格：")
                        + (en ? styleNameEn() : Tone.styleName(this)));
            }
            android.widget.Button elder = findViewById(R.id.rowElder);
            if (elder != null) {
                elder.setText((en ? "Elder mode: " : "老年人模式：")
                        + Lang.s(this, Tone.elder(this) ? "已开启" : "关闭"));
            }
            android.widget.Button nc = findViewById(R.id.rowNoCountdown);
            if (nc != null) {
                boolean on = RemindPrefs.noCountdown(this);
                nc.setText((en ? "No confirm countdown: " : "去掉确认倒计时：")
                        + Lang.s(this, on ? "已开启" : "关闭"));
            }
        } catch (Throwable ignored) {
        }
    }

    private String styleNameEn() {
        int st = Tone.style(this);
        return st == Tone.CAT ? "Cat-speak" : (st == Tone.EN ? "English" : "Plain");
    }

    @Override
    protected void onResume() {
        super.onResume();

        updateRowLabels();
        android.widget.Button cb = findViewById(R.id.rowColor);
        cb.setText((Tone.style(this) == Tone.EN ? "Theme: " : "主题颜色：")
                + AppTheme.hex(AppTheme.accent(this)));

        android.widget.Button r = findViewById(R.id.rowRepeat);
        updateRowLabels();
        int mins = RemindPrefs.repeatMinutes(this);
        if (Tone.style(this) == Tone.EN) {
            r.setText(mins <= 0 ? "Repeat: none" : ("Repeat: every " + mins + " min"));
        } else {
            r.setText(mins <= 0 ? "超时重复提醒：不重复" : ("超时重复提醒：每 " + mins + " 分钟一次"));
        }
    }

    /** 点「检查更新」：按钮下面先出「正在检查...」小字，结果出来再收 */
    private void startCheck() {
        if (!UpdateChecker.isOnline(this)) {
            if (tvCheck != null) {
                tvCheck.setVisibility(View.VISIBLE);
                tvCheck.setText("还没有连接到网络");
            }
            return;
        }
        checking = true;
        dotStep = 0;
        ui.post(dots);
        UpdateChecker.check(this, new UpdateChecker.OnCheck() {
            @Override
            public void onResult(boolean ok, String message) {
                checking = false;
                ui.removeCallbacks(dots);
                if (!ok) {
                    // 没查到（超时、断网都算）：留在原地提示多来几次
                    if (tvCheck != null) {
                        tvCheck.setVisibility(View.VISIBLE);
                        tvCheck.setText("没检查到，再多点几次试试");
                    }
                    return;
                }
                if (tvCheck != null) tvCheck.setVisibility(View.GONE);
                if (message.startsWith("已经是最新版")) {
                    Loc.toast(SettingsActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        checking = false;
        ui.removeCallbacks(dots);
        if (tvCheck != null) tvCheck.setVisibility(View.GONE);
    }

    private void pickColor() {
        final LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (18 * getResources().getDisplayMetrics().density);
        box.setPadding(pad, pad / 2, pad, 0);

        TextView tip = new TextView(this);
        tip.setText("选一个预设，或者自己输入颜色码（#RRGGBB）");
        tip.setTextSize(12);
        tip.setTextColor(0xFF77808F);
        box.addView(tip);

        final EditText input = new EditText(this);
        input.setHint("#RRGGBB");
        input.setText(AppTheme.hex(AppTheme.accent(this)));
        input.setTextSize(14);

        // 预设色卡：两行，每行 5 个
        int size = (int) (38 * getResources().getDisplayMetrics().density);
        int gap = (int) (8 * getResources().getDisplayMetrics().density);
        LinearLayout row = null;
        for (int i = 0; i < AppTheme.PRESETS.length; i++) {
            if (i % 5 == 0) {
                row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setPadding(0, gap, 0, 0);
                box.addView(row);
            }
            TextView sw = new TextView(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
            lp.rightMargin = gap;
            sw.setLayoutParams(lp);
            android.graphics.drawable.GradientDrawable d =
                    AppTheme.round(AppTheme.PRESETS[i], 10, this);
            d.setStroke((int) (1 * getResources().getDisplayMetrics().density),
                    0x33000000);
            sw.setBackgroundDrawable(d);
            final int color = AppTheme.PRESETS[i];
            sw.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    input.setText(AppTheme.hex(color));
                }
            });
            row.addView(sw);
        }

        box.addView(input);

        new Loc(this)
                .setTitle("主题颜色")
                .setView(box)
                .setNegativeButton("取消", null)
                .setPositiveButton("保存", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String v = input.getText().toString().trim();
                        if (!v.startsWith("#")) v = "#" + v;
                        if (!v.matches("#[0-9a-fA-F]{6}")) {
                            Loc.toast(SettingsActivity.this,
                                    "颜色码格式不对，例：#2F6FED", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        AppTheme.setAccent(SettingsActivity.this,
                                android.graphics.Color.parseColor(v));
                        Loc.toast(SettingsActivity.this, "主题色已更新", Toast.LENGTH_SHORT).show();
                        try {
                            WatchSync.pushTheme(SettingsActivity.this);
                        } catch (Throwable ignored) {
                        }
                        recreate();
                    }
                })
                .show();
    }

    private void pickRepeat() {
        final int[] vals = {0, 1, 3, 5, 10, 15, 30};
        String[] items = {Lang.s(this, "不重复"), Lang.s(this, "每 1 分钟"), Lang.s(this, "每 3 分钟"),
                Lang.s(this, "每 5 分钟"), Lang.s(this, "每 10 分钟"), Lang.s(this, "每 15 分钟"),
                Lang.s(this, "每 30 分钟")};
        int cur = RemindPrefs.repeatMinutes(this);
        int idx = 3;
        for (int i = 0; i < vals.length; i++) if (vals[i] == cur) idx = i;
        new Loc(this)
                .setTitle("超时没吃药，隔多久再提醒")
                .setSingleChoiceItems(items, idx, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        RemindPrefs.setRepeatMinutes(SettingsActivity.this, vals[which]);
                        dialog.dismiss();
                        onResume();
                        Loc.toast(SettingsActivity.this,
                                "已设为：" + items[which], Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    private void exportBackup() {
        boolean en = Tone.style(this) == Tone.EN;
        final String[] items = en
                ? new String[]{"Medicine plans", "Dose log", "Theme colour", "DND settings", "Reminder settings"}
                : new String[]{"药品计划", "服药记录", "主题颜色", "勿扰设置", "提醒设置（重复间隔、默认方式）"};
        final boolean[] checked = {true, true, true, true, true};
        new Loc(this)
                .setTitle("导出哪些内容")
                .setMultiChoiceItems(items, checked, new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                        checked[which] = isChecked;
                    }
                })
                .setNegativeButton("取消", null)
                .setPositiveButton("导出", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        int mask = 0;
                        if (checked[0]) mask |= MedStore.SEC_MEDS;
                        if (checked[1]) mask |= MedStore.SEC_LOGS;
                        if (checked[2]) mask |= MedStore.SEC_THEME;
                        if (checked[3]) mask |= MedStore.SEC_DND;
                        if (checked[4]) mask |= MedStore.SEC_REMIND;
                        if (mask == 0) {
                            Loc.toast(SettingsActivity.this, "一项都没选，没有导出", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        doExport(mask);
                    }
                })
                .show();
    }

    private void doExport(int mask) {
        try {
            java.io.File f = MedStore.exportToFolder(this, mask);
            new Loc(this)
                    .setTitle("备份已导出")
                    .setMessage(Tone.s(this, "已存到：\n" + f.getAbsolutePath(),
                            null, "Saved to:\n" + f.getAbsolutePath()))
                    .setPositiveButton("好", null)
                    .show();
        } catch (Throwable t) {
            Loc.toast(this, "导出失败：" + t.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void importBackup() {
        final java.io.File[] fs = MedStore.listBackups(this);
        if (fs.length == 0) {
            new Loc(this)
                    .setTitle("还没有备份")
                    .setMessage(Tone.s(this,
                            "备份文件夹是空的：\n" + MedStore.backupDir(this).getAbsolutePath()
                                    + "\n\n先点上面的「导出备份」生成一份。",
                            null,
                            "The backup folder is empty:\n" + MedStore.backupDir(this).getAbsolutePath()
                                    + "\n\nTap “Export backup” above to create one."))
                    .setPositiveButton("好", null)
                    .show();
            return;
        }
        String[] names = new String[fs.length];
        SimpleDateFormat hm = new SimpleDateFormat("MM-dd HH:mm", Locale.CHINA);
        for (int i = 0; i < fs.length; i++) {
            names[i] = fs[i].getName() + "\n" + hm.format(new Date(fs[i].lastModified()))
                    + " · " + (fs[i].length() / 1024) + " KB";
        }
        new Loc(this)
                .setTitle("选一个备份恢复")
                .setItems(names, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        confirmRestore(fs[which]);
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void confirmRestore(final java.io.File f) {
        String json;
        int backupMeds = 0;
        try {
            json = MedStore.readFile(f);
            org.json.JSONArray a = new org.json.JSONObject(json).optJSONArray("meds");
            backupMeds = a == null ? 0 : a.length();
        } catch (Throwable t) {
            Loc.toast(this, "恢复失败：文件格式不对", Toast.LENGTH_LONG).show();
            return;
        }
        int nowMeds = MedStore.load(this).size();
        if (backupMeds > 0 && nowMeds > 0) {
            new Loc(this)
                    .setTitle(Tone.s(this, "现在已有 " + nowMeds + " 个计划", null,
                            "Currently " + nowMeds + " plans"))
                    .setMessage(Tone.s(this,
                            "这份备份里有 " + backupMeds + " 个计划，要覆盖掉现在的，还是把备份里的加进来？\n\n"
                                    + f.getName() + "\n\n覆盖：现在的计划全部被替换。\n合并：现在的计划保留，备份里的追加在后面。",
                            null,
                            "This backup has " + backupMeds + " plans. Overwrite the current plans or merge them in?\n\n"
                                    + f.getName() + "\n\nOverwrite: current plans are replaced.\nMerge: current plans stay, backup plans are appended."))
                    .setNegativeButton("取消", null)
                    .setNeutralButton("合并新增", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            doRestore(f, MedStore.MODE_MERGE);
                        }
                    })
                    .setPositiveButton("覆盖现有", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            doRestore(f, MedStore.MODE_REPLACE);
                        }
                    })
                    .show();
            return;
        }
        new Loc(this)
                .setTitle("恢复备份")
                .setMessage(Tone.s(this,
                        "会用这个文件里的内容恢复，文件里没有的项目不动。\n\n" + f.getName(),
                        null,
                        "Restores what this file contains; other items stay unchanged.\n\n" + f.getName()))
                .setNegativeButton("取消", null)
                .setPositiveButton("恢复", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        doRestore(f, MedStore.MODE_REPLACE);
                    }
                })
                .show();
    }

    private void doRestore(java.io.File f, int mode) {
        try {
            String msg = MedStore.importFromFile(this, f, mode);
            Scheduler.rescheduleAll(this);
            AppTheme.apply(this);
            Loc.toast(this,
                    Tone.s(this, "已恢复：" + msg + "（主题颜色重启后全部生效）",
                            null,
                            "Restored: " + Loc.frag(this, msg) + " (theme applies fully after restart)"),
                    Toast.LENGTH_LONG).show();
        } catch (Throwable t) {
            Loc.toast(this, "恢复失败：文件格式不对", Toast.LENGTH_LONG).show();
        }
    }

    private void confirmClear() {
        float dp = getResources().getDisplayMetrics().density;
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (20 * dp);
        box.setPadding(pad, (int) (8 * dp), pad, 0);

        TextView msg = new TextView(this);
        msg.setText("会删掉全部吃药计划和服药记录，取消已排好的提醒，并把勿扰、提醒方式、主题颜色、识别 Key 全部恢复默认。\n\n删掉的计划无法恢复。");
        msg.setTextSize(14);
        msg.setTextColor(0xFF444B57);
        msg.setLineSpacing(0, 1.25f);
        box.addView(msg);

        final android.widget.CheckBox cb = new android.widget.CheckBox(this);
        cb.setText("同时删掉备份文件夹里的备份文件");
        cb.setTextSize(14);
        cb.setTextColor(0xFF1B1F27);
        cb.setButtonTintList(android.content.res.ColorStateList.valueOf(AppTheme.accent(this)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = (int) (14 * dp);
        cb.setLayoutParams(lp);
        box.addView(cb);

        new Loc(this)
                .setTitle("清除所有数据？")
                .setView(box)
                .setNegativeButton("取消", null)
                .setPositiveButton("继续", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        countdownClear(cb.isChecked());
                    }
                })
                .show();
    }

    private void countdownClear(final boolean deleteBackups) {
        final AlertDialog d = new Loc(this)
                .setTitle("再确认一次")
                .setMessage(deleteBackups
                        ? "5 秒后可以点确认。\n\n已勾选：备份文件也会被删掉，删了找不回来。"
                        : "5 秒后可以点确认。\n\n备份文件会保留。")
                .setNegativeButton("算了", null)
                .setPositiveButton("确认清除", null)
                .create();
        d.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                final android.widget.Button ok = d.getButton(AlertDialog.BUTTON_POSITIVE);
                if (RemindPrefs.noCountdown(SettingsActivity.this)) {
                    ok.setEnabled(true);
                    ok.setText(Lang.s(SettingsActivity.this, "确认清除"));
                    return;
                }
                ok.setEnabled(false);
                ok.setText(cnt("请稍等", "Please wait", 5));
                final int[] left = {5};
                final android.os.Handler h = new android.os.Handler();
                h.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        left[0]--;
                        if (left[0] > 0) {
                            ok.setText(cnt("请稍等", "Please wait", left[0]));
                            h.postDelayed(this, 1000);
                        } else {
                            ok.setEnabled(true);
                            ok.setText(Lang.s(SettingsActivity.this, "确认清除"));
                        }
                    }
                }, 1000);
                ok.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        MedStore.clearAll(SettingsActivity.this, deleteBackups);
                        d.dismiss();
                        AppTheme.apply(SettingsActivity.this);
                        Loc.toast(SettingsActivity.this,
                                deleteBackups
                                        ? "已清空：计划、记录、设置恢复默认，备份文件也删掉了"
                                        : "已清空：计划、记录、设置恢复默认；备份文件保留",
                                Toast.LENGTH_LONG).show();
                        onResume();
                    }
                });
            }
        });
        d.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();

        if (requestCode == REQ_EXPORT) {
            try {
                OutputStream os = getContentResolver().openOutputStream(uri);
                if (os == null) throw new Exception("无法写入");
                os.write(MedStore.exportData(this).getBytes("UTF-8"));
                os.flush();
                os.close();
                Loc.toast(this, "备份已导出", Toast.LENGTH_SHORT).show();
            } catch (Throwable t) {
                Loc.toast(this, "导出失败：" + t.getMessage(), Toast.LENGTH_LONG).show();
            }
            return;
        }

        if (requestCode == REQ_IMPORT) {
            final String json;
            try {
                InputStream is = getContentResolver().openInputStream(uri);
                if (is == null) throw new Exception("无法读取");
                Scanner sc = new Scanner(is, "UTF-8").useDelimiter("\\A");
                json = sc.hasNext() ? sc.next() : "";
                sc.close();
            } catch (Throwable t) {
                Loc.toast(this, "读取失败：" + t.getMessage(), Toast.LENGTH_LONG).show();
                return;
            }
            new Loc(this)
                    .setTitle("恢复备份")
                    .setMessage(Tone.s(this,
                            "会用备份文件里的内容覆盖当前对应项目，文件里没有的项目不动。",
                            null,
                            "Overwrites the matching items with this backup; other items stay unchanged."))
                    .setNegativeButton("取消", null)
                    .setPositiveButton("覆盖恢复", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            try {
                                String msg = MedStore.importData(SettingsActivity.this, json);
                                Scheduler.rescheduleAll(SettingsActivity.this);
                                Loc.toast(SettingsActivity.this,
                                        "已恢复：" + msg, Toast.LENGTH_LONG).show();
                            } catch (Throwable t) {
                                Loc.toast(SettingsActivity.this,
                                        "恢复失败：文件格式不对", Toast.LENGTH_LONG).show();
                            }
                        }
                    })
                    .show();
        }
    }

    private String cnt(String zhBase, String enBase, int n) {
        return Tone.style(this) == Tone.EN ? (enBase + " (" + n + ")") : (zhBase + "（" + n + "）");
    }

    /** 混乱模式：设置页随便点哪一行都可能触发别的功能 */
    private void chaosAction(int which) {
        switch (which) {
            case 0:
                SettingsPages.openAppInfo(this);
                break;
            case 1:
                SettingsPages.openChannel(this);
                break;
            case 2:
                SettingsPages.openFullScreenIntent(this);
                break;
            case 3:
                pickColor();
                break;
            case 4:
                pickRepeat();
                break;
            case 5:
                startActivity(new Intent(this, WatchActivity.class));
                break;
            case 6:
                Tone.setStyle(this, Chaos.pick(3));
                recreate();
                break;
            case 7: {
                boolean on = !Tone.elder(this);
                Tone.setElder(this, on);
                recreate();
                break;
            }
            case 8: {
                boolean on = !RemindPrefs.noCountdown(this);
                RemindPrefs.setNoCountdown(this, on);
                updateRowLabels();
                break;
            }
            default:
                startActivity(new Intent(this, DiagnosticsActivity.class));
                break;
        }
    }

    /** 日志：告诉用户存哪、能直接分享出来 */
    /** 反馈 / 举报 Bug：填写后提交到仓库 Issues */
    private void showFeedback() {
        android.widget.LinearLayout box = new android.widget.LinearLayout(this);
        box.setOrientation(android.widget.LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        box.setPadding(pad, pad / 2, pad, 0);
        final android.widget.EditText name = new android.widget.EditText(this);
        name.setHint(Lang.s(this, "你的称呼（可不填）"));
        name.setSingleLine(true);
        box.addView(name);
        final android.widget.EditText text = new android.widget.EditText(this);
        text.setHint(Lang.s(this, "说说遇到的问题：哪个版本、手机还是手表、做了什么、出了什么状况"));
        text.setMinLines(4);
        text.setGravity(android.view.Gravity.TOP);
        box.addView(text);
        new Loc(this)
                .setTitle(Lang.s(this, "反馈 / 举报 Bug"))
                .setView(box)
                .setNegativeButton(Lang.s(this, "取消"), null)
                .setPositiveButton(Lang.s(this, "发送反馈"), new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface d, int which) {
                        String t = text.getText().toString().trim();
                        if (t.isEmpty()) {
                            Loc.toast(SettingsActivity.this, Lang.s(SettingsActivity.this, "先写点内容再发送"),
                                    android.widget.Toast.LENGTH_SHORT).show();
                            return;
                        }
                        Loc.toast(SettingsActivity.this, Lang.s(SettingsActivity.this, "正在发送…"),
                                android.widget.Toast.LENGTH_SHORT).show();
                        Feedback.submit(SettingsActivity.this, name.getText().toString(), t, new Feedback.Cb() {
                            @Override
                            public void done(boolean ok, String msg) {
                                Loc.toast(SettingsActivity.this, msg, android.widget.Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                })
                .show();
    }

    private void showLog() {
        final java.io.File dir = Logx.dir(this);
        final java.io.File crash = Logx.latestCrash(this);
        final java.io.File app = Logx.appLog(this);
        String size = "";
        try {
            java.io.File f = (crash != null) ? crash : app;
            if (f != null && f.exists()) {
                size = "（" + Math.max(1, f.length() / 1024) + " KB）";
            }
        } catch (Throwable ignored) {
        }
        String msg = "日志存在手机的这个文件夹里：\n" + dir.getAbsolutePath()
                + "\n\n"
                + (crash != null ? ("最近一次闪退日志：" + crash.getName() + size) : "还没有闪退记录")
                + "\n"
                + "运行日志：app.log" + (app != null && app.exists() ? "" : "（还没有内容）")
                + "\n\n点「分享日志」会直接把文件发到聊天窗口，发给做这个 App 的人就行。";
        AlertDialog.Builder b = new Loc(this)
                .setTitle("日志")
                .setMessage(msg)
                .setPositiveButton("分享日志", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) {
                        shareLog();
                    }
                })
                .setNeutralButton("清空", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) {
                        try {
                            java.io.File dd = Logx.dir(SettingsActivity.this);
                            java.io.File[] fs = dd.listFiles();
                            if (fs != null) {
                                for (java.io.File f : fs) f.delete();
                            }
                            Logx.log(SettingsActivity.this, "app", "日志已清空");
                            Toast.makeText(SettingsActivity.this, "日志已清空", Toast.LENGTH_SHORT).show();
                        } catch (Throwable ignored) {
                        }
                    }
                })
                .setNegativeButton("知道了", null);
        AlertDialog dlg = b.create();
        try {
            dlg.show();
        } catch (Throwable ignored) {
        }
    }

    /** 把日志复制到能分享的目录，再走系统分享 */
    private void shareLog() {
        try {
            java.io.File src = Logx.latestCrash(this);
            if (src == null) src = Logx.appLog(this);
            if (src == null || !src.exists() || src.length() == 0) {
                Toast.makeText(this, "还没有日志，先让问题发生一次再来看", Toast.LENGTH_LONG).show();
                return;
            }
            String stamp = new SimpleDateFormat("MMdd-HHmm", Locale.US).format(new Date());
            java.io.File out = new java.io.File(ApkProvider.dir(this), "日志-" + stamp + ".txt");
            java.io.FileOutputStream fos = new java.io.FileOutputStream(out);
            java.io.FileInputStream fis = new java.io.FileInputStream(src);
            byte[] buf = new byte[4096];
            int n;
            while ((n = fis.read(buf)) > 0) fos.write(buf, 0, n);
            fis.close();
            fos.close();

            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("text/plain");
            i.putExtra(Intent.EXTRA_STREAM, ApkProvider.uriFor(out));
            i.putExtra(Intent.EXTRA_SUBJECT, "吃药提醒日志 " + src.getName());
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(i, "把日志发到哪"));
        } catch (Throwable e) {
            Toast.makeText(this, "分享失败：" + e, Toast.LENGTH_LONG).show();
        }
    }
}
