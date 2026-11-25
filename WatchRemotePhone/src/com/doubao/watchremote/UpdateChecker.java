package com.doubao.watchremote;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.ViewGroup;
import android.widget.TextView;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * 检查更新：从 GitHub 仓库里的 updates/version.json 读最新版本号，
 * 有新版就下载 APK 并交给系统安装。
 */
public class UpdateChecker {

    /*
     * 版本信息地址。raw.githubusercontent.com 在国内经常连不上，
     * 所以主用 jsDelivr（同一个仓库的 CDN 镜像），raw 留作备用。
     */
    public static final String VERSION_URL =
            "https://ghproxy.net/https://raw.githubusercontent.com/Himzer240/watchremote/main/updates/version.json";

    private static final String RAW_BASE = "https://raw.githubusercontent.com/Himzer240/watchremote/main/";
    private static final String JSD = "https://cdn.jsdelivr.net/gh/Himzer240/watchremote@main/";
    private static final String JSD_FAST = "https://fastly.jsdelivr.net/gh/Himzer240/watchremote@main/";
    private static final String PROXY1 = "https://ghproxy.net/";
    private static final String PROXY2 = "https://gh-proxy.com/";
    private static final String IN_REPO = "/Himzer240/watchremote/main/";

    /** 版本信息地址：代理最容易连，jsDelivr 缓存太久放后面 */
    private static String[] versionUrls() {
        return new String[]{
                PROXY1 + RAW_BASE + "updates/version.json",
                PROXY2 + RAW_BASE + "updates/version.json",
                JSD + "updates/version.json",
                RAW_BASE + "updates/version.json"
        };
    }

    private static final Handler main = new Handler(Looper.getMainLooper());

    private static void safeShow(AlertDialog d) {
        if (d == null) return;
        try {
            if (!d.isShowing()) d.show();
        } catch (Throwable ignored) {
        }
    }

    /** 找当前 Activity：拿 Application 的 Context 弹窗会直接 BadToken 崩掉 */
    private static Activity activityOf(Context c) {
        Context cur = c;
        for (int i = 0; i < 8 && cur != null; i++) {
            if (cur instanceof Activity) return (Activity) cur;
            if (!(cur instanceof ContextWrapper)) return null;
            Context base = ((ContextWrapper) cur).getBaseContext();
            if (base == null || base == cur) return null;
            cur = base;
        }
        return null;
    }

    public interface OnCheck {
        void onResult(boolean checked, String message);
    }

