package com.doubao.watchremote;

import android.content.Context;
import android.os.Build;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 简单日志：闪退了就记下来，用户把文件发过来就能查。
 * 存放位置：Android/data/com.doubao.watchremote/files/logs/
 *  - app.log            平时的运行日志（检查更新、下载、出错）
 *  - crash-<时间>.log   闪退时的堆栈
 */
public class Logx {

    private static final long MAX_APP_LOG = 512 * 1024L;

    public static File dir(Context c) {
        File d = null;
        try {
            d = c.getExternalFilesDir("logs");
        } catch (Throwable ignored) {
        }
        if (d == null) d = new File(c.getFilesDir(), "logs");
        if (!d.exists()) d.mkdirs();
        return d;
    }

    public static File appLog(Context c) {
        return new File(dir(c), "app.log");
    }

    /** 最新的一个闪退日志，没有就返回 null */
    public static File latestCrash(Context c) {
        File[] fs = dir(c).listFiles();
        if (fs == null) return null;
        File best = null;
        for (File f : fs) {
            if (!f.getName().startsWith("crash-")) continue;
            if (best == null || f.lastModified() > best.lastModified()) best = f;
        }
        return best;
    }

    private static String now() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date());
    }

    public static void log(Context c, String tag, String msg) {
        try {
            File f = appLog(c);
            if (f.length() > MAX_APP_LOG) {
                File old = new File(dir(c), "app-old.log");
                if (old.exists()) old.delete();
                f.renameTo(old);
                f = appLog(c);
            }
            FileOutputStream out = new FileOutputStream(f, true);
            out.write((now() + " [" + tag + "] " + msg + "\n").getBytes("UTF-8"));
            out.close();
        } catch (Throwable ignored) {
        }
    }

    /** 在 App.onCreate 里装一次，之后任何线程崩了都会留文件 */
    public static void init(final Context ctx) {
        final Context app = ctx.getApplicationContext();
        final Thread.UncaughtExceptionHandler prev = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                try {
                    writeCrash(app, t, e);
                } catch (Throwable ignored) {
                }
                if (prev != null) {
                    prev.uncaughtException(t, e);
                } else {
                    android.os.Process.killProcess(android.os.Process.myPid());
                    System.exit(10);
                }
            }
        });
        log(app, "app", "启动 " + verName(app) + "（code " + verCode(app) + "）");
    }

    private static void writeCrash(Context c, Thread t, Throwable e) {
        String stamp = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(new Date());
        File f = new File(dir(c), "crash-" + stamp + ".log");
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        pw.println("时间: " + now());
        pw.println("版本: " + verName(c) + "（code " + verCode(c) + "）");
        pw.println("设备: " + Build.MANUFACTURER + " " + Build.MODEL + " / Android " + Build.VERSION.RELEASE
                + " / SDK " + Build.VERSION.SDK_INT);
        pw.println("线程: " + (t == null ? "?" : t.getName()));
        pw.println("异常: " + e);
        pw.println("---");
        e.printStackTrace(pw);
        pw.flush();
        try {
            FileOutputStream out = new FileOutputStream(f);
            out.write(sw.toString().getBytes("UTF-8"));
            out.close();
        } catch (Throwable ignored) {
        }
        log(c, "crash", "崩了，已写入 " + f.getName() + "：" + e);
    }

    public static String verName(Context c) {
        try {
            return c.getPackageManager().getPackageInfo(c.getPackageName(), 0).versionName;
        } catch (Throwable e) {
            return "?";
        }
    }

    public static int verCode(Context c) {
        try {
            return c.getPackageManager().getPackageInfo(c.getPackageName(), 0).versionCode;
        } catch (Throwable e) {
            return 0;
        }
    }
}
