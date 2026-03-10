# EchoSpeak 国际化 (i18n) 说明文档

## 概述
EchoSpeak 应用已经支持多语言国际化,目前支持以下语言:

- 🇺🇸 **英语** (English) - 默认语言
- 🇨🇳 **简体中文** (Simplified Chinese)
- 🇹🇼 **繁体中文** (Traditional Chinese)
- 🇯🇵 **日语** (Japanese)
- 🇰🇷 **韩语** (Korean)
- 🇫🇷 **法语** (French)
- 🇪🇸 **西班牙语** (Spanish)
- 🇷🇺 **俄语** (Russian)

## 资源文件结构

```
composeApp/src/androidMain/res/
├── values/                    # 默认 (英语)
│   └── strings.xml
├── values-zh-rCN/            # 简体中文
│   └── strings.xml
├── values-zh-rTW/            # 繁体中文
│   └── strings.xml
├── values-ja/                # 日语
│   └── strings.xml
├── values-ko/                # 韩语
│   └── strings.xml
├── values-fr/                # 法语
│   └── strings.xml
├── values-es/                # 西班牙语
│   └── strings.xml
└── values-ru/                # 俄语
    └── strings.xml
```

## 字符串资源列表

当前应用中定义的字符串资源:

| 资源 ID | 英语 | 简体中文 | 用途 |
|---------|------|----------|------|
| `app_name` | EchoSpeak | 回声说 | 应用名称 |
| `status_recording` | ● Recording... | ● 正在录音... | 录音状态 |
| `status_playing` | ▶ Playing... | ▶ 正在播放... | 播放状态 |
| `status_idle` | Standby | 待机中 | 待机状态 |
| `mode_idle` | Idle | 空闲 | 空闲模式 |
| `mode_recording` | Recording | 录音中 | 录音模式 |
| `mode_playing` | Playing | 播放中 | 播放模式 |

## 架构设计

### 1. 多平台抽象层

```kotlin
// commonMain - 接口定义
interface StringResources {
    val appName: String
    val statusRecording: String
    val statusPlaying: String
    // ...
}

expect fun getStringResources(): StringResources
```

### 2. Android 平台实现

```kotlin
// androidMain - Android 实现
class AndroidStringResources(private val context: Context) : StringResources {
    override val appName: String
        get() = context.getString(R.string.app_name)
    // ...
}
```

### 3. iOS 平台实现

```kotlin
// iosMain - iOS 实现
class IosStringResources : StringResources {
    override val appName: String = "EchoSpeak"
    // 可以扩展为从 .strings 文件读取
}
```

## 使用方法

在 Composable 函数中使用字符串资源:

```kotlin
@Composable
fun MyScreen() {
    val stringRes = getStringResources()
    
    Text(text = stringRes.appName)
    Text(text = stringRes.statusRecording)
}
```

## 添加新语言

### 步骤 1: 创建资源文件夹

```bash
mkdir -p composeApp/src/androidMain/res/values-{语言代码}
```

语言代码参考:
- `de` - 德语
- `it` - 意大利语
- `pt` - 葡萄牙语
- `ar` - 阿拉伯语
- `hi` - 印地语

### 步骤 2: 创建 strings.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">应用名称</string>
    <string name="status_recording">录音状态文本</string>
    <!-- 添加所有字符串资源的翻译 -->
</resources>
```

### 步骤 3: 测试

1. 在 Android 设备/模拟器上更改系统语言
2. 重启应用
3. 验证文本是否正确显示

## 添加新字符串

### 步骤 1: 更新所有 strings.xml 文件

在所有语言的 `strings.xml` 中添加新的字符串资源:

```xml
<string name="new_string_id">翻译文本</string>
```

### 步骤 2: 更新 StringResources 接口

```kotlin
interface StringResources {
    // 现有属性...
    val newString: String
}
```

### 步骤 3: 更新平台实现

Android:
```kotlin
class AndroidStringResources(private val context: Context) : StringResources {
    // 现有属性...
    override val newString: String
        get() = context.getString(R.string.new_string_id)
}
```

iOS:
```kotlin
class IosStringResources : StringResources {
    // 现有属性...
    override val newString: String = "Default Text"
}
```

## 最佳实践

1. **始终使用字符串资源**: 不要在代码中硬编码文本
2. **保持同步**: 添加新字符串时,同时更新所有语言的资源文件
3. **使用描述性 ID**: 字符串 ID 应该清晰表达其用途
4. **考虑文本长度**: 不同语言的文本长度可能差异很大,UI 设计时要考虑
5. **测试所有语言**: 确保在不同语言下 UI 布局正常

## 语言切换逻辑

应用会自动根据设备系统语言显示对应的文本:

1. Android 系统会根据设备语言设置自动选择对应的 `values-xx` 资源
2. 如果设备语言不在支持列表中,会使用默认的 `values` (英语)
3. 用户无需在应用内手动切换语言

## 常见问题

### Q: 如何测试不同语言?
A: 在 Android 设备的系统设置中更改语言,或在模拟器中配置语言。

### Q: 为什么某些语言显示乱码?
A: 确保 `strings.xml` 文件编码为 UTF-8,并且在文件开头声明了 `encoding="utf-8"`。

### Q: 如何支持从右到左 (RTL) 的语言?
A: 在 `AndroidManifest.xml` 中添加 `android:supportsRtl="true"`,并使用 `start`/`end` 而非 `left`/`right` 布局属性。

### Q: iOS 如何实现完整的国际化?
A: 当前 iOS 使用简单的硬编码英文,未来可以扩展为使用 `.strings` 文件或 Kotlin Multiplatform Resources。

## 相关文件

- `composeApp/src/commonMain/kotlin/io/piggydance/echospeak/StringResources.kt` - 字符串资源接口
- `composeApp/src/androidMain/kotlin/io/piggydance/echospeak/StringResourcesAndroid.kt` - Android 实现
- `composeApp/src/iosMain/kotlin/io/piggydance/echospeak/StringResourcesIos.kt` - iOS 实现
- `composeApp/src/commonMain/kotlin/io/piggydance/echospeak/App.kt` - 使用字符串资源的示例

## 更新日志

- **2026-03-10**: 初始国际化支持,添加 8 种语言
