# HyperOS3 EU Localization

为 xiaomi.eu HyperOS3 ROM 恢复原版 HyperOS 功能的 Magisk/KernelSU 模块。

Reintegrate original HyperOS features for xiaomi.eu HyperOS3 ROM.

## 功能 / Features

- 恢复国行版本地化功能（小爱同学、负一屏、短信、传送门、黄页等）
- 小米钱包、公交卡、MiPay 支持
- 日历、天气、音乐、录音机
- 字体、主题管理器、通知过滤
- 国行相册 v5.0（Flutter 架构，宠物相册 / 春节特效 / 证件照）
- 国行相册编辑器 v4.2.5
- MiPush 推送切换至中国区服务器
- Xposed 模块提供签名绕过和系统增强

## 安装要求 / Requirements

- **设备**：Xiaomi 17 Ultra (nezha)
- **ROM**：xiaomi.eu HyperOS 3（仅在该环境下测试通过）
- **Root**：Magisk / KernelSU / APatch
- **Xposed**：LSPosed / LSPatch（用于 Toolbox 模块功能）

## 安装 / Installation

### Magisk 模块

1. 下载 `HyperOS3EULocalization-*.zip`
2. 在 Magisk / KernelSU 管理器中刷入
3. 按音量键提示选择需要的功能（10 秒无操作默认选是）
4. 重启设备

### Toolbox APK（Xposed 签名绕过模块）

1. 安装 `HyperOS3_Toolbox_v*.apk`
2. 在 LSPosed 中启用该模块
3. 勾选需要作用的应用范围（相册、联系人等）
4. 重启设备

## 参考与致谢 / Credits

本项目基于以下开源工作构建，在此致以诚挚感谢：

- [**MinaMichita / MiuiEULocalizationToolsBox**](https://github.com/MinaMichita/MiuiEULocalizationToolsBox)
  原版 MIUI EU 本地化工具箱，本项目最初的灵感与基础来源。

- [**LSHFGJ / HyperOS3EULocalization**](https://github.com/LSHFGJ/HyperOS3EULocalization)
  HyperOS 3 EU 本地化模块的核心实现，本项目直接 fork 自此仓库并在其基础上进行扩展。

感谢两位作者的开创性工作！

## 目录结构 / Structure

```
HyperOS3EULocalization/
├── META-INF/           # Magisk 安装脚本
├── system/             # 系统覆盖文件（APK 通过 Git LFS 存储）
├── toolbox/            # Xposed 模块源码
├── tools/              # 安装辅助脚本
├── lang/               # 多语言配置
├── customize.sh        # 安装脚本（交互式功能选择）
├── service.sh          # 开机启动脚本
├── module.prop         # 模块信息
└── README.md
```

## 许可证 / License

本项目基于 Apache License 2.0 开源。
