package com.doubao.watchremote;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.xiaomi.xms.wearable.Wearable;
import com.xiaomi.xms.wearable.auth.AuthApi;
import com.xiaomi.xms.wearable.auth.Permission;
import com.xiaomi.xms.wearable.message.MessageApi;
import com.xiaomi.xms.wearable.message.OnMessageReceivedListener;
import com.xiaomi.xms.wearable.node.Node;
import com.xiaomi.xms.wearable.node.NodeApi;
import com.xiaomi.xms.wearable.service.OnServiceConnectionListener;
import com.xiaomi.xms.wearable.service.ServiceApi;
import com.xiaomi.xms.wearable.tasks.OnFailureListener;
import com.xiaomi.xms.wearable.tasks.OnSuccessListener;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 常驻通道：注册消息监听，接收手表端的打卡 / 撤销 / 全量次数，写回手机端记录。
 * 连接状态会用中文状态机维护，失败会自动重试，界面上能直接看到。
 */
public class WatchBridge {

    private static final String TAG = "WatchBridge";
    private static final Charset UTF8 = Charset.forName("UTF-8");

    private static Context appCtx;
    private static Runnable onChanged;
    private static final Handler main = new Handler(Looper.getMainLooper());

    private static volatile String state = "未启动";
    private static volatile String detail = "";
    private static volatile String lastRecv = "";
    private static volatile boolean listening;
    private static volatile boolean connecting;
    private static volatile String nodeId;
    private static int retry;
    private static boolean serviceListenerOn;

    public static String stateText() {
        String s = state;
        if (detail != null && detail.length() > 0) s = s + "（" + detail + "）";
        return s;
    }

    /** 按当前表达风格输出状态：English 模式给英文短语 */
    public static String stateText(android.content.Context c) {
        String s = state;
        if (detail != null && detail.length() > 0) s = s + "（" + detail + "）";
        if (Tone.style(c) == Tone.EN) {
            String st = Lang.s(c, state);
            String dt = Lang.s(c, detail == null ? "" : detail);
            return (dt == null || dt.isEmpty() || !Lang.hasHan(dt)) ? st : (st + " (" + dt + ")");
        }
        return s;
    }

    public static String lastRecvText() {
        return lastRecv;
    }

    public static void setOnChanged(Runnable r) {
        onChanged = r;
    }

    /** 用户在手表页点“连接到手表”：只建立通道和监听，不发任何数据 */
    public static void connectOnly(Context context) {
        appCtx = context.getApplicationContext();
        listening = false;
        connecting = false;
        nodeId = null;
        state = "连接中";
        detail = "";
        notifyUi();
        start(appCtx);
    }

    /** 手动重连（顶栏/下拉不再自动调用，保留给诊断用） */
    public static void reconnect() {
        if (appCtx != null) connectOnly(appCtx);
    }

