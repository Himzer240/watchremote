package com.doubao.watchremote;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Medicine {

    public long id;
    public String name = "";
    public String dose = "1片";
    public String meal = "不限";
    public String purpose = "";
    public boolean enabled = true;
    public ArrayList<int[]> times = new ArrayList<>();
    /** 整个计划统一提醒方式：-1 跟随每个时间，0 闹钟、1 悬浮弹窗、2 不提醒 */
    public int groupMode = 0;
    /** 每个时间点的提醒方式：0 闹钟、1 悬浮弹窗、2 不提醒 */
    public ArrayList<Integer> modes = new ArrayList<>();

    public JSONObject toJson() {
        JSONObject o = new JSONObject();
        try {
            o.put("id", id);
            o.put("name", name);
            o.put("dose", dose);
            o.put("meal", meal);
            o.put("purpose", purpose);
            o.put("enabled", enabled);
            JSONArray arr = new JSONArray();
            for (int[] t : times) {
                JSONArray one = new JSONArray();
                one.put(t[0]);
                one.put(t[1]);
                arr.put(one);
            }
            o.put("times", arr);
            JSONArray mA = new JSONArray();
            ensureModes();
            for (int i = 0; i < times.size(); i++) mA.put(modeAt(i));
            o.put("modes", mA);
            o.put("groupMode", groupMode);
        } catch (Exception ignored) {
        }
        return o;
    }

    public static Medicine fromJson(JSONObject o) {
        Medicine m = new Medicine();
        m.id = o.optLong("id", 0L);
        m.name = o.optString("name", "药品");
        m.dose = o.optString("dose", "1片");
        m.meal = o.optString("meal", "不限");
        m.purpose = o.optString("purpose", "");
        m.enabled = o.optBoolean("enabled", true);
        m.groupMode = o.optInt("groupMode", -1);
        if (m.groupMode < -1 || m.groupMode > 2) m.groupMode = -1;
        JSONArray mA = o.optJSONArray("modes");
        if (mA != null) {
            for (int i = 0; i < mA.length(); i++) m.modes.add(mA.optInt(i, 0));
        }
        JSONArray arr = o.optJSONArray("times");
        if (arr != null) {
            for (int i = 0; i < arr.length(); i++) {
                JSONArray one = arr.optJSONArray(i);
                if (one != null && one.length() >= 2) {
                    m.times.add(new int[]{one.optInt(0, 8), one.optInt(1, 0)});
                }
            }
        }
        return m;
    }

    public void ensureModes() {
        while (modes.size() < times.size()) modes.add(0);
        while (modes.size() > times.size()) modes.remove(modes.size() - 1);
        for (int i = 0; i < modes.size(); i++) {
            int v = modes.get(i);
            if (v < 0 || v > 2) modes.set(i, 0);
        }
    }

    public int modeAt(int index) {
        if (index < 0 || index >= modes.size()) return 0;
        return modes.get(index);
    }

    public String timeAt(int index) {
        if (index < 0 || index >= times.size()) return "";
        int[] t = times.get(index);
        return String.format(Locale.CHINA, "%02d:%02d", t[0], t[1]);
    }

    public String timesText() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < times.size(); i++) {
            if (i > 0) sb.append("   ");
            sb.append(timeAt(i));
        }
        return sb.toString();
    }

    public String timesTextWithModes() {
        return timesTextWithModes(false);
    }

    /** asEnglish=true 时提醒方式用英文（Pop-up / Silent） */
    public String timesTextWithModes(boolean asEnglish) {
        ensureModes();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < times.size(); i++) {
            if (i > 0) sb.append("   ");
            sb.append(timeAt(i));
            int md = modeAt(i);
            if (md == 1) sb.append(asEnglish ? "  pop-up" : " 悬浮");
            else if (md == 2) sb.append(asEnglish ? "  silent" : " 静音");
        }
        return sb.toString();
    }

    public int doseCount() {
        return times.size();
    }
}
