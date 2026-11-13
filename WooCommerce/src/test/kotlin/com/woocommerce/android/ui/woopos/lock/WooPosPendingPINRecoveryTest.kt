package com.woocommerce.android.ui.woopos.lock

import com.woocommerce.android.ui.woopos.roles.WooPosPINAuthenticator
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WooPosPendingPINRecoveryTest {
    @get:Rule
    val coroutineRule = WooPosCoroutineTestRule()

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
}
