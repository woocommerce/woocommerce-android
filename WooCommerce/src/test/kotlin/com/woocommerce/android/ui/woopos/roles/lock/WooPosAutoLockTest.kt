package com.woocommerce.android.ui.woopos.roles.lock

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.roles.WooPosLocalRole
import com.woocommerce.android.ui.woopos.roles.pin.WooPosPINEntryStatus
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import com.woocommerce.android.ui.woopos.util.analytics.WooPosElapsedTime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel

@OptIn(ExperimentalCoroutinesApi::class)
class WooPosAutoLockTest {
    @get:Rule
    val coroutineRule = WooPosCoroutineTestRule(StandardTestDispatcher())

    private val sessionState = MutableStateFlow<WooPosAccessSessionState>(
        WooPosAccessSessionState.Unlocked(WooPosLocalRole.CASHIER)
    )
    private val accessSession: WooPosAccessSession = mock { on { state }.thenReturn(sessionState) }
    private val preferences = MutableStateFlow(
        WooPosLockPreferences(inactivityTimeout = WooPosInactivityLockTimeout.ONE_MINUTE)
    )
    private val lockSettings: WooPosLockSettings = mock { on { preferences(SITE_ID) }.thenReturn(preferences) }
    private val site = SiteModel().apply { id = SITE_ID }
    private val selectedSite: SelectedSite = mock {
        on { observe() }.thenReturn(flowOf(site))
        on { getOrNull() }.thenReturn(site)
    }
    private val clock: WooPosElapsedTime = mock { on { milliseconds() }.thenReturn(0) }
    private val autoLock = WooPosAutoLock(accessSession, lockSettings, selectedSite, clock)

    @Test
    fun `given unlocked staff, when the timeout passes without activity, then POS locks`() = runTest {
        // GIVEN
        startInactivityTimer()

        // WHEN
        advanceTimeBy(ONE_MINUTE + 1)

        // THEN
        verify(accessSession).lock()
    }

    @Test
    fun `given unlocked staff, when they interact before the timeout, then the full timeout restarts`() = runTest {
        // GIVEN
        startInactivityTimer()
        advanceTimeBy(ONE_MINUTE - 1_000)

        // WHEN
        autoLock.onUserActivity()
        advanceTimeBy(ONE_MINUTE - 1_000)

        // THEN
        verify(accessSession, never()).lock()
        advanceTimeBy(1_001)
        verify(accessSession).lock()
    }

    @Test
    fun `given a new timeout is chosen, when the old timeout passes, then the new timeout counts from the change`() =
        runTest {
            // GIVEN
            startInactivityTimer()
            advanceTimeBy(ONE_MINUTE - 1_000)

            // WHEN
            preferences.value = preferences.value.copy(inactivityTimeout = WooPosInactivityLockTimeout.TWO_MINUTES)
            advanceTimeBy(ONE_MINUTE)

            // THEN
            verify(accessSession, never()).lock()
            advanceTimeBy(ONE_MINUTE + 1)
            verify(accessSession).lock()
        }

    @Test
    fun `given the device slept past the timeout, when the app returns, then POS locks`() = runTest {
        // GIVEN
        startInactivityTimer()
        whenever(clock.milliseconds()).thenReturn(ONE_MINUTE)

        // WHEN
        autoLock.onAppForegrounded()

        // THEN
        verify(accessSession).lock()
    }

    @Test
    fun `given the timeout has not passed, when the app returns, then POS stays unlocked`() = runTest {
        // GIVEN
        startInactivityTimer()
        whenever(clock.milliseconds()).thenReturn(ONE_MINUTE - 1)

        // WHEN
        autoLock.onAppForegrounded()

        // THEN
        verify(accessSession, never()).lock()
    }

    @Test
    fun `given a payment in progress, when the timeout passes, then POS stays unlocked`() = runTest {
        // GIVEN
        startInactivityTimer()
        autoLock.onPaymentInProgressChanged(true)

        // WHEN
        advanceTimeBy(ONE_MINUTE * 2)

        // THEN
        verify(accessSession, never()).lock()
    }

