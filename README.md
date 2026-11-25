# 手表遥控 WatchRemote

在小米手表上滑动屏幕，直接操控手机界面。**独立新 App**，跟「吃药提醒」完全分开。

## 组成

| 端 | 包名 | 说明 |
|---|---|---|
| 手机端 | `com.doubao.watchremote` | 常驻通道 + 无障碍手势执行 + 手表端安装 |
| 手表端 | `com.doubao.watchremote` | 快应用遥控面板，负责发指令 |

## 手势

| 手表操作 | 手机动作 |
|---|---|
| 上滑 | 内容下翻 |
| 下滑 | 内容上翻 |
| 左滑 | 返回上一页 |
| 右滑 | 翻到下一页 |
| 点一下触控板 | 回到页面顶部 |

底部还有四个大按钮：下翻 / 上翻 / 返回 / 下一页，懒得滑就点。

## 安装步骤

1. 手机装 `releases/android/watchremote-1.0.apk`。
2. 小米运动健康里先把手表和手机连上。
3. 打开「手表遥控」→ 点「连接手表」。
4. 点「开启无障碍服务」，在系统设置列表里打开「手表遥控」（必须开，否则手势发不出去）。
5. 点「安装手表端」，把 `releases/watch/watchremote-1.0.rpk` 装到手表。
6. 手表上打开「手表遥控」，滑动即生效。

## 权限说明

- 无障碍权限只用来 `dispatchGesture`（滑动）和 `GLOBAL_ACTION_BACK`（返回），**不读取屏幕内容**。
- 通讯依赖小米穿戴 SDK（`libs/xms.jar`），走小米运动健康的已连接节点。

## 构建

手机端：

```bash
cd WatchRemotePhone && ./build.sh   # 需要 Android SDK build-tools 34 + JDK 17
```

手表端：

```bash
cd WatchRemote && npx aiot release --enable-jsc
```

产物：`WatchRemotePhone/WatchRemote-1.0.apk`、`WatchRemote/dist/com.doubao.watchremote.release.1.0.rpk`

## 目录

```
WatchRemotePhone/   手机端（Java + 原生 View）
WatchRemote/        手表端（快应用 .ux）
docs/               GitHub Pages 下载页
releases/           安装包
version.json        版本信息
```
