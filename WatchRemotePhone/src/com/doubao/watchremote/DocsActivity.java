package com.doubao.watchremote;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * 疑惑问答：问题是一颗颗按钮，点开才显示答案。
 * 题目和答案都写在 assets/qa.txt：以「1. 」开头的行是新问题，后面的行是答案。
 */
public class DocsActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_docs);
        AppTheme.apply(this);

        findViewById(R.id.btnBack).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        buildItems();
    }

    private void buildItems() {
        LinearLayout box = findViewById(R.id.docsBox);
        float d = getResources().getDisplayMetrics().density;
        String text = readDoc();
        String[] lines = text.split("\n");
        String curQ = null;
        StringBuilder curA = new StringBuilder();
        for (int i = 0; i <= lines.length; i++) {
            String line = i < lines.length ? lines[i].trim() : "";
            boolean isQ = line.matches("^\\d+\\.\\s*(.*)$");
            if (isQ || i == lines.length) {
                if (curQ != null) {
                    addItem(box, curQ, curA.toString().trim(), d);
                }
                if (isQ) {
                    curQ = line.replaceFirst("^\\d+\\.\\s*", "");
                    curA = new StringBuilder();
                }
            } else if (curQ != null && line.length() > 0) {
                if (curA.length() > 0) curA.append("\n");
                curA.append(line);
            }
        }
    }

    /** 一条问答：问题按钮 + 展开后的答案 */
    private void addItem(LinearLayout box, final String q, final String a, float d) {
        final LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        ip.bottomMargin = (int) (10 * d);
        item.setLayoutParams(ip);

        final TextView head = new TextView(this);
        head.setText("＋  " + q);
        head.setTextSize(14);
        head.setTextColor(AppTheme.accent(this));
        head.setTypeface(Typeface.DEFAULT_BOLD);
        head.setBackgroundDrawable(AppTheme.softBordered(this, 14));
        head.setPadding((int) (14 * d), (int) (15 * d), (int) (14 * d), (int) (15 * d));
        head.setClickable(true);
        item.addView(head, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        final LinearLayout answer = new LinearLayout(this);
        answer.setOrientation(LinearLayout.VERTICAL);
        answer.setVisibility(View.GONE);
        answer.setBackgroundDrawable(AppTheme.cardBordered(this, 16));
        answer.setPadding((int) (14 * d), (int) (14 * d), (int) (14 * d), (int) (14 * d));
        LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        ap.topMargin = (int) (6 * d);
        item.addView(answer, ap);

        TextView body = new TextView(this);
        body.setText(a);
        body.setTextSize(13.5f);
        body.setTextColor(getResources().getColor(R.color.text));
        body.setLineSpacing((int) (4 * d), 1f);
        answer.addView(body);

        // 每题按内容给「去操作」的按钮
        if (q.contains("单独装") || q.contains("手表端")) {
            addGoButton(answer, "打开手表管理页", true, new Runnable() {
                @Override
                public void run() {
                    startActivity(new Intent(DocsActivity.this, WatchActivity.class));
                }
            }, d);
        }
        if (q.contains("收不到") || q.contains("收不到计划")) {
            addGoButton(answer, "打开手表管理页同步", true, new Runnable() {
                @Override
                public void run() {
                    startActivity(new Intent(DocsActivity.this, WatchActivity.class));
                }
            }, d);
        }
        if (q.contains("不响") || q.contains("横幅")) {
            addGoButton(answer, "打开通知类别设置", true, new Runnable() {
                @Override
                public void run() {
                    SettingsPages.openChannel(DocsActivity.this);
                }
            }, d);
            addGoButton(answer, "打开应用详情（权限 / 电池）", false, new Runnable() {
                @Override
                public void run() {
                    SettingsPages.openAppInfo(DocsActivity.this);
                }
            }, d);
        }
        if (q.contains("小米")) {
            addGoButton(answer, "打开小米运动健康", true, new Runnable() {
                @Override
                public void run() {
                    openMiHealth();
                }
            }, d);
        }
        if (q.contains("声音")) {
            Button go = new Button(this);
            go.setText("去设置提醒声音");
            go.setAllCaps(false);
            go.setTextSize(14);
            go.setTextColor(0xFFFFFFFF);
            go.setBackground(AppTheme.filled(this));
            LinearLayout.LayoutParams gp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, (int) (48 * d));
            gp.topMargin = (int) (12 * d);
            go.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openSoundSetting();
                }
            });
            answer.addView(go, gp);

            TextView tip = new TextView(this);
            tip.setText("点这个按钮会跳到系统里「吃药提醒」的通知设置，找到「提醒声音」换一个更响的铃声。");
            tip.setTextSize(11.5f);
            tip.setTextColor(getResources().getColor(R.color.sub));
            LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            tp.topMargin = (int) (8 * d);
            answer.addView(tip, tp);
        }

        head.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean open = answer.getVisibility() == View.VISIBLE;
                answer.setVisibility(open ? View.GONE : View.VISIBLE);
                head.setText((open ? "＋  " : "－  ") + q);
            }
        });

        box.addView(item);
    }

    private String readDoc() {
        StringBuilder sb = new StringBuilder();
        try {
            BufferedReader r = new BufferedReader(
                    new InputStreamReader(getAssets().open("qa.txt"), "UTF-8"));
            String line;
            while ((line = r.readLine()) != null) {
                if (line.trim().startsWith("#")) continue;
                sb.append(line).append("\n");
            }
            r.close();
        } catch (Throwable e) {
            sb.append("1. 文档读取失败\n").append(e);
        }
        return sb.toString().trim();
    }

    /** 跳到系统里本应用的通知设置（能直达「提醒声音」这一项） */
    private void openSoundSetting() {
        NotificationHelper.ensureChannel(this);
        String ch = NotificationHelper.channelId(this);
        boolean done = false;
        if (android.os.Build.VERSION.SDK_INT >= 26 && ch != null) {
            try {
                Intent i = new Intent(android.provider.Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS);
                i.putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, getPackageName());
                i.putExtra(android.provider.Settings.EXTRA_CHANNEL_ID, ch);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);
                done = true;
            } catch (Throwable ignored) {
            }
        }
        if (!done) {
            try {
                Intent i = new Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                i.putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, getPackageName());
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);
                done = true;
            } catch (Throwable ignored) {
            }
        }
        if (!done) {
            try {
                Intent i = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                i.setData(android.net.Uri.parse("package:" + getPackageName()));
                startActivity(i);
            } catch (Throwable ignored) {
            }
        }
    }

    /** 答案里的「去操作」按钮 */
    private void addGoButton(LinearLayout answer, String text, boolean primary, Runnable action, float d) {
        Button go = new Button(this);
        go.setText(text);
        go.setAllCaps(false);
        go.setTextSize(14);
        if (primary) {
            go.setTextColor(0xFFFFFFFF);
            go.setBackground(AppTheme.filled(this));
        } else {
            go.setTextColor(AppTheme.accent(this));
            go.setBackgroundDrawable(AppTheme.softBordered(this, 12));
        }
        LinearLayout.LayoutParams gp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, (int) (46 * d));
        gp.topMargin = (int) (10 * d);
        final Runnable act = action;
        go.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                act.run();
            }
        });
        answer.addView(go, gp);
    }

    /** 打开小米运动健康（配合小米手表这一题） */
    private void openMiHealth() {
        String[] pkgs = {"com.mi.health", "com.xiaomi.hm.health"};
        for (String pkg : pkgs) {
            try {
                Intent i = getPackageManager().getLaunchIntentForPackage(pkg);
                if (i != null) {
                    startActivity(i);
                    return;
                }
            } catch (Throwable ignored) {
            }
        }
        try {
            startActivity(new Intent(Intent.ACTION_VIEW,
                    android.net.Uri.parse("market://details?id=com.mi.health")));
        } catch (Throwable ignored) {
        }
    }
}