    /** 有没有网（连上 WiFi 或移动数据，且系统认为可用） */
    public static boolean isOnline(Context c) {
        try {
            android.net.ConnectivityManager cm = (android.net.ConnectivityManager)
                    c.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) return true;
            android.net.NetworkInfo ni = cm.getActiveNetworkInfo();
            return ni != null && ni.isConnected();
        } catch (Throwable e) {
            return true;
        }
    }

    public static void check(final Context c, final OnCheck cb) {
        // 先看下载目录里有没有已经下好、但没装成的包；有更高的版本就直接装
        final Pkg local = bestLocal(c);
        if (local != null) {
            promptInstallLocal(c, local, cb);
            return;
        }
        checkRemote(c, cb);
    }

    /** 已经下载好、版本号高于当前安装版本的那个包 */
    private static final class Pkg {
        File file;
        int code;
        String name;
    }

    private static Pkg bestLocal(Context c) {
        try {
            int installed = c.getPackageManager().getPackageInfo(c.getPackageName(), 0).versionCode;
            File dir = ApkProvider.dir(c);
            File[] fs = dir.listFiles();
            if (fs == null) return null;
            Pkg best = null;
            for (File f : fs) {
                String n = f.getName();
                if (!n.endsWith(".apk")) continue;
                android.content.pm.PackageInfo pi =
                        c.getPackageManager().getPackageArchiveInfo(f.getAbsolutePath(), 0);
                if (pi == null) continue;
                if (pi.versionCode <= installed) continue;
                if (best == null || pi.versionCode > best.code) {
                    best = new Pkg();
                    best.file = f;
                    best.code = pi.versionCode;
                    best.name = pi.versionName;
                }
            }
            return best;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void promptInstallLocal(final Context c, final Pkg pkg, final OnCheck cb) {
        final Activity act = activityOf(c);
        if (act == null || act.isFinishing()) return;
        AlertDialog dd = new Loc(act)
                .setTitle("发现已下载的安装包")
                .setMessage("下载目录里有 v" + pkg.name + "（比现在装的版本新），直接安装吗？\n"
                        + "取消的话会再去 GitHub 检查一次")
                .setNegativeButton("去检查", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) {
                        checkRemote(c, cb);
                    }
                })
                .setPositiveButton("安装", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) {
                        install(c, pkg.file);
                        if (cb != null) cb.onResult(true, "已打开安装器：" + pkg.name);
                    }
                })
                .create();
        safeShow(dd);
    }

    private static void checkRemote(final Context c, final OnCheck cb) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String msg;
                try {
                    JSONObject o = fetchVersion();
                    final int remoteCode = o.optInt("versionCode", 0);
                    final String remoteName = o.optString("versionName", "");
                    final String url = o.optString("url", "");
                    final String note = o.optString("note", "");
                    int localCode = c.getPackageManager().getPackageInfo(c.getPackageName(), 0).versionCode;
                    if (remoteCode <= localCode) {
                        final String m = "已经是最新版（" + remoteName + "）";
                        main.post(new Runnable() {
                            @Override
                            public void run() {
                                if (cb != null) cb.onResult(true, m);
                            }
                        });
                        return;
                    }
                    final String nm = remoteName;
                    final String nt = note;
                    final String u = url;
                    main.post(new Runnable() {
                        @Override
                        public void run() {
                            if (cb != null) cb.onResult(true, "发现新版本 " + nm);
                            promptDownload(c, nm, nt, u);
                        }
                    });
                    return;
                } catch (Throwable e) {
                    Logx.log(c, "update", "检查失败：" + e);
                    msg = "检查更新失败：" + shortMsg(e);
                }
                final String m = msg;
                main.post(new Runnable() {
                    @Override
                    public void run() {
                        if (cb != null) cb.onResult(false, m);
                    }
                });
            }
        }).start();
    }

    /** 给别的地方用：取各镜像里最新的那份版本信息 */
    public static JSONObject latestInfo() throws Exception {
        return fetchVersion();
    }

    /**
     * 版本信息：几个镜像全问一遍，取 versionCode 最大的那个。
     * 单个镜像有缓存、报的是旧版本，也不会把新版本盖掉。
     */
    private static JSONObject fetchVersion() throws Exception {
        JSONObject best = null;
        int bestCode = -1;
        Exception last = null;
        for (String u : versionUrls()) {
            try {
                JSONObject o = fetch(u, 5000);
                int code = o.optInt("versionCode", 0);
                if (code > bestCode) {
                    best = o;
                    bestCode = code;
                }
            } catch (Throwable e) {
                last = (e instanceof Exception) ? (Exception) e : new Exception(e);
            }
        }
        if (best != null) return best;
        throw (last != null) ? last : new Exception("版本地址都不通");
    }

    private static JSONObject fetch(String url) throws Exception {
        return fetch(url, 8000);
    }

    private static JSONObject fetch(String url, int timeout) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setConnectTimeout(timeout);
        conn.setReadTimeout(timeout);
        conn.setRequestProperty("User-Agent", "MedReminder");
        InputStream in = conn.getInputStream();
        StringBuilder sb = new StringBuilder();
        byte[] buf = new byte[2048];
        int n;
        while ((n = in.read(buf)) > 0) sb.append(new String(buf, 0, n, "UTF-8"));
        in.close();
        conn.disconnect();
        return new JSONObject(sb.toString());
    }

    private static void promptDownload(final Context c, final String name, final String note, final String url) {
        final Activity act = activityOf(c);
        if (act == null || act.isFinishing()) return;
        String msg = "最新版本：" + name + (note.isEmpty() ? "" : ("\n" + note)) + "\n\n现在下载并安装吗？";
        AlertDialog d0 = new Loc(act)
                .setTitle("发现新版本")
                .setMessage(msg)
                .setNegativeButton("以后再说", null)
                .setPositiveButton("下载安装", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) {
                        download(c, name, url);
                    }
                })
                .create();
        safeShow(d0);
    }

    private static void download(final Context c, final String name, final String url) {
        final Activity act = activityOf(c);
        if (act == null || act.isFinishing()) return;
        // 本地已经有这个版本的包就不再下载，直接装
        File already = findApk(c, name);
        if (already != null) {
            Logx.log(c, "update", "本地已有 v" + name + " 的安装包，跳过下载");
            install(c, already);
            return;
        }
        final TextView tv = new TextView(act);
        tv.setPadding(46, 46, 46, 30);
        tv.setTextSize(14);
        tv.setText("正在下载 " + name + "…");
        final AlertDialog dlg = new Loc(act)
                .setTitle("下载更新")
                .setView(tv)
                .setCancelable(false)
                .create();
        safeShow(dlg);

        new Thread(new Runnable() {
            @Override
            public void run() {
                File out = new File(ApkProvider.dir(c), "update-" + name + ".apk");
                Throwable lastErr = null;
                boolean ok = false;
                for (String cand : mirrors(url)) {
                    try {
                        grab(cand, out, tv, name);
                        ok = true;
                        break;
                    } catch (Throwable e) {
                        lastErr = e;
                    }
                }
                if (ok) {
                    pruneApks(c, out);
                    main.post(new Runnable() {
                        @Override
                        public void run() {
                            dlg.dismiss();
                            install(c, out);
                        }
                    });
                } else {
                    Logx.log(c, "update", "下载失败：" + lastErr);
                    final String m = "下载失败：" + shortMsg(lastErr);
                    main.post(new Runnable() {
                        @Override
                        public void run() {
                            dlg.dismiss();
                            Loc.toast(c, m, android.widget.Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        }).start();
    }

    /** 同一个包的多个镜像地址：给的先试，再试代理和 jsDelivr、raw */
    private static java.util.List<String> mirrors(String url) {
        java.util.List<String> list = new java.util.ArrayList<String>();
        if (url != null && !url.isEmpty()) list.add(url);
        String tail = null;
        if (url != null) {
            int at = url.indexOf(IN_REPO);
            if (at >= 0) {
                tail = url.substring(at + IN_REPO.length());
            }
        }
        if (tail != null && tail.length() > 0) {
            String rawUrl = RAW_BASE + tail;
            String[] cands = new String[]{
                    PROXY1 + rawUrl,
                    PROXY2 + rawUrl,
                    JSD + tail,
                    JSD_FAST + tail,
                    rawUrl
            };
            for (String cand : cands) {
                if (!list.contains(cand)) list.add(cand);
            }
        }
        return list;
    }

    /** 把 url 下到 out，边下边在 tv 上显示进度；失败抛异常由上层换镜像 */
    private static void grab(String url, File out, final TextView tv, final String name) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(15000);
        conn.setRequestProperty("User-Agent", "MedReminder");
        InputStream in = conn.getInputStream();
        FileOutputStream fos = new FileOutputStream(out);
        byte[] buf = new byte[4096];
        long total = 0;
        int n;
        try {
            while ((n = in.read(buf)) > 0) {
                fos.write(buf, 0, n);
                total += n;
                final long t = total;
                main.post(new Runnable() {
                    @Override
                    public void run() {
                        tv.setText("正在下载 " + name + "…\n已接收 " + (t / 1024) + " KB");
                    }
                });
            }
        } finally {
            try { fos.close(); } catch (Throwable ignored) { }
            try { in.close(); } catch (Throwable ignored) { }
            conn.disconnect();
        }
        if (total <= 0) throw new Exception("下载到空文件");
    }

    /** 下载目录里版本号等于 name 的安装包（下过一次就不再下） */
    private static File findApk(Context c, String name) {
        try {
            File[] fs = ApkProvider.dir(c).listFiles();
            if (fs == null) return null;
            for (File f : fs) {
                if (!f.getName().endsWith(".apk") || f.length() < 1000) continue;
                android.content.pm.PackageInfo pi =
                        c.getPackageManager().getPackageArchiveInfo(f.getAbsolutePath(), 0);
                if (pi == null) continue;
                if (name.equals(pi.versionName)) return f;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    /** 只留最新的安装包，旧的删掉，省空间 */
    private static void pruneApks(Context c, File keep) {
        try {
            File[] fs = ApkProvider.dir(c).listFiles();
            if (fs == null) return;
            for (File f : fs) {
                if (!f.getName().endsWith(".apk")) continue;
                if (keep != null && f.getAbsolutePath().equals(keep.getAbsolutePath())) continue;
                Logx.log(c, "update", "删除旧安装包 " + f.getName());
                f.delete();
            }
        } catch (Throwable ignored) {
        }
    }

    private static void install(Context c, File apk) {
        try {
            Uri uri = ApkProvider.uriFor(apk);
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setDataAndType(uri, "application/vnd.android.package-archive");
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            c.startActivity(i);
        } catch (Throwable e) {
            Loc.toast(c, "打不开安装器，请到应用里手动安装：" + apk.getAbsolutePath(),
                    android.widget.Toast.LENGTH_LONG).show();
        }
    }

    private static String shortMsg(Throwable e) {
        String m = e.getMessage();
        if (m == null) m = e.getClass().getSimpleName();
        return m.length() > 70 ? m.substring(0, 70) : m;
    }

    // ============ 前台自动检查：有更新才弹，弹窗不给点空白关 ============

    private static final String PREF = "app_prefs";
    private static long lastSilentAt = 0;

    private static int ignoredCode(Context c) {
        return c.getSharedPreferences(PREF, Context.MODE_PRIVATE).getInt("ignored_update_code", 0);
    }

    private static void setIgnoredCode(Context c, int code) {
        c.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
                .putInt("ignored_update_code", code).apply();
    }

    /** 静默检查：没更新什么都不弹；有更新弹一个关不掉的弹窗，只能选「更新」或「不更新」 */
    public static void checkSilent(final Context c) {
        long now = System.currentTimeMillis();
        if (now - lastSilentAt < 60 * 1000L) return;
        lastSilentAt = now;

        new Thread(new Runnable() {
            @Override
            public void run() {
                final Pkg local = bestLocal(c);
                if (local != null && local.code > ignoredCode(c)) {
                    main.post(new Runnable() {
                        @Override
                        public void run() {
                            askLocal(c, local);
                        }
                    });
                    return;
                }
                try {
                    JSONObject o = fetchVersion();
                    final int remoteCode = o.optInt("versionCode", 0);
                    final String remoteName = o.optString("versionName", "");
                    final String url = o.optString("url", "");
                    final String note = o.optString("note", "");
                    final int installed;
                    try {
                        installed = c.getPackageManager().getPackageInfo(c.getPackageName(), 0).versionCode;
                    } catch (Throwable e) {
                        return;
                    }
                    if (remoteCode <= installed) return;
                    if (remoteCode <= ignoredCode(c)) return;
                    main.post(new Runnable() {
                        @Override
                        public void run() {
                            askRemote(c, remoteCode, remoteName, note, url);
                        }
                    });
                } catch (Throwable e2) {
                    Logx.log(c, "update", "静默检查失败：" + e2);
                }
            }
        }).start();
    }

    private static void askRemote(final Context c, final int code, final String name,
                                  final String note, final String url) {
        final Activity act = activityOf(c);
        if (act == null || act.isFinishing()) return;
        String msg = "新版本：" + name + (note.isEmpty() ? "" : ("\n" + note));
        AlertDialog d = new Loc(act)
                .setTitle("发现新版本")
                .setMessage(msg)
                .setPositiveButton("更新", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int w) {
                        download(c, name, url);
                    }
                })
                .setNegativeButton("不更新", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int w) {
                        setIgnoredCode(c, code);
                    }
                })
                .create();
        d.setCancelable(false);
        d.setCanceledOnTouchOutside(false);
        safeShow(d);
    }

    private static void askLocal(final Context c, final Pkg pkg) {
        final Activity act = activityOf(c);
        if (act == null || act.isFinishing()) return;
        AlertDialog d = new Loc(act)
                .setTitle("有下载好的新版本")
                .setMessage("已经下好 v" + pkg.name + "，还没装。现在装吗？")
                .setPositiveButton("更新", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int w) {
                        install(c, pkg.file);
                    }
                })
                .setNegativeButton("不更新", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int w) {
                        setIgnoredCode(c, pkg.code);
                    }
                })
                .create();
        d.setCancelable(false);
        d.setCanceledOnTouchOutside(false);
        safeShow(d);
    }
}
