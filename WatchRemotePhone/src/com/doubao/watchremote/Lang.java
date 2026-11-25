package com.doubao.watchremote;

import android.content.Context;

import java.util.HashMap;
import java.util.Map;

/**
 * 界面文案多风格：书面语（原文）/ 喵语 / English。
 * 传中文原文，按当前风格返回对应文案；没有译文的条目回退中文。
 */
public class Lang {

    private static final Map<String, String> CAT = new HashMap<>();
    private static final Map<String, String> EN = new HashMap<>();

    private static void p(String zh, String cat, String en) {
        if (cat != null) CAT.put(zh, cat);
        if (en != null) EN.put(zh, en);
    }

    private static void e(String zh, String en) {
        EN.put(zh, en);
    }

    static {
        LangCat.load(CAT);
        // ---------------- 反馈入口 ----------------
        p("反馈 / 举报 Bug", "反馈 / 举报问题喵", "Feedback / report a bug");
        p("你的称呼（可不填）", "你的称呼喵（可不填）", "Your name (optional)");
        p("说说遇到的问题：哪个版本、手机还是手表、做了什么、出了什么状况",
                "说说遇到的问题喵：哪个版本、手机还是手表、做了什么、出了什么状况～",
                "Describe the issue: version, phone or watch, what you did, what happened");
        p("发送反馈", "发送反馈喵", "Send feedback");
        p("正在发送…", "正在发送喵…", "Sending…");
        p("反馈已收到，谢谢～", "反馈收到啦喵，谢谢～", "Feedback received, thank you!");
        p("发送失败，稍后再试：", "发送失败喵，稍后再试：", "Send failed, try later: ");
        p("先写点内容再发送", "先写点内容再发喵", "Write something first");

        // ---------------- 主界面 ----------------
        p("吃药提醒", "吃药提醒喵～", "Medicine Reminder");
        p("我的药", "我的药喵", "My medicine");
        p("今日已服用", "今天吃掉啦喵", "Taken today");
        p("今天还没有用药安排", "今天还没有吃药安排喵～", "Nothing scheduled today");
        p("今日暂无用药计划", "今天还没有吃药安排喵～", "No medicine scheduled today");
        p("吃过了", "吃掉啦喵", "Taken");
        p("再吃一遍", "再吃一次喵", "Take again");
        p("添加计划", "加个吃药计划喵", "Add plan");
        p("＋ 添加计划", "＋ 加个计划喵", "＋ Add plan");
        p("＋  添加吃药提醒", "＋  加个吃药提醒喵", "＋  Add a reminder");
        p("我按错了（撤销一次）", "点错了，撤销喵", "Undo last dose");
        p("编辑这个计划", "改这个计划喵", "Edit this plan");
        p("删除提醒", "删掉这个提醒喵", "Delete reminder");
        p("确定删除", "真的要删掉吗喵", "Confirm delete");
        p("取消", "算了喵", "Cancel");
        p("删除", "删掉喵", "Delete");
        p("保存", "记好啦喵", "Save");
        p("保存并开启提醒", "存好并开提醒喵", "Save & enable");
        p("先关掉", "先关掉喵", "Keep off");
        p("关闭", "关掉喵", "Close");
        p("吃药计划", "吃药计划喵", "Medicine plan");
        p("提醒诊断", "提醒体检喵", "Reminder check");
        p("提醒诊断（不提醒时点这里）", "提醒不响点这里喵", "Reminder check (tap if silent)");
        p("手表端", "手表那边喵", "Watch app");
        p("手表端（装到手表上看和打卡）", "手表端（装到手上看和打卡）喵", "Watch app (install on watch)");
        p("同步计划到手表", "把计划送去手表喵", "Sync plans to watch");
        p("连接到手表（只连接，不发计划）", "连上手手表喵（先不发东西）", "Connect to watch (no data)");
        p("打开小米运动健康", "打开小米运动健康喵", "Open Mi Fitness");
        p("导出手表安装包（.bin）", "导出手表包装喵（.bin）", "Export watch package (.bin)");
        p("主题颜色", "界面颜色喵", "Theme colour");
        p("外观", "外观喵", "Appearance");
        p("表达风格", "说话风格喵", "Tone");
        p("老年人模式", "长辈模式喵", "Elder mode");
        p("数据备份", "备份喵", "Backup");
        p("危险操作", "小心操作喵", "Danger zone");
        p("事件记录（最新在最上面）", "事件记录喵（新的在上面）", "Event log (newest first)");
        p("全屏通知（锁屏也能弹窗）", "全屏通知喵（锁屏也弹）", "Full-screen alert (even locked)");
        p("通知类别（悬浮通知 / 震动）", "通知类型喵（悬浮 / 震动）", "Notification category (pop-up / vibration)");
        p("应用详情（权限 / 通知 / 电池）", "应用详情喵（权限 / 通知 / 电池）", "App details (permissions / notifications / battery)");
        p("每 5 分钟提醒一次", "每 5 分钟提醒一次喵", "Every 5 minutes");
        p("从备份恢复（列出备份夹里的文件）", "从备份恢复喵", "Restore from backup");
        p("导出备份（可自选要导出哪些内容）", "导出备份喵（自己选内容）", "Export backup (choose what to include)");
        p("清除所有数据", "清空所有数据喵", "Clear all data");
        p("拍照识别", "拍照认药喵", "Scan label");
        p("从相册选", "从相册挑喵", "Pick from gallery");
        p("主要管什么 / 用处（可选）", "主要治什么 / 用处喵（可选）", "Purpose (optional)");
        p("和吃饭的关系", "和吃饭的关系喵", "With meals");
        p("不限", "不限喵", "Any");
        p("饭前", "饭前喵", "Before meal");
        p("饭后", "饭后喵", "After meal");
        p("随餐", "随餐喵", "With meal");
        p("空腹", "空肚子喵", "Empty stomach");
        p("睡前", "睡前喵", "Before bed");
        p("每天", "每天喵", "Daily");
        p("整个计划的提醒方式", "整个计划的提醒方式喵", "Reminder mode for the plan");
        p("整个计划：跟随每个时间单独设置", "整个计划：按每个时间单独设喵", "Plan: follow each time slot");
        p("全部闹钟提醒", "全部用闹钟喵", "All alarms");
        p("全部悬浮弹窗", "全部悬浮弹喵", "All pop-ups");
        p("全部不提醒", "全部不提醒喵", "All silent");
        p("全部全屏闹钟（默认）", "全部用闹钟喵（默认）", "All full-screen alarms (default)");
        p("跟随每个时间单独设置", "按每个时间单独设喵", "Follow each time slot");
        p("勿扰：未开启", "勿扰：没开喵", "DND: off");
        p("去开启 →", "去打开喵 →", "Turn on →");
        p("打开应用的通知设置", "打开通知设置喵", "Open notification settings");
        p("当前状态：", "现在状态喵：", "Status: ");
        p("手表：", "手表喵：", "Watch: ");
        p("今天共吃", "今天一共吃掉", "taken today");
        p("已刷新", "刷新好啦喵", "Refreshed");
        p("刷新中…", "刷新中喵…", "Refreshing…");
        p("已连接", "连上啦喵", "Connected");
        p("连接中", "正在连喵", "Connecting");
        p("未连接", "还没连上喵", "Not connected");
        p("重连中", "正在重连喵", "Reconnecting");
        p("未启动", "没启动喵", "Not started");
        p("未授权", "没给权限喵", "Not authorised");
        p("不可用", "用不了喵", "Unavailable");
        p("测试提醒（不会记账）", "测试提醒喵（不记账）", "Test reminder (no logging)");
        p("10 秒后发一条测试提醒", "10 秒后发一条测试喵", "Send a test alert in 10 s");
        p("10 分钟后再提醒", "10 分钟后再叫你喵", "Snooze 10 min");
        p("确认已服用该次药物？", "真的吃掉这次药了吗喵？", "Confirm this dose?");
        p("是", "是喵", "Yes");
        p("否", "不是喵", "No");
        p("确定再吃", "确定再吃喵", "Confirm extra dose");
        p("该吃药了", "该吃药啦喵", "Time for medicine");
        p("服药提醒", "吃药提醒喵", "Medicine reminder");
        p("次", "次喵", "times");
        p("片", "片喵", "tablet(s)");
        p("粒", "粒喵", "pill(s)");
        p("毫克", "毫克喵", "mg");

        // ---------------- 备份 / 引导 长文案 ----------------
        p("备份存在应用自己的目录里，不进系统的“下载”夹；覆盖安装不会丢，卸载前先导出。完整路径导出后会提示。",
                "备份放在应用自己的文件夹喵，不进“下载”夹；覆盖安装不会丢，卸载前记得导出～",
                "Backups live in the app's own folder, not Downloads. Survives updates; export before uninstalling. The full path is shown after export.");
        p("会删掉全部吃药计划、服药记录，取消已排好的提醒，并把勿扰、提醒方式、主题颜色等设置恢复默认。点按钮时会问你要不要连备份文件一起删。",
                "会删掉全部计划、记录和排好的提醒，设置也回默认喵。点按钮时会问要不要连备份一起删～",
                "Deletes all plans, dose logs and scheduled alerts, and resets DND, reminder mode and theme colour. You'll be asked whether to delete backups too.");
        p("提示：允许本应用后台运行并加入电池优化白名单，否则到点可能弹不出。重启手机后提醒会自动恢复。",
                "小提示喵：让本应用在后台运行、加入电池优化白名单，不然到点可能弹不出来～重启手机后提醒会自动恢复。",
                "Tip: allow background running and add this app to the battery whitelist, or alerts may not appear. Reminders are restored after a reboot.");
        p("照片或说明书上传后自动识别药名、用量、次数、时间与用途，识别完请核对。",
                "拍照或传说明书，自动认出药名、用量、次数、时间和用处喵，认完记得核对一下～",
                "Upload a photo or leaflet and the name, dose, times and purpose are filled in automatically. Please verify after scanning.");
        p("手机一键把计划送到手表（走小米运动健康的穿戴通道，不用连电脑、不用导文件）。提醒还是手机发，手表负责看和打卡。",
                "手机一键把计划送到手表喵（走小米运动健康的通道，不用连电脑和导文件）。提醒还是手机发，手表负责看和打卡～",
                "One tap sends plans to the watch through Mi Fitness. No computer or file export needed. Alerts still come from the phone; the watch is for viewing and logging.");
        p("第一次用：先把表端装上", "第一次用：先装手表端喵", "First time: install the watch app first");

        // ---------------- 布局：添加页 ----------------
        e("添加提醒", "Add reminder");
        e("编辑提醒", "Edit reminder");
        e("药品名称", "Medicine name");
        e("例如：维生素C、头孢克肟", "e.g. Vitamin C, Cefixime");
        e("每次用量", "Dose per take");
        e("例如：1片 / 半片 / 10ml / 2粒", "e.g. 1 tablet / half a tablet / 10 ml / 2 pills");
        e("每天分几次吃", "Doses per day");
        e("例如：细菌感染、退烧、降压、胃酸过多", "e.g. infection, fever, blood pressure, stomach acidity");
        e("拍照识别", "Scan label");
        e("从相册选", "Pick from gallery");
        e("拍说明书 / 处方，自动填表", "Photo of the leaflet or prescription, filled in automatically");
        e("拍照或选图后上传识别，药名、用量、每天几次、时间、饭前饭后、用处自动填好，识别完请核对。需要联网。",
                "Upload a photo and the name, dose, doses per day, times, meal relation and purpose are filled in. Needs internet. Please verify after scanning.");
        e("识别设置（在线识别 Key）", "Scan settings (online API keys)");
        e("选完对整个计划生效；想按每个时间不同，就选第一项，再到下面每个时间点单独改。",
                "Applies to the whole plan. To set slots differently, choose the first item and adjust each time slot below.");
        e("点这一行可把整个计划统一成闹钟 / 悬浮弹窗 / 不提醒；下面的每个时间点还能各自单独设。",
                "Tap this row to set every slot to alarm / pop-up / silent; each slot below can still differ.");
        e("正在识别…", "Scanning…");
        e("‹ 返回", "‹ Back");
        e("返回", "Back");
        e("保存并开启提醒", "Save & enable");

        // ---------------- 布局：主页 ----------------
        e("测试一下提醒（5 秒后）", "Test an alert (in 5 s)");
        e("还没有添加药品\n点下面的按钮，添加你的第一个吃药提醒",
                "No medicine yet\nTap the button below to add your first reminder");
        e("暂无记录", "No records yet");
        e("提示：允许本应用后台运行并加入电池优化白名单，否则到点可能弹不出。重启手机后提醒会自动恢复。",
                "Tip: allow background running and add this app to the battery whitelist, or alerts may not appear. Reminders are restored after a reboot.");

        // ---------------- 布局：闹钟页 ----------------
        e("✓  已服用", "✓  Taken");
        e("10 分钟后再提醒", "Snooze 10 min");
        e("每次 1片 · 饭后", "1 tablet · After meal");
        e("药品名", "Medicine");

        // ---------------- 布局：设置页 ----------------
        e("设置", "Settings");
        e("外观", "Appearance");
        e("主题颜色", "Theme colour");
        e("表达风格", "Tone");
        e("老年人模式", "Elder mode");
        e("提醒诊断（不提醒时点这里）", "Reminder check (tap if it stays silent)");
        e("手表端（装到手表上看和打卡）", "Watch app (install on the watch)");
        e("全屏通知（锁屏也能弹窗）", "Full-screen alert (even when locked)");
        e("通知类别（悬浮通知 / 震动）", "Notification category (pop-up / vibration)");
        e("应用详情（权限 / 通知 / 电池）", "App details (permissions / notifications / battery)");
        e("系统设置入口", "System settings");
        e("超时未吃药，隔多久再提醒", "Repeat if a dose is missed");
        e("备份存在应用自己的目录里，不进系统的“下载”夹；覆盖安装不会丢，卸载前先导出。完整路径导出后会提示。",
                "Backups live in the app's own folder, not Downloads. Survives updates; export before uninstalling. The full path is shown after export.");
        e("会删掉全部吃药计划、服药记录，取消已排好的提醒，并把勿扰、提醒方式、主题颜色等设置恢复默认。点按钮时会问你要不要连备份文件一起删。",
                "Deletes all plans, dose logs and scheduled alerts, and resets DND, reminder mode and theme colour. You'll be asked whether to delete backups too.");
        e("说明：通知类别默认已开启悬浮通知和震动；全屏通知只有安卓 14 及以上需要单独允许，系统里没有这个开关的机型可以直接用。",
                "Note: pop-up alerts and vibration are on by default. Full-screen alerts need separate permission only on Android 14+; phones without that switch work as is.");
        e("导出备份（可自选要导出哪些内容）", "Export backup (choose what to include)");
        e("从备份恢复（列出备份夹里的文件）", "Restore from backup");

        // ---------------- 布局：诊断页 ----------------
        e("提醒诊断", "Reminder check");
        e("这里列出“能不能提醒”的每一道关卡。哪一行是 ❌，就点下面的按钮去开。",
                "Every check for whether an alert can fire is listed here. For any ❌, tap the button below to enable it.");
        e("打开应用的通知设置", "Open notification settings");
        e("重新排定全部提醒", "Reschedule all alerts");
        e("事件记录（最新在最上面）", "Event log (newest first)");
        e("清空记录", "Clear log");
        e("10 秒后发一条测试提醒", "Send a test alert in 10 s");

        // ---------------- 布局：手表页 ----------------
        e("手表端", "Watch app");
        e("手机一键把计划送到手表（走小米运动健康的穿戴通道，不用连电脑、不用导文件）。提醒还是手机发，手表负责看和打卡。",
                "One tap sends plans to the watch through Mi Fitness. No computer or file export needed. Alerts still come from the phone; the watch is for viewing and logging.");
        e("1. 点「导出手表安装包」，应用目录里会生成 .bin\n2. 用「表盘自定义工具」选这个 .bin 装到手表\n3. 手表应用列表出现「吃药提醒」\n4. 回到这里点「同步计划到手表」，计划就过去了\n\n以后改了计划，只要再点一次同步就行。",
                "1. Tap “Export watch package”; a .bin file is created in the app folder\n2. Use the watch-face tool to install that .bin on the watch\n3. “Medicine Reminder” appears in the watch app list\n4. Come back here and tap “Sync plans to watch”\n\nAfter later changes, just sync again.");

        // ---------------- 列表项 ----------------
        e("吃过了", "Taken");
        e("删除", "Delete");

        // ---------------- 对话框 ----------------
        e("手表未连接", "Watch not connected");
        e("手机与手表之间的数据通道当前不可用。\n\n请确认：\n1. 手表与手机已完成配对，蓝牙已开启\n2. 打开小米运动健康，设备显示为在线\n3. 手表上已安装「吃药提醒」\n\n随后可在手表页点击「连接到手表」重新建立连接。",
                "The phone–watch data channel is unavailable.\n\nPlease check:\n1. The watch is paired with the phone and Bluetooth is on\n2. Mi Fitness is open and the device shows as online\n3. The “Medicine Reminder” watch app is installed\n\nYou can then tap “Connect to watch” on the watch page to reconnect.");
        e("前往手表页", "Open watch page");
        e("稍后", "Later");
        e("确认过量记录", "Confirm extra dose");
        e("我按错了", "Undo last dose");
        e("确认撤销", "Confirm undo");
        e("关于 · 吃药提醒", "About · Medicine Reminder");
        e("打开作者主页", "Open author's page");
        e("没装 B 站也没浏览器，浏览器打开 b23.tv/ic5GAoB 就行",
                "No bilibili or browser installed; open b23.tv/ic5GAoB in a browser");
        e("勿扰", "Do not disturb");
        e("关闭勿扰", "DND off");
        e("全天勿扰", "DND all day");
        e("只在一段时间内勿扰", "DND during a time window");
        e("勿扰时间段", "DND time window");
        e("开始时间", "Start time");
        e("结束时间（跨零点也可以，比如 22:00 到 07:00）", "End time (may cross midnight, e.g. 22:00 to 07:00)");
        e("已关闭勿扰", "DND turned off");
        e("已开启全天勿扰，勿扰期间所有提醒都不响", "DND is on all day; no alerts will sound");
        e("勿扰时段已保存", "DND window saved");
        e("首次使用引导（", "First-run setup (");
        e("项必开）", " required)");
        e("开完会自动进入下一项，也可以直接跳过。", "After enabling, it moves to the next item. You can also skip.");
        e("跳过引导", "Skip");
        e("开启通知权限", "Enable notifications");
        e("不开通知，到点完全不会有提醒。点「去开启」后在系统弹窗里允许通知。",
                "Without notifications there will be no alerts at all. Tap “Turn on” and allow notifications in the system prompt.");
        e("允许准时提醒", "Allow exact alarms");
        e("允许后提醒才能卡着点响，否则可能延迟几分钟。点「去开启」后在列表里允许本应用。",
                "Only with this permission can alerts fire on time; otherwise they may be delayed. Tap “Turn on” and allow this app in the list.");
        e("打开全屏通知", "Enable full-screen alerts");
        e("安卓 14 起锁屏弹出提醒页需要单独允许全屏通知，不开锁屏时只会出横幅。",
                "On Android 14+ a full-screen alert on the lock screen needs this permission; without it you only get a banner.");
        e("加入电池优化白名单", "Add to battery whitelist");
        e("不加白名单，后台可能被系统清掉，提醒会哑火。",
                "Without the whitelist the app may be killed in the background and alerts will stop.");
        e("必开项都开好了，可以正常用", "Everything required is enabled; you are good to go");
        e("已跳过，之后可以在设置里再开", "Skipped; you can enable it later in Settings");
        e("没找到小米运动健康", "Mi Fitness not found");
        e("打不开：", "Cannot open: ");
        e("创建目录失败", "Could not create folder");
        e("已导出到 吃药提醒备份/手表端", "Exported to Medicine Reminder Backup/Watch");
        e("导出失败：", "Export failed: ");
        e("准备同步…", "Preparing to sync…");
        e("读取失败：", "Read failed: ");
        e("识别结果", "Scan result");
        e("识别设置", "Scan settings");
        e("还没保存哦", "Not saved yet");
        e("这个计划的改动还没保存，离开就丢了。", "This plan has unsaved changes; they will be lost if you leave.");
        e("继续编辑", "Keep editing");
        e("放弃修改", "Discard changes");
        e("请先填写药品名称", "Please enter a medicine name");
        e("这台手机没有可用的相机应用", "No camera app available");
        e("没有可用的相册应用", "No gallery app available");
        e("照片读取失败，换一张清晰的再试", "Could not read the photo; try a sharper one");
        e("图片里没有清晰文字", "No clear text in the image");
        e("识别失败：", "Scan failed: ");
        e("识别服务没返回文字，换张更清楚的图再试", "The service returned no text; try a clearer image");
        e("连不上识别服务器，检查手机网络", "Cannot reach the scan service; check your network");
        e("网络太慢或服务器超时，稍后再试", "Slow network or server timeout; try again later");
        e("还没填识别 Key，点“识别设置”填一个", "No API key yet; tap “Scan settings” to add one");
        e("重新识别", "Scan again");
        e("填百度 OCR 的 API Key + Secret Key（每月免费额度，准确率高）；\n不填百度就默认用 OCR.space 的免费 Key。",
                "Fill in Baidu OCR API Key + Secret Key (free monthly quota, higher accuracy).\nLeave empty to use the free OCR.space key.");
        e("百度 API Key（可不填）", "Baidu API Key (optional)");
        e("百度 Secret Key（可不填）", "Baidu Secret Key (optional)");
        e("主题色已更新", "Theme colour updated");
        e("选一个预设，或者自己输入颜色码（#RRGGBB）", "Pick a preset or type a colour code (#RRGGBB)");
        e("颜色码格式不对，例：#2F6FED", "Wrong colour format, e.g. #2F6FED");
        e("超时没吃药，隔多久再提醒", "Repeat if a dose is missed");
        e("导出哪些内容", "What to export");
        e("一项都没选，没有导出", "Nothing selected; nothing exported");
        e("备份已导出", "Backup exported");
        e("已存到：\n", "Saved to:\n");
        e("还没有备份", "No backups yet");
        e("备份文件夹是空的：\n", "The backup folder is empty:\n");
        e("选一个备份恢复", "Choose a backup to restore");
        e("恢复备份", "Restore backup");
        e("会用这个文件里的内容恢复，文件里没有的项目不动。\n\n", "Restores what this file contains; other items stay unchanged.\n\n");
        e("覆盖现有", "Overwrite");
        e("合并新增", "Merge");
        e("清除所有数据？", "Clear all data?");
        e("再确认一次", "Confirm once more");
        e("同时删掉备份文件夹里的备份文件", "Also delete backup files");
        e("恢复失败：文件格式不对", "Restore failed: bad file format");
        e("已恢复：", "Restored: ");
        e("（主题颜色重启后全部生效）", " (theme applies fully after restart)");
        e("这个文件里没有可恢复的内容", "This file has nothing to restore");
        e("会删掉全部吃药计划和服药记录，取消已排好的提醒，并把勿扰、提醒方式、主题颜色、识别 Key 全部恢复默认。\n\n删掉的计划无法恢复。",
                "Deletes all plans and dose logs, cancels scheduled alerts and resets DND, reminder mode, theme and scan keys.\n\nDeleted plans cannot be recovered.");
        e("5 秒后可以点确认。\n\n备份文件会保留。", "Confirm in 5 seconds.\n\nBackup files will be kept.");
        e("5 秒后可以点确认。\n\n已勾选：备份文件也会被删掉，删了找不回来。",
                "Confirm in 5 seconds.\n\nChecked: backup files will also be deleted and cannot be recovered.");
        e("已清空：计划、记录、设置恢复默认，备份文件也删掉了", "Cleared: plans, logs and settings reset; backups deleted");
        e("已清空：计划、记录、设置恢复默认；备份文件保留", "Cleared: plans, logs and settings reset; backups kept");
        e("当前系统（Android ", "Current system (Android ");
        e("）不需要单独开全屏通知，锁屏提醒由通知类别里的悬浮/横幅控制",
                ") does not need full-screen permission; lock-screen alerts follow the category's pop-up/banner setting");
        e("全屏通知已经允许，锁屏时会直接弹出提醒页", "Full-screen alerts allowed; the reminder page opens on the lock screen");
        e("这台手机的系统里没有“全屏通知”开关，一般可以直接用；", "This phone has no full-screen alert switch; it usually works as is; ");
        e("锁屏能不能弹出，取决于通知类别和系统的锁屏通知设置", "whether it shows on the lock screen depends on the category and system lock-screen settings");
        e("已重新排定", "All alerts rescheduled");
        e("10 秒后测试，等一下", "Test in 10 seconds; please wait");
        e("手动重排了全部提醒", "Manually rescheduled all alerts");
        e("手动测试：10 秒后触发", "Manual test: fires in 10 s");

        // ---------------- 设置页标签 / 选项 ----------------
        e("表达风格：", "Tone: ");
        e("老年人模式：", "Elder mode: ");
        e("主题颜色：", "Theme: ");
        e("超时重复提醒：不重复", "Repeat: none");
        e("超时重复提醒：每 ", "Repeat: every ");
        e("分钟一次", " min");
        e("书面语", "Plain");
        e("喵语", "Cat-speak");
        e("English", "English");
        e("书面语（简洁，适合所有人）", "Plain language (clear, for everyone)");
        e("喵语（带语气词和颜文字）", "Cat-speak (particles and emoticons)");
        e("已开启", "On");
        e("关闭", "Off");
        e("已开启老年人模式：字体放大、界面精简、隐藏手表功能",
                "Elder mode on: larger text, simpler screens, watch features hidden");
        e("已关闭老年人模式", "Elder mode off");

        // ---------------- 提醒方式 / 勿扰 ----------------
        e("闹钟提醒", "Alarm");
        e("悬浮弹窗", "Pop-up");
        e("不提醒", "Silent");
        e("跟随全局", "Follow global");
        e("勿扰：全天开启（当前不提醒）", "DND: all day on (no alerts now)");
        e("（正在勿扰）", " (DND now)");

        // ---------------- 状态 / 计数句式 ----------------
        e("今天已服 ", "Taken today ");
        e(" / ", " / ");
        e(" 次", " times");
        e("次", "times");
        e("已关闭提醒", "Reminders off");
        e("每次 ", "Each ");
        e(" · ", " · ");
        e("服用", "take");
        e("用于：", "For: ");
        e("今天 0/2", "Taken 0/2");
        e("次了，再记就是过量，请确认真的吃了。", " doses today; another would be an overdose. Please confirm you really took it.");
        e("次。\n\n确认刚刚已经吃过一次吗？", " doses today. Did you just take one?");
        e("今天已经吃满 ", "You have already reached ");
        e("「", "“");
        e("」", "”");
        e("每次 ", "Each ");
        e("次，已记录 ", " doses, logged ");
        e("撤销后今天已服 ", "After undo, taken today: ");
        e("」今天还没有服药记录，不用撤销", "” has no dose logged today; nothing to undo");
        e("没找到可撤销的记录", "No record to undo");
        e("已撤销一次，今天「", "Undone. Today “");
        e("已记录一次，今天「", "Logged. Today “");
        e("已服 ", " taken ");
        e("已改成今天吃 ", "Changed to ");
        e(" 次", " doses today");
        e("只改你今天的记录，不会改这个计划要吃几次\n现在：", "Only your log for today changes; the plan stays the same\nNow: ");
        e("只改你今天的记录，不会改这个计划要吃几次\n改成：", "Only your log for today changes; the plan stays the same\nChange to: ");
        e("今天 ", "Today ");
        e("明天 ", "Tomorrow ");
        e("后天 ", "In 2 days ");
        e("开始", "starts");
        e(" · 下次 ", " · Next ");
        e(" · 计划 ", " · plan ");
        e(" · 精确", " · exact");
        e(" · 非精确（未允许准时提醒）", " · inexact (exact alarms not allowed)");
        e(" · 今天共吃 ", " · taken today ");
        e(" · 最近 ", " · last ");
        e("已刷新：计划、提醒、手表连接", "Refreshed: plans, alerts, watch link");
        e("已开启提醒", "Reminders on");
        e("已关闭该药品的提醒", "Reminders off for this medicine");
        e("计划 ", "plan ");
        e("新增", "new");
        e("覆盖为", "overwritten");
        e("记录新增 ", "log added ");
        e("合计", "total");
        e("不可用", "Unavailable");
        e("未知", "Unknown");
        e("未知错误", "Unknown error");
        e("无", "none");
        e("（还没有记录）", "(no records yet)");
        e("已删除的药品", "Deleted medicine");
        e("每天：", "Daily: ");
        e("时间：", "Time: ");
        e("用量：", "Dose: ");
        e("药名：", "Name: ");
        e("用处：", "Purpose: ");
        e("饭前饭后：", "With meals: ");
        e("第 ", "Dose ");
        e("今天 08:00 开始", "Starts today 08:00");
        e("闹钟提醒", "Alarm");
        e("包装", "Packaging");
        e("规格", "Specification");
        e("成分", "Ingredients");
        e("主治", "Indications");
        e("用法", "Usage");
        e("用量", "Dose");
        e("贮藏", "Storage");
        e("有效期", "Expiry");
        e("批号", "Batch");
        e("生产", "Manufacturer");
        e("批准", "Approval");
        e("适用", "Indication");

        // ---------------- 手表通道细节 ----------------
        e("穿戴服务断开", "Wearable service disconnected");
        e("穿戴服务：已连上", "Wearable service: connected");
        e("穿戴服务：断开，状态改为未连接", "Wearable service: disconnected, state set to not connected");
        e("穿戴服务不可用：", "Wearable service unavailable: ");
        e("没找到手表。先打开小米运动健康，确认手表已连接（蓝牙开着）。",
                "Watch not found. Open Mi Fitness and make sure the watch is connected (Bluetooth on).");
        e("小米运动健康里没有已连接的手表", "No connected watch in Mi Fitness");
        e("正在查找手表…", "Looking for the watch…");
        e("请求手表权限（手机会弹授权）…", "Requesting watch permission (your phone will ask)…");
        e("权限已通过，正在打开手表应用…", "Permission granted; opening the watch app…");
        e("手表应用已打开，正在发送计划…", "Watch app opened; sending plans…");
        e("已发到手表。手表上打开「吃药提醒」就能看到新计划。",
                "Sent to the watch. Open “Medicine Reminder” there to see the new plan.");
        e("手表上没装「吃药提醒」应用，先用表盘自定义工具把 .bin 装到手表上。",
                "The “Medicine Reminder” watch app is not installed. Install the .bin with the watch-face tool first.");
        e("删掉手表上的旧版，用 App 里导出的新版 .bin 重装一次。",
                "Remove the old version from the watch and reinstall the new .bin exported from this app.");
        e("手表权限被拒绝或失败：", "Watch permission denied or failed: ");
        e("打开手表应用失败（", "Could not open the watch app (");
        e("权限校验失败：", "Permission check failed: ");
        e("权限接口异常：", "Permission API error: ");
        e("获取设备列表失败：", "Could not get the device list: ");
        e("消息接口异常：", "Message API error: ");
        e("发送失败：", "Send failed: ");
        e("服务器返回 ", "Server returned ");
        e("正在校验权限…", "Checking permission…");
        e("已找到设备：", "Device found: ");
        e("收到手表：打卡 id=", "From watch: dose taken id=");
        e("收到手表：撤销 id=", "From watch: undo id=");
        e("收到手表：全量次数，校正 ", "From watch: full counts, reconciled ");
        e("手表消息解析失败 ", "Could not parse the watch message ");
        e("生成计划数据失败：", "Could not build plan data: ");
        e("手表通道：SDK 不可用 ", "Watch channel: SDK unavailable ");
        e("手表通道：发现设备 ", "Watch channel: device found ");
        e("手表通道：取设备失败 ", "Watch channel: device lookup failed ");
        e("手表通道：授权失败 ", "Watch channel: authorisation failed ");
        e("手表通道：权限校验失败 ", "Watch channel: permission check failed ");
        e("手表通道：没有设备", "Watch channel: no device");
        e("手表通道：监听已注册", "Watch channel: listener registered");
        e("手表通道：监听注册失败 ", "Watch channel: listener registration failed ");
        e("随后可在手表页点击「连接到手表」重新建立连接。", "You can then tap “Connect to watch” on the watch page to reconnect.");
        e("这台手机没有小米穿戴服务", "This phone has no Xiaomi wearable service");

        // ---------------- 备份分区 / 导入结果 ----------------
        e("计划", "Plans");
        e("记录", "Logs");
        e("设置", "Settings");
        e("提醒", "Reminders");
        e("备份", "Backup");
        e("导出", "Export");
        e("恢复", "Restore");
        e("已导出：\n", "Exported:\n");
        e("计划新增 ", "Plans added ");
        e("计划覆盖为 ", "Plans replaced with ");
        e("记录新增 ", "Logs added ");
        e("这个文件里没有可恢复的内容", "This file has nothing to restore");
        e("现在已有 ", "Currently ");
        e("个计划", " plans");
        e("这份备份里有 ", "This backup has ");
        e("个计划，要覆盖掉现在的，还是把备份里的加进来？\n\n",
                " plans. Overwrite the current ones or merge the backup in?\n\n");
        e("覆盖：现在的计划全部被替换。\n合并：现在的计划保留，备份里的追加在后面。",
                "Overwrite: current plans are replaced.\nMerge: current plans stay, backup plans are appended.");
        e("一个纯本地运行的吃药提醒工具，给需要长期按时吃药的人用。\n\n",
                "A fully offline medicine reminder for people who take medicine on a schedule.\n\n");
        e("能做的事：\n", "What it does:\n");
        e("· 添加多个药品计划，每个计划可分 1–6 次，分别设时间、用量、饭前/饭后/随餐\n",
                "· Multiple plans; each plan can have 1–6 doses with its own times, dose and meal relation\n");
        e("· 拍说明书 / 处方 / 药盒，在线识别后自动填好药名、用量、次数、时间、饭前饭后、用处\n",
                "· Photograph a leaflet / prescription / box to auto-fill name, dose, frequency, times, meal relation and purpose\n");
        e("· 每个时间点单独选提醒方式：闹钟（全屏 + 铃声 + 震动）、悬浮弹窗、不提醒\n",
                "· Per-dose alert style: alarm (full screen + sound + vibration), pop-up, or silent\n");
        e("· 到点没吃药会按设置间隔重复提醒（默认 5 分钟，可在设置改）\n",
                "· Repeats at a set interval if a dose is missed (5 min by default, changeable in Settings)\n");
        e("· 首页可一键「吃过了」补记，吃满变「再吃一遍」；「⋯ → 我按错了」可撤销\n",
                "· One-tap “Taken” logging on the home page; “Take again” when the day is complete; undo via “⋯ → Undo last dose”\n");
        e("· 勿扰（全天 / 时间段），勿扰期间所有提醒都不响\n",
                "· Do not disturb (all day or a time window); nothing sounds during DND\n");
        e("· 今日已服记录、数据备份与恢复、主题颜色自定义\n\n",
                "· Today's log, backup/restore and custom theme colour\n\n");
        e("数据全部存在本机，只有拍照识别时会上传那一张图。\n\n",
                "All data stays on this phone; only a photo you scan is uploaded.\n\n");
        e("制作者：萧茂蜜呀\n", "By: 萧茂蜜呀\n");
        e("哔哩哔哩：萧茂蜜呀\n", "bilibili: 萧茂蜜呀\n");
        e("版本 ", "Version ");
        e("（", " (");
        e("）", ")");

        // ---------------- 其余短词 ----------------
        e("暂停该计划", "Pause this plan");
        e("恢复该计划", "Resume this plan");
        e("已暂停该计划的提醒", "Reminders paused for this plan");
        e("已恢复该计划的提醒", "Reminders resumed for this plan");
        e("确认清除", "Confirm clear");
        e("请稍等", "Please wait");
        e("覆盖恢复", "Restore & overwrite");
        e("算了", "Never mind");
        e("去开启", "Turn on");
        e("开启通知", "Enable notifications");
        e("加入白名单", "Add to whitelist");
        e("全部全屏闹钟", "All full-screen alarms");
        e("全部悬浮弹窗", "All pop-ups");
        e("全部不提醒", "All silent");
        e("开启通知", "Enable notifications");
        e("加入白名单", "Add to whitelist");
        e("松手刷新", "Release to refresh");
        e("吃药提醒（已关闭，可删除）", "Medicine Reminder (off, safe to delete)");
        e("到点提醒你按时吃药", "Reminds you when it's time to take medicine");
        e("测试提醒", "Test alert");
        e("1 粒", "1 pill");
        e("1片", "1 tablet");
        e("测试", "Test");
        e("与食物同服", "With food");
        e("进餐时", "During meals");
        e("餐前", "Before meal");
        e("餐后", "After meal");
        e("餐前服用", "Take before meal");
        e("中午", "Noon");
        e("早", "Morning");
        e("晚", "Evening");
        e("药品计划", "Medicine plans");
        e("服药记录", "Dose log");
        e("勿扰设置", "DND settings");
        e("提醒设置（重复间隔、默认方式）", "Reminder settings (repeat interval, default mode)");
        e("无法写入", "Cannot write");
        e("无法读取", "Cannot read");
        e("下拉刷新", "Pull to refresh");
        e("已刷新 ", "Refreshed ");
        e("刷新中…", "Refreshing…");
        e("已导出：\n", "Exported:\n");
        e("准备同步…", "Preparing to sync…");
        e("识别设置（在线识别 Key）", "Scan settings (online API keys)");
        e("选择", "Choose");
        e("完成", "Done");
        e("上一步", "Back");
        e("下一步", "Next");
        e("好", "OK");
        e("继续", "Continue");
        e("测试", "Test");
        e("关闭（不会响）", "Off (silent)");
        e("全屏闹钟", "Full-screen alarm");
        e("不重复", "No repeat");
        e("每 1 分钟", "Every 1 min");
        e("每 3 分钟", "Every 3 min");
        e("每 5 分钟", "Every 5 min");
        e("每 10 分钟", "Every 10 min");
        e("每 15 分钟", "Every 15 min");
        e("每 30 分钟", "Every 30 min");
        e("已保存", "Saved");
        e("已保存 · 第一次提醒：", "Saved · first alert: ");
        e("已保存，当前用：", "Saved; using: ");
        e("已设为：", "Set to: ");
        e("请稍等（", "Please wait (");
        e("确认吃过（", "Confirm taken (");
        e("确认吃过", "Confirm taken");
        e("保存（", "Save (");
        e("已发通知：", "Notification sent: ");
        e("还没吃？第 ", "Not taken yet? #");
        e("次提醒（", " reminder (");
        e("次提醒：", " reminder: ");
        e("次，已服用 ", " doses, taken ");
        e("次，已经吃掉 ", " doses, already taken ");
        e("次，已记录 ", " doses, logged ");
        e("今天要吃药 ", "Doses today: ");
        e("今日计划 ", "Plan today: ");
        e("次，已服用 ", " doses, taken ");
        e("今天已经吃满 ", "Already reached ");
        e("现在共 ", "now ");
        e("，现在共 ", ", now ");
        e("，系统通知开关：", ", system notifications: ");
        e("，跳过重复 ", ", skipping repeat ");
        e("通知权限：", "Notification permission: ");
        e("准时提醒（精确闹钟）：", "Exact alarms: ");
        e("电池优化白名单：", "Battery whitelist: ");
        e("全屏通知：", "Full-screen alerts: ");
        e("类别重要级：", "Channel importance: ");
        e("类别铃声：", "Channel sound: ");
        e("类别震动：", "Channel vibration: ");
        e("ℹ️ 正在使用的通知类别：", "ℹ️ Notification channel in use: ");
        e("❌ 这个类别在系统里不存在（可能被用户删了）\n", "❌ This channel does not exist (maybe deleted)\n");
        e("中/低（可能没有横幅）", "Medium/low (banner may be missing)");
        e("高（可横幅/悬浮）", "High (banner/pop-up)");
        e("未允许，锁屏只会出横幅", "Not allowed; only a banner on the lock screen");
        e("未允许，会被系统延迟甚至不触发", "Not allowed; may be delayed or not fire");
        e("未加入，后台可能被清", "Not whitelisted; may be killed");
        e("未允许", "Not allowed");
        e("已允许", "Allowed");
        e("已加入", "Added");
        e("未配置", "Not configured");
        e("· 还没有开启通知权限，到点不会弹出提醒", "· Notifications are off; nothing will appear on time");
        e("· 未允许“准时提醒”，提醒可能延迟几分钟", "· Exact alarms not allowed; alerts may be a few minutes late");
        e("· 【必开】系统未允许“全屏通知”，锁屏时可能只弹出横幅，请打开全屏通知",
                "· [Required] Full-screen alerts not allowed; only a banner may show on the lock screen");
        e("· 建议把本应用加入电池优化白名单，避免后台被清理",
                "· Add this app to the battery whitelist so it is not killed in the background");
        e("必开项都开好了，可以正常用", "All required items are on; everything works");
        e("当前状态：连接中", "Status: connecting");
        e("排定 ", "Scheduled ");
        e("排定失败(", "Scheduling failed (");
        e("退化为非精确闹钟也失败：", "Inexact alarm fallback also failed: ");
        e("普通闹钟也失败：", "Plain alarm also failed: ");
        e("闹钟触发 medId=", "Alarm fired medId=");
        e("打开了全屏提醒页", "Full-screen alert page opened");
        e("这个时间今天已经过了，从明天开始：", "This time has passed today; starting tomorrow: ");
        e("这个时间今天已经过了，从明天开始：", "This time has passed today; starting tomorrow: ");
        e("找不到这个计划（可能已被删除），跳过", "Plan not found (deleted?); skipped");
        e("这个计划已关闭或时间已改，跳过", "Plan is off or the time changed; skipped");
        e("今天次数已吃满，本次不提醒", "Daily doses complete; no alert this time");
        e("没发通知：没有通知权限", "No alert: notifications are off");
    }

