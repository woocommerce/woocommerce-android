package com.woocommerce.android.ui.woopos.roles.lock

import androidx.annotation.AnyThread
import androidx.annotation.MainThread
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.cardreader.WooPosCardReaderFacade
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
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@MainThread
@ActivityRetainedScoped
class WooPosAccessSession @Inject constructor(
    featureFlagRepository: FeatureFlagRepository,
    private val selectedSite: SelectedSite,
    private val rateLimiter: WooPosLocalRateLimiter,
    private val recovery: WooPosPINRecovery,
    private val lockPreferences: WooPosLockPreferences,
    private val cardReaderFacade: WooPosCardReaderFacade,
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

    val paymentState get() = cardReaderFacade.paymentState
    val isPaymentPending: Boolean get() = cardReaderFacade.isPaymentPending
    val canLock: Boolean get() = isEnabled && !isPaymentPending

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
        if (!flagEnabled) {
            lockPreferences.clearRestoreState()
            return
        }
        if (_state.value == WooPosAccessSessionState.Exiting) return
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
        if (isPaymentPending && _state.value.isLocked) cardReaderFacade.cancelPayment()
        val protection = rateLimiter.isProtectionEnabled(id)
        if (!isCurrent(request, id)) return
        val retained = if (protection.getOrNull() == true) {
            lockPreferences.settings(id).first().fold(
                onSuccess = { lockPreferences.retainPOS(id) { isCurrent(request, id) } },
                onFailure = { Result.failure(it) },
            )
        } else if (protection.getOrNull() == false) {
            lockPreferences.clearRestoreState(id) { isCurrent(request, id) }
        } else {
            Result.success(Unit)
        }
        if (!isCurrent(request, id)) return
        _state.value = when {
            protection.getOrNull() == false -> WooPosAccessSessionState.Unprotected
            retained.isFailure || protection.isFailure ->
                WooPosAccessSessionState.Locked(WooPosPINEntryStatus.StorageUnavailable)
            _state.value is WooPosAccessSessionState.Unlocked -> _state.value
            else -> WooPosAccessSessionState.Locked(WooPosPINEntryStatus.Checking)
        }
        if (_state.value.isLocked) refreshWait()
    }

    internal fun retireIfContextChanged() {
        if (!isSessionCurrent && _state.value is WooPosAccessSessionState.Unlocked) {
            generation.value++
            _state.value = WooPosAccessSessionState.Locked(WooPosPINEntryStatus.Initializing)
        }
    }

    internal fun blockForStorageFailure() {
        if (!flagEnabled) return
        generation.value++
        _state.value = WooPosAccessSessionState.Locked(WooPosPINEntryStatus.StorageUnavailable)
    }

    suspend fun lock() {
        if (!canLock) return
        lockSession()
    }

    suspend fun lockOnBackground() {
        lockSession()
        if (_state.value.isLocked) cardReaderFacade.cancelPayment()
    }

    private suspend fun lockSession() {
        if (!isEnabled || _state.value == WooPosAccessSessionState.Exiting ||
            _state.value.status == WooPosPINEntryStatus.StorageUnavailable
        ) return
        generation.value++
        _state.value = WooPosAccessSessionState.Locked(WooPosPINEntryStatus.Checking)
        refreshWait()
    }

    suspend fun unlock(pin: String) {
        if (!flagEnabled || _state.value == WooPosAccessSessionState.Exiting) return
        if (isPaymentPending) {
            if (_state.value == WooPosAccessSessionState.Unprotected) {
                initialize()
                if (isEnabled) lockOnBackground()
            }
            return
        }
        val previous = _state.value
        if (previous is WooPosAccessSessionState.Unlocked) return
        if (previous.isLocked && !previous.status.canEnterPIN &&
            previous.status != WooPosPINEntryStatus.Initializing
        ) return
        authenticate { id -> rateLimiter.authenticate(id, pin) }
    }

    suspend fun recover() {
        if (isPaymentPending) return
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
        val preferences = lockPreferences.settings(id).first()
        if (!isCurrent(request, id)) return
        if (preferences.isFailure) {
            _state.value = WooPosAccessSessionState.Locked(WooPosPINEntryStatus.StorageUnavailable)
            return
        }
        val retained = lockPreferences.retainPOS(id) { isCurrent(request, id) }
        if (!isCurrent(request, id)) return
        if (retained.isFailure) {
            _state.value = WooPosAccessSessionState.Locked(WooPosPINEntryStatus.StorageUnavailable)
            return
        }
        val reset = rateLimiter.resetAfterRecovery(id)
        currentCoroutineContext().ensureActive()
        if (isCurrent(request, id) && !isPaymentPending) {
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
        if (!isCurrent(request, id) || isPaymentPending) return
        if (result is WooPosLocalRateLimiterResult.Authenticated &&
            (previous == WooPosAccessSessionState.Unprotected || previous.status == WooPosPINEntryStatus.Initializing)
        ) {
            val retained = lockPreferences.settings(id).first().fold(
                onSuccess = { lockPreferences.retainPOS(id) { isCurrent(request, id) } },
                onFailure = { Result.failure(it) },
            )
            if (!isCurrent(request, id) || isPaymentPending) return
            if (retained.isFailure) {
                _state.value = WooPosAccessSessionState.Locked(WooPosPINEntryStatus.StorageUnavailable)
                return
            }
        }
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

    suspend fun exitPOS(onExit: () -> Unit) {
        if (!flagEnabled) {
            onExit()
            return
        }
        if (isPaymentPending || _state.value.isLocked) return
        val id = selectedSite.getOrNull()?.id
        val protectionEnabled = isEnabled
        val request = ++generation.value
        _state.value = WooPosAccessSessionState.Exiting
        val cleared = lockPreferences.clearRestoreState(id) {
            request == generation.value && id == selectedSite.getOrNull()?.id &&
                initialLogoutVersion == logoutVersion.value
        }
        currentCoroutineContext().ensureActive()
        if (request != generation.value || id != selectedSite.getOrNull()?.id ||
            initialLogoutVersion != logoutVersion.value
        ) return
        if (cleared.isFailure && protectionEnabled) {
            _state.value = WooPosAccessSessionState.Locked(WooPosPINEntryStatus.StorageUnavailable)
        } else {
            onExit()
        }
    }

    private fun isCurrent(request: Long, id: Int): Boolean =
        request == generation.value && id == selectedSite.getOrNull()?.id &&
            initialLogoutVersion == logoutVersion.value
}
