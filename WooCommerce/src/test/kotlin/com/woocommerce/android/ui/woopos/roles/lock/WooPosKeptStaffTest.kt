package com.woocommerce.android.ui.woopos.roles.lock

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Lifecycle.State.CREATED
import androidx.lifecycle.Lifecycle.State.STARTED
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ProcessLifecycleOwner
import com.woocommerce.android.ui.woopos.roles.WooPosLocalRole
import com.woocommerce.android.ui.woopos.util.analytics.WooPosElapsedTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class WooPosKeptStaffTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val processLifecycle = ProcessLifecycleOwner.get().lifecycle as LifecycleRegistry
    private val preferences = MutableStateFlow(WooPosLockPreferences(locksWhenPOSOpens = false))
    private val lockSettings: WooPosLockSettings = mock { on { preferences(SITE_ID) }.thenReturn(preferences) }
    private val clock: WooPosElapsedTime = mock { on { milliseconds() }.thenReturn(0) }
    private lateinit var keptStaff: WooPosKeptStaff

    @Before
    fun setUp() {
        processLifecycle.currentState = STARTED
        keptStaff = WooPosKeptStaff(lockSettings, clock)
    }

    @After
    fun tearDown() {
        processLifecycle.currentState = CREATED
    }

    @Test
    fun `given launch locking is off, when POS reopens within the timeout, then the staff resumes`() = runTest {
        // GIVEN
        keptStaff.keep(SITE_ID, WooPosLocalRole.CASHIER, LOGOUT_VERSION)
        whenever(clock.milliseconds()).thenReturn(FIVE_MINUTES - 1)

        // WHEN
        val role = keptStaff.resume(SITE_ID, LOGOUT_VERSION)

        // THEN
        assertThat(role).isEqualTo(WooPosLocalRole.CASHIER)
    }

    @Test
    fun `given launch locking is on, when POS reopens, then no staff resumes`() = runTest {
        // GIVEN
        preferences.value = preferences.value.copy(locksWhenPOSOpens = true)
        keptStaff.keep(SITE_ID, WooPosLocalRole.CASHIER, LOGOUT_VERSION)

        // WHEN
        val role = keptStaff.resume(SITE_ID, LOGOUT_VERSION)

        // THEN
        assertThat(role).isNull()
    }

    @Test
    fun `given the timeout passed, when POS reopens, then no staff resumes`() = runTest {
        // GIVEN
        keptStaff.keep(SITE_ID, WooPosLocalRole.CASHIER, LOGOUT_VERSION)
        whenever(clock.milliseconds()).thenReturn(FIVE_MINUTES)

        // WHEN
        val role = keptStaff.resume(SITE_ID, LOGOUT_VERSION)

        // THEN
        assertThat(role).isNull()
    }

    @Test
    fun `given the timeout is Never, when POS reopens much later, then the staff resumes`() = runTest {
        // GIVEN
        preferences.value = preferences.value.copy(inactivityTimeout = WooPosInactivityLockTimeout.NEVER)
        keptStaff.keep(SITE_ID, WooPosLocalRole.CASHIER, LOGOUT_VERSION)
        whenever(clock.milliseconds()).thenReturn(FIVE_MINUTES * 100)

        // WHEN
        val role = keptStaff.resume(SITE_ID, LOGOUT_VERSION)

        // THEN
        assertThat(role).isEqualTo(WooPosLocalRole.CASHIER)
    }

    @Test
    fun `given background locking is on, when the app went to background before reopening, then no staff resumes`() =
        runTest {
            // GIVEN
            keptStaff.keep(SITE_ID, WooPosLocalRole.CASHIER, LOGOUT_VERSION)
            processLifecycle.currentState = CREATED

            // WHEN
            val role = keptStaff.resume(SITE_ID, LOGOUT_VERSION)

            // THEN
            assertThat(role).isNull()
        }

    @Test
    fun `given background locking is on, when POS closes while the app is in background, then no staff resumes`() =
        runTest {
            // GIVEN
            processLifecycle.currentState = CREATED
            keptStaff.keep(SITE_ID, WooPosLocalRole.CASHIER, LOGOUT_VERSION)

            // WHEN
            val role = keptStaff.resume(SITE_ID, LOGOUT_VERSION)

            // THEN
            assertThat(role).isNull()
        }

    @Test
    fun `given background locking is off, when the app went to background before reopening, then the staff resumes`() =
        runTest {
            // GIVEN
            preferences.value = preferences.value.copy(locksInBackground = false)
            keptStaff.keep(SITE_ID, WooPosLocalRole.CASHIER, LOGOUT_VERSION)
            processLifecycle.currentState = CREATED

            // WHEN
            val role = keptStaff.resume(SITE_ID, LOGOUT_VERSION)

            // THEN
            assertThat(role).isEqualTo(WooPosLocalRole.CASHIER)
        }

    @Test
    fun `given staff kept before a logout, when POS reopens, then no staff resumes`() = runTest {
        // GIVEN
        keptStaff.keep(SITE_ID, WooPosLocalRole.CASHIER, LOGOUT_VERSION)

        // WHEN
        val role = keptStaff.resume(SITE_ID, LOGOUT_VERSION + 1)

        // THEN
        assertThat(role).isNull()
    }

    @Test
    fun `given staff kept for another store, when POS reopens, then no staff resumes`() = runTest {
        // GIVEN
        keptStaff.keep(SITE_ID + 1, WooPosLocalRole.CASHIER, LOGOUT_VERSION)

        // WHEN
        val role = keptStaff.resume(SITE_ID, LOGOUT_VERSION)

        // THEN
        assertThat(role).isNull()
    }

    @Test
    fun `given staff resumed once, when POS reopens again, then no staff resumes`() = runTest {
        // GIVEN
        keptStaff.keep(SITE_ID, WooPosLocalRole.CASHIER, LOGOUT_VERSION)
        keptStaff.resume(SITE_ID, LOGOUT_VERSION)

        // WHEN
        val role = keptStaff.resume(SITE_ID, LOGOUT_VERSION)

        // THEN
        assertThat(role).isNull()
    }

    private companion object {
        const val SITE_ID = 1
        const val LOGOUT_VERSION = 3L
        const val FIVE_MINUTES = 5 * 60_000L
    }
}