    public static void start(Context context) {
        if (appCtx == null) appCtx = context.getApplicationContext();
        attachServiceListener();
        if (listening || connecting) return;
        connecting = true;
        state = "连接中";
        detail = "";
        notifyUi();
        try {
            NodeApi nodeApi = Wearable.getNodeApi(appCtx);
            nodeApi.getConnectedNodes()
                    .addOnSuccessListener(new OnSuccessListener<List<Node>>() {
                        @Override
                        public void onSuccess(List<Node> nodes) {
                            if (nodes == null || nodes.isEmpty()) {
                                connecting = false;
                                state = "未连接";
                                detail = "小米运动健康里没有已连接的手表";
                                EventLog.add(appCtx, "手表通道：没有设备");
                                notifyUi();
                                // 失败后保持未连接，等用户手动点连接
                                return;
                            }
                            Node node = nodes.get(0);
                            nodeId = node.id;
                            detail = node.name == null ? "" : node.name;
                            EventLog.add(appCtx, "手表通道：发现设备 " + detail);
                            askPermission(node.id);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(Exception e) {
                            connecting = false;
                            state = "未连接";
                            detail = shortMsg(e);
                            EventLog.add(appCtx, "手表通道：取设备失败 " + shortMsg(e));
                            notifyUi();
                            // 失败后保持未连接，等用户手动点连接
                        }
                    });
        } catch (Throwable e) {
            connecting = false;
            state = "不可用";
            detail = "这台手机没有小米穿戴服务";
            EventLog.add(appCtx, "手表通道：SDK 不可用 " + e);
            notifyUi();
        }
    }

    private static void markEverConnected() {
        try {
            appCtx.getSharedPreferences("ui_hint", Context.MODE_PRIVATE)
                    .edit().putBoolean("watch_ever_connected", true).apply();
        } catch (Throwable ignored) {
        }
    }

    /** 是否成功连过一次（用来决定之后要不要提示） */
    public static boolean everConnected(Context c) {
        try {
            return c.getSharedPreferences("ui_hint", Context.MODE_PRIVATE)
                    .getBoolean("watch_ever_connected", false);
        } catch (Throwable e) {
            return false;
        }
    }

    private static void attachServiceListener() {
        if (serviceListenerOn) return;
        try {
            Wearable.getServiceApi(appCtx).registerServiceConnectionListener(new OnServiceConnectionListener() {
                @Override
                public void onServiceConnected() {
                    EventLog.add(appCtx, "穿戴服务：已连上");
                }

                @Override
                public void onServiceDisconnected() {
                    listening = false;
                    connecting = false;
                    state = "未连接";
                    detail = "穿戴服务断开";
                    EventLog.add(appCtx, "穿戴服务：断开，状态改为未连接");
                    notifyUi();
                }
            });
            serviceListenerOn = true;
        } catch (Throwable ignored) {
        }
    }

    private static void askPermission(final String id) {
        try {
            AuthApi authApi = Wearable.getAuthApi(appCtx);
            authApi.checkPermissions(id, new Permission[]{Permission.DEVICE_MANAGER})
                    .addOnSuccessListener(new OnSuccessListener<boolean[]>() {
                        @Override
                        public void onSuccess(boolean[] granted) {
                            if (granted != null && granted.length > 0 && granted[0]) {
                                listen(id);
                            } else {
                                authApi.requestPermission(id, Permission.DEVICE_MANAGER)
                                        .addOnSuccessListener(new OnSuccessListener<Permission[]>() {
                                            @Override
                                            public void onSuccess(Permission[] p) {
                                                listen(id);
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(Exception e) {
                                                connecting = false;
                                                state = "未授权";
                                                detail = shortMsg(e);
                                                EventLog.add(appCtx, "手表通道：授权失败 " + shortMsg(e));
                                                notifyUi();
                                            }
                                        });
                            }
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(Exception e) {
                            connecting = false;
                            state = "未连接";
                            detail = shortMsg(e);
                            EventLog.add(appCtx, "手表通道：权限校验失败 " + shortMsg(e));
                            notifyUi();
                            // 失败后保持未连接，等用户手动点连接
                        }
                    });
        } catch (Throwable e) {
            connecting = false;
            state = "不可用";
            detail = shortMsg(null);
            notifyUi();
        }
    }

    private static void listen(final String id) {
        try {
            MessageApi api = Wearable.getMessageApi(appCtx);
            api.addListener(id, new OnMessageReceivedListener() {
                @Override
                public void onMessageReceived(String from, byte[] message) {
                    if (message == null) return;
                    handle(new String(message, UTF8));
                }
            }).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void v) {
                    connecting = false;
                    listening = true;
                    retry = 0;
                    state = "已连接";
                    markEverConnected();
                    WatchSync.pushTheme(appCtx);
                    EventLog.add(appCtx, "手表通道：监听已注册");
                    notifyUi();
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(Exception e) {
                    String m = shortMsg(e).toLowerCase(Locale.US);
                    connecting = false;
                    if (m.contains("registered")) {
                        // 之前已注册，还没断
                        listening = true;
                        state = "已连接";
                        markEverConnected();
                        WatchSync.pushTheme(appCtx);
                        notifyUi();
                        return;
                    }
                    listening = false;
                    state = "未连接";
                    detail = shortMsg(e);
                    EventLog.add(appCtx, "手表通道：监听注册失败 " + shortMsg(e));
                    notifyUi();
                    // 失败后保持未连接，等用户手动点连接
                }
            });
        } catch (Throwable e) {
            connecting = false;
            state = "不可用";
            detail = shortMsg(null);
            notifyUi();
        }
    }

    private static void scheduleRetry() {
        if (retry >= 5) return;
        retry++;
        main.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!listening && !connecting) {
                    connecting = false;
                    start(appCtx);
                }
            }
        }, 3000);
    }

    private static void handle(String raw) {
        Log.d(TAG, "recv " + raw);
        String stamp = new SimpleDateFormat("HH:mm:ss", Locale.CHINA).format(new Date());
        try {
            JSONObject o = new JSONObject(raw);
            if (!o.has("tag")) {
                // 兼容被二次编码的情况："{\"tag\":...}"
                String once = o.optString("data", "");
                if (once.isEmpty()) once = o.keys().hasNext() ? o.optString(o.keys().next(), "") : "";
                if (once.trim().startsWith("{")) o = new JSONObject(once);
            }
            String tag = o.optString("tag", "");
            if ("wantTheme".equals(tag)) {
                // 手表要颜色：回一条 theme
                WatchSync.pushTheme(appCtx);
                return;
            }
            if ("remote".equals(tag)) {
                String act = o.optString("act", "");
                if ("remoteGesture".equals(act)) {
                    String dir = o.optString("dir", "");
                    float len = (float) o.optDouble("len", 0.5);
                    RemoteService.gesture(dir, len);
                    lastRecv = stamp + " 表上" + dir + " (" + len + ")";
                } else if ("tap".equals(act)) {
                    RemoteService.tap();
                    lastRecv = stamp + " 表上点按";
                } else {
                    RemoteService.perform(act);
                    lastRecv = stamp + " 表上遥控 " + act;
                }
                notifyUi();
                return;
            }
            if ("pause".equals(tag)) {
                long pid = parseId(o.optString("id", ""));
                Medicine pm = pid > 0 ? MedStore.find(appCtx, pid) : null;
                if (pm != null) {
                    pm.enabled = false;
                    MedStore.upsert(appCtx, pm);
                    Scheduler.cancelMedicine(appCtx, pm);
                    for (int i = 0; i < pm.times.size(); i++) Scheduler.cancelRepeat(appCtx, pm.id, i);
                    WatchSync.pushPlans(appCtx);
                    lastRecv = stamp + " 表上暂停「" + pm.name + "」";
                    EventLog.add(appCtx, "收到手表：暂停「" + pm.name + "」");
                    notifyUi();
                }
                return;
            }
            if ("enable".equals(tag)) {
                // 手表上直接点「重新打开」：把手机端这个计划重新开启提醒
                long medId = parseId(o.optString("id", ""));
                Medicine med = medId > 0 ? MedStore.find(appCtx, medId) : null;
                if (med == null) {
                    lastRecv = stamp + " 表上重新打开（找不到计划）";
                    EventLog.add(appCtx, "收到手表：重新打开，但没找到 id=" + medId);
                    return;
                }
                med.enabled = true;
                MedStore.upsert(appCtx, med);
                WatchFlags.clearPaused(appCtx, medId);
                Scheduler.rescheduleAll(appCtx);
                WatchSync.pushPlans(appCtx);
                lastRecv = stamp + " 表上重新打开「" + med.name + "」";
                EventLog.add(appCtx, "收到手表：重新打开「" + med.name + "」，已发回最新计划");
                notifyUi();
                return;
            }
            if ("wantPlans".equals(tag)) {
                // 手表刚连上就来要计划：直接推一份最新的
                WatchSync.pushPlans(appCtx);
                EventLog.add(appCtx, "手表请求计划，已推送");
                return;
            }
            if ("taken".equals(tag)) {
                long medId = parseId(o.optString("id", ""));
                if (medId > 0) {
                    MedStore.markTaken(appCtx, medId);
                    for (int i = 0; i <= 6; i++) Scheduler.cancelRepeat(appCtx, medId, i);
                    lastRecv = stamp + (Tone.style(appCtx) == Tone.EN ? "  logged on watch" : " 表上打卡");
                    EventLog.add(appCtx, "收到手表：打卡 id=" + medId);
                    notifyUi();
                }
            } else if ("undo".equals(tag)) {
                long medId = parseId(o.optString("id", ""));
                if (medId > 0) {
                    boolean ok = MedStore.removeLastTaken(appCtx, medId);
                    lastRecv = stamp + (Tone.style(appCtx) == Tone.EN ? "  undone on watch" : " 表上撤销");
                    EventLog.add(appCtx, "收到手表：撤销 id=" + medId + (ok ? "" : "（没记录可撤）"));
                    notifyUi();
                }
            } else if ("pauseState".equals(tag)) {
                JSONArray paused = o.optJSONArray("paused");
                JSONArray disabled = o.optJSONArray("disabled");
                long at = o.optLong("at", 0L);
                // 谁的操作更新听谁的：表上这条比手机本机最后一次改动还旧，就不采纳
                if (at > 0 && at < SyncStamp.last(appCtx)) {
                    lastRecv = stamp + " 表上操作比手机旧，忽略并回推最新";
                    EventLog.add(appCtx, "收到手表：暂停状态已过时，按手机为准");
                    WatchSync.pushPlans(appCtx);
                    return;
                }
                WatchFlags.save(appCtx, paused, disabled);
                if (at > 0) SyncStamp.set(appCtx, at);
                // 表上暂停/禁用的计划，手机这边也不排提醒
                Scheduler.rescheduleAll(appCtx);
                int np = paused == null ? 0 : paused.length();
                int nd = disabled == null ? 0 : disabled.length();
                lastRecv = stamp + " 表上计划状态（暂停 " + np + " / 禁用 " + nd + "）";
                EventLog.add(appCtx, "收到手表：计划状态，暂停 " + np + " 个、禁用 " + nd + " 个");
                // 立刻回推一份，保证两边马上一致
                WatchSync.pushPlans(appCtx);
                notifyUi();
            } else if ("sync".equals(tag)) {
                JSONArray counts = o.optJSONArray("counts");
                if (counts != null) {
                    int fixed = 0;
                    for (int i = 0; i < counts.length(); i++) {
                        JSONObject one = counts.optJSONObject(i);
                        if (one == null) continue;
                        long medId = parseId(one.optString("id", ""));
                        int target = one.optInt("count", 0);
                        if (medId <= 0) continue;
                        if (reconcile(medId, target)) fixed++;
                    }
                    lastRecv = stamp + (Tone.style(appCtx) == Tone.EN
                            ? ("  counts from watch (" + fixed + " fixed)")
                            : (" 表上次数（校正 " + fixed + " 个）"));
                    EventLog.add(appCtx, "收到手表：全量次数，校正 " + fixed + " 个计划");
                    notifyUi();
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "parse fail: " + e);
            EventLog.add(appCtx, "手表消息解析失败 " + raw);
        }
    }

    private static boolean reconcile(long medId, int target) {
        int cur = MedStore.takenCount(appCtx, medId);
        boolean changed = false;
        while (cur < target) {
            MedStore.markTaken(appCtx, medId);
            cur++;
            changed = true;
        }
        while (cur > target) {
            if (!MedStore.removeLastTaken(appCtx, medId)) break;
            cur--;
            changed = true;
        }
        return changed;
    }

    private static long parseId(String s) {
        try {
            return Long.parseLong(s);
        } catch (Exception e) {
            return -1L;
        }
    }

    private static String shortMsg(Exception e) {
        if (e == null) return "";
        String m = e.getMessage();
        if (m == null) m = e.getClass().getSimpleName();
        return m.length() > 40 ? m.substring(0, 40) : m;
    }

    private static void notifyUi() {
        main.post(new Runnable() {
            @Override
            public void run() {
                Runnable r = onChanged;
                if (r != null) r.run();
            }
        });
    }
}
