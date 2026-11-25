package com.doubao.watchremote;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 从 OCR 文本里猜出药名、用量、次数、时间、饭前饭后、用处 */
public class PrescriptionParser {

    public String name = "";
    public String dose = "";
    public String meal = "不限";
    public String purpose = "";
    public int count = 0;
    public List<int[]> times = new ArrayList<>();
    public String raw = "";

    private static final Pattern NAME_PAT = Pattern.compile(
            "(?:药品名称|药物名称|通用名称|商品名称|品名|药名)[:：\\s]*([^\\n\\r]{2,20})");
    private static final Pattern NAME_FALLBACK = Pattern.compile(
            "([\\u4e00-\\u9fa5A-Za-z0-9·\\-（）()]{2,18}?"
                    + "(?:缓释胶囊|软胶囊|缓释片|肠溶片|咀嚼片|泡腾片|分散片|口崩片|口服液|混悬液|滴眼液|滴耳液|滴鼻液|喷雾剂|气雾剂|注射液|胶囊|颗粒|糖浆|软膏|乳膏|凝胶|洗剂|栓|酊|片|丸|散))");
    private static final Pattern DOSE_PAT = Pattern.compile(
            "(?:每次|一次|单次|每回)\\s*[:：]?\\s*([0-9一二三四五六七八九十半两.]+)\\s*"
                    + "(片|粒|袋|包|支|毫升|ml|mL|毫克|mg|克|g|滴|喷|丸|胶囊|单位|IU)");
    private static final Pattern DOSE_PAT2 = Pattern.compile(
            "([0-9]+(?:\\.[0-9]+)?)\\s*(片|粒|袋|支|毫升|ml|mL|mg|g)");
    private static final Pattern FREQ_PAT = Pattern.compile(
            "(?:一日|每天|每日|1日|日)\\s*([0-9一二三四五六七八九十]+)\\s*次");
    private static final Pattern FREQ_PAT2 = Pattern.compile(
            "([0-9一二三四五六七八九十]+)\\s*次\\s*/\\s*(?:日|天)");
    private static final Pattern TIME_PAT = Pattern.compile("([01]?\\d|2[0-3])[:：]([0-5]\\d)");
    private static final Pattern EVERY_HOUR_PAT = Pattern.compile(
            "每\\s*([0-9一二三四五六七八九十]+)\\s*(?:个)?小时");
    private static final Pattern PURPOSE_PAT = Pattern.compile(
            "(?:功能主治|适应症|适应证|主治|用于|作用是|功效|作用)[:：]?\\s*([^\\n\\r]{4,80})");

