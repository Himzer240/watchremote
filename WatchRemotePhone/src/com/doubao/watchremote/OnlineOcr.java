package com.doubao.watchremote;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Scanner;

/**
 * 在线文字识别：百度 OCR（需 Key/Secret）优先，没填就用 OCR.space（免费 Key）。
 * 图片只在识别时上传一次，不存服务器。
 */
public class OnlineOcr {

    private static final String PREF = "ocr_prefs";
    public static final String K_BAIDU_KEY = "baidu_key";
    public static final String K_BAIDU_SECRET = "baidu_secret";
    public static final String K_SPACE_KEY = "space_key";
    private static final String K_TOKEN = "baidu_token";
    private static final String K_TOKEN_TIME = "baidu_token_time";

    public interface Callback {
        void onDone(String text, String error);
    }

    public static String get(Context c, String key, String def) {
        String v = c.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString(key, def);
        return v == null ? def : v.trim();
    }

    public static void put(Context c, String key, String value) {
        c.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .edit().putString(key, value == null ? "" : value.trim()).apply();
    }

    public static boolean hasBaidu(Context c) {
        return !get(c, K_BAIDU_KEY, "").isEmpty() && !get(c, K_BAIDU_SECRET, "").isEmpty();
    }

    public static boolean hasSpace(Context c) {
        return !get(c, K_SPACE_KEY, "helloworld").isEmpty();
    }

    public static String providerName(Context c) {
        if (hasBaidu(c)) return Tone.s(c, "百度 OCR（高精度）", "百度 OCR 喵（高精度）", "Baidu OCR (high accuracy)");
        if (hasSpace(c)) return "OCR.space";
        return Tone.s(c, "未配置", "还没配置喵", "not configured");
    }

