package io.piggydance.echospeak.speechecho

import io.piggydance.echospeak.speechecho.context.AbilityContext
import io.piggydance.echospeak.speechecho.states.StateFactory
import io.piggydance.echospeak.speechecho.states.StateMachine

class SpeechEchoImpl : ISpeechEcho {
    private val stateMachine = StateMachine()

    // 这里要做成一个依赖注入, 并绑定生命周期
    private val abilityContext: AbilityContext = AbilityContext()

    override suspend fun turnOn(config: EchoConfig) {
        val newState = StateFactory.createState(config, abilityContext)
        stateMachine.setState(newState)
    }

    override suspend fun turnOff() {
        stateMachine.destroy()
    }
}