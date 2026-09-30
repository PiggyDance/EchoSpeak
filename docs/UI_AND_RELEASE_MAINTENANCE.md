# 2026-09 UI、生命周期与正式版优化维护

Android 主屏保留初版圆形 60 频段声波、头像、状态颜色和自动录音回放，只增加设置入口。独立设置页有工具栏返回与系统返回，三个既有 VAD 引擎的选择自动保存，默认 Silero；选中当前项与打开/关闭设置都不重启录音。自动降噪没有新增未经验证的参数。使用方法、颜色含义、切换中断当前未完成录音、模型加载与本机处理说明，集中在按需打开的指南。权限页只保留必要的行动文案，并消除重复引导层。新增文本覆盖八种既有语言。

`AudioSessionRunner` 将整个控制器生命周期放在同一 Mutex 中，取消时不可取消地关闭旧会话，再允许新会话进入。共享 runner 跨 Activity 重建、权限 gate 重入串行化，创建、录音停止和 native 资源清理在 IO 上执行。`SpeechDetector` 先停止采集、等待检测/降噪协程退出，再清理缓冲并释放 VAD；播放器等待旧声波任务退出后释放 DeepFilterNet。DeepFilterNet 上游 loader 没有取消 API，`CloseAfterLoad` 确保早于加载完成的 close 仍在完成回调释放 native 实例，且同一 pointer 不重复释放。

## 正式版 R8

[Google Play 技术质量要求](https://support.google.com/googleplay/android-developer/answer/17492799)规定 2027 年 2 月起，Apps 的 DEX 超过 10 MB 时需达到优化、混淆、缩减各 25%。正式版启用 R8 与资源缩减，使用 Android 优化默认规则；debug 保持未缩减，方便手机验收。

遵循 [Android R8 启用说明](https://developer.android.com/topic/performance/app-optimization/enable-app-optimization)和 [JNI keep 规则说明](https://developer.android.com/topic/performance/app-optimization/keep-rule-examples)，未添加覆盖整个 App 的 keep：Silero/WebRTC 自带 VAD/ONNX consumer rules；LiteRT 自带反射标记规则；DeepFilterNet 的 JNI downcall 由 Android 默认 native 规则保留。AGP 8.11.2 / Gradle 8.14.3 支持项目 Kotlin 2.2.20。

启用 R8 前保存的维护候选 AAB 有 4 个 DEX、60,409,580 bytes；R8 后有 1 个 DEX、5,814,560 bytes（减少 90.37%）。这是本地 DEX 体积比较，不能代替 Play Console 对三项优化率的独立评估。最终发行签名由维护者处理。

## 本地验证

```sh
./gradlew :composeApp:testDebugUnitTest :composeApp:assembleDebug \
  :composeApp:assembleRelease :composeApp:bundleRelease :composeApp:lintRelease \
  :composeApp:assembleDebugAndroidTest
python3 scripts/verify_native_page_sizes.py \
  composeApp/build/outputs/apk/debug/composeApp-debug.apk \
  composeApp/build/outputs/apk/release/composeApp-release-unsigned.apk \
  composeApp/build/outputs/bundle/release/composeApp-release.aab
```

11 项单元测试通过，含快速取消/切换的先关后开、取消等待项不建 native 实例、失败启动关闭一次、异步模型晚加载的幂等清理、既有 YAMNet 窗口/标签/时长逻辑。`lintRelease` 为 0 error、72 既有 warning。三个产物的全部 6 个 arm64 ELF、APK ZIP 对齐与 AAB PAGE_ALIGNMENT_16K 静态检查通过。API 33 模拟器仅代表 4 KB 页运行，不能代替 16 KB 系统真机和真实录音音质验收。

## 明确后续事项

连续不断的人声目前仍可令 `SpeechDetector` 的 ByteArrayOutputStream 无限增长，且 reset 保留最高容量。后续应给单段录音设置帧完整的字节上限（建议先评估 5 分钟），到达上限正常回放后重建缓冲，并对上限前一帧/达到上限/尾帧和反复长段录音做边界与内存回归。本轮没有改变语音阈值、分段规则或 DSP。

登录入口仍关闭；不新增账户功能。若未来恢复登录，需另行对照 Google Play 2027 年 4 月的设备迁移登录恢复要求。
