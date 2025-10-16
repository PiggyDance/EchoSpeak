package io.piggydance.echospeak.speechecho.states

import io.piggydance.basicdeps.Log
import io.piggydance.echospeak.speechecho.command.Command

private const val TAG = "StateMachine"

// 简单的通用状态机,仅可在主线程使用,避免多线程修改状态
class StateMachine {
    private var curState: IState = OffState()

    suspend fun setState(state: IState?) {
        Log.d(TAG, "setState: $state")
        if (state == null || curState == state) {
            Log.d(TAG, "setState: null or same state, ignore")
            return
        }
        curState.onExitState()
        curState = state
        curState.onEnterState()
    }

    suspend fun onCommand(command: Command) {
        Log.i(TAG, "onCommand: $command")
        curState.onCommand(command)
    }

    suspend fun destroy() {
        Log.i(TAG, "destroy")
        curState.onExitState()
        curState = OffState()
    }
}