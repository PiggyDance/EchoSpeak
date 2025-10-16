package io.piggydance.echospeak.speechecho.abilities

interface IAudioAbility : IAbility {
    suspend fun startRecord()

    suspend fun stopRecord()

    suspend fun startPlayback()

    suspend fun stopPlayback()
}