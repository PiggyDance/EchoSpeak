package io.piggydance.echospeak.speechecho.states

import io.piggydance.basicdeps.Log
import io.piggydance.echospeak.speechecho.command.Command

import io.piggydance.echospeak.speechecho.context.AbilityContext

private const val TAG = "SentenceEchoState"

class SentenceEchoState(private val abilityContext: AbilityContext) : IState {
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