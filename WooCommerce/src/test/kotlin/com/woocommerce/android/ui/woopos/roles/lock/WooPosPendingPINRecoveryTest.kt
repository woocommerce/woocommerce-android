package com.woocommerce.android.ui.woopos.roles.lock

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
    fun `when pending recovery is requested, then no role is granted`() = runTest {
        assertThat(WooPosPendingPINRecovery().authenticate(1).isFailure).isTrue()
    }
}
