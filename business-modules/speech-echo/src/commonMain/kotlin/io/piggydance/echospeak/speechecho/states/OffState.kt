package io.piggydance.echospeak.speechecho.states

import io.piggydance.basicdeps.Log
import io.piggydance.echospeak.speechecho.command.Command

private const val TAG = "OffStateState"

class OffState : IState {
    override fun onEnterState() {
        Log.d(TAG, "onEnterState")
    }

    override fun onExitState() {
        Log.d(TAG, "onExitState")
    }

    override fun onCommand(command: Command) {
        Log.d(TAG, "onCommand: $command")
    }
}