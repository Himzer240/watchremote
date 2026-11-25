package com.doubao.watchremote;

import android.content.Context;

import com.xiaomi.xms.wearable.Wearable;
import com.xiaomi.xms.wearable.auth.AuthApi;
import com.xiaomi.xms.wearable.auth.Permission;
import com.xiaomi.xms.wearable.message.MessageApi;
import com.xiaomi.xms.wearable.node.Node;
import com.xiaomi.xms.wearable.node.NodeApi;
import com.xiaomi.xms.wearable.tasks.OnFailureListener;
import com.xiaomi.xms.wearable.tasks.OnSuccessListener;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.Charset;
import java.util.List;

/**
 * 通过小米运动健康的穿戴服务（XMS Wearable SDK）把手表端要显示的计划发过去。
 * 手表端用 @system.interconnect 收，收到 tag=plans 就整表替换计划。
 */
public class WatchSync {

    public interface Callback {
        void onStatus(String msg);

        void onDone(boolean ok, String msg);
    }

    private final Context ctx;
    private final Callback cb;

    public WatchSync(Context ctx, Callback cb) {
        this.ctx = ctx.getApplicationContext();
        this.cb = cb;
    }

    private void status(String s) {
        if (cb != null) cb.onStatus(Loc.frag(ctx, s));
    }

    private void done(boolean ok, String s) {
        if (cb != null) cb.onDone(ok, Loc.frag(ctx, s));
    }

