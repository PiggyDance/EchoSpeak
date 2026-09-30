# Android's optimized defaults retain JNI downcalls, including NativeDeepFilterNet.
# Silero/WebRTC and LiteRT provide consumer rules for ONNX, their VAD JNI bridges,
# and reflection-marked LiteRT classes. Do not keep all app/library code.
