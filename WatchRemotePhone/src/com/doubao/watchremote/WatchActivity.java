package com.doubao.watchremote;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class WatchActivity extends Activity {

    private static final String ASSET_NAME = "watchremote.bin";
    private static final String OUT_NAME = "手表遥控-v1.0.bin";

    private TextView txtSync;
    private TextView txtPath;
    private TextView txtConn;
    private final android.os.Handler poll = new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable pollTask = new Runnable() {
        @Override
        public void run() {
            if (txtConn != null) txtConn.setText(Tone.s(WatchActivity.this, "当前状态：", "现在状态喵：", "Status: ")
                    + WatchBridge.stateText(WatchActivity.this));
            applyConnectedState();
            poll.postDelayed(this, 900);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_watch);
        AppTheme.apply(this);

        txtSync = (TextView) findViewById(R.id.txtSync);
        txtPath = (TextView) findViewById(R.id.txtPath);
        txtConn = (TextView) findViewById(R.id.txtConn);

        findViewById(R.id.btnBack).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        findViewById(R.id.btnConnect).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                WatchBridge.connectOnly(WatchActivity.this);
                if (txtConn != null) txtConn.setText(Tone.s(WatchActivity.this, "当前状态：", "现在状态喵：", "Status: ")
                    + Lang.s(WatchActivity.this, "连接中"));
            }
        });

        findViewById(R.id.btnSync).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startSync();
            }
        });

        findViewById(R.id.btnDownloadWatch).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                downloadWatchPkg();
            }
        });

        findViewById(R.id.btnExport).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exportPackage();
            }
        });

        findViewById(R.id.btnOpenHealth).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openHealth();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            WatchSync.pushTheme(this);
        } catch (Throwable ignored) {
        }
        poll.post(pollTask);
    }

    @Override
    protected void onPause() {
        super.onPause();
        poll.removeCallbacks(pollTask);
    }

    private void startSync() {
        txtSync.setText(Lang.s(this, "准备同步…"));
        new WatchSync(this, new WatchSync.Callback() {
            @Override
            public void onStatus(String msg) {
                txtSync.setText(msg);
            }

            @Override
            public void onDone(boolean ok, String msg) {
                txtSync.setText((ok ? "✔ " : "✖ ") + msg);
                Loc.toast(WatchActivity.this, msg, Toast.LENGTH_LONG).show();
            }
        }).sync();
    }

    /** 没连上手表时：只留「连接手表」和「同步计划」，其它按钮灰掉 */
    private void applyConnectedState() {
        boolean connected = WatchBridge.stateText(this).startsWith("已连接");
        setBtnEnabled(R.id.btnDownloadWatch, connected);
        setBtnEnabled(R.id.btnExport, connected);
        // 「打开小米运动健康」始终可点：没连上手表时恰恰需要靠它去连
        setBtnEnabled(R.id.btnOpenHealth, true);
    }

    private void setBtnEnabled(int id, boolean on) {
        View v = findViewById(id);
        if (v == null) return;
        v.setEnabled(on);
        v.setClickable(on);
        v.setAlpha(on ? 1f : 0.42f);
    }

    /** 从 GitHub 的 version.json 里取表端包地址并下载，存到手机内部，以后安装就用这份最新的 */
    private void downloadWatchPkg() {
        if (!UpdateChecker.isOnline(this)) {
            Loc.toast(this, "还没有连接到网络", Toast.LENGTH_SHORT).show();
            txtPath.setText("还没有连接到网络，连上网络再下载表端软件");
            return;
        }
        final TextView tv = new TextView(this);
        float d = getResources().getDisplayMetrics().density;
        tv.setPadding((int) (20 * d), (int) (20 * d), (int) (20 * d), (int) (10 * d));
        tv.setTextSize(13);
        tv.setText("正在读取 GitHub 上的表端版本…");
        final android.app.AlertDialog dlg = new Loc(this)
                .setTitle("下载表端软件")
                .setView(tv)
                .setNegativeButton("取消", null)
                .create();
        dlg.show();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    org.json.JSONObject o = UpdateChecker.latestInfo();
                    String url = o.optString("watch", "");
                    final String ver = o.optString("watchVersion", "");
                    if (url.isEmpty()) {
                        throw new Exception("version.json 里没有 watch 字段");
                    }
                    File dir = new File(getFilesDir(), "watch");
                    if (!dir.exists() && !dir.mkdirs()) throw new Exception("建目录失败");
                    final File out = new File(dir, "medwatch-" + (ver.isEmpty() ? "latest" : ver) + ".bin");
                    // 本地已经是这个版本或更新的，就不再下载，免得反复下载把内存塞满
                    String curPath = getSharedPreferences("app_prefs", MODE_PRIVATE).getString("watch_pkg_path", "");
                    String curVer = getSharedPreferences("app_prefs", MODE_PRIVATE).getString("watch_pkg_version", "");
                    final File curFile = (curPath == null || curPath.isEmpty()) ? null : new File(curPath);
                    if (curFile != null && curFile.exists() && curFile.length() > 1000
                            && !ver.isEmpty() && verCmp(curVer, ver) >= 0) {
                        pruneWatch(dir, curFile);
                        final String have = curVer.isEmpty() ? ver : curVer;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                dlg.dismiss();
                                txtPath.setText("本地已经有最新的表端 v" + have + "，不用再下载了，直接点「安装手表端」");
                                Loc.toast(WatchActivity.this, "已经有最新的表端 v" + have + "，不重复下载",
                                        Toast.LENGTH_LONG).show();
                            }
                        });
                        return;
                    }
                    final java.net.HttpURLConnection c2 =
                            (java.net.HttpURLConnection) new java.net.URL(url).openConnection();
                    c2.setConnectTimeout(10000);
                    c2.setReadTimeout(20000);
                    java.io.InputStream in2 = c2.getInputStream();
                    FileOutputStream fos = new FileOutputStream(out);
                    long got = 0;
                    int k;
                    byte[] b2 = new byte[4096];
                    while ((k = in2.read(b2)) > 0) {
                        fos.write(b2, 0, k);
                        got += k;
                        final long g = got;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                tv.setText("正在下载表端安装包…\n已接收 " + (g / 1024) + " KB");
                            }
                        });
                    }
                    fos.close();
                    in2.close();
                    c2.disconnect();
                    getSharedPreferences("app_prefs", MODE_PRIVATE).edit()
                            .putString("watch_pkg_path", out.getAbsolutePath())
                            .putString("watch_pkg_version", ver)
                            .apply();
                    pruneWatch(dir, out);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dlg.dismiss();
                            txtPath.setText("表端安装包已更新到 " + (ver.isEmpty() ? "最新版" : ("v" + ver))
                                    + "，点「安装手表端」就能装到手表");
                            Loc.toast(WatchActivity.this, "表端安装包下载完成", Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (final Throwable e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dlg.dismiss();
                            Loc.toast(WatchActivity.this, "下载失败：" + e, Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        }).start();
    }

    /** 版本号比较：a 比 b 新返回正数，相同返回 0 */
    private static int verCmp(String a, String b) {
        String[] x = (a == null ? "" : a).trim().split("\\.");
        String[] y = (b == null ? "" : b).trim().split("\\.");
        int n = Math.max(x.length, y.length);
        for (int i = 0; i < n; i++) {
            int xi = 0, yi = 0;
            try {
                xi = i < x.length ? Integer.parseInt(x[i].replaceAll("[^0-9]", "")) : 0;
            } catch (Throwable ignored) {
                xi = 0;
            }
            try {
                yi = i < y.length ? Integer.parseInt(y[i].replaceAll("[^0-9]", "")) : 0;
            } catch (Throwable ignored) {
                yi = 0;
            }
            if (xi != yi) return xi > yi ? 1 : -1;
        }
        return 0;
    }

    /** 表端包只留一份最新的，旧的删掉 */
    private static void pruneWatch(File dir, File keep) {
        try {
            File[] fs = dir.listFiles();
            if (fs == null) return;
            for (File f : fs) {
                if (!f.getName().endsWith(".bin")) continue;
                if (keep != null && f.getAbsolutePath().equals(keep.getAbsolutePath())) continue;
                f.delete();
            }
        } catch (Throwable ignored) {
        }
    }

    /** 下载过就用下载的那份，没下载过用包里内置的 */
    private File watchSource() throws Exception {
        String p = getSharedPreferences("app_prefs", MODE_PRIVATE).getString("watch_pkg_path", "");
        if (p != null && p.length() > 0) {
            File f = new File(p);
            if (f.exists() && f.length() > 1000) return f;
        }
        File tmp = new File(getCacheDir(), "medwatch-asset.bin");
        InputStream in = getAssets().open(ASSET_NAME);
        FileOutputStream os = new FileOutputStream(tmp);
        byte[] buf = new byte[4096];
        int len;
        while ((len = in.read(buf)) > 0) os.write(buf, 0, len);
        os.flush();
        os.close();
        in.close();
        return tmp;
    }

    private void exportPackage() {
        try {
            File dir = new File(getExternalFilesDir("吃药提醒备份"), "手表端");
            if (!dir.exists() && !dir.mkdirs()) {
                Loc.toast(this, "创建目录失败", Toast.LENGTH_SHORT).show();
                return;
            }
            File src = watchSource();
            String ver = getSharedPreferences("app_prefs", MODE_PRIVATE)
                    .getString("watch_pkg_version", "");
            File out = new File(dir, ver.isEmpty() ? "吃药提醒手表端-内置版.bin"
                    : ("吃药提醒手表端-v" + ver + ".bin"));
            copyFile(src, out);
            // 放到缓存里，用 content:// 交给系统：弹出「用哪个应用打开」，用户挑安装工具点安装
            File share = new File(ApkProvider.dir(this), OUT_NAME);
            copyFile(out, share);
            txtPath.setText(Tone.s(this, "已导出：\n", "导出好啦喵：\n", "Exported:\n")
                    + out.getAbsolutePath());
            openWithInstaller(share);
        } catch (Exception e) {
            txtPath.setText(Tone.s(this, "导出失败：", "导出没成功喵：", "Export failed: ")
                    + e.getMessage());
            Loc.toast(this, "导出失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void copyFile(File src, File dst) throws Exception {
        InputStream in = new java.io.FileInputStream(src);
        FileOutputStream os = new FileOutputStream(dst);
        byte[] buf = new byte[4096];
        int len;
        while ((len = in.read(buf)) > 0) {
            os.write(buf, 0, len);
        }
        os.flush();
        os.close();
        in.close();
    }

    /** 让系统弹窗问「用哪个应用打开」这个 .bin，用户选表盘自定义工具之类就能装到手表 */
    private void openWithInstaller(File bin) {
        android.net.Uri uri = ApkProvider.uriFor(bin);
        Intent view = new Intent(Intent.ACTION_VIEW);
        view.setDataAndType(uri, "application/octet-stream");
        view.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(Intent.createChooser(view, "选一个装手表的程序（表盘自定义工具等）"));
            Loc.toast(this, "选好程序后点安装，就能装到手表", Toast.LENGTH_SHORT).show();
        } catch (Throwable e) {
            Intent any = new Intent(Intent.ACTION_VIEW);
            any.setDataAndType(uri, "*/*");
            any.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                startActivity(Intent.createChooser(any, "选一个装手表的程序（表盘自定义工具等）"));
            } catch (Throwable e2) {
                Loc.toast(this, "没有能打开 .bin 的程序，手动去 吃药提醒备份/手表端 里找",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private void openHealth() {
        try {
            Intent it = getPackageManager().getLaunchIntentForPackage("com.mi.health");
            if (it == null) {
                Loc.toast(this, "没找到小米运动健康", Toast.LENGTH_SHORT).show();
                return;
            }
            startActivity(it);
        } catch (Exception e) {
            Loc.toast(this, "打不开：" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