    public static void recognize(final Context c, final Bitmap src, final Callback cb) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String text = null;
                String err = null;
                try {
                    Bitmap small = shrink(src);
                    byte[] jpg = compressJpeg(small);
                    byte[] bytes = jpg;
                    String b64 = Base64.encodeToString(bytes, Base64.NO_WRAP);

                    if (hasBaidu(c)) {
                        String token = baiduToken(c);
                        text = baiduOcr(token, b64);
                    } else if (hasSpace(c)) {
                        text = spaceOcr(get(c, K_SPACE_KEY, "helloworld"), b64);
                    } else {
                        err = "还没填识别 Key，点“识别设置”填一个";
                    }
                    if (err == null && (text == null || text.trim().isEmpty())) {
                        err = "识别服务没返回文字，换张更清楚的图再试";
                    }
                } catch (Throwable t) {
                    err = friendly(t);
                }
                cb.onDone(text, err);
            }
        }).start();
    }

    private static Bitmap shrink(Bitmap b) {
        int max = Math.max(b.getWidth(), b.getHeight());
        int limit = max > 1600 ? 1600 : max;
        if (max <= 1600) return b;
        float r = limit / (float) max;
        Bitmap n = Bitmap.createScaledBitmap(b, Math.round(b.getWidth() * r),
                Math.round(b.getHeight() * r), true);
        if (n != b) {
            try {
                b.recycle();
            } catch (Throwable ignored) {
            }
        }
        return n;
    }

    private static byte[] compressJpeg(Bitmap b) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        b.compress(Bitmap.CompressFormat.JPEG, 88, out);
        return out.toByteArray();
    }

    private static String friendly(Throwable t) {
        String n = t.getClass().getSimpleName();
        String m = t.getMessage() == null ? "" : t.getMessage();
        if (n.contains("UnknownHost") || n.contains("ConnectException")) {
            return "连不上识别服务器，检查手机网络";
        }
        if (n.contains("SocketTimeout")) return "网络太慢或服务器超时，稍后再试";
        return "识别失败：" + n + (m.isEmpty() ? "" : " " + m);
    }

    // ---------------- 百度 OCR ----------------

    private static String baiduToken(Context c) throws Exception {
        long now = System.currentTimeMillis();
        String cached = get(c, K_TOKEN, "");
        long t = c.getSharedPreferences(PREF, Context.MODE_PRIVATE).getLong(K_TOKEN_TIME, 0);
        if (!cached.isEmpty() && now - t < 25L * 24 * 3600 * 1000) return cached;

        String url = "https://aip.baidubce.com/oauth/2.0/token?grant_type=client_credentials"
                + "&client_id=" + URLEncoder.encode(get(c, K_BAIDU_KEY, ""), "UTF-8")
                + "&client_secret=" + URLEncoder.encode(get(c, K_BAIDU_SECRET, ""), "UTF-8");
        String body = request("GET", url, null, null);
        JSONObject o = new JSONObject(body);
        if (o.has("error")) {
            throw new Exception("百度 Key 不对：" + o.optString("error_description"));
        }
        String token = o.optString("access_token");
        if (token.isEmpty()) throw new Exception("百度没返回 token");
        c.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
                .putString(K_TOKEN, token).putLong(K_TOKEN_TIME, now).apply();
        return token;
    }

    private static String baiduOcr(String token, String b64) throws Exception {
        String url = "https://aip.baidubce.com/rest/2.0/ocr/v1/accurate_basic?access_token="
                + URLEncoder.encode(token, "UTF-8");
        String body = "image=" + URLEncoder.encode(b64, "UTF-8")
                + "&detect_language=false&paragraph=false";
        String resp = request("POST", url, body, "application/x-www-form-urlencoded");
        JSONObject o = new JSONObject(resp);
        if (o.has("error_code")) {
            throw new Exception("百度识别失败：" + o.optInt("error_code")
                    + " " + o.optString("error_msg"));
        }
        JSONArray arr = o.optJSONArray("words_result");
        if (arr == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < arr.length(); i++) {
            sb.append(arr.optJSONObject(i).optString("words")).append('\n');
        }
        return sb.toString();
    }

    // ---------------- OCR.space ----------------

    private static String spaceOcr(String key, String b64) throws Exception {
        String url = "https://api.ocr.space/parse/image";
        String body = "base64Image=" + URLEncoder.encode("data:image/jpeg;base64," + b64, "UTF-8")
                + "&language=chs&isOverlayRequired=false&scale=true"
                + "&detectOrientation=true&OCREngine=2";
        String resp = request("POST", url, body, "application/x-www-form-urlencoded", key);
        JSONObject o = new JSONObject(resp);
        if (o.optBoolean("IsErroredOnProcessing")) {
            JSONArray em = o.optJSONArray("ErrorMessage");
            StringBuilder msg = new StringBuilder();
            if (em != null) {
                for (int i = 0; i < em.length(); i++) {
                    if (i > 0) msg.append(" / ");
                    msg.append(em.optString(i));
                }
            }
            throw new Exception("OCR.space 报错：" + (msg.length() == 0 ? "未知错误" : msg.toString()));
        }
        JSONArray arr = o.optJSONArray("ParsedResults");
        if (arr == null || arr.length() == 0) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < arr.length(); i++) {
            sb.append(arr.optJSONObject(i).optString("ParsedText")).append('\n');
        }
        return sb.toString();
    }

    // ---------------- HTTP ----------------

    private static String request(String method, String url, String body, String ct) throws Exception {
        return request(method, url, body, ct, null);
    }

    private static String request(String method, String url, String body, String ct, String apiKey)
            throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod(method);
        conn.setConnectTimeout(20000);
        conn.setReadTimeout(40000);
        if (apiKey != null) conn.setRequestProperty("apikey", apiKey);
        if (body != null) {
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", ct == null ? "text/plain" : ct);
            OutputStream os = conn.getOutputStream();
            OutputStreamWriter w = new OutputStreamWriter(os, "UTF-8");
            w.write(body);
            w.flush();
            w.close();
        }
        int code = conn.getResponseCode();
        InputStream is = code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream();
        String resp = readAll(is);
        if (code < 200 || code >= 300) {
            throw new Exception("服务器返回 " + code + (resp.length() > 60 ? resp.substring(0, 60) : resp));
        }
        return resp;
    }

    private static String readAll(InputStream is) {
        if (is == null) return "";
        Scanner s = new Scanner(is, "UTF-8").useDelimiter("\\A");
        String out = s.hasNext() ? s.next() : "";
        s.close();
        return out;
    }
}