    @Test
    fun `given an almost expired timeout, when a payment ends, then a full interval starts`() = runTest {
        // GIVEN
        startInactivityTimer()
        advanceTimeBy(ONE_MINUTE - 1_000)
        autoLock.onPaymentInProgressChanged(true)
        runCurrent()
        advanceTimeBy(ONE_MINUTE * 2)
        whenever(clock.milliseconds()).thenReturn(ONE_MINUTE * 3)
        autoLock.onAppForegrounded()
        verify(accessSession, never()).lock()

        // WHEN
        autoLock.onPaymentInProgressChanged(false)
        runCurrent()
        advanceTimeBy(ONE_MINUTE - 1)
        runCurrent()

        // THEN
        verify(accessSession, never()).lock()
        advanceTimeBy(1)
        runCurrent()
        verify(accessSession).lock()
    }

    @Test
    fun `given a running timeout, when Never is selected, then its deadline no longer locks POS`() = runTest {
        // GIVEN
        startInactivityTimer()
        advanceTimeBy(ONE_MINUTE - 1_000)

        // WHEN
        preferences.value = preferences.value.copy(inactivityTimeout = WooPosInactivityLockTimeout.NEVER)
        runCurrent()
        advanceTimeBy(ONE_MINUTE * 20)
        whenever(clock.milliseconds()).thenReturn(ONE_MINUTE * 20)
        autoLock.onAppForegrounded()

        // THEN
        verify(accessSession, never()).lock()
    }

    @Test
    fun `given the timeout is Never, when time passes, then POS stays unlocked`() = runTest {
        // GIVEN
        preferences.value = preferences.value.copy(inactivityTimeout = WooPosInactivityLockTimeout.NEVER)
        startInactivityTimer()

        // WHEN
        advanceTimeBy(ONE_MINUTE * 20)

        // THEN
        verify(accessSession, never()).lock()
    }

    @Test
    fun `given the timeout fires, when locking changes the session, then the lock still finishes`() = runTest {
        // GIVEN
        whenever { accessSession.lock() }.doSuspendableAnswer {
            sessionState.value = WooPosAccessSessionState.Locked(WooPosPINEntryStatus.Checking)
            delay(1)
            sessionState.value = WooPosAccessSessionState.Locked(WooPosPINEntryStatus.Idle)
        }
        startInactivityTimer()

        // WHEN
        advanceTimeBy(ONE_MINUTE + 10)

        // THEN
        assertThat(sessionState.value).isEqualTo(WooPosAccessSessionState.Locked(WooPosPINEntryStatus.Idle))
    }

    @Test
    fun `given background locking is on, when the app goes to background, then POS locks`() = runTest {
        // WHEN
        autoLock.onAppBackgrounded()

        // THEN
        verify(accessSession).lock()
    }

    @Test
    fun `given background locking is off, when the app goes to background, then POS stays unlocked`() = runTest {
        // GIVEN
        preferences.value = preferences.value.copy(locksInBackground = false)

        // WHEN
        autoLock.onAppBackgrounded()

        // THEN
        verify(accessSession, never()).lock()
    }

    @Test
    fun `given POS is already locked, when the app goes to background, then the lock is not restarted`() = runTest {
        // GIVEN
        sessionState.value = WooPosAccessSessionState.Locked(WooPosPINEntryStatus.Wait(30))

        // WHEN
        autoLock.onAppBackgrounded()

        // THEN
        verify(accessSession, never()).lock()
    }

    @Test
    fun `given after-sale locking is on, when a new order starts, then POS locks`() = runTest {
        // GIVEN
        preferences.value = preferences.value.copy(locksAfterSale = true)

        // WHEN
        autoLock.onNewOrderStarted()

        // THEN
        verify(accessSession).lock()
    }

    @Test
    fun `given after-sale locking is off, when a new order starts, then POS stays unlocked`() = runTest {
        // WHEN
        autoLock.onNewOrderStarted()

        // THEN
        verify(accessSession, never()).lock()
    }

    @Test
    fun `given a payment in progress, when locking manually, then POS stays unlocked`() = runTest {
        // GIVEN
        autoLock.onPaymentInProgressChanged(true)

        // WHEN
        autoLock.lockManually()

        // THEN
        verify(accessSession, never()).lock()
    }

    private fun TestScope.startInactivityTimer() {
        backgroundScope.launch { autoLock.lockAfterInactivity() }
        runCurrent()
    }

    private companion object {
        const val SITE_ID = 1
        const val ONE_MINUTE = 60_000L
    }
}
