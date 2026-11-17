package com.woocommerce.android.ui.woopos.roles.lock

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.roles.WooPosLocalRole
import com.woocommerce.android.ui.woopos.roles.lock.WooPosLockScreenState.Status
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
    private val _state = MutableStateFlow(
        WooPosLockScreenState(
            isLocked = flagEnabled,
            status = if (flagEnabled) Status.Checking else Status.Idle,
        )
    )
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
            _state.value = WooPosLockScreenState(isLocked = true, status = Status.Checking)
        }
        if (id == null) {
            _state.value = WooPosLockScreenState(isLocked = true, status = Status.StorageUnavailable)
            return@withLock
        }
        val request = generation
        val protectionResult = rateLimiter.isProtectionEnabled(id)
        if (!isCurrent(request, id)) return@withLock
        protectionEnabled = protectionResult.getOrDefault(true)
        if (protectionResult.getOrNull() == true && activeRole == null && unlockWith != null) {
            val result = rateLimiter.authenticate(id, unlockWith)
            if (!isCurrent(request, id)) return@withLock
            applyResult(result)
            if (_state.value.isLocked) refreshWait()
            return@withLock
        }
        applyProtection(protectionResult)
        if (_state.value.isLocked) refreshWait()
    }

    private fun applyProtection(protectionResult: Result<Boolean>) {
        _state.value = when {
            protectionResult.isFailure -> {
                pin = ""
                activeRole = null
                WooPosLockScreenState(isLocked = true, status = Status.StorageUnavailable)
            }
            protectionResult.getOrThrow() -> _state.value.copy(
                isLocked = activeRole == null,
                status = if (activeRole == null) Status.Checking else Status.Idle,
            )
            else -> {
                pin = ""
                activeRole = null
                WooPosLockScreenState()
            }
        }
    }

    suspend fun lock() {
        if (!isEnabled) return
        generation++
        activeRole = null
        val id = localSiteId
        pin = ""
        _state.value = WooPosLockScreenState(isLocked = true, status = Status.Checking)
        if (id == null) {
            _state.value = WooPosLockScreenState(isLocked = true, status = Status.StorageUnavailable)
            return
        }
        refreshWait()
    }

    suspend fun enterDigit(digit: Int) {
        val current = _state.value
        if (digit !in MIN_DIGIT..MAX_DIGIT || !current.canEnterPIN) return
        pin += digit
        _state.value = current.copy(enteredDigits = pin.length, status = Status.Idle)
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
        _state.value = _state.value.copy(enteredDigits = pin.length, status = Status.Idle)
    }

    suspend fun recover() {
        if (!_state.value.isLocked || _state.value.status == Status.Checking) return
        pin = ""
        authenticate { id -> rateLimiter.recover(id) }
    }

    suspend fun refreshWait() {
        val id = localSiteId ?: return
        val request = generation
        val wait = rateLimiter.remainingSeconds(id)
        if (!isCurrent(request, id)) return
        _state.value = _state.value.copy(
            status = when {
                wait.isFailure -> Status.StorageUnavailable
                wait.getOrThrow() > 0 -> Status.Wait(wait.getOrThrow())
                _state.value.status == Status.Incorrect -> Status.Incorrect
                _state.value.status == Status.StorageUnavailable -> Status.StorageUnavailable
                else -> Status.Idle
            },
        )
    }

    private suspend fun authenticate(action: suspend (Int) -> WooPosLocalRateLimiterResult) {
        val id = localSiteId
        if (id == null) {
            _state.value = WooPosLockScreenState(isLocked = true, status = Status.StorageUnavailable)
            return
        }
        val request = ++generation
        val previousStatus = _state.value.status
        _state.value = _state.value.copy(status = Status.Checking)
        transitions.withLock {
            if (!isCurrent(request, id)) return@withLock
            val result = action(id)
            if (!isCurrent(request, id)) return@withLock
            applyResult(result, previousStatus)
        }
    }

    private fun applyResult(result: WooPosLocalRateLimiterResult, previousStatus: Status = Status.Idle) {
        activeRole = (result as? WooPosLocalRateLimiterResult.Authenticated)?.role
        if (result == WooPosLocalRateLimiterResult.ProtectionOff) protectionEnabled = false
        _state.value = when (result) {
            is WooPosLocalRateLimiterResult.Authenticated -> WooPosLockScreenState()
            is WooPosLocalRateLimiterResult.Incorrect -> WooPosLockScreenState(
                isLocked = true,
                status = if (result.remainingSeconds > 0) Status.Wait(result.remainingSeconds) else Status.Incorrect,
            )
            is WooPosLocalRateLimiterResult.Waiting -> WooPosLockScreenState(
                isLocked = true, status = Status.Wait(result.remainingSeconds),
            )
            WooPosLocalRateLimiterResult.ProtectionOff -> WooPosLockScreenState()
            WooPosLocalRateLimiterResult.Unavailable -> WooPosLockScreenState(
                isLocked = true, status = Status.StorageUnavailable,
            )
            WooPosLocalRateLimiterResult.RecoveryUnavailable -> _state.value.copy(
                status = previousStatus, enteredDigits = 0,
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