    public static PrescriptionParser parse(String text) {
        PrescriptionParser p = new PrescriptionParser();
        p.raw = text == null ? "" : text;
        String t = p.raw.replace('：', ':').replace('（', '(').replace('）', ')');

        // 药名
        Matcher m = NAME_PAT.matcher(t);
        if (m.find()) {
            p.name = cleanName(m.group(1));
        }
        if (p.name.isEmpty()) {
            m = NAME_FALLBACK.matcher(t);
            while (m.find()) {
                String cand = cleanName(m.group(1));
                if (cand.length() >= 2 && containsHan(cand) && !isNoise(cand)) {
                    p.name = cand;
                    break;
                }
            }
        }
        if (p.name.isEmpty()) {
            for (String line : t.split("[\\n\\r]+")) {
                String s = line.trim();
                if (s.length() >= 2 && s.length() <= 18 && containsHan(s) && !isNoise(s)) {
                    p.name = cleanName(s);
                    break;
                }
            }
        }

        // 用量
        m = DOSE_PAT.matcher(t);
        if (m.find()) {
            p.dose = cnNum(m.group(1)) + normalizeUnit(m.group(2));
        } else {
            m = DOSE_PAT2.matcher(t);
            if (m.find()) {
                p.dose = m.group(1) + normalizeUnit(m.group(2));
            }
        }

        // 次数
        int freq = 0;
        m = FREQ_PAT.matcher(t);
        if (m.find()) freq = (int) parseCn(m.group(1));
        if (freq == 0) {
            m = FREQ_PAT2.matcher(t);
            if (m.find()) freq = (int) parseCn(m.group(1));
        }
        if (freq == 0) {
            if (t.matches("(?is).*\\btid\\b.*")) freq = 3;
            else if (t.matches("(?is).*\\bbid\\b.*")) freq = 2;
            else if (t.matches("(?is).*\\bqid\\b.*")) freq = 4;
            else if (t.matches("(?is).*\\bqd\\b.*")) freq = 1;
        }
        if (freq == 0) {
            m = EVERY_HOUR_PAT.matcher(t);
            if (m.find()) {
                int h = (int) parseCn(m.group(1));
                if (h >= 2 && h <= 12) {
                    int startHour = 8;
                    int n = 0;
                    for (int hh = startHour; hh < 22 && n < 6; hh += h) {
                        p.times.add(new int[]{hh, 0});
                        n++;
                    }
                    freq = p.times.size();
                }
            }
        }
        p.count = Math.max(0, Math.min(6, freq));

        // 明确写出的时间点
        List<int[]> explicit = new ArrayList<>();
        m = TIME_PAT.matcher(t);
        while (m.find() && explicit.size() < 6) {
            explicit.add(new int[]{Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2))});
        }
        if (!explicit.isEmpty()) {
            p.times = explicit;
            if (p.count == 0) p.count = explicit.size();
        } else if (p.times.isEmpty()) {
            List<int[]> kw = new ArrayList<>();
            if (t.contains("早") || t.contains("晨")) kw.add(new int[]{8, 0});
            if (t.contains("中午") || t.contains("午")) kw.add(new int[]{12, 0});
            if (t.contains("晚") || t.contains("睡前")) kw.add(new int[]{20, 0});
            if (!kw.isEmpty()) p.times = kw;
        }
        if (p.times.isEmpty() && p.count > 0) {
            p.times = defaultTimes(p.count);
        }

        // 饭前饭后
        if (t.contains("饭前") || t.contains("餐前") || t.contains("空腹") || t.contains("餐前服用")) {
            p.meal = "饭前";
        } else if (t.contains("饭后") || t.contains("餐后")) {
            p.meal = "饭后";
        } else if (t.contains("随餐") || t.contains("与食物同服") || t.contains("进餐时")) {
            p.meal = "随餐";
        }

        // 用处
        m = PURPOSE_PAT.matcher(t);
        if (m.find()) {
            p.purpose = trimPurpose(m.group(1));
        }

        return p;
    }

    public static List<int[]> defaultTimes(int n) {
        List<int[]> out = new ArrayList<>();
        int[][] table;
        if (n == 1) table = new int[][]{{8, 0}};
        else if (n == 2) table = new int[][]{{8, 0}, {20, 0}};
        else if (n == 3) table = new int[][]{{8, 0}, {12, 30}, {19, 0}};
        else if (n == 4) table = new int[][]{{8, 0}, {12, 0}, {17, 0}, {21, 0}};
        else if (n == 5) table = new int[][]{{7, 30}, {10, 30}, {13, 30}, {17, 0}, {20, 30}};
        else table = new int[][]{{7, 0}, {10, 0}, {13, 0}, {16, 0}, {19, 0}, {22, 0}};
        for (int i = 0; i < Math.min(n, table.length); i++) out.add(table[i]);
        return out;
    }

    private static boolean containsHan(String s) {
        return s.matches(".*[\\u4e00-\\u9fa5].*");
    }

    private static boolean isNoise(String s) {
        String[] bad = {"用法", "用量", "规格", "批号", "批准", "生产", "说明", "成分", "功能",
                "主治", "适用", "贮藏", "包装", "有效期", "日期", "医院", "医师", "处方",
                "患者", "姓名", "性别", "年龄", "科室", "编号", "签名", "金额", "合计"};
        for (String b : bad) if (s.contains(b)) return true;
        return s.matches("^[\\d\\s./:-]+$");
    }

    private static String cleanName(String s) {
        String x = s.trim().replaceAll("^[0-9.、*\\s]+", "").trim();
        x = x.replaceAll("[，,;；].*$", "").trim();
        x = x.replaceAll("(?:每次|一次|单次|每日|每天|一日|用法|用量|规格|饭前|饭后|睡前|口服|温水|请).*$", "").trim();
        x = x.replaceAll("[（(].*?[)）]", "").trim();
        return x.length() > 20 ? x.substring(0, 20) : x;
    }

    private static String trimPurpose(String s) {
        String x = s.trim().replaceAll("^[、,，:：\\s]+", "");
        x = x.replaceAll("[。；;].*$", "");
        return x.length() > 60 ? x.substring(0, 60) : x;
    }

    private static String normalizeUnit(String u) {
        if ("毫升".equals(u)) return "ml";
        if ("毫克".equals(u)) return "mg";
        if ("克".equals(u)) return "g";
        return u;
    }

    private static String cnNum(String s) {
        if (s.matches("[0-9.]+")) return s;
        double v = parseCn(s);
        if (v == Math.floor(v)) return String.valueOf((int) v);
        return String.valueOf(v);
    }

    private static double parseCn(String s) {
        if (s == null || s.isEmpty()) return 0;
        if (s.matches("[0-9]+(?:\\.[0-9]+)?")) return Double.parseDouble(s);
        if (s.equals("半")) return 0.5;
        if (s.equals("两")) return 2;
        String digits = "零一二三四五六七八九";
        if (s.length() == 1) {
            int i = digits.indexOf(s.charAt(0));
            return i >= 0 ? i : 0;
        }
        if (s.startsWith("十")) {
            int rest = s.length() > 1 ? digits.indexOf(s.charAt(1)) : 0;
            return 10 + Math.max(0, rest);
        }
        if (s.length() == 2 && s.charAt(1) == '十') {
            int tens = digits.indexOf(s.charAt(0));
            return Math.max(0, tens) * 10;
        }
        if (s.length() == 3 && s.charAt(1) == '十') {
            int tens = digits.indexOf(s.charAt(0));
            int ones = digits.indexOf(s.charAt(2));
            return Math.max(0, tens) * 10 + Math.max(0, ones);
        }
        return 0;
    }
}
