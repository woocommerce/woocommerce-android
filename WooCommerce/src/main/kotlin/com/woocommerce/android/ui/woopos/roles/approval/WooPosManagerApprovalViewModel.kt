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
            accessSession.approvalScopeChanges.collect {
                pending?.let { if (!it.isCurrent()) dismiss() }
            }
        }
    }

    internal fun requestExit(
        event: WooPosNavigationEvent.Exit,
        onShowConfirmation: () -> Unit,
        onExitFromHome: () -> Unit,
        onExit: () -> Unit,
    ) {
        when (event) {
            WooPosNavigationEvent.RequestExitFromHome -> requestApproval(
                WooPosManagerApprovalAction.ExitFromHome,
                onAllowed = onShowConfirmation,
                onApproved = onExitFromHome,
            )
            WooPosNavigationEvent.ConfirmExitFromHome ->
                requestApproval(WooPosManagerApprovalAction.ExitFromHome, onExitFromHome)
            WooPosNavigationEvent.BackFromSplashClicked, WooPosNavigationEvent.ExitPosClicked ->
                requestApproval(WooPosManagerApprovalAction.Exit, onExit)
        }
    }

    internal fun requestApproval(
        action: WooPosManagerApprovalAction,
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
        _state.value = WooPosManagerApprovalState(action, WooPosPINEntryStatus.Initializing)
        job = viewModelScope.launch { refreshWait(request) }
    }

    internal fun onUIEvent(event: WooPosManagerApprovalUIEvent) {
        when (event) {
            is WooPosManagerApprovalUIEvent.DigitTapped -> onDigit(event.digit)
            WooPosManagerApprovalUIEvent.DeleteTapped -> onDelete()
            WooPosManagerApprovalUIEvent.DismissRequested -> dismiss()
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
                    clear()
                    request.perform()
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
        _state.value = WooPosManagerApprovalState(request.action, status)
    }

    private fun PendingAction.isCurrent(): Boolean =
        pending === this && accessSession.approvalScope() == scope

    private class PendingAction(
        val scope: WooPosAccessSession.ApprovalScope,
        val action: WooPosManagerApprovalAction,
        val perform: () -> Unit,
    )
}

internal sealed interface WooPosManagerApprovalUIEvent {
    data class DigitTapped(val digit: Int) : WooPosManagerApprovalUIEvent
    data object DeleteTapped : WooPosManagerApprovalUIEvent
    data object DismissRequested : WooPosManagerApprovalUIEvent
}
