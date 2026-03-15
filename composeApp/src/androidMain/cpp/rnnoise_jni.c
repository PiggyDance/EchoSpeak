// JNI bridge for RNNoise — connects Kotlin RnnoiseProcessor to the RNNoise C library
// RNNoise API: https://github.com/xiph/rnnoise
//
// Key facts:
//   - rnnoise_get_frame_size() == 480 samples @ 48000 Hz
//   - Input/output are float arrays in range [-32768, 32767]
//   - Each call to rnnoise_process_frame() returns the voice probability (0.0–1.0)

#include <jni.h>
#include <stdlib.h>
#include <string.h>
#include "rnnoise/include/rnnoise.h"

// ── rnnoise_create ────────────────────────────────────────────────────────────
// Returns a pointer to a DenoiseState cast to jlong (opaque handle for Kotlin)
JNIEXPORT jlong JNICALL
Java_io_piggydance_echospeak_audio_RnnoiseProcessor_nativeCreate(
        JNIEnv *env, jobject thiz) {
    DenoiseState *state = rnnoise_create(NULL);  // NULL = use built-in model
    return (jlong)(intptr_t) state;
}

// ── rnnoise_destroy ───────────────────────────────────────────────────────────
JNIEXPORT void JNICALL
Java_io_piggydance_echospeak_audio_RnnoiseProcessor_nativeDestroy(
        JNIEnv *env, jobject thiz, jlong handle) {
    DenoiseState *state = (DenoiseState *)(intptr_t) handle;
    if (state != NULL) rnnoise_destroy(state);
}

// ── rnnoise_get_frame_size ────────────────────────────────────────────────────
JNIEXPORT jint JNICALL
Java_io_piggydance_echospeak_audio_RnnoiseProcessor_nativeGetFrameSize(
        JNIEnv *env, jobject thiz) {
    return rnnoise_get_frame_size();  // typically 480
}

// ── rnnoise_process_frame ─────────────────────────────────────────────────────
// Processes one frame of PCM 16-bit samples (as ShortArray from Kotlin).
// Denoises in-place: reads from inBuf, writes denoised result back to inBuf.
// Returns voice probability (0.0–1.0).
JNIEXPORT jfloat JNICALL
Java_io_piggydance_echospeak_audio_RnnoiseProcessor_nativeProcessFrame(
        JNIEnv *env, jobject thiz,
        jlong handle,
        jshortArray inBuf,
        jint offset,
        jshortArray outBuf,
        jint outOffset,
        jint length) {

    DenoiseState *state = (DenoiseState *)(intptr_t) handle;
    if (state == NULL || length <= 0) return 0.0f;

    // Get pointers to Java short arrays
    jshort *inPtr  = (*env)->GetShortArrayElements(env, inBuf, NULL);
    jshort *outPtr = (*env)->GetShortArrayElements(env, outBuf, NULL);

    // Convert short → float (RNNoise expects floats in [-32768, 32767] range)
    float *floatIn  = (float *) malloc(length * sizeof(float));
    float *floatOut = (float *) malloc(length * sizeof(float));

    for (int i = 0; i < length; i++) {
        floatIn[i] = (float) inPtr[offset + i];
    }

    float vad_prob = rnnoise_process_frame(state, floatOut, floatIn);

    // Convert float → short and write result
    for (int i = 0; i < length; i++) {
        float v = floatOut[i];
        if (v >  32767.0f) v =  32767.0f;
        if (v < -32768.0f) v = -32768.0f;
        outPtr[outOffset + i] = (jshort) v;
    }

    free(floatIn);
    free(floatOut);

    (*env)->ReleaseShortArrayElements(env, inBuf,  inPtr,  JNI_ABORT);
    (*env)->ReleaseShortArrayElements(env, outBuf, outPtr, 0);  // 0 = copy back

    return vad_prob;
}
