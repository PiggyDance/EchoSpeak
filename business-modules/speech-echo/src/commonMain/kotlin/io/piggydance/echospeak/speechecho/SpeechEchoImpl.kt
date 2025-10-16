package io.piggydance.echospeak.speechecho

import io.piggydance.echospeak.speechecho.states.StateFactory
import io.piggydance.echospeak.speechecho.states.StateMachine

class SpeechEchoImpl : ISpeechEcho {
    private val stateMachine = StateMachine()

    override suspend fun turnOn(config: EchoConfig) {
        val newState = StateFactory.createState(config)
        stateMachine.setState(newState)
    }

    override suspend fun turnOff() {
        stateMachine.destroy()
    }
}