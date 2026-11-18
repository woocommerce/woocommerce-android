package com.woocommerce.android.ui.woopos.roles.pin

import com.woocommerce.android.ui.woopos.roles.WooPosCapability
import com.woocommerce.android.ui.woopos.roles.WooPosLocalRole
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyNoInteractions

@OptIn(ExperimentalCoroutinesApi::class)
class WooPosPINAuthenticatorTest {
    @get:Rule
    val coroutineRule = WooPosCoroutineTestRule()
    private val authenticator = WooPosPINAuthenticator(coroutineRule.testDispatchers)

    @Test
    fun `given invalid PIN formats, when authenticating, then hashes are not compared`() = runTest {
        // GIVEN
        val hash: WooPosPINHash = mock()
        val pins = mapOf(WooPosLocalRole.ADMIN to hash)

        // WHEN
        listOf("", "123", "12345", "12a4", "１２３４", "١٢٣٤", " 123", "123\n").forEach { pin ->
            assertThat(authenticator.authenticate(pins, pin).getOrThrow()).isNull()
        }

        // THEN
        verifyNoInteractions(hash)
    }

    @Test
    fun `given a stored role PIN, when authenticating, then the role and its capabilities are returned`() = runTest {
        // GIVEN
        val pins = mapOf(WooPosLocalRole.CASHIER to WooPosPINHash.create("1234"))

        // WHEN
        val role = authenticator.authenticate(pins, "1234").getOrThrow()

        // THEN
        assertThat(role).isEqualTo(WooPosLocalRole.CASHIER)
        assertThat(role?.hasCapability(WooPosCapability.VIEW_ORDERS)).isTrue()
        assertThat(role?.hasCapability(WooPosCapability.ISSUE_REFUNDS)).isFalse()
    }

    @Test
    fun `given ambiguous matching PINs, when authenticating, then no role is selected`() = runTest {
        // GIVEN
        val hash = WooPosPINHash.create("1234")
        val pins = mapOf(WooPosLocalRole.ADMIN to hash, WooPosLocalRole.CASHIER to hash)

        // THEN
        assertThat(authenticator.authenticate(pins, "1234").getOrThrow()).isNull()
    }
}
