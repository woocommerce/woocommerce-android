package com.woocommerce.android.ui.woopos.lock

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.roles.WooPosOperator
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.FeatureFlagRepository
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

@ActivityRetainedScoped
class WooPosLockSession @Inject constructor(
    private val featureFlagRepository: FeatureFlagRepository,
    private val selectedSite: SelectedSite,
    private val access: WooPosPINAccess,
    private val lockedPOS: WooPosLockedPOSStore,
) {
    private val flagEnabled = featureFlagRepository.isEnabled(FeatureFlag.WOO_POS_MANUAL_LOCK)
    private val _state = MutableStateFlow(WooPosLockScreenState(isLocked = flagEnabled, isChecking = flagEnabled))
    val state = _state.asStateFlow()
    private var localSiteId: Int? = null
    private var pin = ""
    private var generation = 0L
    private var isExiting = false
    private var hasExited = false
    private val transitions = Mutex()

    val isEnabled: Boolean get() = flagEnabled && _state.value.protectionEnabled

    suspend fun initialize() = transitions.withLock {
        if (!flagEnabled || hasExited) return@withLock
        val id = selectedSite.getOrNull()?.id
        if (id != localSiteId) {
            generation++
            localSiteId = id
            pin = ""
            _state.value = WooPosLockScreenState(isLocked = true, isChecking = true)
        }
        if (id == null) {
            _state.value = WooPosLockScreenState(isLocked = true, error = WooPosLockError.STORAGE)
            return@withLock
        }
        val request = generation
        val protected = access.protection(id)
        if (!isCurrent(request, id)) return@withLock
        // Retain the marker until explicit POS exit: a process restart never restores operator authority.
        val marked = if (protected.getOrNull() == false) lockedPOS.clear(id) else lockedPOS.mark(id)
        if (!isCurrent(request, id)) return@withLock
        _state.value = when {
            protected.isFailure || (protected.getOrNull() == true && marked.isFailure) -> {
                pin = ""
                WooPosLockScreenState(isLocked = true, error = WooPosLockError.STORAGE)
            }
            protected.getOrThrow() -> _state.value.copy(
                isLocked = _state.value.activeOperator == null,
                isChecking = false,
                protectionEnabled = true,
            )
            else -> {
                pin = ""
                WooPosLockScreenState(protectionEnabled = false)
            }
        }
        if (_state.value.isLocked) refreshWait()
    }

    fun lock() {
        if (!isEnabled || isExiting || hasExited) return
        generation++
        pin = ""
        _state.value = WooPosLockScreenState(isLocked = true, remainingSeconds = _state.value.remainingSeconds)
    }

    suspend fun enterDigit(digit: Int) {
        val current = _state.value
        if (digit !in MIN_DIGIT..MAX_DIGIT || !current.canEnterPIN) return
        pin += digit
        _state.value = current.copy(enteredDigits = pin.length, error = null)
        if (pin.length == WooPosLockScreenState.PIN_LENGTH) {
            val submitted = pin
            pin = ""
            authenticate { id -> access.authenticate(id, submitted) }
        }
    }

    fun deleteDigit() {
        if (!_state.value.canEnterPIN) return
        pin = pin.dropLast(1)
        _state.value = _state.value.copy(enteredDigits = pin.length, error = null)
    }

    suspend fun recover() {
        if (!_state.value.isLocked || _state.value.isChecking) return
        pin = ""
        authenticate { id -> access.recover(id) }
    }

    suspend fun refreshWait() {
        val id = localSiteId ?: return
        val request = generation
        val wait = access.remainingSeconds(id)
        if (!isCurrent(request, id)) return
        _state.value = _state.value.copy(
            remainingSeconds = wait.getOrDefault(0),
            error = if (wait.isFailure) WooPosLockError.STORAGE else _state.value.error,
        )
    }

    suspend fun exitPOS(): Boolean = transitions.withLock {
        if (hasExited || _state.value.isLocked) return@withLock false
        val id = localSiteId
        if (isEnabled && id != null) {
            isExiting = true
            val cleared = lockedPOS.clear(id)
            isExiting = false
            if (cleared.isFailure) {
                lock()
                _state.value = _state.value.copy(error = WooPosLockError.STORAGE)
                return@withLock false
            }
        }
        hasExited = true
        generation++
        pin = ""
        _state.value = WooPosLockScreenState(protectionEnabled = false)
        true
    }

    private suspend fun authenticate(action: suspend (Int) -> WooPosPINAccessResult) {
        val id = localSiteId
        if (id == null) {
            _state.value = WooPosLockScreenState(isLocked = true, error = WooPosLockError.STORAGE)
            return
        }
        val request = ++generation
        _state.value = _state.value.copy(isChecking = true)
        transitions.withLock {
            if (!isCurrent(request, id)) return@withLock
            var result = action(id)
            if (!isCurrent(request, id)) return@withLock
            if (result is WooPosPINAccessResult.Authenticated && lockedPOS.mark(id).isFailure) {
                result = WooPosPINAccessResult.Unavailable
            }
            if (!isCurrent(request, id)) return@withLock
            _state.value = when (result) {
                is WooPosPINAccessResult.Authenticated -> WooPosLockScreenState(activeOperator = result.operator)
                is WooPosPINAccessResult.Incorrect -> WooPosLockScreenState(
                    isLocked = true, remainingSeconds = result.remainingSeconds, error = WooPosLockError.INCORRECT,
                )
                is WooPosPINAccessResult.Waiting -> WooPosLockScreenState(
                    isLocked = true, remainingSeconds = result.remainingSeconds,
                )
                WooPosPINAccessResult.ProtectionOff -> WooPosLockScreenState(protectionEnabled = false)
                WooPosPINAccessResult.Unavailable -> WooPosLockScreenState(
                    isLocked = true, error = WooPosLockError.STORAGE,
                )
                WooPosPINAccessResult.RecoveryUnavailable -> _state.value.copy(
                    isChecking = false, enteredDigits = 0, error = WooPosLockError.RECOVERY_UNAVAILABLE,
                )
            }
        }
    }

    private fun isCurrent(request: Long, id: Int): Boolean =
        !hasExited && request == generation && id == selectedSite.getOrNull()?.id

    companion object {
        private const val MIN_DIGIT = 0
        private const val MAX_DIGIT = 9
    }
}

data class WooPosLockScreenState(
    val isLocked: Boolean = false,
    val enteredDigits: Int = 0,
    val isChecking: Boolean = false,
    val protectionEnabled: Boolean = true,
    val activeOperator: WooPosOperator? = null,
    val remainingSeconds: Long = 0,
    val error: WooPosLockError? = null,
) {
    val canEnterPIN: Boolean get() = isLocked && !isChecking && remainingSeconds == 0L

    companion object {
        const val PIN_LENGTH = 4
    }
}

enum class WooPosLockError { INCORRECT, STORAGE, RECOVERY_UNAVAILABLE }
