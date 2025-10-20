package io.piggydance.echospeak.speechecho.states.substates

import io.piggydance.basicdeps.Log
import io.piggydance.echospeak.speechecho.command.Command
import io.piggydance.echospeak.speechecho.states.IState

private const val TAG = "DelaySubstateEchoing"

class DelaySubstateEchoing : IState {
    override suspend fun onEnterState() {
        Log.d(TAG, "onEnterState")
    }

    override suspend fun onExitState() {
        Log.d(TAG, "onExitState")
    }

    override suspend fun onCommand(command: Command) {
        Log.d(TAG, "onCommand: $command")
    }
}