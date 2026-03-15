package io.piggydance.echospeak.audio

import kotlin.math.abs
import kotlin.math.sqrt

/**
 * 播放降噪处理器
 *
 * 在将录音数据送入 AudioTrack 播放前进行处理，改善听感。
 * 处理链（离线，一次性处理完整 PCM 数据）：
 *
 *   原始 PCM
 *     → Step 1: 去除 DC 偏置（避免低频嗡嗡声）
 *     → Step 2: 高通滤波（截止 ≈ 80 Hz，去除空调/震动等低频噪声）
 *     → Step 3: 软噪声门（平滑衰减近静音段的背景嘶嘶声）
 *     → 处理后 PCM → AudioTrack
 *
 * 输入格式：16-bit PCM，小端序，Mono，16000 Hz
 */
class PlaybackProcessor {

    // ── 高通滤波器（IIR 一阶 Butterworth）──────────────────────────────────────
    // 推导：alpha = RC / (RC + dt)
    //       RC = 1 / (2π * fc)，fc = 80 Hz
    //       dt = 1 / 16000 = 62.5 μs
    //       alpha ≈ 0.9969
    private val hpAlpha = 0.9969f
    private var hpPrevInput  = 0f
    private var hpPrevOutput = 0f

    // ── 软噪声门参数 ──────────────────────────────────────────────────────────
    // 每次评估一帧（512 样本 ≈ 32 ms）的 RMS，低于阈值则平滑衰减
    private val noiseGateThreshold = 250f   // RMS 触发阈值（16-bit 最大 32767）
    private val noiseGateMinGain   = 0.15f  // 深静音时的最低增益（不完全置零，避免咔嚓声）
    private val noiseGateFrameSize = 512    // 帧大小（样本数）

    /**
     * 对一段完整的 PCM 数据（16-bit 小端序）做降噪处理，返回处理后的 PCM。
     */
    fun process(data: ByteArray): ByteArray {
        val sampleCount = data.size / 2
        if (sampleCount == 0) return data

        val samples = FloatArray(sampleCount)

        // ── Step 1：解码 PCM → Float，并去除 DC 偏置 ────────────────────────
        var dcSum = 0f
        for (i in 0 until sampleCount) {
            val raw = (data[i * 2].toInt() and 0xFF) or (data[i * 2 + 1].toInt() shl 8)
            samples[i] = if (raw > 32767) raw - 65536f else raw.toFloat()
            dcSum += samples[i]
        }
        val dcOffset = dcSum / sampleCount
        for (i in samples.indices) {
            samples[i] -= dcOffset
        }

        // ── Step 2：高通滤波，去除低频环境噪声 ──────────────────────────────
        // y[n] = alpha * (y[n-1] + x[n] - x[n-1])
        for (i in samples.indices) {
            val x = samples[i]
            val y = hpAlpha * (hpPrevOutput + x - hpPrevInput)
            hpPrevInput  = x
            hpPrevOutput = y
            samples[i]   = y
        }

        // ── Step 3：软噪声门，衰减背景嘶嘶声 ────────────────────────────────
        // 以 noiseGateFrameSize 样本为一帧，计算帧 RMS，
        // 在帧内从上一帧增益线性过渡到当前帧增益，保证边界无咔嚓声。
        val frameCount = sampleCount / noiseGateFrameSize
        var prevGain = 1f

        for (f in 0 until frameCount) {
            val start = f * noiseGateFrameSize
            val end   = start + noiseGateFrameSize

            // 计算当前帧 RMS
            var rmsSum = 0f
            for (i in start until end) rmsSum += samples[i] * samples[i]
            val rms = sqrt(rmsSum / noiseGateFrameSize)

            // 低于阈值时线性插值到 minGain（软衰减，不是硬切）
            val targetGain = if (rms < noiseGateThreshold) {
                noiseGateMinGain + (1f - noiseGateMinGain) * (rms / noiseGateThreshold)
            } else {
                1f
            }

            // 帧内从 prevGain 线性过渡到 targetGain，消除帧间增益跳变引起的咔嚓声
            val frameLen = end - start
            for (i in start until end) {
                val t    = (i - start).toFloat() / frameLen
                val gain = prevGain + (targetGain - prevGain) * t
                samples[i] *= gain
            }

            prevGain = targetGain
        }

        // ── Step 4：峰值归一化 + 增益提升 ───────────────────────────────────────
        // 将信号归一化到目标响度（目标峰值 = 满量程的 85%），保证播放声音足够响亮。
        // 增益倍数上限 8x，防止对噪声门已衰减的极安静片段过度放大。
        val targetPeak = 32767f * 0.85f   // 目标峰值
        val currentPeak = samples.maxOfOrNull { abs(it) } ?: 0f
        if (currentPeak > 100f) {          // 忽略近乎无声的片段
            val gainFactor = (targetPeak / currentPeak).coerceAtMost(8f)
            for (i in samples.indices) {
                samples[i] *= gainFactor
            }
        }

        // ── Step 5：编码回 PCM 16-bit 小端序 ────────────────────────────────
        val result = ByteArray(data.size)
        for (i in 0 until sampleCount) {
            val v = samples[i].toInt().coerceIn(-32768, 32767)
            result[i * 2]     = (v and 0xFF).toByte()
            result[i * 2 + 1] = ((v shr 8) and 0xFF).toByte()
        }
        return result
    }

    /**
     * 重置滤波器状态。每次新的播放开始前调用，防止上次滤波器尾迹影响本次播放。
     */
    fun reset() {
        hpPrevInput  = 0f
        hpPrevOutput = 0f
    }
}