    public void sync() {
        status("正在查找手表…");
        NodeApi nodeApi;
        try {
            nodeApi = Wearable.getNodeApi(ctx);
        } catch (Throwable e) {
            done(false, "穿戴服务不可用：" + e);
            return;
        }
        nodeApi.getConnectedNodes()
                .addOnSuccessListener(new OnSuccessListener<List<Node>>() {
                    @Override
                    public void onSuccess(List<Node> nodes) {
                        if (nodes == null || nodes.isEmpty()) {
                            done(false, "没找到手表。先打开小米运动健康，确认手表已连接（蓝牙开着）。");
                            return;
                        }
                        Node node = nodes.get(0);
                        status("已找到设备：" + node.name + "，正在校验权限…");
                        requestPermission(node);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(Exception e) {
                        done(false, "获取设备列表失败：" + e.getMessage()
                                + "（检查小米运动健康是否已连接手表）");
                    }
                });
    }

    private void requestPermission(final Node node) {
        try {
            AuthApi authApi = Wearable.getAuthApi(ctx);
            authApi.checkPermissions(node.id, new Permission[]{Permission.DEVICE_MANAGER})
                    .addOnSuccessListener(new OnSuccessListener<boolean[]>() {
                        @Override
                        public void onSuccess(boolean[] granted) {
                            boolean ok = granted != null && granted.length > 0 && granted[0];
                            if (ok) {
                                status("权限已通过，正在打开手表应用…");
                                openThenSend(node);
                                return;
                            }
                            status("请求手表权限（手机会弹授权）…");
                            authApi.requestPermission(node.id, Permission.DEVICE_MANAGER)
                                    .addOnSuccessListener(new OnSuccessListener<Permission[]>() {
                                        @Override
                                        public void onSuccess(Permission[] permissions) {
                                            status("权限已通过，正在打开手表应用…");
                                            openThenSend(node);
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(Exception e) {
                                            done(false, "手表权限被拒绝或失败：" + e.getMessage());
                                        }
                                    });
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(Exception e) {
                            done(false, "权限校验失败：" + e.getMessage());
                        }
                    });
        } catch (Throwable e) {
            done(false, "权限接口异常：" + e);
        }
    }

    private void openThenSend(final Node node) {
        try {
            Wearable.getNodeApi(ctx).launchWearApp(node.id, "/pages/index")
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void v) {
                            status("手表应用已打开，正在发送计划…");
                            new android.os.Handler(android.os.Looper.getMainLooper())
                                    .postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            send(node);
                                        }
                                    }, 900);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(Exception e) {
                            String m = e.getMessage() == null ? "" : e.getMessage();
                            if (m.toLowerCase().contains("install")) {
                                done(false, "手表上找不到「吃药提醒」（包名 com.doubao.watchremote）。用表盘自定义工具装 v1.2.1 的 .bin 到手表，装好再点同步。\n如果表上还留着旧版（旧包名），先在手表应用列表里把它删掉。");
                            } else {
                                status("打开手表应用失败（" + m + "），直接尝试发送…");
                                send(node);
                            }
                        }
                    });
        } catch (Throwable e) {
            send(node);
        }
    }

    /** 到点提醒时，顺带点亮/震动手表（表上收到就响，手机端仍是主提醒） */
    public static void remind(final Context context, final String name, final String time) {
        final Context ctx = context.getApplicationContext();
        try {
            Wearable.getNodeApi(ctx).getConnectedNodes()
                    .addOnSuccessListener(new OnSuccessListener<List<Node>>() {
                        @Override
                        public void onSuccess(List<Node> nodes) {
                            if (nodes == null || nodes.isEmpty()) return;
                            try {
                                JSONObject o = new JSONObject();
                                o.put("tag", "remind");
                                o.put("name", name);
                                o.put("time", time);
                                Wearable.getMessageApi(ctx).sendMessage(nodes.get(0).id,
                                        o.toString().getBytes(Charset.forName("UTF-8")));
                            } catch (Throwable ignored) {
                            }
                        }
                    });
        } catch (Throwable ignored) {
        }
    }

    /** 手机端改了“今天已吃次数”后，把最新计划+次数推给手表（不打开表端应用） */
    public static void pushPlans(final Context context) {
        pushPlans(context, 0);
    }

    private static void pushPlans(final Context context, final int retry) {
        final Context ctx = context.getApplicationContext();
        try {
            Wearable.getNodeApi(ctx).getConnectedNodes()
                    .addOnSuccessListener(new OnSuccessListener<List<Node>>() {
                        @Override
                        public void onSuccess(List<Node> nodes) {
                            if (nodes == null || nodes.isEmpty()) return;
                            try {
                                final String body = buildPayload(ctx);
                                Wearable.getMessageApi(ctx).sendMessage(nodes.get(0).id,
                                        body.getBytes(Charset.forName("UTF-8")))
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(Exception e) {
                                                if (retry < 2) {
                                                    new android.os.Handler(android.os.Looper.getMainLooper())
                                                            .postDelayed(new Runnable() {
                                                                @Override
                                                                public void run() {
                                                                    pushPlans(ctx, retry + 1);
                                                                }
                                                            }, 1200);
                                                }
                                            }
                                        });
                            } catch (Throwable ignored) {
                            }
                        }
                    });
        } catch (Throwable ignored) {
        }
    }

    /** 请求手表把它今天的次数推回来（手机端据此校正记录） */
    private void sendPull(Node node) {
        try {
            JSONObject o = new JSONObject();
            o.put("tag", "pull");
            Wearable.getMessageApi(ctx).sendMessage(node.id, o.toString().getBytes(Charset.forName("UTF-8")));
        } catch (Throwable ignored) {
        }
    }

    /** 外部主动触发一次“让手表推次数” */
    public static void pull(final Context context) {
        final Context ctx = context.getApplicationContext();
        try {
            Wearable.getNodeApi(ctx).getConnectedNodes()
                    .addOnSuccessListener(new OnSuccessListener<List<Node>>() {
                        @Override
                        public void onSuccess(List<Node> nodes) {
                            if (nodes == null || nodes.isEmpty()) return;
                            try {
                                JSONObject o = new JSONObject();
                                o.put("tag", "pull");
                                Wearable.getMessageApi(ctx).sendMessage(nodes.get(0).id,
                                        o.toString().getBytes(Charset.forName("UTF-8")));
                            } catch (Throwable ignored) {
                            }
                        }
                    });
        } catch (Throwable ignored) {
        }
    }

    private void send(Node node) {
        String payload;
        try {
            payload = buildPayload(ctx);
        } catch (Throwable e) {
            done(false, "生成计划数据失败：" + e);
            return;
        }
        try {
            MessageApi messageApi = Wearable.getMessageApi(ctx);
            messageApi.sendMessage(node.id, payload.getBytes(Charset.forName("UTF-8")))
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void v) {
                            sendPull(node);
                            done(true, "已发到手表。手表上打开「吃药提醒」就能看到新计划。");
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(Exception e) {
                            String m = e.getMessage() == null ? "" : e.getMessage();
                            if (m.toLowerCase().contains("signature")) {
                                done(false, "签名不匹配：手表端安装包和这个 App 不是同一个证书签的，"
                                        + "删掉手表上的旧版，用 App 里导出的新版 .bin 重装一次。");
                            } else if (m.toLowerCase().contains("install")) {
                                done(false, "手表上没装「吃药提醒」应用，先用表盘自定义工具把 .bin 装到手表上。");
                            } else {
                                done(false, "发送失败：" + m);
                            }
                        }
                    });
        } catch (Throwable e) {
            done(false, "消息接口异常：" + e);
        }
    }

    /** 生成手表端需要的 JSON：{"tag":"plans","plans":[{"name","dose","meal","purpose","times":["08:00"]}]} */
    public static String buildPayload(Context ctx) throws Exception {
        List<Medicine> meds = MedStore.load(ctx);
        JSONObject root = new JSONObject();
        root.put("tag", "plans");
        JSONArray arr = new JSONArray();
        for (Medicine m : meds) {
            if (m.times.isEmpty()) continue;
            // 手机端关掉提醒的计划也要下发，手表那边只在「管理次数」里显示成已关闭
            JSONObject o = new JSONObject();
            o.put("id", String.valueOf(m.id));
            o.put("name", m.name);
            o.put("dose", m.dose);
            o.put("meal", "不限".equals(m.meal) ? "" : m.meal);
            o.put("purpose", m.purpose == null ? "" : m.purpose);
            o.put("off", !m.enabled);
            o.put("t", SyncStamp.last(ctx));
            JSONArray t = new JSONArray();
            for (int[] hm : m.times) {
                t.put(String.format(java.util.Locale.US, "%02d:%02d", hm[0], hm[1]));
            }
            o.put("times", t);
            o.put("today", MedStore.takenCount(ctx, m.id));
            arr.put(o);
        }
        root.put("plans", arr);
        root.put("color", AppTheme.hex(AppTheme.accent(ctx)));
        // 端到端时间戳 + 手机端的暂停名单，手表据此对齐
        root.put("at", SyncStamp.last(ctx));
        try {
            root.put("paused", new JSONArray(WatchFlags.pausedJson(ctx)));
        } catch (Throwable ignored) {
            root.put("paused", new JSONArray());
        }
        return root.toString();
    }

    /** 手机端换了主题色，顺手把手表端的配色也推过去 */
    public static void pushTheme(final Context context) {
        final Context ctx = context.getApplicationContext();
        final String hex = AppTheme.hex(AppTheme.accent(ctx));
        try {
            Wearable.getNodeApi(ctx).getConnectedNodes()
                    .addOnSuccessListener(new OnSuccessListener<List<Node>>() {
                        @Override
                        public void onSuccess(List<Node> nodes) {
                            if (nodes == null || nodes.isEmpty()) return;
                            try {
                                JSONObject o = new JSONObject();
                                o.put("tag", "theme");
                                o.put("color", hex);
                                Wearable.getMessageApi(ctx).sendMessage(nodes.get(0).id,
                                        o.toString().getBytes(Charset.forName("UTF-8")));
                            } catch (Throwable ignored) {
                            }
                        }
                    });
        } catch (Throwable ignored) {
        }
    }

    /** 手机打开应用时直接把手表端应用拉起来（NodeApi.launchWearApp） */
    public static void openWatchApp(final Context context, final String page) {
        final Context ctx = context.getApplicationContext();
        try {
            com.xiaomi.xms.wearable.Wearable.getNodeApi(ctx).getConnectedNodes()
                    .addOnSuccessListener(new com.xiaomi.xms.wearable.tasks.OnSuccessListener<java.util.List<com.xiaomi.xms.wearable.node.Node>>() {
                        @Override
                        public void onSuccess(java.util.List<com.xiaomi.xms.wearable.node.Node> nodes) {
                            if (nodes == null || nodes.isEmpty()) return;
                            try {
                                com.xiaomi.xms.wearable.Wearable.getNodeApi(ctx)
                                        .launchWearApp(nodes.get(0).id, page == null ? "/pages/index" : page);
                            } catch (Throwable ignored) {
                            }
                        }
                    });
        } catch (Throwable ignored) {
        }
    }
}
