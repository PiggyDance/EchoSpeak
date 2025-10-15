package io.piggydance.echospeak.speechecho.states

import io.piggydance.echospeak.speechecho.command.Command

interface IState {

    fun onEnterState()

    fun onExitState()

    fun onCommand(command: Command)
}
