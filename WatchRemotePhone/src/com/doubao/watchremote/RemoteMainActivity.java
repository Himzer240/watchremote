package com.doubao.watchremote;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

/** 手表遥控 —— 手机端主界面 */
public class RemoteMainActivity extends Activity {

    private TextView txtState;
    private TextView txtA11y;
    private final Handler h = new Handler(Looper.getMainLooper());

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            refresh();
            h.postDelayed(this, 900);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ScrollView sc = new ScrollView(this);
        sc.setBackgroundColor(Color.parseColor("#0F1417"));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        float d = getResources().getDisplayMetrics().density;
        int pad = (int) (20 * d);
        root.setPadding(pad, (int) (34 * d), pad, pad);

        TextView title = new TextView(this);
        title.setText("手表遥控");
        title.setTextSize(26);
        title.setTextColor(Color.parseColor("#F2F6F8"));
        title.setTypeface(Typeface.DEFAULT_BOLD);
        root.addView(title);

        TextView sub = new TextView(this);
        sub.setText("在手表上滑动屏幕，直接操控这台手机");
        sub.setTextSize(13);
        sub.setTextColor(Color.parseColor("#8A99A1"));
        sub.setPadding(0, (int) (6 * d), 0, (int) (20 * d));
        root.addView(sub);

        txtState = new TextView(this);
        txtState.setTextSize(15);
        txtState.setTextColor(Color.parseColor("#E8EEF1"));
        root.addView(txtState);

        txtA11y = new TextView(this);
        txtA11y.setTextSize(13);
        txtA11y.setPadding(0, (int) (6 * d), 0, (int) (22 * d));
        root.addView(txtA11y);

        root.addView(btn("连接手表", "#1098AD", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                WatchBridge.connectOnly(RemoteMainActivity.this);
                Toast.makeText(RemoteMainActivity.this, "正在连接手表…", Toast.LENGTH_SHORT).show();
            }
        }));
        root.addView(btn("开启无障碍服务", "#1F2C33", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
                    Toast.makeText(RemoteMainActivity.this, "在列表里找到「手表遥控」并打开", Toast.LENGTH_LONG).show();
                } catch (Throwable e) {
                    Toast.makeText(RemoteMainActivity.this, "打不开无障碍设置：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        }));
        root.addView(btn("安装手表端", "#1F2C33", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                installWatch();
            }
        }));
        root.addView(btn("使用说明", "#1F2C33", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(RemoteMainActivity.this)
                        .setTitle("怎么用")
                        .setMessage("1. 先在小米运动健康里把手表和手机连上\n"
                                + "2. 点「连接手表」\n"
                                + "3. 点「开启无障碍服务」，在系统列表里打开「手表遥控」\n"
                                + "4. 点「安装手表端」，按提示装到手表\n"
                                + "5. 手表上打开「手表遥控」→ 滑动屏幕\n\n"
                                + "上滑=下翻，下滑=上翻，左滑=返回，右滑=下一页，点一下=回顶部")
                        .setPositiveButton("知道了", null)
                        .show();
            }
        }));

        TextView foot = new TextView(this);
        foot.setText("不读屏幕内容，只执行你在手表上发出的手势。");
        foot.setTextSize(12);
        foot.setTextColor(Color.parseColor("#5F6E76"));
        foot.setPadding(0, (int) (22 * d), 0, 0);
        root.addView(foot);

        sc.addView(root);
        setContentView(sc);
        WatchBridge.setOnChanged(new Runnable() {
            @Override
            public void run() {
                refresh();
            }
        });
        try {
            WatchBridge.start(this);
        } catch (Throwable ignored) {
        }
    }

    /** 把内置的手表端包导出，交给系统弹窗选安装工具 */
    private void installWatch() {
        try {
            File dir = ApkProvider.dir(this);
            File out = new File(dir, "手表遥控-v1.0.bin");
            InputStream in = getAssets().open("watchremote.bin");
            FileOutputStream os = new FileOutputStream(out);
            byte[] buf = new byte[4096];
            int len;
            while ((len = in.read(buf)) > 0) os.write(buf, 0, len);
            os.flush(); os.close(); in.close();

            Uri uri = ApkProvider.uriFor(out);
            Intent view = new Intent(Intent.ACTION_VIEW);
            view.setDataAndType(uri, "application/octet-stream");
            view.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(Intent.createChooser(view, "选一个装手表的程序（如表盘自定义工具）"));
            Toast.makeText(this, "选好程序后点安装，就装到手表了", Toast.LENGTH_LONG).show();
        } catch (Throwable e) {
            try {
                Intent any = new Intent(Intent.ACTION_VIEW);
                any.setDataAndType(ApkProvider.uriFor(new File(ApkProvider.dir(this), "手表遥控-v1.0.bin")), "*/*");
                any.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(Intent.createChooser(any, "选一个装手表的程序"));
            } catch (Throwable e2) {
                Toast.makeText(this, "导出失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private Button btn(String text, String color, View.OnClickListener l) {
        float d = getResources().getDisplayMetrics().density;
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextSize(16);
        b.setTextColor(Color.WHITE);
        GradientDrawable g = new GradientDrawable();
        g.setColor(Color.parseColor(color));
        g.setCornerRadius(14 * d);
        b.setBackground(g);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, (int) (52 * d));
        lp.bottomMargin = (int) (12 * d);
        b.setLayoutParams(lp);
        b.setOnClickListener(l);
        return b;
    }

    private void refresh() {
        if (txtState == null) return;
        try {
            txtState.setText("手表连接：" + WatchBridge.stateText(this));
        } catch (Throwable ignored) {
        }
        boolean on = RemoteService.alive();
        txtA11y.setText(on ? "无障碍：已开启，手表滑动会直接生效"
                : "无障碍：未开启，点下面按钮去开启");
        txtA11y.setTextColor(on ? Color.parseColor("#5BD6A2") : Color.parseColor("#F0A35E"));
    }

    @Override
    protected void onResume() {
        super.onResume();
        h.post(tick);
    }

    @Override
    protected void onPause() {
        super.onPause();
        h.removeCallbacks(tick);
    }
}
