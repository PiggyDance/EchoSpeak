# Android 16 KB 页面兼容性

候选版本：`versionCode = 3`、`versionName = 1.1.1`。

## 根因与修复

1.1 版本 AAB 的 6 个 arm64 原生库中，只有 `libtask_audio_jni.so` 的 `PT_LOAD`
对齐值为 `0x1000`。它来自 YAMNet 2.0.10 的传递依赖
`org.tensorflow:tensorflow-lite-task-audio:0.4.4`。DeepFilterNet 的 `libdf.so`、
Silero 的 ONNX Runtime、WebRTC 和 Compose 原生库已为 `0x4000` 对齐。

移除旧 Task Audio 依赖，使用官方 `com.google.ai.edge.litert:litert:1.4.2` Interpreter
运行原 YAMNet 模型。该版本保持 Android API 21+ 支持，符合本 App 的 minSdk 24。
同一模型、521 类标签和 Speech 索引 0 保留；适配器保持原有 15600 样本窗口、
左侧静音填充、滚动样本顺序、PCM16 归一化、最高类别选择和严格大于 0.3 的阈值。
连续语音/静音计数以及原调用中的 duration 对应关系也保持不变。
Silero、WebRTC、YAMNet 和 DeepFilterNet 四项能力继续存在。

移除 `useLegacyPackaging = true` 及其错误绕过说明。压缩 ZIP 内的原生库不能修复
ELF 段对齐；AGP 8.11.2 使用默认的未压缩原生库打包方式。

模型 SHA-256：
`10c95ea3eb9a7bb4cb8bddf6feb023250381008177ac162ce169694d05c317de`。
模型与 [上游 2.0.10](https://github.com/gkonovalov/android-vad/tree/2.0.10/yamnet/src/main/assets)
和原 1.1 AAB 完全相同；`yamnet_labels.txt` 从该模型内嵌的标签文件原样提取。
来源、MIT 通知和 Apache 2.0 许可证随 App 放在 `assets/YAMNET_NOTICE.txt` 和
`assets/YAMNET_LICENSE.txt`。

官方参考：[Android 页面大小要求](https://developer.android.com/guide/practices/page-sizes)、
[LiteRT Android 版本支持](https://developers.google.com/edge/litert/android)。

## 本地验证

使用 JDK 17 或更高版本：

```sh
./gradlew :composeApp:testDebugUnitTest :composeApp:assembleRelease :composeApp:bundleRelease :composeApp:assembleDebugAndroidTest
python3 -m unittest discover -s scripts -p 'test_*.py' -v
python3 scripts/verify_native_page_sizes.py \
  composeApp/build/outputs/apk/release/composeApp-release-unsigned.apk \
  composeApp/build/outputs/bundle/release/composeApp-release.aab
```

校验脚本读取所有 64 位 ELF 的每个 `PT_LOAD` 段，检查对齐、文件偏移与虚拟地址
是否满足 16 KB 同余条件，并检查 APK 中未压缩原生库的 ZIP 数据偏移。
AAB 的 ZIP 偏移不等同于安装 APK 的偏移；发布前还需校验 bundletool 生成的 APK。
可再使用 Android Build Tools：

```sh
zipalign -c -P 16 -v 4 composeApp/build/outputs/apk/release/composeApp-release-unsigned.apk
```

2026-09-30 本地验证：Release APK/AAB 构建成功；4 个 YAMNet 行为测试和原 1 个
common 测试通过；4 个 ELF 校验器测试与 3 个模型资产/张量契约测试通过。
完整 `lintRelease` 通过；补齐了原有缺失的法语、西班牙语、俄语、日语、韩语和繁中
监听状态/模式翻译。两个 Release 成品中全部 6 个 arm64
库的 `PT_LOAD` 对齐均为 `0x4000`，APK ZIP 对齐检查通过；AAB 的 `BundleConfig.pb`
明确设置 `uncompress_native_libraries.enabled = true` 和 `PAGE_ALIGNMENT_16K`。
Release 产物未签名，尚未上传 Google Play。

## 运行验证范围

`NativeAudioSmokeTest` 只用合成静音，检查三种 VAD 的初始化/推理和 DeepFilterNet
模型加载/处理。两个测试在独立的 Android API 33 arm64 模拟器上实际运行通过，
其中 YAMNet 输入/输出 shape 检查也在真实 Interpreter 初始化时通过。
该模拟器的页面大小为 4096；实际录音/降噪效果和 16 KB 页面设备运行仍需验证。
没有在用户 USB 手机安装或启动应用。

Debug App 使用独立包名 `io.piggydance.echospeak.debug`，避免覆盖正式版。
Google Sign-In 的 OAuth 配置可能仅对应正式包名；Debug 登录不在本次修复验证范围。
