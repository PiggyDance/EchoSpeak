package io.piggydance.echospeak.speechecho.states

import io.piggydance.echospeak.speechecho.command.Command

interface IState {

    suspend fun onEnterState()

    suspend fun onExitState()

    suspend fun onCommand(command: Command)
}
