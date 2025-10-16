package io.piggydance.echospeak.speechecho.states

import io.piggydance.echospeak.speechecho.EchoConfig
import io.piggydance.echospeak.speechecho.EchoMode

object StateFactory {
    fun createState(config: EchoConfig): IState {
        return when (config.mode) {
            EchoMode.Delay -> DelayEchoState()
            EchoMode.Sentence -> SentenceEchoState()
        }
    }
}