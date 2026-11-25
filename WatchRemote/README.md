# 吃药提醒 · 手表端（Vela 快应用）

面向 Redmi Watch 6 / HyperOS 手表的 Xiaomi Vela JS 应用（快应用，rpk 包），
和手机端 APK「吃药提醒」配套使用：手机负责到点提醒，手表负责看下一顿 + 一键打卡。

## 目录结构

```
MedWatch/
├── src/
│   ├── manifest.json           # 应用配置（包名 com.doubao.medwatch，designWidth 432）
│   ├── app.ux                  # 应用入口
│   ├── common/
│   │   └── icon.png            # 192x192 图标
│   └── pages/index/index.ux    # 唯一页面：下一顿卡片 + 计划列表 + 打卡/撤销
└── README.md
```

## 已实现的功能

- **下一顿卡片**：自动算出今天最近的一次待吃（时间已过则显示「该补记」），显示药名、剂量、饭前/饭后、用途
- **一键打卡**：点卡片上的「吃过了」按钮记一次，短振动反馈，Toast 提示 `已记一次 1/3`
- **计划列表**：最多显示前 3 个计划，每行显示药名 / 下次时间 / 今日进度 `x/y`
- **点行打卡**：点列表某一行，直接给该计划记一次
- **过量保护**：某计划今天的次数已经吃完时再打卡，会弹「今天已 N 次 / 过量有风险，5 秒后自动取消」，5 秒倒计时结束自动取消，只有点「确定再吃」才会记录
- **我按错了**：红色描边按钮，撤销最近记录的一次
- **每日自动清零**：按 `YYYY-MM-DD` 存进度，跨天自动重置
- **本地持久化**：`@system.storage`，重启手表不丢

## 能力边界（重要，别抱错期待）

1. **手表端不会自己到点弹提醒**。Vela 的 `setTimeout` 只在应用前台运行时有效，没有后台常驻和系统级定时唤醒接口，快应用关掉就停了。
   → 到点提醒仍然靠**手机 App 发通知**，通知同步到手表（小米运动健康里给「吃药提醒」开通知）。
2. 手表端**只能显示前 3 个计划**，且不能在表上编辑药名/时间（Vela 没有通用键盘输入）。
   → 计划仍在手机端编辑；手表端读的是内置的 3 个示例计划：阿莫西林（08:00/12:30/19:00）、维生素 D（08:00）、降压药（07:30/19:30）。
   改计划：直接改 `src/pages/index/index.ux` 里的 `DEFAULT_PLANS`，或者以后加手机↔手表通信。
3. 未做手机↔手表数据同步。要做的话需要手机 APK 集成 Vela 的 interconnect SDK，并且 **rpk 和手机 APK 用同一张证书签名**，否则通信会被拒（官方 FAQ 明确说了）。

## 怎么装到手表

### 路线 A：用同步器（Debug 安装，不经过商店）

1. 手机装「小米运动健康」，和手表正常配对连接
2. 小米运动健康 → 我的 → 关于 → **Debug** → 第三方应用
3. 点「Click to input package name」→ 随便输个字符 → 选「Install third app」→ 选本地的 `吃药提醒.rpk`
4. 装成功会有 Toast，手表应用列表里出现「吃药提醒」

> 如果「关于」里没有 Debug 入口，说明你这版零售固件没开这个开关。这种情况只能用路线 B 上架商店，没有别的办法绕。

### 路线 B：上架小米手表应用商店

需要企业开发者账号、软著、快应用 ICP 备案、审核（审核规范里明确要软著和备案）。流程见小米澎湃OS开发者平台「快应用接入及上架」与「快应用上架&更新」文档。

## 怎么打包 rpk

用 **AIoT-IDE**（小米官方，支持 Windows / Mac / Ubuntu）：

1. 打开 AIoT-IDE → 导入本项目（指向 `MedWatch` 目录）
2. 选设备为手表（432×514），模拟器可直接预览界面
3. 打包（Build）→ 生成 `.rpk`
4. 真机调试：按上面路线 A 的 Debug 入口装进去

注意：`manifest.json` 里的 `package` 不要和手机 APK 的 `com.doubao.medreminder` 重名（官方要求快应用包名与原生应用不一致）。

## 后续可以加（你确认要再做）

- 手机端推计划到手表（interconnect，双向通信，需同证书）
- 手表端长按 / 双指手势切换计划
- 表盘小组件显示「下一顿」时间

## 命令行打包（不用装 AIoT-IDE）

```bash
cd MedWatch
npm i -D aiot-toolkit@2.0.5
npx aiot build      # 开发包：dist/com.doubao.medwatch.debug.1.0.rpk
npx aiot release    # 发布包：dist/com.doubao.medwatch.release.1.0.rpk（需 sign/private.pem + certificate.pem）
```

已产出：`吃药提醒手表端-v1.0.rpk`（release）、`吃药提醒手表端-v1.0-debug.rpk`（debug）。

## v1.2 更新（手机一键同步）
- 表端新增 `system.interconnect`，主页监听 `tag=plans` 消息，收到后整表替换计划并存进 `mw_plans`。
- 表端安装包改用手机 App 同一个证书签名（SHA-256 `38:07:78:...:53`），否则 SDK 会返回 SignatureVerifyFailed。
- 手机端 App 6.0.0 内置小米穿戴 SDK（XMS 1.4）：连接手表 → 授权 DEVICE_MANAGER → 打开表端 → 发送计划 JSON。
- 装配顺序：表盘自定义工具装 .bin → App 里点「同步计划到手表」。
