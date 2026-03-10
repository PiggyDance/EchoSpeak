# 如何测试多语言国际化

## 快速测试指南

### Android 设备/模拟器

#### 方法1: 更改系统语言

1. 打开 **设置** (Settings)
2. 进入 **系统** > **语言和输入法** (System > Languages & input)
3. 点击 **语言** (Languages)
4. 添加并选择想要测试的语言:
   - 简体中文 (中文(简体))
   - 繁体中文 (中文(繁體))
   - 日语 (日本語)
   - 韩语 (한국어)
   - 法语 (Français)
   - 西班牙语 (Español)
   - 俄语 (Русский)
5. 将目标语言拖到列表顶部
6. 重启 EchoSpeak 应用
7. 验证应用界面文字是否正确显示

#### 方法2: 使用模拟器快速切换

在 Android Studio 的模拟器中:

1. 点击模拟器侧边栏的 **更多** (三个点图标)
2. 选择 **设置** (Settings)
3. 在 **System** 标签下,找到 **Language**
4. 选择想要测试的语言
5. 应用会自动刷新

#### 方法3: 使用 ADB 命令快速切换

```bash
# 切换到简体中文
adb shell "setprop persist.sys.locale zh-CN; setprop ctl.restart zygote"

# 切换到繁体中文
adb shell "setprop persist.sys.locale zh-TW; setprop ctl.restart zygote"

# 切换到日语
adb shell "setprop persist.sys.locale ja-JP; setprop ctl.restart zygote"

# 切换到韩语
adb shell "setprop persist.sys.locale ko-KR; setprop ctl.restart zygote"

# 切换到法语
adb shell "setprop persist.sys.locale fr-FR; setprop ctl.restart zygote"

# 切换到西班牙语
adb shell "setprop persist.sys.locale es-ES; setprop ctl.restart zygote"

# 切换到俄语
adb shell "setprop persist.sys.locale ru-RU; setprop ctl.restart zygote"

# 切换回英语
adb shell "setprop persist.sys.locale en-US; setprop ctl.restart zygote"
```

**注意**: 使用 ADB 命令会重启 zygote 进程,所有应用都会重启。

## 验证清单

测试每种语言时,请检查以下内容:

### ✅ 文本显示
- [ ] 应用名称正确显示
- [ ] 状态文本(录音/播放/待机)正确显示
- [ ] 所有文本清晰可读,无乱码

### ✅ UI 布局
- [ ] 文本没有被截断
- [ ] UI 元素对齐正常
- [ ] 长文本不会导致布局错乱

### ✅ 功能测试
- [ ] 录音功能正常
- [ ] 播放功能正常
- [ ] 状态切换正确

## 当前支持的语言及对照表

| 语言 | Locale 代码 | 资源文件夹 | 示例文本 |
|------|------------|-----------|---------|
| 英语 | en-US | values | Recording... |
| 简体中文 | zh-CN | values-zh-rCN | 正在录音... |
| 繁体中文 | zh-TW | values-zh-rTW | 正在錄音... |
| 日语 | ja-JP | values-ja | 録音中... |
| 韩语 | ko-KR | values-ko | 녹음 중... |
| 法语 | fr-FR | values-fr | Enregistrement... |
| 西班牙语 | es-ES | values-es | Grabando... |
| 俄语 | ru-RU | values-ru | Запись... |

## 常见问题排查

### Q: 切换语言后应用还是显示英文?
**A**: 可能的原因:
1. 应用需要重启才能生效 - 完全关闭应用后重新打开
2. 缓存问题 - 清除应用数据或重新安装
3. 资源文件可能损坏 - 检查对应语言的 strings.xml 文件

### Q: 显示乱码?
**A**: 检查:
1. strings.xml 文件编码是否为 UTF-8
2. XML 文件头部是否声明了 `encoding="utf-8"`
3. Android Studio 的文件编码设置

### Q: 某些语言文本显示不全?
**A**: 调整:
1. 检查文本长度是否超出 UI 容器
2. 考虑使用 `maxLines` 或 `overflow` 属性
3. 对于过长文本,考虑缩写或调整字体大小

### Q: 如何查看当前设备语言?
**A**: 使用 ADB 命令:
```bash
adb shell getprop persist.sys.locale
```

## 自动化测试建议

可以编写 Instrumented Test 来自动化测试多语言:

```kotlin
@Test
fun testChineseLocale() {
    // 设置中文语言环境
    val locale = Locale.SIMPLIFIED_CHINESE
    val config = Configuration(context.resources.configuration)
    config.setLocale(locale)
    
    val localizedContext = context.createConfigurationContext(config)
    val resources = localizedContext.resources
    
    // 验证字符串资源
    assertEquals("正在录音...", resources.getString(R.string.status_recording))
}
```

## 报告问题

如果发现翻译错误或显示问题,请提供:

1. **截图**: 显示问题的截图
2. **设备信息**: 设备型号和 Android 版本
3. **语言设置**: 当前设备语言
4. **预期vs实际**: 预期看到什么,实际看到什么
5. **复现步骤**: 如何触发该问题

## 相关资源

- [Android 国际化官方文档](https://developer.android.com/guide/topics/resources/localization)
- [语言和区域代码列表](https://developer.android.com/reference/java/util/Locale)
- [国际化最佳实践](https://developer.android.com/guide/topics/resources/providing-resources)
