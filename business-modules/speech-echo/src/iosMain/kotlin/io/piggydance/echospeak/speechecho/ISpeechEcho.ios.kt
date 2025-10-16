package io.piggydance.echospeak.speechecho

actual interface ISpeechEcho {
    actual suspend fun turnOn(config: EchoConfig)
    actual suspend fun turnOff()
}