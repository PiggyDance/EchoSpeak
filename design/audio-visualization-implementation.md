# 音频可视化实现文档

## 概述

实现了一个由真实音频数据驱动的科幻流光风格音频可视化界面，能够根据"录音"和"播放"两种模式显示不同的颜色效果。

## 架构设计

### 1. 数据流架构

```
AudioRecorder → SpeechDetector → AudioVisualizerManager → App UI
                                          ↑
AudioPlayer ──────────────────────────────┘
```

### 2. 核心组件

#### AudioVisualizerManager (单例)
- **位置**: `composeApp/src/androidMain/kotlin/io/piggydance/echospeak/audio/AudioVisualizerManager.kt`
- **职责**: 
  - 收集音频数据（录音/播放）
  - 计算音频频谱（60个频段）
  - 通过 StateFlow 分发可视化数据
- **数据结构**:
  ```kotlin
  data class VisualizerData(
      val mode: AudioMode,           // IDLE/RECORDING/PLAYING
      val spectrum: List<Float>      // 60个频段，范围 0-1
  )
  ```

#### 频谱计算算法
- 将音频数据分成 60 个频段
- 计算每个频段的 RMS（均方根）能量
- 归一化到 0-1 范围
- 应用平方根缩放使低音量更可见

### 3. UI 组件

#### SciFiAudioVisualizer
- **外圈光环**: 3层旋转霓虹光环，根据模式变色
- **中圈波形**: 60条径向音频条，实时显示频谱数据
- **中心核心**: 发光球体，根据模式变色并脉冲

#### 颜色方案

| 模式 | 光环颜色 | 波形渐变 | 核心颜色 | 状态文字 |
|------|---------|---------|---------|---------|
| 录音 (RECORDING) | 粉红色系 (#FF0080) | 粉红→紫色 | 粉红 | "● 正在录音..." |
| 播放 (PLAYING) | 绿色系 (#00FF80) | 绿色→青色 | 绿色 | "▶ 正在播放..." |
| 空闲 (IDLE) | 青色系 (#00F5FF) | 青色→蓝色 | 蓝色 | "待机中" |

## 实现细节

### 1. 录音模式数据采集

**文件**: `SpeechDetector.kt`

```kotlin
private suspend fun processAudioFrame(frame: ByteArray) {
    // 每帧音频数据都更新到可视化管理器
    AudioVisualizerManager.updateRecordingData(frameCopy)
    // ... VAD 检测逻辑
}
```

- **更新频率**: 每 32ms（512 samples @ 16kHz）
- **数据来源**: 麦克风实时录音
- **特点**: 实时响应用户说话

### 2. 播放模式数据采集

**文件**: `AudioPlayer.kt`

```kotlin
private fun startVisualizerUpdates(data: ByteArray, durationMs: Long) {
    // 每 50ms 提取当前播放位置的音频帧
    val frameData = data.copyOfRange(startIndex, endIndex)
    AudioVisualizerManager.updatePlaybackData(frameData)
}
```

- **更新频率**: 每 50ms
- **数据来源**: 播放缓冲区
- **特点**: 同步播放进度

### 3. 平台适配

#### Android 实现
- **文件**: `AppAndroid.kt`
- 使用 `collectAsStateWithLifecycle()` 订阅音频数据
- 实时更新 UI

#### iOS 实现
- **文件**: `AppIos.kt`
- 占位实现，显示空闲状态
- 未来可扩展

## 动画效果

### 1. 持续动画
- **旋转**: 20秒一圈，线性匀速
- **脉冲**: 1.5秒周期，0.8-1.2倍缩放
- **粒子**: 30秒周期，星空漂浮

### 2. 数据驱动动画
- **波形高度**: 由频谱数据实时控制
- **颜色渐变**: 根据振幅插值
- **光环亮度**: 激活时增强

## 性能优化

1. **频谱计算**: 简化的能量计算，避免 FFT 开销
2. **更新频率**: 50ms 间隔，平衡流畅度和性能
3. **Canvas 绘制**: 纯 Compose Canvas，无位图开销
4. **协程管理**: 使用 Job 管理生命周期，及时取消

## 使用示例

### 录音时
```
用户说话 → AudioRecorder 捕获
         → SpeechDetector 处理
         → AudioVisualizerManager.updateRecordingData()
         → UI 显示粉红色波形动画
```

### 播放时
```
AudioPlayer.play() → 启动可视化更新协程
                  → 每 50ms 提取音频帧
                  → AudioVisualizerManager.updatePlaybackData()
                  → UI 显示绿色波形动画
```

### 空闲时
```
AudioVisualizerManager.reset()
→ UI 显示蓝色静态动画
```

## 未来扩展

1. **更精细的频谱分析**: 使用 FFT 获得真实频率分布
2. **更多可视化模式**: 柱状图、波浪、粒子爆发等
3. **用户自定义**: 颜色主题、动画速度、灵敏度调节
4. **iOS 平台支持**: 实现 iOS 音频数据采集
5. **录音回放同步**: 播放时显示原始录音的频谱

## 技术栈

- **UI**: Jetpack Compose Multiplatform
- **音频**: Android AudioRecord/AudioTrack
- **状态管理**: Kotlin Flow + StateFlow
- **动画**: Compose Animation API
- **协程**: Kotlin Coroutines

## 文件清单

```
composeApp/src/
├── androidMain/kotlin/io/piggydance/echospeak/
│   ├── audio/
│   │   ├── AudioVisualizerManager.kt  (新增)
│   │   ├── AudioRecorder.kt           (修改)
│   │   ├── AudioPlayer.kt             (修改)
│   │   └── SpeechDetector.kt          (修改)
│   └── AppAndroid.kt                  (新增)
├── iosMain/kotlin/io/piggydance/echospeak/
│   └── AppIos.kt                      (新增)
└── commonMain/kotlin/io/piggydance/echospeak/
    └── App.kt                         (重写)
```

## 测试建议

1. **录音测试**: 对着麦克风说话，观察粉红色波形是否跟随声音变化
2. **播放测试**: 播放录音，观察绿色波形是否同步播放
3. **切换测试**: 录音→播放→空闲，观察颜色平滑过渡
4. **性能测试**: 长时间运行，检查内存和 CPU 占用
5. **边界测试**: 静音环境、嘈杂环境、快速切换模式

---

**实现日期**: 2026-02-02  
**版本**: 1.0  
**作者**: AI Assistant
