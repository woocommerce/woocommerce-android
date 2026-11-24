package com.woocommerce.android.ui.woopos.roles.approval

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.ui.woopos.roles.lock.WooPosAccessSession
import com.woocommerce.android.ui.woopos.roles.lock.WooPosLocalRateLimiterResult
import com.woocommerce.android.ui.woopos.roles.pin.PIN_COUNTDOWN_INTERVAL_MILLIS
import com.woocommerce.android.ui.woopos.roles.pin.WooPosPINEntryStatus
import com.woocommerce.android.ui.woopos.roles.pin.WooPosPINHash
import com.woocommerce.android.ui.woopos.root.navigation.WooPosNavigationEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WooPosApprovalViewModel @Inject constructor(
    private val accessSession: WooPosAccessSession,
) : ViewModel() {
    private val _state = MutableStateFlow<WooPosApprovalState?>(null)
    internal val state = _state.asStateFlow()
    private var pending: PendingAction? = null
    private var pin = ""
    private var job: Job? = null

    init {
        viewModelScope.launch {
            accessSession.approvalScopeChanges.collect {
                pending?.let { if (!it.isCurrent()) dismiss() }
            }
        }
    }

    internal fun requestExit(
        event: WooPosNavigationEvent.Exit,
        onExitFromHome: () -> Unit,
        onExit: () -> Unit,
    ) {
        when (event) {
            WooPosNavigationEvent.ConfirmExitFromHome ->
                requestApproval(WooPosApprovalAction.Exit, onExitFromHome)
            WooPosNavigationEvent.BackFromSplashClicked, WooPosNavigationEvent.ExitPosClicked ->
                requestApproval(WooPosApprovalAction.Exit, onExit)
        }
    }

    internal fun requestApproval(
        action: WooPosApprovalAction,
        onAllowed: () -> Unit,
    ) {
        if (pending != null) return
        if (accessSession.allows(action.capability)) {
            onAllowed()
            return
        }
        beginApproval(action) {
            onApproved()
            true
        }
    }

    internal fun requestFreshApproval(action: WooPosManagerApprovalAction, onApproved: suspend () -> Boolean) {
        if (pending != null || !accessSession.allows(action.capability)) return
        beginApproval(action, onApproved)
    }

    private fun beginApproval(action: WooPosManagerApprovalAction, perform: suspend () -> Boolean) {
        val scope = accessSession.approvalScope() ?: return
        val request = PendingAction(scope, action, perform)
        pending = request
        _state.value = WooPosApprovalState(action, WooPosPINEntryStatus.Initializing)
        job = viewModelScope.launch { refreshWait(request) }
    }

    internal fun onUIEvent(event: WooPosApprovalUIEvent) {
        when (event) {
            is WooPosApprovalUIEvent.DigitTapped -> onDigit(event.digit)
            WooPosApprovalUIEvent.DeleteTapped -> onDelete()
            WooPosApprovalUIEvent.DismissRequested -> dismiss()
        }
    }

    private fun onDigit(digit: Int) {
        val current = state.value ?: return
        val request = pending ?: return
        if (digit !in WooPosPINHash.PIN_DIGIT_RANGE || !current.status.canEnterPIN) return
        pin += digit
        if (pin.length < WooPosPINHash.PIN_LENGTH) {
            _state.value = current.copy(status = WooPosPINEntryStatus.Idle, enteredDigits = pin.length)
            return
        }
        val submitted = pin
        pin = ""
        _state.value = current.copy(status = WooPosPINEntryStatus.Checking, enteredDigits = submitted.length)
        job = viewModelScope.launch {
            val result = accessSession.approve(request.scope, submitted, request.action.capability)
            ensureActive()
            if (!request.isCurrent()) {
                dismiss()
                return@launch
            }
            when (result) {
                is WooPosLocalRateLimiterResult.Authenticated -> {
                    complete(request)
                }
                is WooPosLocalRateLimiterResult.Incorrect ->
                    if (result.remainingSeconds > 0) {
                        waitAndRefresh(request, result.remainingSeconds)
                    } else {
                        show(request, WooPosPINEntryStatus.Incorrect)
                    }
                is WooPosLocalRateLimiterResult.Waiting -> waitAndRefresh(request, result.remainingSeconds)
                WooPosLocalRateLimiterResult.ProtectionOff,
                WooPosLocalRateLimiterResult.Unavailable -> show(request, WooPosPINEntryStatus.StorageUnavailable)
            }
        }
    }

    private suspend fun complete(request: PendingAction) {
        if (request.action == WooPosManagerApprovalAction.DisablePINProtection) {
            val completed = request.perform()
            currentCoroutineContext().ensureActive()
            if (pending === request) {
                if (completed) clear() else show(request, WooPosPINEntryStatus.StorageUnavailable)
            }
        } else {
            clear()
            request.perform()
        }
    }

    private fun onDelete() {
        val current = state.value ?: return
        if (!current.status.canEnterPIN || pin.isEmpty()) return
        pin = pin.dropLast(1)
        _state.value = current.copy(enteredDigits = pin.length)
    }

    internal fun dismiss() {
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
                seconds == null -> WooPosPINEntryStatus.StorageUnavailable
                seconds == 0L -> WooPosPINEntryStatus.Idle
                else -> WooPosPINEntryStatus.Wait(seconds)
            }
            show(request, status)
            if (status !is WooPosPINEntryStatus.Wait) return
            delay(PIN_COUNTDOWN_INTERVAL_MILLIS)
        }
        if (pending === request) dismiss()
    }

    private suspend fun waitAndRefresh(request: PendingAction, seconds: Long) {
        show(request, WooPosPINEntryStatus.Wait(seconds))
        delay(PIN_COUNTDOWN_INTERVAL_MILLIS)
        refreshWait(request)
    }

    private fun show(request: PendingAction, status: WooPosPINEntryStatus) {
        _state.value = WooPosApprovalState(request.action, status)
    }

    private fun PendingAction.isCurrent(): Boolean =
        pending === this && accessSession.approvalScope() == scope

    private class PendingAction(
        val scope: WooPosAccessSession.ApprovalScope,
        val action: WooPosManagerApprovalAction,
        val perform: suspend () -> Boolean,
    )
}

internal sealed interface WooPosApprovalUIEvent {
    data class DigitTapped(val digit: Int) : WooPosApprovalUIEvent
    data object DeleteTapped : WooPosApprovalUIEvent
    data object DismissRequested : WooPosApprovalUIEvent
}
