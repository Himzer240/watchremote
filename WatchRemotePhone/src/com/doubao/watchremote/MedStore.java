package com.doubao.watchremote;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import android.os.Environment;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MedStore {

    private static final String PREF = "med_data";
    private static final String KEY = "list";
    private static final String LOG_PREF = "med_log";
    private static final String LOG_KEY = "log";

    public static List<Medicine> load(Context c) {
        ArrayList<Medicine> out = new ArrayList<>();
        SharedPreferences sp = c.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        String raw = sp.getString(KEY, "[]");
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o != null) out.add(Medicine.fromJson(o));
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    public static void save(Context c, List<Medicine> list) {
        JSONArray arr = new JSONArray();
        for (Medicine m : list) arr.put(m.toJson());
        c.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .edit().putString(KEY, arr.toString()).apply();
        // 手机端一改就盖个时间戳，并立刻推给手表，免得两边各说各话
        SyncStamp.touch(c);
        WatchSync.pushPlans(c);
    }

    public static Medicine find(Context c, long id) {
        for (Medicine m : load(c)) {
            if (m.id == id) return m;
        }
        return null;
    }

    public static void upsert(Context c, Medicine med) {
        List<Medicine> list = load(c);
        boolean replaced = false;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id == med.id) {
                list.set(i, med);
                replaced = true;
                break;
            }
        }
        if (!replaced) list.add(med);
        save(c, list);
    }

    public static void remove(Context c, long id) {
        List<Medicine> list = load(c);
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id == id) {
                list.remove(i);
                break;
            }
        }
        save(c, list);
    }

    /** 覆盖安装/升级时数据本来就在本地；这里再兜一层：老版本用过的其它键名也搬过来 */
    public static void migrateLegacy(Context c) {
        try {
            SharedPreferences sp = c.getSharedPreferences(PREF, Context.MODE_PRIVATE);
            if (new JSONArray(sp.getString(KEY, "[]")).length() > 0) return;

            String[][] cand = {
                    {"med_data", "med_list"}, {"med_data", "medicines"}, {"med_data", "items"},
                    {"med_data_v1", "list"}, {"medicines", "list"}, {"meds", "list"}
            };
            for (String[] pair : cand) {
                String raw = c.getSharedPreferences(pair[0], Context.MODE_PRIVATE)
                        .getString(pair[1], "");
                if (raw == null || !raw.trim().startsWith("[")) continue;
                JSONArray arr = new JSONArray(raw);
                if (arr.length() == 0) continue;
                sp.edit().putString(KEY, raw).apply();
                return;
            }
        } catch (Throwable ignored) {
        }
    }

    // 备份分区开关
    public static final int SEC_MEDS = 1;
    public static final int SEC_LOGS = 2;
    public static final int SEC_THEME = 4;
    public static final int SEC_DND = 8;
    public static final int SEC_REMIND = 16;
    public static final int SEC_ALL = 31;

    /** 导出备份（默认全部） */
    public static String exportData(Context c) {
        return exportData(c, SEC_ALL);
    }

    /** 按选择的分区导出 */
    public static String exportData(Context c, int sections) {
        JSONObject o = new JSONObject();
        try {
            o.put("app", "MedReminder");
            o.put("version", 2);
            o.put("at", System.currentTimeMillis());
            if ((sections & SEC_MEDS) != 0) {
                o.put("meds", new JSONArray(c.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                        .getString(KEY, "[]")));
            }
            if ((sections & SEC_LOGS) != 0) o.put("logs", logArray(c));
            if ((sections & SEC_THEME) != 0) {
                JSONObject t = new JSONObject();
                t.put("color", AppTheme.accent(c));
                o.put("theme", t);
            }
            if ((sections & SEC_DND) != 0) {
                JSONObject d = new JSONObject();
                d.put("on", RemindPrefs.dndOn(c));
                d.put("all", RemindPrefs.dndAllDay(c));
                d.put("start", RemindPrefs.dndStart(c));
                d.put("end", RemindPrefs.dndEnd(c));
                o.put("dnd", d);
            }
            if ((sections & SEC_REMIND) != 0) {
                JSONObject r = new JSONObject();
                r.put("repeat", RemindPrefs.repeatMinutes(c));
                r.put("global", RemindPrefs.globalMode(c));
                o.put("remind", r);
            }
        } catch (Throwable ignored) {
        }
        return o.toString();
    }

    public static final int MODE_REPLACE = 0;   // 覆盖现有计划
    public static final int MODE_MERGE = 1;     // 保留现有，备份里的计划追加进来

    /** 从备份恢复（默认覆盖），只覆盖文件里有的分区 */
    public static String importData(Context c, String json) throws Exception {
        return importData(c, json, MODE_REPLACE);
    }

    /** 从备份恢复，mode 决定计划是覆盖还是追加；返回恢复了哪些内容 */
    public static String importData(Context c, String json, int mode) throws Exception {
        JSONObject o = new JSONObject(json);
        StringBuilder sb = new StringBuilder();
        JSONArray meds = o.optJSONArray("meds");
        if (meds != null) {
            if (mode == MODE_MERGE) {
                List<Medicine> cur = load(c);
                int added = 0;
                int skipped = 0;
                for (int i = 0; i < meds.length(); i++) {
                    JSONObject mo = meds.optJSONObject(i);
                    if (mo == null) continue;
                    Medicine bm = Medicine.fromJson(mo);
                    if (isSamePlan(cur, bm)) {
                        skipped++;
                        continue;
                    }
                    bm.id = freshId(cur, bm.id);
                    cur.add(bm);
                    added++;
                }
                save(c, cur);
                sb.append("计划新增 ").append(added).append(" 个");
                if (skipped > 0) sb.append("，跳过重复 ").append(skipped).append(" 个");
                sb.append("，现在共 ").append(cur.size()).append(" 个");
            } else {
                c.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                        .edit().putString(KEY, meds.toString()).apply();
                sb.append("计划覆盖为 ").append(meds.length()).append(" 个");
            }
        }
        JSONArray logs = o.optJSONArray("logs");
        if (logs != null) {
            if (mode == MODE_MERGE) {
                JSONArray cur = logArray(c);
                java.util.HashSet<String> have = new java.util.HashSet<String>();
                for (int i = 0; i < cur.length(); i++) have.add(String.valueOf(cur.opt(i)));
                int added = 0;
                for (int i = 0; i < logs.length(); i++) {
                    String one = String.valueOf(logs.opt(i));
                    if (have.add(one)) {
                        cur.put(logs.opt(i));
                        added++;
                    }
                }
                c.getSharedPreferences(LOG_PREF, Context.MODE_PRIVATE)
                        .edit().putString(LOG_KEY, cur.toString()).apply();
                if (sb.length() > 0) sb.append("、");
                sb.append("记录新增 ").append(added).append(" 条");
            } else {
                c.getSharedPreferences(LOG_PREF, Context.MODE_PRIVATE)
                        .edit().putString(LOG_KEY, logs.toString()).apply();
                if (sb.length() > 0) sb.append("、");
                sb.append("服药记录 ").append(logs.length()).append(" 条");
            }
        }
        JSONObject t = o.optJSONObject("theme");
        if (t != null) {
            AppTheme.setAccent(c, t.optInt("color", AppTheme.DEFAULT));
            if (sb.length() > 0) sb.append("、");
            sb.append("主题颜色");
        }
        JSONObject d = o.optJSONObject("dnd");
        if (d != null) {
            RemindPrefs.restoreDnd(c, d.optBoolean("on", false), d.optBoolean("all", true),
                    d.optInt("start", 22 * 60), d.optInt("end", 7 * 60));
            if (sb.length() > 0) sb.append("、");
            sb.append("勿扰设置");
        }
        JSONObject r = o.optJSONObject("remind");
        if (r != null) {
            RemindPrefs.setRepeatMinutes(c, r.optInt("repeat", 5));
            RemindPrefs.setGlobalMode(c, r.optInt("global", RemindPrefs.MODE_FOLLOW));
            if (sb.length() > 0) sb.append("、");
            sb.append("提醒设置");
        }
        return sb.length() == 0 ? "这个文件里没有可恢复的内容" : sb.toString();
    }

    /** 合并时判断是不是同一个计划：药名 + 用量 + 各次时间都相同就算重复 */
    private static boolean isSamePlan(List<Medicine> cur, Medicine bm) {
        for (Medicine m : cur) {
            if (m.name == null || bm.name == null) continue;
            if (!m.name.trim().equalsIgnoreCase(bm.name.trim())) continue;
            String d1 = m.dose == null ? "" : m.dose.trim();
            String d2 = bm.dose == null ? "" : bm.dose.trim();
            if (!d1.equals(d2)) continue;
            if (m.times.size() != bm.times.size()) continue;
            boolean same = true;
            for (int t = 0; t < m.times.size(); t++) {
                int[] a = m.times.get(t);
                int[] b = bm.times.get(t);
                if (a[0] != b[0] || a[1] != b[1]) {
                    same = false;
                    break;
                }
            }
            if (same) return true;
        }
        return false;
    }

    /** 合并恢复时给撞了 id 的计划换一个新 id */
    private static long freshId(List<Medicine> cur, long wanted) {
        boolean used = false;
        for (Medicine m : cur) {
            if (m.id == wanted) {
                used = true;
                break;
            }
        }
        if (!used) return wanted;
        long id = System.currentTimeMillis();
        while (true) {
            boolean clash = false;
            for (Medicine m : cur) {
                if (m.id == id) {
                    clash = true;
                    break;
                }
            }
            if (!clash) return id;
            id++;
        }
    }

    /** 清除所有数据：取消闹钟、清空计划记录、设置回默认；备份文件按 deleteBackups 决定 */
    public static void clearAll(Context c) {
        clearAll(c, false);
    }

    public static void clearAll(Context c, boolean deleteBackups) {
        try {
            for (Medicine m : load(c)) {
                Scheduler.cancelMedicine(c, m);
            }
        } catch (Throwable ignored) {
        }
        c.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().clear().apply();
        c.getSharedPreferences(LOG_PREF, Context.MODE_PRIVATE).edit().clear().apply();
        // 设置全部回默认，只留下通知渠道的 ID，避免把已经建好的渠道弄丢
        try {
            android.content.SharedPreferences ap = c.getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
            String chId = ap.getString("channel_id", null);
            int chSeq = ap.getInt("channel_seq", 0);
            ap.edit().clear().apply();
            if (chId != null) {
                ap.edit().putString("channel_id", chId).putInt("channel_seq", chSeq).apply();
            }
        } catch (Throwable ignored) {
        }
        try {
            c.getSharedPreferences("ocr_prefs", Context.MODE_PRIVATE).edit().clear().apply();
        } catch (Throwable ignored) {
        }
        AppTheme.setAccent(c, AppTheme.DEFAULT);
        if (deleteBackups) {
            try {
                File[] fs = backupDir(c).listFiles();
                if (fs != null) {
                    for (File f : fs) {
                        if (f.isFile()) f.delete();
                    }
                }
            } catch (Throwable ignored) {
            }
        }
    }

    /** 应用自己的备份目录，不用系统的“下载”夹 */
    public static File backupDir(Context c) {
        File d = null;
        try {
            d = c.getExternalFilesDir("吃药提醒备份");
        } catch (Throwable ignored) {
        }
        if (d == null) d = new File(c.getFilesDir(), "吃药提醒备份");
        if (!d.exists()) d.mkdirs();
        return d;
    }

    public static File exportToFolder(Context c) throws Exception {
        return exportToFolder(c, SEC_ALL);
    }

    public static File exportToFolder(Context c, int sections) throws Exception {
        File d = backupDir(c);
        String name = "吃药提醒备份-"
                + new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.CHINA).format(new Date())
                + ".json";
        File f = new File(d, name);
        FileOutputStream out = new FileOutputStream(f);
        out.write(exportData(c, sections).getBytes("UTF-8"));
        out.flush();
        out.close();
        return f;
    }

    public static File[] listBackups(Context c) {
        File[] fs = backupDir(c).listFiles();
        if (fs == null) return new File[0];
        java.util.Arrays.sort(fs, new java.util.Comparator<File>() {
            @Override
            public int compare(File a, File b) {
                return Long.compare(b.lastModified(), a.lastModified());
            }
        });
        return fs;
    }

    public static String importFromFile(Context c, File f) throws Exception {
        return importData(c, readFile(f), MODE_REPLACE);
    }

    public static String importFromFile(Context c, File f, int mode) throws Exception {
        return importData(c, readFile(f), mode);
    }

    /** 读备份文件全文 */
    public static String readFile(File f) throws Exception {
        byte[] buf = new byte[(int) f.length()];
        FileInputStream in = new FileInputStream(f);
        int n = 0;
        while (n < buf.length) {
            int r = in.read(buf, n, buf.length - n);
            if (r <= 0) break;
            n += r;
        }
        in.close();
        return new String(buf, 0, n, "UTF-8");
    }

    // ---------- 服药记录 ----------

    public static void markTaken(Context c, long medId) {
        SharedPreferences sp = c.getSharedPreferences(LOG_PREF, Context.MODE_PRIVATE);
        JSONArray arr;
        try {
            arr = new JSONArray(sp.getString(LOG_KEY, "[]"));
        } catch (Exception e) {
            arr = new JSONArray();
        }
        try {
            JSONObject o = new JSONObject();
            o.put("med", medId);
            o.put("t", System.currentTimeMillis());
            arr.put(o);
            while (arr.length() > 300) {
                arr.remove(0);
            }
        } catch (Exception ignored) {
        }
        sp.edit().putString(LOG_KEY, arr.toString()).apply();
    }

    private static JSONArray logArray(Context c) {
        SharedPreferences sp = c.getSharedPreferences(LOG_PREF, Context.MODE_PRIVATE);
        try {
            return new JSONArray(sp.getString(LOG_KEY, "[]"));
        } catch (Exception e) {
            return new JSONArray();
        }
    }

    public static int takenCount(Context c, long medId) {
        int count = 0;
        JSONArray arr = logArray(c);
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o == null) continue;
            if (o.optLong("med", -1) != medId) continue;
            if (o.optLong("t", 0) >= dayStart()) count++;
        }
        return count;
    }

    /** 这个时间点响过之后，用户有没有吃过 */
    public static boolean takenAfter(Context c, long medId, long since) {
        JSONArray arr = logArray(c);
        for (int i = arr.length() - 1; i >= 0; i--) {
            JSONObject o = arr.optJSONObject(i);
            if (o == null) continue;
            if (o.optLong("med", -1) == medId && o.optLong("t", 0) >= since) return true;
        }
        return false;
    }

    /** 撤销今天最近的一条服药记录，返回是否撤到 */
    /** 手动把“今天已吃次数”改成 target（只动服药记录，不动计划本身） */
    public static void setTakenCount(Context c, long medId, int target) {
        int cur = takenCount(c, medId);
        while (cur < target) {
            markTaken(c, medId);
            cur++;
        }
        while (cur > target) {
            if (!removeLastTaken(c, medId)) break;
            cur--;
        }
    }

    public static boolean removeLastTaken(Context c, long medId) {
        SharedPreferences sp = c.getSharedPreferences(LOG_PREF, Context.MODE_PRIVATE);
        JSONArray arr = logArray(c);
        int hit = -1;
        for (int i = arr.length() - 1; i >= 0; i--) {
            JSONObject o = arr.optJSONObject(i);
            if (o == null) continue;
            if (o.optLong("med", -1) == medId && o.optLong("t", 0) >= dayStart()) {
                hit = i;
                break;
            }
        }
        if (hit < 0) return false;
        arr.remove(hit);
        sp.edit().putString(LOG_KEY, arr.toString()).apply();
        return true;
    }

    public static int takenTotalToday(Context c) {
        int count = 0;
        JSONArray arr = logArray(c);
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o != null && o.optLong("t", 0) >= dayStart()) count++;
        }
        return count;
    }

    public static String todayLogText(Context c) {
        JSONArray arr = logArray(c);
        List<Medicine> meds = load(c);
        StringBuilder sb = new StringBuilder();
        SimpleDateFormat hm = new SimpleDateFormat("HH:mm", Locale.CHINA);
        int shown = 0;
        for (int i = arr.length() - 1; i >= 0 && shown < 8; i--) {
            JSONObject o = arr.optJSONObject(i);
            if (o == null) continue;
            long t = o.optLong("t", 0);
            if (t < dayStart()) continue;
            long medId = o.optLong("med", -1);
            String name = "已删除的药品";
            for (Medicine m : meds) {
                if (m.id == medId) {
                    name = m.name;
                    break;
                }
            }
            if (sb.length() > 0) sb.append("\n");
            sb.append(hm.format(new Date(t))).append("  ").append(name);
            shown++;
        }
        return sb.length() == 0 ? "暂无记录" : sb.toString();
    }

    private static long dayStart() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }
}
