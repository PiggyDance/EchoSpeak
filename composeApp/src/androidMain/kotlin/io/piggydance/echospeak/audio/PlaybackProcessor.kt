package io.piggydance.echospeak.audio

import kotlin.math.*

/**
 * 播放降噪处理器 — STFT 频域谱减法（Spectral Subtraction）
 *
 * 处理链：
 *   原始 PCM + VAD 帧标签
 *     → Step 1: DC 去偏 + 高通滤波（去低频环境噪声）
 *     → Step 2: 从 VAD 静音帧估算噪声功率谱（noise power spectrum）
 *     → Step 3: STFT 分帧 → 每个频率箱独立 Wiener 增益 → IFFT 重合成（overlap-add）
 *                人声帧：谱减法去除噪声分量，保留语音频谱形状
 *                静音帧：深度衰减（gain floor = 0.05）
 *     → Step 4: 峰值归一化（基于人声帧）
 *
 * 与时域方法的本质区别：
 *   时域方法：每帧只有一个增益系数（无法区分不同频率的信噪比）
 *   STFT方法：每帧有 257 个增益系数（每个频率箱单独处理，精准保留语音频谱）
 *
 * 输入格式：16-bit PCM，小端序，Mono，16000 Hz
 */
class PlaybackProcessor {

    companion object {
        private const val FFT_SIZE  = 512       // FFT 帧大小（样本数 = 32ms @ 16kHz）
        private const val HOP_SIZE  = 128       // 帧移（样本数，75% 重叠）
        private const val NUM_BINS  = FFT_SIZE / 2 + 1  // 单边频率箱数量（257）
    }

    // ── 高通滤波器（IIR 一阶 Butterworth，fc ≈ 80 Hz）───────────────────────
    private val hpAlpha = 0.9969f
    private var hpPrevInput  = 0f
    private var hpPrevOutput = 0f

    // ── Hann 窗（用于 STFT 分帧和 OLA 重合成）───────────────────────────────
    // 75% 重叠时，Hann 窗的归一化因子为 1.5，这里预先除掉，让 OLA 自动归一化
    private val hannWindow = FloatArray(FFT_SIZE) { i ->
        (0.5f * (1.0 - cos(2.0 * PI * i / FFT_SIZE))).toFloat()
    }

    // ── Wiener 增益参数 ───────────────────────────────────────────────────────
    // 过减系数：降低到 1.0 减少过度衰减，避免"空旷感"（musical noise）
    // 取值范围 0.8~2.0；太大 → 空旷/金属感，太小 → 降噪不足
    private val overSubtraction  = 1.2f
    // 人声帧增益下限：提高到 0.35，填平频谱空洞底部，保留声音厚度
    // 取值范围 0.15~0.5；太小 → 空旷感，太大 → 底噪残留
    private val speechGainFloor  = 0.3f
    // 静音帧增益下限：保持较低，静音段仍然压制
    private val silenceGainFloor = 0.05f
    // 增益频谱平滑窗口：对相邻频率箱的增益做移动平均，消除孤立空洞
    // 取值范围 1（不平滑）~ 11；越大越平滑，但会损失高频细节
    private val gainSmoothRadius = 4

