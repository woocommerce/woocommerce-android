package com.woocommerce.android.ui.woopos.roles.lock

import androidx.annotation.MainThread
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.roles.WooPosCapability
import com.woocommerce.android.ui.woopos.roles.WooPosLocalRole
import com.woocommerce.android.ui.woopos.roles.lock.WooPosLockScreenState.Status
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.FeatureFlagRepository
import com.woocommerce.android.util.WooLog
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.CancellationException
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
    private val recovery: WooPosPINRecovery,
) {
    private val flagEnabled = featureFlagRepository.isEnabled(FeatureFlag.WOO_POS_ROLES)
    private val _state = MutableStateFlow<WooPosAccessSessionState>(
        if (flagEnabled) WooPosAccessSessionState.Locked(Status.Initializing) else WooPosAccessSessionState.Unprotected
    )
    val state = _state.asStateFlow()
    private var localSiteId: Int? = null
    private var generation = 0L
    internal val logoutVersion = rateLimiter.logoutVersion
    private val initialLogoutVersion = logoutVersion.value

    val isEnabled: Boolean get() = flagEnabled && _state.value != WooPosAccessSessionState.Unprotected

    fun allows(capability: WooPosCapability): Boolean =
        !flagEnabled || (isSessionCurrent && _state.value.allows(capability))

    private val isSessionCurrent: Boolean
        get() = initialLogoutVersion == logoutVersion.value && localSiteId == selectedSite.getOrNull()?.id

    internal fun approvalScope(): ApprovalScope? {
        val id = localSiteId ?: return null
        return if (_state.value is WooPosAccessSessionState.Unlocked && isSessionCurrent) {
            ApprovalScope(id, generation)
        } else {
            null
        }
    }

    internal suspend fun approvalWait(scope: ApprovalScope): Result<Long> =
        rateLimiter.remainingSeconds(scope.siteId)

    internal suspend fun approve(
        scope: ApprovalScope,
        pin: String,
        capability: WooPosCapability,
    ): WooPosLocalRateLimiterResult {
        if (approvalScope() != scope) return WooPosLocalRateLimiterResult.Unavailable
        return rateLimiter.authenticate(scope.siteId, pin, capability)
    }

    internal data class ApprovalScope(val siteId: Int, val generation: Long)

    suspend fun initialize() {
        if (!flagEnabled) return
        val id = selectedSite.getOrNull()?.id
        if (id != localSiteId) {
            localSiteId = id
            _state.value = WooPosAccessSessionState.Locked(Status.Initializing)
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
        val previous = _state.value as? WooPosAccessSessionState.Locked ?: return
        if (previous.status == Status.Initializing || previous.status == Status.Checking) return
        val id = localSiteId ?: return
        val request = ++generation
        _state.value = WooPosAccessSessionState.Locked(Status.Checking)
        try {
            val role = recoveryRole(id)
            currentCoroutineContext().ensureActive()
            if (isCurrent(request, id)) {
                if (role == null) _state.value = previous else completeRecovery(id, request, previous, role)
            }
        } catch (e: CancellationException) {
            if (isCurrent(request, id)) _state.value = previous
            throw e
        }
    }

    private suspend fun completeRecovery(
        id: Int,
        request: Long,
        previous: WooPosAccessSessionState.Locked,
        role: WooPosLocalRole,
    ) {
        val reset = rateLimiter.resetAfterRecovery(id)
        currentCoroutineContext().ensureActive()
        if (isCurrent(request, id)) {
            _state.value = if (reset.isSuccess) WooPosAccessSessionState.Unlocked(role) else previous
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun recoveryRole(id: Int): WooPosLocalRole? = try {
        recovery.authenticate(id).getOrNull()?.takeIf { it.hasCapability(WooPosCapability.MANAGE_STORE_SETTINGS) }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        WooLog.e(WooLog.T.POS, "Local PIN recovery authentication failed: ${e.javaClass.simpleName}")
        null
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
        val result = try {
            action(id)
        } catch (e: CancellationException) {
            if (isCurrent(request, id)) _state.value = previous
            throw e
        }
        currentCoroutineContext().ensureActive()
        if (!isCurrent(request, id)) return
        _state.value = when (result) {
            is WooPosLocalRateLimiterResult.Authenticated -> WooPosAccessSessionState.Unlocked(result.role)
            is WooPosLocalRateLimiterResult.Incorrect -> WooPosAccessSessionState.Locked(
                if (result.remainingSeconds > 0) Status.Wait(result.remainingSeconds) else Status.Incorrect,
            )
            is WooPosLocalRateLimiterResult.Waiting ->
                WooPosAccessSessionState.Locked(Status.Wait(result.remainingSeconds))
            WooPosLocalRateLimiterResult.ProtectionOff -> WooPosAccessSessionState.Unprotected
            WooPosLocalRateLimiterResult.Unavailable -> WooPosAccessSessionState.Locked(Status.StorageUnavailable)
        }
    }

    private fun isCurrent(request: Long, id: Int): Boolean =
        request == generation && id == selectedSite.getOrNull()?.id
}
