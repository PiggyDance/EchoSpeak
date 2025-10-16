package io.piggydance.echospeak.speechecho.states

import io.piggydance.echospeak.speechecho.EchoConfig
import io.piggydance.echospeak.speechecho.EchoMode
import io.piggydance.echospeak.speechecho.context.AbilityContext

object StateFactory {
    fun createState(config: EchoConfig, abilityContext: AbilityContext): IState {
        return when (config.mode) {
            EchoMode.Delay -> DelayEchoState(abilityContext)
            EchoMode.Sentence -> SentenceEchoState(abilityContext)
        }
    }
}