    /**
     * 对完整语音片段做频域谱减法降噪。
     * 使用 VAD 标签精确区分人声帧和静音帧，避免误判。
     */
    fun process(segment: SpeechSegment): ByteArray {
        val data = segment.pcm
        val sampleCount = data.size / 2
        if (sampleCount < FFT_SIZE) return data

        val vadLabels      = segment.vadFrameLabels
        val vadFrameSamples = segment.vadFrameSizeSamples

        // ── Step 1a：解码 PCM → Float + DC 去偏 ─────────────────────────────
        val samples = FloatArray(sampleCount)
        var dcSum = 0.0
        for (i in 0 until sampleCount) {
            val raw = (data[i * 2].toInt() and 0xFF) or (data[i * 2 + 1].toInt() shl 8)
            samples[i] = if (raw > 32767) raw - 65536f else raw.toFloat()
            dcSum += samples[i]
        }
        val dc = (dcSum / sampleCount).toFloat()
        for (i in samples.indices) samples[i] -= dc

        // ── Step 1b：高通滤波，去除低频环境噪声 ─────────────────────────────
        for (i in samples.indices) {
            val x = samples[i]
            val y = hpAlpha * (hpPrevOutput + x - hpPrevInput)
            hpPrevInput = x; hpPrevOutput = y
            samples[i] = y
        }

        // ── Step 2：从 VAD 静音帧估算噪声功率谱 ─────────────────────────────
        //
        // 对每个静音帧做 FFT，累加各频率箱的功率，最后取平均。
        // 这比用 RMS 精确得多——可以捕捉到噪声在频域的形状
        // （比如空调噪声集中在低频，风扇噪声是宽带等）。
        val noisePower = FloatArray(NUM_BINS)
        var silenceFrameCount = 0

        for (f in vadLabels.indices) {
            if (!vadLabels[f]) {
                // 从 VAD 帧头开始取 FFT 帧，避免居中偏移导致 start < 0
                val start = f * vadFrameSamples
                if (start + FFT_SIZE <= sampleCount) {
                    val re = FloatArray(FFT_SIZE)
                    val im = FloatArray(FFT_SIZE)
                    for (i in 0 until FFT_SIZE) re[i] = samples[start + i] * hannWindow[i]
                    fft(re, im, inverse = false)
                    for (k in 0 until NUM_BINS) {
                        noisePower[k] += re[k] * re[k] + im[k] * im[k]
                    }
                    silenceFrameCount++
                }
            }
        }

        val hasNoiseEstimate = silenceFrameCount > 0
        if (hasNoiseEstimate) {
            for (k in 0 until NUM_BINS) noisePower[k] /= silenceFrameCount
        }

        // ── Step 3：STFT 谱减法 + 重合成（Overlap-Add）──────────────────────
        val output    = FloatArray(sampleCount)
        val winSum    = FloatArray(sampleCount)

        var pos = 0
        while (pos + FFT_SIZE <= sampleCount) {
            // 确定本 FFT 帧是否为人声（查询 FFT 帧起始位置所在的 VAD 帧）
            // 用 pos 而非中心，避免短帧时跨 VAD 帧边界误判
            val vadIdx   = (pos / vadFrameSamples).coerceAtMost(vadLabels.size - 1)
            val isSpeech = vadLabels.getOrElse(vadIdx) { false }

            // 加窗 + FFT
            val re = FloatArray(FFT_SIZE)
            val im = FloatArray(FFT_SIZE)
            for (i in 0 until FFT_SIZE) re[i] = samples[pos + i] * hannWindow[i]
            fft(re, im, inverse = false)

            // ── 每个频率箱独立 Wiener 增益 ──────────────────────────────────
            //
            // 当前帧功率：P(k) = |X(k)|² = re[k]² + im[k]²
            // 噪声功率：  N(k) = noisePower[k]（来自静音帧估算）
            //
            // Wiener 增益（最大似然估计）：
            //   G(k) = max(floor, P(k) - β*N(k)) / P(k)
            //         = max(floor, 1 - β * N(k)/P(k))
            //
            // β = overSubtraction（补偿低估）
            // floor 随帧类型不同（人声帧 0.20，静音帧 0.05）
            //
            // 直觉：
            //   SNR 高（|X(k)| >> N(k)）→ 增益接近 1.0，几乎不衰减
            //   SNR 低（|X(k)| ≈ N(k)）→ 增益接近 floor，大幅衰减
            val gainFloor = if (isSpeech) speechGainFloor else silenceGainFloor

            // ── 第一步：计算每个频率箱的原始增益 ───────────────────────────────
            val gains = FloatArray(NUM_BINS)
            for (k in 0 until NUM_BINS) {
                val power = re[k] * re[k] + im[k] * im[k]
                gains[k] = if (!hasNoiseEstimate || power < 1f) {
                    gainFloor
                } else {
                    val noiseFrac = noisePower[k] / power  // N(k)/P(k)
                    (1f - overSubtraction * noiseFrac).coerceIn(gainFloor, 1f)
                }
            }

            // ── 第二步：增益频谱平滑（移动平均）────────────────────────────────
            // 对相邻频率箱的增益做平均，消除孤立的频谱空洞（"空旷感"根本原因）
            // gainSmoothRadius = 4 → 窗口 = 9 个频率箱（9 * 31.25 Hz ≈ 281 Hz）
            val smoothedGains = FloatArray(NUM_BINS)
            for (k in 0 until NUM_BINS) {
                val lo = (k - gainSmoothRadius).coerceAtLeast(0)
                val hi = (k + gainSmoothRadius).coerceAtMost(NUM_BINS - 1)
                var sum = 0f
                for (j in lo..hi) sum += gains[j]
                smoothedGains[k] = sum / (hi - lo + 1)
            }

            // ── 第三步：将平滑后的增益应用到频谱 ───────────────────────────────
            for (k in 0 until NUM_BINS) {
                val gain = smoothedGains[k]
                re[k] *= gain
                im[k] *= gain
                // 实数信号 FFT 具有共轭对称性，镜像频率箱也要同样处理
                if (k > 0 && k < NUM_BINS - 1) {
                    re[FFT_SIZE - k] *= gain
                    im[FFT_SIZE - k] *= gain
                }
            }

            // IFFT
            fft(re, im, inverse = true)

            // Overlap-Add（加窗合成）
            for (i in 0 until FFT_SIZE) {
                val w = hannWindow[i]
                output[pos + i] += re[i] * w
                winSum[pos + i] += w * w
            }

            pos += HOP_SIZE
        }

        // OLA 归一化（除以重叠窗能量之和）
        for (i in output.indices) {
            if (winSum[i] > 1e-6f) output[i] /= winSum[i]
        }

        // ── Step 4：峰值归一化（仅基于人声帧，避免底噪放大）──────────────────
        val targetPeak = 32767f * 0.85f
        var speechPeak = 0f
        for (i in output.indices) {
            val f = (i / vadFrameSamples).coerceAtMost(vadLabels.size - 1)
            if (vadLabels.getOrElse(f) { false }) {
                val a = abs(output[i])
                if (a > speechPeak) speechPeak = a
            }
        }
        if (speechPeak > 200f) {
            val gain = (targetPeak / speechPeak).coerceIn(0.5f, 8f)
            for (i in output.indices) output[i] *= gain
        }

        // ── Step 5：编码回 PCM 16-bit 小端序 ────────────────────────────────
        val result = ByteArray(data.size)
        for (i in 0 until sampleCount) {
            val v = output[i].toInt().coerceIn(-32768, 32767)
            result[i * 2]     = (v and 0xFF).toByte()
            result[i * 2 + 1] = ((v shr 8) and 0xFF).toByte()
        }
        return result
    }

