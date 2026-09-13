# 重邮校园网 Android 客户端 (CQUPT Net App)

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-green?logo=android&logoColor=white" alt="Platform">
  <img src="https://img.shields.io/badge/Language-Kotlin-orange?logo=kotlin&logoColor=white" alt="Language">
  <img src="https://img.shields.io/badge/Framework-Jetpack%20Compose-blue?logo=jetpack-compose&logoColor=white" alt="Framework">
  <a href="https://opensource.org/licenses/MIT"><img src="https://img.shields.io/badge/License-MIT-yellow.svg" alt="License"></a>
</p>

一个使用 **Kotlin** 和 **Jetpack Compose** 编写的现代化重邮（CQUPT）校园网认证客户端。旨在为重邮学子提供更加流畅、美观且便捷的网络认证体验。

---

## ✨ 核心特性

- 🎨 **现代化 UI 设计**：全量采用 Jetpack Compose 构建，支持深色模式与灵动的玻璃拟态（Glassmorphism）视觉风格。
- ⚡ **极速认证**：深度集成 Dr.COM 协议，实现一键登录、快速注销及实时状态同步。
- 🔍 **智能网络诊断**：自动监测网络环境，精准获取 IPv4 地址，实时反馈连接链路状态。
- 🔒 **便捷配置管理**：极简的账号设置界面，加密存储用户凭据，支持主流运营商一键切换。

## 登录保持

主页底部开启“登录保持”后，立即检查登录状态，随后每 60 秒检查一次；未登录时使用设置中的账号、密码和运营商自动登录。无 WiFi 时等待，网络请求失败时下次重试。请求仅通过 WiFi，不读取或判断 WiFi 名称，也不需要定位权限。

开启前请先保存账号密码。后台使用前台服务和常驻通知，可从主页开关或通知关闭；手动登出也会关闭登录保持。开关会保存，重新打开 App 时恢复。持续运行使用唤醒锁，会增加耗电；系统省电、强制停止或厂商后台限制可能延迟或中断检查，无法保证所有设备熄屏后严格每分钟执行。未添加开机自启。

## 🛠️ 技术栈

- **UI 框架**: Jetpack Compose (Material 3)
- **开发语言**: Kotlin
- **网络逻辑**: 基于协程的异步网络请求
- **数据持久化**: SharedPreferences (加密存储建议)

## 🚀 编译与运行

### 环境要求

| 工具 | 最低版本 |
| :--- | :--- |
| Android SDK | 35+ |
| Gradle | 8.9+ |
| JDK | 17+ |

### 快速开始

1. **克隆项目**
   ```bash
   git clone https://github.com/MeTerminator/cqupt-net-app.git
   ```

2. **编译 APK**
   ```bash
   ./gradlew :app:assembleDebug
   ```

3. **安装到设备**
   ```bash
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

## 📂 项目结构

```text
app/src/main/java/top/met6/cquptnet/
├── MainActivity.kt      # UI 核心入口与 Compose 视图组件
├── SDK.kt               # 校园网认证协议核心逻辑 (Dr.COM)
└── SettingsManager.kt   # 用户偏好设置与凭据持久化管理
```

## 🙏 致谢

本项目在开发过程中参考了以下优秀开源项目：

- [CQUPT-Net-SDK](https://github.com/Auto-CQUPT-Plan/CQUPT-Net-SDK) - 提供了核心的重邮校园网认证逻辑参考。

## 📄 开源协议

本项目采用 [MIT License](LICENSE) 协议开源。

Copyright (c) 2026 MeTerminator
