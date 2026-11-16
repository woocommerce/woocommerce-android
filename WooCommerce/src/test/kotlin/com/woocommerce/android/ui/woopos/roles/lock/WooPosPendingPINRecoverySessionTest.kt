package com.woocommerce.android.ui.woopos.roles.lock

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.roles.pin.WooPosPINAuthenticator
import com.woocommerce.android.ui.woopos.roles.pin.WooPosPINHash
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
            val fixture = WooPosPINAccessTestFixture(coroutineRule.testDispatchers)
            val pending = WooPosPendingPINRecovery()
            val access = access(fixture, pending)
            val flags: FeatureFlagRepository = mock {
                on { isEnabled(FeatureFlag.WOO_POS_ROLES) }.thenReturn(true)
            }
            val site: SelectedSite = mock { on { getOrNull() }.thenReturn(SiteModel().apply { id = 1 }) }
            val session = WooPosLockSession(flags, site, access)
            session.initialize()
            repeat(5) {
                repeat(WooPosPINHash.PIN_LENGTH) { session.enterDigit(0) }
            }
            assertThat(session.state.value.remainingSeconds).isEqualTo(30)
            val records = fixture.records.toMap()
            val pins = fixture.pins.getOrThrow().toMap()

            // WHEN
            assertThat(pending.authenticate(1).isFailure).isTrue()
            session.recover()

            // THEN
            assertUnavailable(session)
            assertThat(session.state.value.remainingSeconds).isEqualTo(30)
            assertThat(fixture.records).isEqualTo(records)
            assertThat(fixture.pins.getOrThrow()).isEqualTo(pins)
            fixture.elapsed += 30_000
            session.refreshWait()
            session.recover()
            assertUnavailable(session)
            assertThat(fixture.records).isEqualTo(records)
            "1234".forEach { session.enterDigit(it.digitToInt()) }
            assertThat(session.activeRole).isEqualTo(fixture.admin)
            assertThat(session.state.value.isLocked).isFalse()
        }

    private fun access(fixture: WooPosPINAccessTestFixture, recovery: WooPosPINRecovery) = WooPosPINAccess(
        fixture.store,
        WooPosPINAuthenticator(fixture.store, coroutineRule.testDispatchers),
        fixture.storage,
        fixture.clock,
        recovery,
        coroutineRule.testDispatchers,
    )

    private fun assertUnavailable(session: WooPosLockSession) {
        assertThat(session.state.value.isLocked).isTrue()
        assertThat(session.activeRole).isNull()
        assertThat(session.state.value.error).isNotEqualTo(WooPosLockError.STORAGE)
    }
}