    /**
     * 重置滤波器状态。每次新的播放开始前调用。
     */
    fun reset() {
        hpPrevInput  = 0f
        hpPrevOutput = 0f
    }

    // ── Cooley-Tukey 基 2 FFT（原地，复数数组）──────────────────────────────
    //
    // 输入：re[]（实部）+ im[]（虚部），长度必须为 2 的幂
    // inverse=true  → IFFT（并除以 n 完成归一化）
    private fun fft(re: FloatArray, im: FloatArray, inverse: Boolean) {
        val n = re.size

        // 位翻转置换（bit-reversal permutation）
        var j = 0
        for (i in 1 until n) {
            var bit = n shr 1
            while (j and bit != 0) { j = j xor bit; bit = bit shr 1 }
            j = j xor bit
            if (i < j) {
                var t = re[i]; re[i] = re[j]; re[j] = t
                t = im[i]; im[i] = im[j]; im[j] = t
            }
        }

        // 蝶形运算（Cooley-Tukey）
        var len = 2
        while (len <= n) {
            val angle = (if (inverse) 2.0 else -2.0) * PI / len
            val wRe = cos(angle).toFloat()
            val wIm = sin(angle).toFloat()
            var i = 0
            while (i < n) {
                var curRe = 1f; var curIm = 0f
                val half = len shr 1
                for (jj in 0 until half) {
                    val uRe = re[i + jj];        val uIm = im[i + jj]
                    val vRe = re[i + jj + half] * curRe - im[i + jj + half] * curIm
                    val vIm = re[i + jj + half] * curIm + im[i + jj + half] * curRe
                    re[i + jj]        = uRe + vRe;  im[i + jj]        = uIm + vIm
                    re[i + jj + half] = uRe - vRe;  im[i + jj + half] = uIm - vIm
                    val nr = curRe * wRe - curIm * wIm
                    curIm = curRe * wIm + curIm * wRe
                    curRe = nr
                }
                i += len
            }
            len = len shl 1
        }

        // IFFT：除以 n 归一化
        if (inverse) {
            val ni = 1f / n
            for (i in 0 until n) { re[i] *= ni; im[i] *= ni }
        }
    }
}
