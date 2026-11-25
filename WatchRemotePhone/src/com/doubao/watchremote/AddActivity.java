package com.doubao.watchremote;

import android.app.Activity;
import android.app.AlertDialog;
import android.media.ExifInterface;
import android.graphics.Matrix;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.TimePicker;

import java.util.Calendar;
import java.util.Locale;
import android.widget.Toast;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;

public class AddActivity extends Activity {

    private EditText inputName;
    private EditText inputDose;
    private EditText inputPurpose;
    private RadioGroup mealGroup;
    private LinearLayout timeBox;
    private TextView txtCount;
    private TextView actTitle;

    private static final int REQ_CAMERA = 1001;
    private static final int REQ_GALLERY = 1002;
    private File photoFile;
    private boolean recognizing = false;
    private TextView ocrStatus;

    private int count = 2;
    private long editId = -1;
    private Medicine editing;
    private final ArrayList<int[]> times = new ArrayList<>();
    private final ArrayList<Integer> modes = new ArrayList<>();
    private int groupMode = RemindPrefs.MODE_ALARM;
    private String initSnapshot = "";
    private View modeScrim;
    private View modePanel;
    private boolean modeDrawerOpen = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add);

        inputName = findViewById(R.id.inputName);
        inputDose = findViewById(R.id.inputDose);
        inputPurpose = findViewById(R.id.inputPurpose);
        ocrStatus = findViewById(R.id.ocrStatus);
        modeScrim = findViewById(R.id.modeScrim);
        modePanel = findViewById(R.id.modePanel);
        findViewById(R.id.inputGroupMode).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openModeDrawer();
            }
        });
        modeScrim.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeModeDrawer();
            }
        });
        findViewById(R.id.modeFollow).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickGroupMode(-1);
            }
        });
        findViewById(R.id.modeAlarm).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickGroupMode(RemindPrefs.MODE_ALARM);
            }
        });
        findViewById(R.id.modeHeads).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickGroupMode(RemindPrefs.MODE_HEADS);
            }
        });
        findViewById(R.id.modeSilent).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickGroupMode(RemindPrefs.MODE_SILENT);
            }
        });
        mealGroup = findViewById(R.id.mealGroup);
        timeBox = findViewById(R.id.timeBox);
        txtCount = findViewById(R.id.txtCount);
        AppTheme.apply(this);
        actTitle = findViewById(R.id.actTitle);

        editId = getIntent().getLongExtra("medId", -1);
        if (editId >= 0) {
            editing = MedStore.find(this, editId);
        }

        findViewById(R.id.btnBack).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmExit();
            }
        });

        findViewById(R.id.btnMinus).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (count > 1) {
                    count--;
                    applyCount();
                }
            }
        });

        findViewById(R.id.btnPlus).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (count < 6) {
                    count++;
                    applyCount();
                }
            }
        });

        findViewById(R.id.btnSave).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                save();
            }
        });

        findViewById(R.id.btnCamera).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePhoto();
            }
        });

        findViewById(R.id.btnOcrConfig).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ocrSettingsDialog();
            }
        });
        findViewById(R.id.btnGallery).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickPhoto();
            }
        });

        if (editing != null) {
            actTitle.setText(Lang.s(this, "编辑提醒"));
            inputName.setText(editing.name);
            inputDose.setText(editing.dose);
            inputPurpose.setText(editing.purpose);
            selectMeal(editing.meal);
            for (int[] t : editing.times) times.add(new int[]{t[0], t[1]});
            editing.ensureModes();
            groupMode = editing.groupMode;
            for (int i = 0; i < editing.times.size(); i++) modes.add(editing.modeAt(i));
            count = Math.max(1, Math.min(6, times.size()));
        } else {
            count = 2;
            times.add(defaultTime(0, 2));
            times.add(defaultTime(1, 2));
            modes.add(RemindPrefs.MODE_ALARM);
            modes.add(RemindPrefs.MODE_ALARM);
        }

        txtCount.setText(String.valueOf(count));
        updateGroupModeText();
        buildPickers();
        initSnapshot = snapshot();
    }

    private void applyCount() {
        syncFromPickers();
        while (times.size() < count) {
            times.add(defaultTime(times.size(), count));
            modes.add(RemindPrefs.MODE_ALARM);
        }
        while (times.size() > count) {
            times.remove(times.size() - 1);
            if (!modes.isEmpty()) modes.remove(modes.size() - 1);
        }
        txtCount.setText(String.valueOf(count));
        updateGroupModeText();
        buildPickers();
        initSnapshot = snapshot();
    }

    private void buildPickers() {
        timeBox.removeAllViews();
        for (int i = 0; i < times.size(); i++) {
            View row = getLayoutInflater().inflate(R.layout.row_time, timeBox, false);
            TextView label = row.findViewById(R.id.rowLabel);
            TimePicker picker = row.findViewById(R.id.rowPicker);
            label.setText(Tone.s(this, "第 " + (i + 1) + " 次", "第 " + (i + 1) + " 次喵",
                    "Dose " + (i + 1)));
            picker.setIs24HourView(Boolean.TRUE);
            int[] t = times.get(i);
            picker.setHour(t[0]);
            picker.setMinute(t[1]);

            final TextView chip = row.findViewById(R.id.rowMode);
            int md = i < modes.size() ? modes.get(i) : RemindPrefs.MODE_ALARM;
            chip.setTag(md);
            chip.setText(Lang.s(this, RemindPrefs.modeName(md)));
            if (groupMode >= 0) chip.setAlpha(0.45f);
            else chip.setAlpha(1f);
            final TextView hint = row.findViewById(R.id.rowHint);
            hint.setText(hintFor(t[0], t[1]));
            picker.setOnTimeChangedListener(new TimePicker.OnTimeChangedListener() {
                @Override
                public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
                    hint.setText(hintFor(hourOfDay, minute));
                }
            });

            chip.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int cur = (Integer) chip.getTag();
                    int next = cur == RemindPrefs.MODE_ALARM ? RemindPrefs.MODE_HEADS
                            : (cur == RemindPrefs.MODE_HEADS ? RemindPrefs.MODE_SILENT
                            : RemindPrefs.MODE_ALARM);
                    chip.setTag(next);
                    chip.setText(Lang.s(AddActivity.this, RemindPrefs.modeName(next)));
                }
            });
            timeBox.addView(row);
        }
    }

    private void syncFromPickers() {
        int n = Math.min(times.size(), timeBox.getChildCount());
        for (int i = 0; i < n; i++) {
            View row = timeBox.getChildAt(i);
            TimePicker picker = row.findViewById(R.id.rowPicker);
            times.get(i)[0] = picker.getHour();
            times.get(i)[1] = picker.getMinute();
            TextView chip = row.findViewById(R.id.rowMode);
            Object tag = chip.getTag();
            int md = tag instanceof Integer ? (Integer) tag : RemindPrefs.MODE_ALARM;
            if (i < modes.size()) modes.set(i, md); else modes.add(md);
        }
    }

    private void selectMeal(String meal) {
        int id = R.id.mealAny;
        if ("饭前".equals(meal)) id = R.id.mealBefore;
        else if ("饭后".equals(meal)) id = R.id.mealAfter;
        else if ("随餐".equals(meal)) id = R.id.mealWith;
        ((RadioButton) findViewById(id)).setChecked(true);
    }

    private String getMeal() {
        int id = mealGroup.getCheckedRadioButtonId();
        if (id == R.id.mealBefore) return "饭前";
        if (id == R.id.mealAfter) return "饭后";
        if (id == R.id.mealWith) return "随餐";
        return "不限";
    }

    /** 默认时间跟着现在走：第一次在当前时间 1 小时后，后面每 4 小时一次，超过 23:45 就落到早上 7:30 */
    private int[] defaultTime(int index, int total) {
        Calendar now = Calendar.getInstance();
        int baseMin = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE) + 60;
        baseMin = (baseMin / 5) * 5;          // 取整到 5 分钟
        int cur = baseMin + index * 4 * 60;
        int dayShift = 0;
        while (cur >= 24 * 60) {
            cur -= 24 * 60;
            dayShift++;
        }
        if (dayShift == 0 && cur >= 23 * 60 + 45) cur = 7 * 60 + 30;  // 太晚就落到明早
        if (baseMin >= 24 * 60) {
            cur = (7 * 60 + 30) + index * 3 * 60;
            while (cur >= 24 * 60) cur -= 24 * 60;
        }
        return new int[]{cur / 60, cur % 60};
    }

    /** 判断这个时间今天还是明天开始 */
    private String hintFor(int hour, int minute) {
        Calendar now = Calendar.getInstance();
        int nowMin = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
        int t = hour * 60 + minute;
        String hm = String.format(Locale.CHINA, "%02d:%02d", hour, minute);
        boolean en = Tone.style(this) == Tone.EN;
        if (en) return (t > nowMin ? "Starts today " : "Starts tomorrow ") + hm;
        return (t > nowMin ? "今天 " : "明天 ") + hm + " 开始";
    }


    // ---------------- 拍照 / 相册识别 ----------------

    private void takePhoto() {
        if (recognizing) return;
        try {
            photoFile = new File(LocalFileProvider.dir(this),
                    "scan_" + System.currentTimeMillis() + ".jpg");
            Uri out = LocalFileProvider.uriFor(photoFile);
            Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            i.putExtra(MediaStore.EXTRA_OUTPUT, out);
            i.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivityForResult(i, REQ_CAMERA);
        } catch (Exception e) {
            Loc.toast(this, "这台手机没有可用的相机应用", Toast.LENGTH_SHORT).show();
        }
    }

    private void pickPhoto() {
        if (recognizing) return;
        try {
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.setType("image/*");
            i.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(i, REQ_GALLERY);
        } catch (Exception e) {
            Loc.toast(this, "没有可用的相册应用", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) return;
        Bitmap bmp = null;
        try {
            if (requestCode == REQ_CAMERA) {
                if (photoFile != null && photoFile.exists() && photoFile.length() > 0) {
                    bmp = decodeFile(photoFile);
                } else if (data != null && data.getExtras() != null) {
                    Object o = data.getExtras().get("data");
                    if (o instanceof Bitmap) bmp = (Bitmap) o;
                }
            } else if (requestCode == REQ_GALLERY) {
                Uri uri = data == null ? null : data.getData();
                if (uri != null) bmp = decodeUri(uri);
            }
        } catch (Throwable t) {
            bmp = null;
        }
        if (bmp == null) {
            Loc.toast(this, "照片读取失败，换一张清晰的再试", Toast.LENGTH_SHORT).show();
            return;
        }
        runOcr(bmp);
    }

    private Bitmap decodeFile(File f) {
        int orient = 0;
        try {
            orient = new ExifInterface(f.getAbsolutePath()).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, 0);
        } catch (Throwable ignored) {
        }
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(f.getAbsolutePath(), o);
        o.inSampleSize = sampleSize(o.outWidth, o.outHeight);
        o.inJustDecodeBounds = false;
        Bitmap b = BitmapFactory.decodeFile(f.getAbsolutePath(), o);
        return limitSize(rotate(b, orient));
    }

    private Bitmap decodeUri(Uri uri) throws Exception {
        int orient = 0;
        try {
            InputStream ex = getContentResolver().openInputStream(uri);
            if (ex != null) {
                orient = new ExifInterface(ex).getAttributeInt(
                        ExifInterface.TAG_ORIENTATION, 0);
                ex.close();
            }
        } catch (Throwable ignored) {
        }
        InputStream is = getContentResolver().openInputStream(uri);
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(is, null, o);
        if (is != null) is.close();
        o.inSampleSize = sampleSize(o.outWidth, o.outHeight);
        o.inJustDecodeBounds = false;
        is = getContentResolver().openInputStream(uri);
        Bitmap b = BitmapFactory.decodeStream(is, null, o);
        if (is != null) is.close();
        return limitSize(rotate(b, orient));
    }

    /** 按 EXIF 方向把照片摆正，竖拍的照片不摆正会认不准 */
    private Bitmap rotate(Bitmap b, int orient) {
        if (b == null) return null;
        int deg = 0;
        switch (orient) {
            case ExifInterface.ORIENTATION_ROTATE_90: deg = 90; break;
            case ExifInterface.ORIENTATION_ROTATE_180: deg = 180; break;
            case ExifInterface.ORIENTATION_ROTATE_270: deg = 270; break;
            default: return b;
        }
        try {
            Matrix m = new Matrix();
            m.postRotate(deg);
            Bitmap n = Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), m, true);
            if (n != b) b.recycle();
            return n;
        } catch (Throwable t) {
            return b;
        }
    }

    private int sampleSize(int w, int h) {
        int max = Math.max(w, h);
        int s = 1;
        while (max / s > 1800) s *= 2;
        return s;
    }

    private Bitmap limitSize(Bitmap b) {
        if (b == null) return null;
        int max = Math.max(b.getWidth(), b.getHeight());
        if (max <= 1800) return b;
        float r = 1800f / max;
        Bitmap n = Bitmap.createScaledBitmap(b, Math.round(b.getWidth() * r),
                Math.round(b.getHeight() * r), true);
        if (n != b) b.recycle();
        return n;
    }

    private void runOcr(final Bitmap bmp) {
        if (!OnlineOcr.hasBaidu(this) && !OnlineOcr.hasSpace(this)) {
            ocrSettingsDialog();
            return;
        }
        recognizing = true;
        ocrStatus.setVisibility(View.VISIBLE);
        ocrStatus.setText(Tone.s(this,
                "正在用 " + OnlineOcr.providerName(this) + " 识别…",
                "正在用 " + OnlineOcr.providerName(this) + " 认字喵…",
                "Scanning with " + OnlineOcr.providerName(this) + "…"));
        OnlineOcr.recognize(this, bmp, new OnlineOcr.Callback() {
            @Override
            public void onDone(final String text, final String error) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        recognizing = false;
                        ocrStatus.setVisibility(View.GONE);
                        try {
                            bmp.recycle();
                        } catch (Throwable ignored) {
                        }
                        if (error != null || text == null || text.trim().isEmpty()) {
            Loc.toast(AddActivity.this,
                                    Tone.s(AddActivity.this,
                                            "没识别出文字：" + (error == null ? "图片里没有清晰文字" : error),
                                            null,
                                            "No text recognised: " + (error == null ? "the image has no clear text" : error)),
                                    Toast.LENGTH_LONG).show();
                            return;
                        }
                        PrescriptionParser p = PrescriptionParser.parse(text);
                        applyParsed(p);
                        showResult(p, text);
                    }
                });
            }
        });
    }

    private void applyParsed(PrescriptionParser p) {
        if (!p.name.isEmpty()) inputName.setText(p.name);
        if (!p.dose.isEmpty()) inputDose.setText(p.dose);
        if (!p.purpose.isEmpty()) inputPurpose.setText(p.purpose);
        selectMeal(p.meal);

        if (p.count >= 1 && p.count <= 6) {
            count = p.count;
        }
        times.clear();
        if (!p.times.isEmpty()) {
            for (int[] t : p.times) times.add(new int[]{t[0], t[1]});
        }
        while (times.size() < count) {
            int[][] d = PrescriptionParser.defaultTimes(count).toArray(new int[0][]);
            for (int i = times.size(); i < count && i < d.length; i++) {
                times.add(new int[]{d[i][0], d[i][1]});
            }
            if (times.size() < count) times.add(new int[]{8, 0});
        }
        while (times.size() > count) times.remove(times.size() - 1);
        modes.clear();
        for (int i = 0; i < times.size(); i++) modes.add(RemindPrefs.MODE_ALARM);

        txtCount.setText(String.valueOf(count));
        updateGroupModeText();
        buildPickers();
        initSnapshot = snapshot();
    }

    private void showResult(PrescriptionParser p, String text) {
        boolean en = Tone.style(this) == Tone.EN;
        String none = en ? "not recognised" : "未识别";
        StringBuilder sb = new StringBuilder();
        sb.append(en ? "Name: " : "药名：").append(p.name.isEmpty() ? none : p.name).append("\n");
        sb.append(en ? "Dose: " : "用量：").append(p.dose.isEmpty() ? none : p.dose).append("\n");
        sb.append(en ? "Doses/day: " : "每天：")
                .append(p.count > 0 ? (en ? p.count + "" : p.count + " 次") : none).append("\n");
        sb.append(en ? "With meals: " : "饭前饭后：").append(Lang.s(this, p.meal)).append("\n");
        if (!p.purpose.isEmpty()) sb.append(en ? "Purpose: " : "用处：").append(p.purpose).append("\n");
        sb.append(en ? "\nAuto-filled; please check and save.\n\nOriginal:\n"
                : "\n已自动填好，请核对后保存。\n\n原文：");
        String cut = text.replace('\n', ' ').trim();
        if (cut.length() > 120) cut = cut.substring(0, 120) + "…";
        sb.append(cut);

        new Loc(this)
                .setTitle("识别结果")
                .setMessage(sb.toString())
                .setNegativeButton("重新识别", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        pickPhoto();
                    }
                })
                .setPositiveButton("好", null)
                .show();
    }

    private void ocrSettingsDialog() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (18 * getResources().getDisplayMetrics().density);
        box.setPadding(pad, pad / 2, pad, 0);

        TextView tip = new TextView(this);
        tip.setText("填百度 OCR 的 API Key + Secret Key（每月免费额度，准确率高）；\n不填百度就默认用 OCR.space 的免费 Key。");
        tip.setTextSize(12);
        tip.setTextColor(0xFF666666);
        box.addView(tip);

        final EditText e1 = new EditText(this);
        e1.setHint("百度 API Key（可不填）");
        e1.setText(OnlineOcr.get(this, OnlineOcr.K_BAIDU_KEY, ""));
        box.addView(e1);

        final EditText e2 = new EditText(this);
        e2.setHint("百度 Secret Key（可不填）");
        e2.setText(OnlineOcr.get(this, OnlineOcr.K_BAIDU_SECRET, ""));
        box.addView(e2);

        final EditText e3 = new EditText(this);
        e3.setHint("OCR.space Key");
        e3.setText(OnlineOcr.get(this, OnlineOcr.K_SPACE_KEY, "helloworld"));
        box.addView(e3);

        new Loc(this)
                .setTitle("识别设置")
                .setView(box)
                .setNegativeButton("取消", null)
                .setPositiveButton("保存", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        OnlineOcr.put(AddActivity.this, OnlineOcr.K_BAIDU_KEY,
                                e1.getText().toString());
                        OnlineOcr.put(AddActivity.this, OnlineOcr.K_BAIDU_SECRET,
                                e2.getText().toString());
                        OnlineOcr.put(AddActivity.this, OnlineOcr.K_SPACE_KEY,
                                e3.getText().toString());
                        Loc.toast(AddActivity.this,
                                "已保存，当前用：" + OnlineOcr.providerName(AddActivity.this),
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    /** 用来判断有没有改动，决定返回时要不要提示 */
    private String snapshot() {
        syncFromPickers();
        StringBuilder sb = new StringBuilder();
        sb.append(inputName.getText().toString().trim()).append('|')
                .append(inputDose.getText().toString().trim()).append('|')
                .append(inputPurpose.getText().toString().trim()).append('|')
                .append(getMeal()).append('|').append(count).append('|')
                .append(groupMode).append('|');
        for (int[] t : times) sb.append(t[0]).append(':').append(t[1]).append(',');
        sb.append('|');
        for (int i = 0; i < modes.size(); i++) sb.append(modes.get(i)).append(',');
        return sb.toString();
    }

    private void confirmExit() {
        if (snapshot().equals(initSnapshot)) {
            finish();
            return;
        }
        new Loc(this)
                .setTitle("还没保存哦")
                .setMessage("这个计划的改动还没保存，离开就丢了。")
                .setNegativeButton("继续编辑", null)
                .setPositiveButton("放弃修改", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .show();
    }

    @Override
    public void onBackPressed() {
        if (modeDrawerOpen) {
            closeModeDrawer();
            return;
        }
        confirmExit();
    }

    private void pickGroupMode(int mode) {
        groupMode = mode;
        updateGroupModeText();
        buildPickers();
        closeModeDrawer();
    }

    private void openModeDrawer() {
        modeDrawerOpen = true;
        refreshModeDrawer();
        modeScrim.setVisibility(View.VISIBLE);
        modeScrim.setAlpha(0f);
        modePanel.setVisibility(View.VISIBLE);
        modePanel.post(new Runnable() {
            @Override
            public void run() {
                modePanel.setTranslationY(modePanel.getHeight());
                modePanel.animate().translationY(0f).setDuration(200).start();
            }
        });
        modeScrim.animate().alpha(1f).setDuration(200).start();
    }

    private void closeModeDrawer() {
        if (!modeDrawerOpen) return;
        modeDrawerOpen = false;
        modeScrim.animate().alpha(0f).setDuration(160).start();
        modePanel.animate().translationY(modePanel.getHeight()).setDuration(160)
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        modeScrim.setVisibility(View.GONE);
                        modePanel.setVisibility(View.GONE);
                    }
                }).start();
    }

    private void refreshModeDrawer() {
        boolean en = Tone.style(this) == Tone.EN;
        String mark = groupMode == -1 ? "✓  " : "     ";
        ((TextView) findViewById(R.id.modeFollow)).setText(mark
                + (en ? "Follow each time slot" : "跟随每个时间单独设置"));
        mark = groupMode == RemindPrefs.MODE_ALARM ? "✓  " : "     ";
        ((TextView) findViewById(R.id.modeAlarm)).setText(mark
                + (en ? "All full-screen alarms (default)" : "全部全屏闹钟（默认）"));
        mark = groupMode == RemindPrefs.MODE_HEADS ? "✓  " : "     ";
        ((TextView) findViewById(R.id.modeHeads)).setText(mark
                + (en ? "All pop-ups" : "全部悬浮弹窗"));
        mark = groupMode == RemindPrefs.MODE_SILENT ? "✓  " : "     ";
        ((TextView) findViewById(R.id.modeSilent)).setText(mark
                + (en ? "All silent" : "全部不提醒"));
    }

    private void updateGroupModeText() {
        TextView t = findViewById(R.id.inputGroupMode);
        boolean en = Tone.style(this) == Tone.EN;
        String s;
        if (groupMode == RemindPrefs.MODE_ALARM) s = en ? "Plan: all full-screen alarms" : "整个计划：全部全屏闹钟";
        else if (groupMode == RemindPrefs.MODE_HEADS) s = en ? "Plan: all pop-ups" : "整个计划：全部悬浮弹窗";
        else if (groupMode == RemindPrefs.MODE_SILENT) s = en ? "Plan: all silent" : "整个计划：全部不提醒";
        else s = en ? "Plan: follow each time slot" : "整个计划：跟随每个时间单独设置";
        t.setText(s);
    }

    private void save() {
        String name = inputName.getText().toString().trim();
        if (name.isEmpty()) {
            Loc.toast(this, "请先填写药品名称", Toast.LENGTH_SHORT).show();
            return;
        }
        String dose = inputDose.getText().toString().trim();
        if (dose.isEmpty()) dose = "1片";

        syncFromPickers();

        Medicine m = editing != null ? editing : new Medicine();
        if (editing == null) {
            m.id = System.currentTimeMillis();
            m.enabled = true;
        }
        m.name = name;
        m.dose = dose;
        m.meal = getMeal();
        m.purpose = inputPurpose.getText().toString().trim();
        m.times.clear();
        for (int[] t : times) m.times.add(new int[]{t[0], t[1]});
        m.groupMode = groupMode;
        m.modes.clear();
        while (m.modes.size() < times.size()) m.modes.add(RemindPrefs.MODE_ALARM);
        for (int i = 0; i < times.size(); i++) {
            int md = i < modes.size() ? modes.get(i) : RemindPrefs.MODE_ALARM;
            m.modes.set(i, md);
        }

        MedStore.upsert(this, m);
        Scheduler.rescheduleAll(this);
        WatchSync.pushPlans(this);
        NotificationHelper.ensureChannel(this);

        String firstHint = "已保存";
        if (!times.isEmpty()) {
            Calendar now = Calendar.getInstance();
            int nowMin = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
            int nextT = 24 * 60;
            boolean anyFuture = false;
            for (int[] t : times) {
                int v = t[0] * 60 + t[1];
                if (v > nowMin) {
                    anyFuture = true;
                    nextT = Math.min(nextT, v);
                }
            }
            if (!anyFuture) {
                for (int[] t : times) nextT = Math.min(nextT, t[0] * 60 + t[1]);
            }
            boolean en = Tone.style(this) == Tone.EN;
            firstHint = en
                    ? ("Saved · first alert: " + (anyFuture ? "today " : "tomorrow ")
                    + String.format(Locale.CHINA, "%02d:%02d", nextT / 60, nextT % 60))
                    : ("已保存 · 第一次提醒：" + (anyFuture ? "今天 " : "明天 ")
                    + String.format(Locale.CHINA, "%02d:%02d", nextT / 60, nextT % 60));
        }
        Loc.toast(this, firstHint, Toast.LENGTH_LONG).show();
        finish();
    }
}
