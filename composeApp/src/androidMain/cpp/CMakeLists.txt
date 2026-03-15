cmake_minimum_required(VERSION 3.22.1)
project("echospeak_rnnoise")

# ── RNNoise 核心库 ────────────────────────────────────────────────────────────
# 只编译库核心文件，排除带 main() 的工具文件
# rnnoise_data.c：由 download_model.sh 提供，包含神经网络权重和模型初始化
# parse_lpcnet_weights.c：提供 parse_weights()/rnn_parse_weights 实现
add_library(
    rnnoise
    STATIC
    rnnoise/src/celt_lpc.c
    rnnoise/src/denoise.c
    rnnoise/src/kiss_fft.c
    rnnoise/src/nnet.c
    rnnoise/src/nnet_default.c
    rnnoise/src/parse_lpcnet_weights.c
    rnnoise/src/pitch.c
    rnnoise/src/rnn.c
    rnnoise/src/rnnoise_data.c
    rnnoise/src/rnnoise_tables.c
)

target_include_directories(rnnoise PUBLIC
    rnnoise/include
    rnnoise/src
)

# RNNoise 编译选项
# 注意：arch.h（来自 libopus）明确禁止 -ffast-math（除非定义 FLOAT_APPROX），
# 否则在 NaN 输入时可能崩溃。使用 -O3 已足够优化。
target_compile_options(rnnoise PRIVATE
    -O3
    -DRNNOISE_BUILD
)

# ── JNI 胶水层（暴露给 Kotlin 的 native 方法）────────────────────────────────
add_library(
    echospeak_rnnoise
    SHARED
    rnnoise_jni.c
)

target_include_directories(echospeak_rnnoise PRIVATE
    rnnoise/include
)

# 链接 rnnoise 静态库 + Android log 库
find_library(log-lib log)

target_link_libraries(
    echospeak_rnnoise
    rnnoise
    ${log-lib}
)
