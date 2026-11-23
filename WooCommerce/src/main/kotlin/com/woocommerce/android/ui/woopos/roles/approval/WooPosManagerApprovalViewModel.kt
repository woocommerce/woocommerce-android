package com.woocommerce.android.ui.woopos.roles.approval

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.ui.woopos.roles.lock.WooPosAccessSession
import com.woocommerce.android.ui.woopos.roles.lock.WooPosLocalRateLimiterResult
import com.woocommerce.android.ui.woopos.roles.lock.WooPosLockScreenState.Status
import com.woocommerce.android.ui.woopos.roles.pin.WooPosPINHash
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WooPosManagerApprovalViewModel @Inject constructor(
    private val accessSession: WooPosAccessSession,
) : ViewModel() {
    private val _state = MutableStateFlow<WooPosManagerApprovalState?>(null)
    internal val state = _state.asStateFlow()
    private var pending: PendingAction? = null
    private var pin = ""
    private var job: Job? = null

    init {
        viewModelScope.launch {
            combine(accessSession.state, accessSession.logoutVersion) { _, _ -> Unit }.collect {
                pending?.let { if (!it.isCurrent()) cancel() }
            }
        }
    }

    internal fun run(
        action: WooPosApprovalAction,
        onAllowed: () -> Unit,
        onApproved: () -> Unit = onAllowed,
    ) {
        if (pending != null) return
        if (accessSession.allows(action.capability)) {
            onAllowed()
            return
        }
        val scope = accessSession.approvalScope() ?: return
        val request = PendingAction(scope, action, onApproved)
        pending = request
        _state.value = WooPosManagerApprovalState(action, Status.Checking)
        job = viewModelScope.launch { refreshWait(request) }
    }

    internal fun onDigit(digit: Int) {
        val current = state.value ?: return
        val request = pending ?: return
        if (digit !in 0..MAX_DIGIT || !current.canEnterPIN) return
        pin += digit
        if (pin.length < WooPosPINHash.PIN_LENGTH) {
            _state.value = current.copy(status = Status.Idle, enteredDigits = pin.length)
            return
        }
        val submitted = pin
        pin = ""
        _state.value = current.copy(status = Status.Checking, enteredDigits = submitted.length)
        job = viewModelScope.launch {
            val result = accessSession.approve(request.scope, submitted, request.action.capability)
            ensureActive()
            if (!request.isCurrent()) {
                cancel()
                return@launch
            }
            when (result) {
                is WooPosLocalRateLimiterResult.Authenticated -> {
                    clear()
                    request.perform()
                }
                is WooPosLocalRateLimiterResult.Incorrect ->
                    if (result.remainingSeconds > 0) {
                        waitAndRefresh(request, result.remainingSeconds)
                    } else {
                        show(request, Status.Incorrect)
                    }
                is WooPosLocalRateLimiterResult.Waiting -> waitAndRefresh(request, result.remainingSeconds)
                WooPosLocalRateLimiterResult.ProtectionOff,
                WooPosLocalRateLimiterResult.Unavailable -> show(request, Status.StorageUnavailable)
            }
        }
    }

    internal fun onDelete() {
        val current = state.value ?: return
        if (!current.canEnterPIN || pin.isEmpty()) return
        pin = pin.dropLast(1)
        _state.value = current.copy(enteredDigits = pin.length)
    }

    internal fun cancel() {
        job?.cancel()
        clear()
    }

    private fun clear() {
        job = null
        pending = null
        pin = ""
        _state.value = null
    }

    private suspend fun refreshWait(request: PendingAction) {
        while (request.isCurrent()) {
            val remaining = accessSession.approvalWait(request.scope)
            currentCoroutineContext().ensureActive()
            if (!request.isCurrent()) break
            val seconds = remaining.getOrNull()
            val status = when {
                seconds == null -> Status.StorageUnavailable
                seconds == 0L -> Status.Idle
                else -> Status.Wait(seconds)
            }
            show(request, status)
            if (status !is Status.Wait) return
            delay(COUNTDOWN_INTERVAL_MILLIS)
        }
        if (pending === request) cancel()
    }

    private suspend fun waitAndRefresh(request: PendingAction, seconds: Long) {
        show(request, Status.Wait(seconds))
        delay(COUNTDOWN_INTERVAL_MILLIS)
        refreshWait(request)
    }

    private fun show(request: PendingAction, status: Status) {
        _state.value = WooPosManagerApprovalState(request.action, status)
    }

    private fun PendingAction.isCurrent(): Boolean =
        pending === this && accessSession.approvalScope() == scope

    private class PendingAction(
        val scope: WooPosAccessSession.ApprovalScope,
        val action: WooPosApprovalAction,
        val perform: () -> Unit,
    )

    private companion object {
        const val MAX_DIGIT = 9
        const val COUNTDOWN_INTERVAL_MILLIS = 1000L
    }
}
