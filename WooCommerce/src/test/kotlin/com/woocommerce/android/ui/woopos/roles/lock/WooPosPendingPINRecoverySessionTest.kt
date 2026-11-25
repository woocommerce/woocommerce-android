package com.woocommerce.android.ui.woopos.roles.lock

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.roles.pin.WooPosPINEntryStatus
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.FeatureFlagRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.wordpress.android.fluxc.model.SiteModel

@OptIn(ExperimentalCoroutinesApi::class)
class WooPosPendingPINRecoverySessionTest {
    @get:Rule
    val coroutineRule = WooPosCoroutineTestRule()

    @Test
    fun `given a waiting lock, when pending recovery is requested, then authority and durable protection stay unchanged`() =
        runTest {
            // GIVEN
            val fixture = WooPosLocalRateLimiterTestFixture(coroutineRule.testDispatchers)
            val pending = WooPosPendingPINRecovery()
            val access = fixture.rateLimiter()
            val flags: FeatureFlagRepository = mock {
                on { isEnabled(FeatureFlag.WOO_POS_ROLES) }.thenReturn(true)
            }
            val site: SelectedSite = mock { on { getOrNull() }.thenReturn(SiteModel().apply { id = 1 }) }
            val session =
                WooPosAccessSession(flags, site, access, pending, fixture.lockPreferences, fixture.cardReaderFacade)
            session.initialize()
            repeat(5) { session.unlock("0000") }
            assertThat(session.state.value.status).isEqualTo(WooPosPINEntryStatus.Wait(30))
            val records = fixture.records.toMap()
            val pins = fixture.pins.getOrThrow().toMap()

            // WHEN
            assertThat(pending.authenticate(1).isFailure).isTrue()
            session.recover()

            // THEN
            assertUnavailable(session)
            assertThat(session.state.value.status).isEqualTo(WooPosPINEntryStatus.Wait(30))
            assertThat(fixture.records).isEqualTo(records)
            assertThat(fixture.pins.getOrThrow()).isEqualTo(pins)
            fixture.elapsed += 30_000
            session.refreshWait()
            session.recover()
            assertUnavailable(session)
            assertThat(fixture.records).isEqualTo(records)
            session.unlock("1234")
            assertThat(session.state.value.activeRole).isEqualTo(fixture.admin)
            assertThat(session.state.value.isLocked).isFalse()
        }

    private fun assertUnavailable(session: WooPosAccessSession) {
        assertThat(session.state.value.isLocked).isTrue()
        assertThat(session.state.value.activeRole).isNull()
        assertThat(session.state.value.status).isNotEqualTo(WooPosPINEntryStatus.StorageUnavailable)
    }
}
