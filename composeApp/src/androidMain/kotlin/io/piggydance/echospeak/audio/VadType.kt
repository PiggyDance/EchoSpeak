package io.piggydance.echospeak.audio

/**
 * VAD (Voice Activity Detection) 引擎类型
 *
 * - [SILERO]  : DNN/ONNX 模型，帧 512 样本（32ms@16kHz），精度高，需要 Context
 * - [WEBRTC]  : GMM 算法，帧 320 样本（20ms@16kHz），轻量快速，无需 Context
 * - [YAMNET]  : DNN/TFLite 模型，帧 243 样本（~15ms@16kHz），支持 521 种声音分类，需要 Context
 */
enum class VadType(val displayName: String) {
    SILERO("Silero VAD (DNN)"),
    WEBRTC("WebRTC VAD (GMM)"),
    YAMNET("YAMNet VAD (TFLite)"),
}
