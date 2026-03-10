# EchoSpeak 🎙️

一款具有科幻风格音频可视化效果的跨平台语音回声应用。

## ✨ 特性

- 🎨 **科幻视觉效果**: 动态粒子背景和实时音频可视化
- 🎙️ **实时录音**: 支持高质量音频录制
- ▶️ **音频播放**: 播放录制的音频并显示动态波形
- 🌍 **多语言支持**: 支持 8 种语言
- 📱 **跨平台**: 基于 Kotlin Multiplatform,支持 Android 和 iOS

## 🌐 支持的语言

- 🇺🇸 English (英语)
- 🇨🇳 简体中文
- 🇹🇼 繁體中文 (繁体中文)
- 🇯🇵 日本語 (日语)
- 🇰🇷 한국어 (韩语)
- 🇫🇷 Français (法语)
- 🇪🇸 Español (西班牙语)
- 🇷🇺 Русский (俄语)

应用会自动根据设备系统语言显示对应的界面文字。

## 📁 项目结构

* [/composeApp](./composeApp/src) - 跨平台共享代码
  - [commonMain](./composeApp/src/commonMain/kotlin) - 所有平台通用代码
  - [androidMain](./composeApp/src/androidMain/kotlin) - Android 特定代码
  - [iosMain](./composeApp/src/iosMain/kotlin) - iOS 特定代码

* [/iosApp](./iosApp/iosApp) - iOS 应用入口

* [/docs](./docs) - 文档目录
  - [INTERNATIONALIZATION.md](./docs/INTERNATIONALIZATION.md) - 国际化实现详解
  - [HOW_TO_TEST_I18N.md](./docs/HOW_TO_TEST_I18N.md) - 多语言测试指南

## 🚀 构建和运行

### Android 应用

在 IDE 中使用运行配置,或在终端中构建:

**macOS/Linux:**
```shell
./gradlew :composeApp:assembleDebug
```

**Windows:**
```shell
.\gradlew.bat :composeApp:assembleDebug
```

### iOS 应用

在 IDE 中使用运行配置,或在 Xcode 中打开 [/iosApp](./iosApp) 目录并运行。

## 🧪 测试多语言

查看 [多语言测试指南](./docs/HOW_TO_TEST_I18N.md) 了解如何测试不同语言。

快速切换语言(Android):
```bash
# 简体中文
adb shell "setprop persist.sys.locale zh-CN; setprop ctl.restart zygote"

# 英语
adb shell "setprop persist.sys.locale en-US; setprop ctl.restart zygote"
```

## 📖 文档

- [国际化实现详解](./docs/INTERNATIONALIZATION.md) - 了解多语言实现架构
- [多语言测试指南](./docs/HOW_TO_TEST_I18N.md) - 如何测试各种语言
- [设计文档](./design/) - UI 和功能设计说明

## 🛠️ 技术栈

- **Kotlin Multiplatform** - 跨平台开发
- **Jetpack Compose** - 现代化 UI 框架
- **Koin** - 依赖注入
- **Kotlin Coroutines** - 异步处理
- **Android Audio API** - 音频录制和播放

## 📝 许可证

本项目仅供学习和参考使用。

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)