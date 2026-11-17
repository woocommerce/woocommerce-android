package com.woocommerce.android.ui.woopos.roles.lock

import androidx.annotation.MainThread
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.roles.lock.WooPosLockScreenState.Status
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.FeatureFlagRepository
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@MainThread
@ActivityRetainedScoped
class WooPosAccessSession @Inject constructor(
    featureFlagRepository: FeatureFlagRepository,
    private val selectedSite: SelectedSite,
    private val rateLimiter: WooPosLocalRateLimiter,
) {
    private val flagEnabled = featureFlagRepository.isEnabled(FeatureFlag.WOO_POS_ROLES)
    private val _state = MutableStateFlow<WooPosAccessSessionState>(
        if (flagEnabled) WooPosAccessSessionState.Locked(Status.Checking) else WooPosAccessSessionState.Unprotected
    )
    val state = _state.asStateFlow()
    private var localSiteId: Int? = null
    private var generation = 0L

    val isEnabled: Boolean get() = flagEnabled && _state.value != WooPosAccessSessionState.Unprotected

    suspend fun initialize() {
        if (!flagEnabled) return
        val id = selectedSite.getOrNull()?.id
        if (id != localSiteId) {
            localSiteId = id
            _state.value = WooPosAccessSessionState.Locked(Status.Checking)
        }
        val request = ++generation
        if (id == null) {
            _state.value = WooPosAccessSessionState.Locked(Status.StorageUnavailable)
            return
        }
        val protection = rateLimiter.isProtectionEnabled(id)
        if (!isCurrent(request, id)) return
        _state.value = when {
            protection.isFailure -> WooPosAccessSessionState.Locked(Status.StorageUnavailable)
            !protection.getOrThrow() -> WooPosAccessSessionState.Unprotected
            _state.value is WooPosAccessSessionState.Unlocked -> _state.value
            else -> WooPosAccessSessionState.Locked(Status.Checking)
        }
        if (_state.value.isLocked) refreshWait()
    }

    suspend fun lock() {
        if (!isEnabled) return
        generation++
        _state.value = WooPosAccessSessionState.Locked(Status.Checking)
        refreshWait()
    }

    suspend fun unlock(pin: String) {
        if (!flagEnabled || _state.value is WooPosAccessSessionState.Unlocked) return
        val status = _state.value.status
        if (status is Status.Wait || status == Status.StorageUnavailable) return
        authenticate { id -> rateLimiter.authenticate(id, pin) }
    }

    suspend fun recover() {
        if (!_state.value.isLocked || _state.value.status == Status.Checking) return
        authenticate { id -> rateLimiter.recover(id) }
    }

    suspend fun refreshWait() {
        val current = _state.value as? WooPosAccessSessionState.Locked ?: return
        val id = localSiteId
        if (id == null) {
            _state.value = WooPosAccessSessionState.Locked(Status.StorageUnavailable)
            return
        }
        val request = generation
        val wait = rateLimiter.remainingSeconds(id)
        if (!isCurrent(request, id) || _state.value != current) return
        _state.value = WooPosAccessSessionState.Locked(
            when {
                wait.isFailure -> Status.StorageUnavailable
                wait.getOrThrow() > 0 -> Status.Wait(wait.getOrThrow())
                current.status == Status.Incorrect -> Status.Incorrect
                current.status == Status.StorageUnavailable -> Status.StorageUnavailable
                else -> Status.Idle
            },
        )
    }

    private suspend fun authenticate(action: suspend (Int) -> WooPosLocalRateLimiterResult) {
        val previous = _state.value
        val request = ++generation
        val id = localSiteId ?: selectedSite.getOrNull()?.id
        if (id == null || !isCurrent(request, id)) {
            _state.value = WooPosAccessSessionState.Locked(Status.StorageUnavailable)
            return
        }
        localSiteId = id
        if (previous.isLocked) _state.value = WooPosAccessSessionState.Locked(Status.Checking)
        val result = action(id)
        currentCoroutineContext().ensureActive()
        if (!isCurrent(request, id)) return
        _state.value = when (result) {
            is WooPosLocalRateLimiterResult.Authenticated -> WooPosAccessSessionState.Unlocked(result.role)
            is WooPosLocalRateLimiterResult.Incorrect -> WooPosAccessSessionState.Locked(
                if (result.remainingSeconds > 0) Status.Wait(result.remainingSeconds) else Status.Incorrect,
            )
            is WooPosLocalRateLimiterResult.Waiting -> WooPosAccessSessionState.Locked(Status.Wait(result.remainingSeconds))
            WooPosLocalRateLimiterResult.ProtectionOff -> WooPosAccessSessionState.Unprotected
            WooPosLocalRateLimiterResult.Unavailable -> WooPosAccessSessionState.Locked(Status.StorageUnavailable)
            WooPosLocalRateLimiterResult.RecoveryUnavailable -> previous
        }
    }

    private fun isCurrent(request: Long, id: Int): Boolean =
        request == generation && id == selectedSite.getOrNull()?.id
}
