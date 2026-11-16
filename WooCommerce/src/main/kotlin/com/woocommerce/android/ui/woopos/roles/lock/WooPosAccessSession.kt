package com.woocommerce.android.ui.woopos.roles.lock

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.roles.WooPosLocalRole
import com.woocommerce.android.ui.woopos.roles.pin.WooPosPINHash
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.FeatureFlagRepository
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

@ActivityRetainedScoped
class WooPosAccessSession @Inject constructor(
    featureFlagRepository: FeatureFlagRepository,
    private val selectedSite: SelectedSite,
    private val rateLimiter: WooPosLocalRateLimiter,
) {
    private val flagEnabled = featureFlagRepository.isEnabled(FeatureFlag.WOO_POS_ROLES)
    private val _state = MutableStateFlow(WooPosLockScreenState(isLocked = flagEnabled, isChecking = flagEnabled))
    val state = _state.asStateFlow()
    var activeRole: WooPosLocalRole? = null
        private set
    private var protectionEnabled = true
    private var localSiteId: Int? = null
    private var pin = ""
    private var generation = 0L
    private val transitions = Mutex()

    val isEnabled: Boolean get() = flagEnabled && protectionEnabled

    suspend fun initialize(unlockWith: String? = null) = transitions.withLock {
        if (!flagEnabled) return@withLock
        val id = selectedSite.getOrNull()?.id
        if (id != localSiteId) {
            generation++
            activeRole = null
            localSiteId = id
            pin = ""
            _state.value = WooPosLockScreenState(isLocked = true, isChecking = true)
        }
        if (id == null) {
            _state.value = WooPosLockScreenState(isLocked = true, error = WooPosLockError.STORAGE)
            return@withLock
        }
        val request = generation
        val protectionResult = rateLimiter.protection(id)
        if (!isCurrent(request, id)) return@withLock
        protectionEnabled = protectionResult.getOrDefault(true)
        if (protectionResult.getOrNull() == true && activeRole == null && unlockWith != null) {
            val result = rateLimiter.authenticate(id, unlockWith)
            if (!isCurrent(request, id)) return@withLock
            applyResult(result)
            if (_state.value.isLocked) refreshWait()
            return@withLock
        }
        _state.value = when {
            protectionResult.isFailure -> {
                pin = ""
                activeRole = null
                WooPosLockScreenState(isLocked = true, error = WooPosLockError.STORAGE)
            }
            protectionResult.getOrThrow() -> _state.value.copy(
                isLocked = activeRole == null,
                isChecking = activeRole == null,
                error = _state.value.error.takeUnless { it == WooPosLockError.STORAGE },
            )
            else -> {
                pin = ""
                activeRole = null
                WooPosLockScreenState()
            }
        }
        if (_state.value.isLocked) finishChecking(request, id)
    }

    suspend fun lock() {
        if (!isEnabled) return
        val request = ++generation
        activeRole = null
        val id = localSiteId
        pin = ""
        _state.value = WooPosLockScreenState(isLocked = true, isChecking = true)
        if (id == null) {
            _state.value = WooPosLockScreenState(isLocked = true, error = WooPosLockError.STORAGE)
            return
        }
        finishChecking(request, id)
    }

    private suspend fun finishChecking(request: Long, id: Int) {
        refreshWait()
        if (isCurrent(request, id)) _state.value = _state.value.copy(isChecking = false)
    }

    suspend fun enterDigit(digit: Int) {
        val current = _state.value
        if (digit !in MIN_DIGIT..MAX_DIGIT || !current.canEnterPIN) return
        pin += digit
        _state.value = current.copy(enteredDigits = pin.length, error = null)
        if (pin.length == WooPosPINHash.PIN_LENGTH) {
            unlock(pin)
        }
    }

    private suspend fun unlock(submitted: String) {
        if (!_state.value.canEnterPIN) return
        pin = ""
        authenticate { id -> rateLimiter.authenticate(id, submitted) }
    }

    fun deleteDigit() {
        if (!_state.value.canEnterPIN) return
        pin = pin.dropLast(1)
        _state.value = _state.value.copy(enteredDigits = pin.length, error = null)
    }

    suspend fun recover() {
        if (!_state.value.isLocked || _state.value.isChecking) return
        pin = ""
        authenticate { id -> rateLimiter.recover(id) }
    }

    suspend fun refreshWait() {
        val id = localSiteId ?: return
        val request = generation
        val wait = rateLimiter.remainingSeconds(id)
        if (!isCurrent(request, id)) return
        _state.value = _state.value.copy(
            remainingSeconds = wait.getOrDefault(0),
            error = if (wait.isFailure) WooPosLockError.STORAGE else _state.value.error,
        )
    }

    private suspend fun authenticate(action: suspend (Int) -> WooPosLocalRateLimiterResult) {
        val id = localSiteId
        if (id == null) {
            _state.value = WooPosLockScreenState(isLocked = true, error = WooPosLockError.STORAGE)
            return
        }
        val request = ++generation
        _state.value = _state.value.copy(isChecking = true)
        transitions.withLock {
            if (!isCurrent(request, id)) return@withLock
            val result = action(id)
            if (!isCurrent(request, id)) return@withLock
            applyResult(result)
        }
    }

    private fun applyResult(result: WooPosLocalRateLimiterResult) {
        activeRole = (result as? WooPosLocalRateLimiterResult.Authenticated)?.role
        if (result == WooPosLocalRateLimiterResult.ProtectionOff) protectionEnabled = false
        _state.value = when (result) {
            is WooPosLocalRateLimiterResult.Authenticated -> WooPosLockScreenState()
            is WooPosLocalRateLimiterResult.Incorrect -> WooPosLockScreenState(
                isLocked = true, remainingSeconds = result.remainingSeconds, error = WooPosLockError.INCORRECT,
            )
            is WooPosLocalRateLimiterResult.Waiting -> WooPosLockScreenState(
                isLocked = true, remainingSeconds = result.remainingSeconds,
            )
            WooPosLocalRateLimiterResult.ProtectionOff -> WooPosLockScreenState()
            WooPosLocalRateLimiterResult.Unavailable -> WooPosLockScreenState(
                isLocked = true, error = WooPosLockError.STORAGE,
            )
            WooPosLocalRateLimiterResult.RecoveryUnavailable -> _state.value.copy(
                isChecking = false, enteredDigits = 0,
            )
        }
    }

    private fun isCurrent(request: Long, id: Int): Boolean =
        request == generation && id == selectedSite.getOrNull()?.id

    companion object {
        private const val MIN_DIGIT = 0
        private const val MAX_DIGIT = 9
    }
}
