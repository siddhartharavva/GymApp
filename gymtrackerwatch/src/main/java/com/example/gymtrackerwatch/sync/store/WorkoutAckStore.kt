package com.example.gymtrackerwatch.sync.store

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object WorkoutAckStore {
    private val _ackReceived = MutableStateFlow(false)
    val ackReceived: StateFlow<Boolean> = _ackReceived

    fun signalAck() {
        _ackReceived.value = true
    }

    fun consume() {
        _ackReceived.value = false
    }
}
