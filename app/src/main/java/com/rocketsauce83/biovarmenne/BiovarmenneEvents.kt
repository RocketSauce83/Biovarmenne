package com.rocketsauce83.biovarmenne

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object BiovarmenneEvents {
    private val _fillPin = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val fillPin = _fillPin.asSharedFlow()

    private val _resetGuard = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val resetGuard = _resetGuard.asSharedFlow()
    private val _cancelStk = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val cancelStk = _cancelStk.asSharedFlow()

    fun sendPin(pin: String) { _fillPin.tryEmit(pin) }
    fun sendResetGuard() { _resetGuard.tryEmit(Unit) }
    fun sendCancelStk() { _cancelStk.tryEmit(Unit) }
}