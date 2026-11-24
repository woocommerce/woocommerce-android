package com.woocommerce.android.ui.woopos.roles.lock

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.FeatureFlagRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import kotlinx.coroutines.test.runCurrent
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel

@OptIn(ExperimentalCoroutinesApi::class)
class WooPosRestorePOSTest {
    @get:Rule val coroutineRule = WooPosCoroutineTestRule()
    private val preferences: WooPosLockPreferences = mock()
    private var site = SiteModel().apply { id = 1 }
    private val selectedSite: SelectedSite = mock { on { getOrNull() }.thenAnswer { site } }
    private val flags: FeatureFlagRepository = mock {
        on { isEnabled(FeatureFlag.WOO_POS_ROLES) }.thenReturn(true)
    }
    private val limiter: WooPosLocalRateLimiter = mock {
        on { logoutVersion }.thenReturn(MutableStateFlow(0L))
    }
    private val restore = WooPosRestorePOS(preferences, selectedSite, flags, limiter)

    @Test
    fun `given unreadable restore state, when protection is enabled, then launcher restores locked POS`() = runTest {
        // GIVEN
        whenever(preferences.restoreSiteId()).thenReturn(Result.failure(IllegalStateException()))
        whenever(limiter.isProtectionEnabled(1)).thenReturn(Result.success(true))

        // THEN
        assertThat(restore()).isTrue()
    }

    @Test
    fun `given unreadable PINs and a saved POS, when launching, then main content stays unavailable`() = runTest {
        // GIVEN
        whenever(preferences.restoreSiteId()).thenReturn(Result.success(1))
        whenever(limiter.isProtectionEnabled(1)).thenReturn(Result.failure(IllegalStateException()))

        // THEN
        assertThat(restore()).isTrue()
    }

    @Test
    fun `given another store marker, when launching, then that POS is not restored`() = runTest {
        // GIVEN
        whenever(preferences.restoreSiteId()).thenReturn(Result.success(2))

        // THEN
        assertThat(restore()).isFalse()
        verify(preferences).clearRestoreState(eq(2), any())
    }

    @Test
    fun `given protection removed, when launching a saved POS, then its marker is cleared`() = runTest {
        // GIVEN
        whenever(preferences.restoreSiteId()).thenReturn(Result.success(1))
        whenever(limiter.isProtectionEnabled(1)).thenReturn(Result.success(false))

        // THEN
        assertThat(restore()).isFalse()
        verify(preferences).clearRestoreState(eq(1), any())
    }

    @Test
    fun `given Roles off, when launching, then old restoration is cleared`() = runTest {
        // GIVEN
        whenever(flags.isEnabled(FeatureFlag.WOO_POS_ROLES)).thenReturn(false)

        // THEN
        assertThat(restore()).isFalse()
        verify(preferences).clearRestoreState()
    }

    @Test
    fun `given a store changes during the PIN read, when the old result returns, then it cannot restore POS`() = runTest {
        // GIVEN
        val gate = CompletableDeferred<Unit>()
        whenever(preferences.restoreSiteId()).thenReturn(Result.success(1))
        whenever(limiter.isProtectionEnabled(1)).doSuspendableAnswer {
            gate.await()
            Result.success(true)
        }
        val result = async { restore() }
        runCurrent()

        // WHEN
        site = SiteModel().apply { id = 2 }
        gate.complete(Unit)

        // THEN
        assertThat(result.await()).isFalse()
    }
}
