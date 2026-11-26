package com.woocommerce.android.ui.woopos.roles.lock

import androidx.annotation.MainThread
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.util.analytics.WooPosElapsedTime
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** Locks POS on the triggers that the store's Security settings turn on. */
@MainThread
@ActivityRetainedScoped
class WooPosAutoLock @Inject constructor(
    private val accessSession: WooPosAccessSession,
    private val lockSettings: WooPosLockSettings,
    private val selectedSite: SelectedSite,
    private val clock: WooPosElapsedTime,
) {
    private val userActivity = MutableStateFlow(0L)
    private var inactivityDeadline: Long? = null
    private val _isPaymentInProgress = MutableStateFlow(false)
    val isPaymentInProgress: StateFlow<Boolean> = _isPaymentInProgress.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun lockAfterInactivity() {
        selectedSite.observe()
            .map { it?.id }
            .distinctUntilChanged()
            .flatMapLatest { id -> if (id == null) emptyFlow() else lockSettings.preferences(id) }
            .flatMapLatest { preferences ->
                combine(accessSession.state, isPaymentInProgress, userActivity) { session, isPaying, _ ->
                    preferences.inactivityTimeout.duration.takeIf {
                        session is WooPosAccessSessionState.Unlocked && !isPaying
                    }
                }
            }
            .collectLatest { timeout ->
                inactivityDeadline = timeout?.let { clock.milliseconds() + it.inWholeMilliseconds }
                if (timeout == null) return@collectLatest
                delay(timeout)
                lockAfterDeadline()
            }
    }

    // The timer's delay pauses in deep sleep, so the deadline is checked again when the app returns.
    suspend fun onAppForegrounded() {
        if (inactivityDeadline?.let { clock.milliseconds() >= it } == true) lockAfterDeadline()
    }

    private suspend fun lockAfterDeadline() {
        inactivityDeadline = null
        // Locking changes the state that restarts the timer, so it must finish first.
        withContext(NonCancellable) { accessSession.lock() }
    }

    fun onUserActivity() {
        userActivity.update { it + 1 }
    }

    fun onPaymentInProgressChanged(isInProgress: Boolean) {
        _isPaymentInProgress.value = isInProgress
    }

    suspend fun onAppBackgrounded() {
        if (preferences().locksInBackground) lockUnlockedSession()
    }

    suspend fun onNewOrderStarted() {
        if (preferences().locksAfterSale) lockUnlockedSession()
    }

    suspend fun lockManually() {
        if (!isPaymentInProgress.value) accessSession.lock()
    }

    private suspend fun preferences(): WooPosLockPreferences =
        selectedSite.getOrNull()?.id?.let { lockSettings.preferences(it).first() } ?: WooPosLockPreferences()

    private suspend fun lockUnlockedSession() {
        if (accessSession.state.value is WooPosAccessSessionState.Unlocked) accessSession.lock()
    }
}
