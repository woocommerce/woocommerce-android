package com.woocommerce.android.ui.woopos.lock

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.FeatureFlagRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel

@OptIn(ExperimentalCoroutinesApi::class)
class WooPosRestoreLockedPOSTest {
    @get:Rule
    val coroutineRule = WooPosCoroutineTestRule()
    private val flags: FeatureFlagRepository = mock {
        on { isEnabled(FeatureFlag.WOO_POS_MANUAL_LOCK) }.thenReturn(true)
    }
    private val selectedSite: SelectedSite = mock {
        on { getOrNull() }.thenReturn(SiteModel().apply { id = 1 })
    }

    @Test
    fun `given marked protected POS, when normal startup resolves, then restore POS before management content`() = runTest {
        // GIVEN
        val fixture = fixture().apply { markedStores.add(1) }

        // THEN
        assertThat(restore(fixture)()).isTrue()
    }

    @Test
    fun `given no PINs or no marker, when normal startup resolves, then preserve unrestricted launch`() = runTest {
        // GIVEN
        val fixture = fixture()
        assertThat(restore(fixture)()).isFalse()
        fixture.markedStores.add(1)
        fixture.credentials = Result.success(emptyList())

        // THEN
        assertThat(restore(fixture)()).isFalse()
        assertThat(fixture.markedStores).isEmpty()
    }

    @Test
    fun `given disabled flag or missing store, when normal startup resolves, then normal startup is preserved`() = runTest {
        // GIVEN
        val fixture = fixture().apply { markedStores.add(1) }
        whenever(flags.isEnabled(FeatureFlag.WOO_POS_MANUAL_LOCK)).thenReturn(false)
        assertThat(restore(fixture)()).isFalse()

        // WHEN
        whenever(flags.isEnabled(FeatureFlag.WOO_POS_MANUAL_LOCK)).thenReturn(true)
        whenever(selectedSite.getOrNull()).thenReturn(null)

        // THEN
        assertThat(restore(fixture)()).isFalse()
    }

    @Test
    fun `given corrupt marker or unreadable credentials, when resolving marked POS, then fail closed into POS lock`() = runTest {
        // GIVEN
        val fixture = fixture().apply { markerError = true }
        assertThat(restore(fixture)()).isTrue()

        // WHEN
        fixture.markerError = false
        fixture.markedStores.add(1)
        fixture.credentials = Result.failure(IllegalStateException())

        // THEN
        assertThat(restore(fixture)()).isTrue()
    }

    @Test
    fun `given store switch during startup read, when result returns, then old store cannot force the new store into POS`() = runTest {
        // GIVEN
        val fixture = fixture().apply { markedStores.add(1) }
        val gate = CompletableDeferred<Unit>()
        fixture.credentialReadGate = gate
        val result = async { restore(fixture)() }
        runCurrent()

        // WHEN
        whenever(selectedSite.getOrNull()).thenReturn(SiteModel().apply { id = 2 })
        gate.complete(Unit)

        // THEN
        assertThat(result.await()).isFalse()
    }

    @Test
    fun `given A to protected marked B during credentials read, when resolving startup, then B is checked and stays protected`() = runTest {
        // GIVEN
        val fixture = fixture().apply { markedStores.addAll(listOf(1, 2)) }
        val gate = CompletableDeferred<Unit>()
        fixture.credentialReadGate = gate
        val result = async { restore(fixture)() }
        runCurrent()

        // WHEN
        whenever(selectedSite.getOrNull()).thenReturn(SiteModel().apply { id = 2 })
        gate.complete(Unit)

        // THEN
        assertThat(result.await()).isTrue()
        assertThat(fixture.credentialReads).isEqualTo(2)
    }

    @Test
    fun `when probing normal launcher, then only enabled marked stores require the restore gate`() {
        // GIVEN
        val fixture = fixture()
        val restore = restore(fixture)
        assertThat(restore.needsCheck()).isFalse()

        // WHEN
        fixture.markedStores.add(1)

        // THEN
        assertThat(restore.needsCheck()).isTrue()
        whenever(flags.isEnabled(FeatureFlag.WOO_POS_MANUAL_LOCK)).thenReturn(false)
        assertThat(restore.needsCheck()).isFalse()
    }

    private fun fixture() = WooPosPINAccessTestFixture(coroutineRule.testDispatchers)
    private fun restore(fixture: WooPosPINAccessTestFixture) =
        WooPosRestoreLockedPOS(selectedSite, flags, fixture.source, fixture.lockedPOS)
}
