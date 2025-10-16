package io.piggydance.echospeak.speechecho

enum class EchoMode {
    Sentence,
    Delay,
}

data class EchoConfig(
    val mode: EchoMode,
)

interface ISpeechEcho {

    /**
     * 启动语音Echo,传入EchoConfig指定具体的声音回显模式及其参数.
     */
    suspend fun turnOn(config: EchoConfig)

     /**
      * 停止语音Echo.
      */
    suspend fun turnOff()
}
