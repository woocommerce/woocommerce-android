package com.woocommerce.android.ui.woopos.roles.lock

import androidx.annotation.AnyThread
import androidx.annotation.MainThread
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.roles.WooPosCapability
import com.woocommerce.android.ui.woopos.roles.WooPosLocalRole
import com.woocommerce.android.ui.woopos.roles.pin.WooPosPINEntryStatus
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.FeatureFlagRepository
import com.woocommerce.android.util.WooLog
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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
        if (flagEnabled) {
            WooPosAccessSessionState.Locked(WooPosPINEntryStatus.Initializing)
        } else {
            WooPosAccessSessionState.Unprotected
        }
    )
    val state = _state.asStateFlow()

    // Store commit guards read the session off main; mutations stay on main.
    @Volatile private var localSiteId: Int? = null
    private val generation = MutableStateFlow(0L)
    private val logoutVersion = rateLimiter.logoutVersion
    private val initialLogoutVersion = logoutVersion.value
    internal val approvalScopeChanges = combine(state, generation, logoutVersion) { _, _, _ -> Unit }

    val isEnabled: Boolean get() = flagEnabled && _state.value != WooPosAccessSessionState.Unprotected

    @AnyThread
    fun allows(capability: WooPosCapability): Boolean =
        !flagEnabled || (isSessionCurrent && _state.value.allows(capability))

    @get:AnyThread
    private val isSessionCurrent: Boolean
        get() = initialLogoutVersion == logoutVersion.value && localSiteId == selectedSite.getOrNull()?.id

    @AnyThread
    internal fun approvalScope(): ApprovalScope? {
        val id = localSiteId ?: return null
        return if (_state.value is WooPosAccessSessionState.Unlocked && isSessionCurrent) {
            ApprovalScope(id, generation.value)
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
            _state.value = WooPosAccessSessionState.Locked(WooPosPINEntryStatus.Initializing)
        }
        val request = ++generation.value
        if (id == null) {
            _state.value = WooPosAccessSessionState.Locked(WooPosPINEntryStatus.StorageUnavailable)
            return
        }
        val protection = rateLimiter.isProtectionEnabled(id)
        if (!isCurrent(request, id)) return
        _state.value = when {
            protection.isFailure -> WooPosAccessSessionState.Locked(WooPosPINEntryStatus.StorageUnavailable)
            !protection.getOrThrow() -> WooPosAccessSessionState.Unprotected
            _state.value is WooPosAccessSessionState.Unlocked -> _state.value
            else -> WooPosAccessSessionState.Locked(WooPosPINEntryStatus.Checking)
        }
        if (_state.value.isLocked) refreshWait()
    }

    suspend fun lock() {
        if (!isEnabled) return
        generation.value++
        _state.value = WooPosAccessSessionState.Locked(WooPosPINEntryStatus.Checking)
        refreshWait()
    }

    suspend fun unlock(pin: String) {
        if (!flagEnabled || _state.value is WooPosAccessSessionState.Unlocked) return
        val status = _state.value.status
        if (status is WooPosPINEntryStatus.Wait || status == WooPosPINEntryStatus.StorageUnavailable) return
        authenticate { id -> rateLimiter.authenticate(id, pin) }
    }

    suspend fun recover() {
        val previous = _state.value as? WooPosAccessSessionState.Locked ?: return
        if (previous.status == WooPosPINEntryStatus.Initializing || previous.status == WooPosPINEntryStatus.Checking) {
            return
        }
        val id = localSiteId ?: return
        val request = ++generation.value
        _state.value = WooPosAccessSessionState.Locked(WooPosPINEntryStatus.Checking)
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
            _state.value = WooPosAccessSessionState.Locked(WooPosPINEntryStatus.StorageUnavailable)
            return
        }
        val request = generation.value
        val wait = rateLimiter.remainingSeconds(id)
        if (!isCurrent(request, id) || _state.value != current) return
        _state.value = WooPosAccessSessionState.Locked(
            when {
                wait.isFailure -> WooPosPINEntryStatus.StorageUnavailable
                wait.getOrThrow() > 0 -> WooPosPINEntryStatus.Wait(wait.getOrThrow())
                current.status == WooPosPINEntryStatus.Incorrect -> WooPosPINEntryStatus.Incorrect
                current.status == WooPosPINEntryStatus.StorageUnavailable -> WooPosPINEntryStatus.StorageUnavailable
                else -> WooPosPINEntryStatus.Idle
            },
        )
    }

    private suspend fun authenticate(action: suspend (Int) -> WooPosLocalRateLimiterResult) {
        val previous = _state.value
        val request = ++generation.value
        val id = localSiteId ?: selectedSite.getOrNull()?.id
        if (id == null || !isCurrent(request, id)) {
            _state.value = WooPosAccessSessionState.Locked(WooPosPINEntryStatus.StorageUnavailable)
            return
        }
        localSiteId = id
        if (previous.isLocked) _state.value = WooPosAccessSessionState.Locked(WooPosPINEntryStatus.Checking)
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
                if (result.remainingSeconds > 0) {
                    WooPosPINEntryStatus.Wait(result.remainingSeconds)
                } else {
                    WooPosPINEntryStatus.Incorrect
                },
            )
            is WooPosLocalRateLimiterResult.Waiting ->
                WooPosAccessSessionState.Locked(WooPosPINEntryStatus.Wait(result.remainingSeconds))
            WooPosLocalRateLimiterResult.ProtectionOff -> WooPosAccessSessionState.Unprotected
            WooPosLocalRateLimiterResult.Unavailable ->
                WooPosAccessSessionState.Locked(WooPosPINEntryStatus.StorageUnavailable)
        }
    }

    private fun isCurrent(request: Long, id: Int): Boolean =
        request == generation.value && id == selectedSite.getOrNull()?.id
}
