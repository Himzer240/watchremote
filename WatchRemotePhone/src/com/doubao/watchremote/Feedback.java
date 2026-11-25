package com.doubao.watchremote;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 反馈 / 举报 Bug：提交到 GitHub 仓库的 Issues（feedback 标签）。
 * 走和下载一样的 gh-proxy 代理通道，国内网络也能发；直连 api.github.com 兜底。
 * 令牌只有这个仓库的 Issue 读写权限。
 */
public class Feedback {

    public interface Cb { void done(boolean ok, String msg); }

    private static final String[] BASES = {
            "https://gh-proxy.com/https://api.github.com",
            "https://api.github.com"
    };

    private static String tok() {
        String a = "Z2l0aHViX3BhdF8xMUJWWEZXVUkwV2dUYWQzaFN5NmNDX2d3SktXbmZzaWdmQW";
        String b = "4zTzNMOFFhamlKT2xjNkx4ZTFBYmxKSzJUMTJiS0RKTDM1WkRTVmh1SW1EZWo3";
        try {
            return new String(android.util.Base64.decode(a + b, android.util.Base64.DEFAULT),
                    Charset.forName("UTF-8"));
        } catch (Throwable t) {
            return "";
        }
    }

    public static void submit(final Context c, final String name, final String text, final Cb cb) {
        final Context app = c.getApplicationContext();
        final Handler ui = new Handler(Looper.getMainLooper());
        new Thread(new Runnable() {
            @Override
            public void run() {
                String last = "";
                for (String base : BASES) {
                    try {
                        JSONObject body = new JSONObject();
                        String t = text.trim();
                        body.put("title", "【反馈】" + t.substring(0, Math.min(30, t.length())).replace("\n", " "));
                        StringBuilder sb = new StringBuilder();
                        sb.append("**称呼：** ").append(name == null || name.trim().isEmpty() ? "匿名用户" : name.trim()).append("\n\n");
                        sb.append("**内容：**\n").append(t).append("\n\n---\n");
                        sb.append("提交时间：").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA).format(new Date())).append("\n");
                        sb.append("App 版本：v").append(Logx.verName(app)).append("（code ").append(Logx.verCode(app)).append("）\n");
                        sb.append("设备：").append(android.os.Build.MANUFACTURER).append(" ").append(android.os.Build.MODEL)
                                .append(" · Android ").append(android.os.Build.VERSION.RELEASE).append("\n");
                        sb.append("来自：手机端反馈入口");
                        body.put("body", sb.toString());
                        body.put("labels", new org.json.JSONArray().put("feedback"));

                        HttpURLConnection h = (HttpURLConnection) new URL(
                                base + "/repos/Himzer240/medreminder/issues").openConnection();
                        h.setRequestMethod("POST");
                        h.setConnectTimeout(15000);
                        h.setReadTimeout(20000);
                        h.setRequestProperty("Authorization", "Bearer " + tok());
                        h.setRequestProperty("Accept", "application/vnd.github+json");
                        h.setRequestProperty("Content-Type", "application/json");
                        h.setDoOutput(true);
                        OutputStream os = h.getOutputStream();
                        os.write(body.toString().getBytes(Charset.forName("UTF-8")));
                        os.flush();
                        os.close();
                        int code = h.getResponseCode();
                        h.disconnect();
                        if (code == 201 || code == 200) {
                            final String okMsg = Lang.s(app, "反馈已收到，谢谢～");
                            ui.post(new Runnable() { @Override public void run() { cb.done(true, okMsg); } });
                            return;
                        }
                        last = "HTTP " + code;
                    } catch (Throwable e) {
                        last = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
                    }
                }
                final String err = last;
                ui.post(new Runnable() {
                    @Override public void run() {
                        cb.done(false, Lang.s(app, "发送失败，稍后再试：") + err);
                    }
                });
            }
        }, "feedback").start();
    }
}
