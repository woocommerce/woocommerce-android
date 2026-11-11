package com.woocommerce.android.ui.woopos.lock

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.roles.WooPosPINAuthenticator
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
class WooPosPendingPINRecoveryTest {
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
                on { isEnabled(FeatureFlag.WOO_POS_MANUAL_LOCK) }.thenReturn(true)
            }
            val site: SelectedSite = mock { on { getOrNull() }.thenReturn(SiteModel().apply { id = 1 }) }
            val session = WooPosLockSession(flags, site, access, fixture.lockedPOS)
            session.initialize()
            repeat(5) { repeat(WooPosLockScreenState.PIN_LENGTH) { session.enterDigit(0) } }
            assertThat(session.state.value.remainingSeconds).isEqualTo(30)
            val records = fixture.records.toMap()
            val credentials = fixture.credentials.getOrThrow().toList()
            val markers = fixture.markedStores.toSet()

            // WHEN
            assertThat(pending.authenticate(1).isFailure).isTrue()
            session.recover()

            // THEN
            assertUnavailable(session)
            assertThat(session.state.value.remainingSeconds).isEqualTo(30)
            assertThat(fixture.records).isEqualTo(records)
            assertThat(fixture.credentials.getOrThrow()).isEqualTo(credentials)
            assertThat(fixture.markedStores).isEqualTo(markers)
            fixture.elapsed += 30_000
            session.refreshWait()
            session.recover()
            assertUnavailable(session)
            assertThat(fixture.records).isEqualTo(records)
            assertThat(fixture.markedStores).isEqualTo(markers)
            "1234".forEach { session.enterDigit(it.digitToInt()) }
            assertThat(session.state.value.activeOperator).isEqualTo(fixture.admin)
            assertThat(session.state.value.isLocked).isFalse()
        }

    @Test
    fun `given unavailable local clock and storage, when pending recovery runs, then neither is accessed`() = runTest {
        // GIVEN
        val fixture = WooPosPINAccessTestFixture(coroutineRule.testDispatchers)
        var storageCalls = 0
        var clockCalls = 0
        val storage = object : WooPosPINAttemptStorage {
            override fun read(localSiteId: Int): WooPosPINAttemptState {
                storageCalls++
                error("Recovery must fail before reading local attempts")
            }
            override fun write(localSiteId: Int, state: WooPosPINAttemptState) {
                storageCalls++
                error("Recovery must fail before writing local attempts")
            }
            override fun clearAll() {
                storageCalls++
                error("Recovery must not erase local attempts")
            }
        }
        val clock = object : WooPosPINClock {
            override fun now(): WooPosPINTime {
                clockCalls++
                error("Unavailable boot identity")
            }
        }
        val access = WooPosPINAccess(
            fixture.source,
            WooPosPINAuthenticator(fixture.source, coroutineRule.testDispatchers),
            storage,
            clock,
            WooPosPendingPINRecovery(),
            coroutineRule.testDispatchers,
        )

        // THEN
        assertThat(access.recover(1)).isEqualTo(WooPosPINAccessResult.RecoveryUnavailable)
        assertThat(storageCalls).isZero()
        assertThat(clockCalls).isZero()
        assertThat(fixture.credentialReads).isZero()
    }

    private fun access(fixture: WooPosPINAccessTestFixture, recovery: WooPosPINRecovery) = WooPosPINAccess(
        fixture.source,
        WooPosPINAuthenticator(fixture.source, coroutineRule.testDispatchers),
        fixture.storage,
        fixture.clock,
        recovery,
        coroutineRule.testDispatchers,
    )

    private fun assertUnavailable(session: WooPosLockSession) {
        assertThat(session.state.value.isLocked).isTrue()
        assertThat(session.state.value.activeOperator).isNull()
        assertThat(session.state.value.error).isEqualTo(WooPosLockError.RECOVERY_UNAVAILABLE)
    }
}