    public static boolean hasHan(String s) {
        if (s == null) return false;
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch >= 0x4E00 && ch <= 0x9FFF) return true;
        }
        return false;
    }

    public static String s(Context c, String zh) {
        if (zh == null) return "";
        int st = Tone.style(c);
        if (zh.indexOf('\\') >= 0) {
            // 布局 XML 里写的是字面 \n，换成真正的换行再查一次
            String alt = zh.replace("\\n", "\n");
            String hit = s(c, alt);
            if (!hit.equals(alt)) return hit;
        }
        if (st == Tone.CAT) {
            String v = CAT.get(zh);
            return v == null ? autoCat(zh) : v;
        }
        if (st == Tone.EN) {
            String v = EN.get(zh);
            return v == null ? zh : v;
        }
        return zh;
    }

    /** 喵语兜底：没有人工译文的条目，自动喵化，保证不残留书面语 */
    private static String autoCat(String zh) {
        if (!hasHan(zh)) return zh;
        String t = zh.replace("请", "").replace("您", "你");
        // 结尾标点前插「喵」
        int end = t.length();
        while (end > 0 && "？！。：;…～~ ".indexOf(t.charAt(end - 1)) >= 0) end--;
        if (end == 0) return zh;
        String head = t.substring(0, end);
        String tail = t.substring(end);
        if (head.endsWith("喵")) return t;
        return head + "喵" + tail;
    }
}
