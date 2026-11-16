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
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class WooPosPINAuthenticatorTest {
    @get:Rule
    val coroutineRule = WooPosCoroutineTestRule()
    private val store: WooPosLocalPINStore = mock()
    private val authenticator = WooPosPINAuthenticator(store, coroutineRule.testDispatchers)

    @Test
    fun `given invalid PIN formats, when authenticating, then the store is not read`() = runTest {
        // WHEN
        listOf("", "123", "12345", "12a4", "１２３４", "١٢٣٤", " 123", "123\n").forEach { pin ->
            assertThat(authenticator.authenticate(1, pin).getOrThrow()).isNull()
        }

        // THEN
        verifyNoInteractions(store)
    }

    @Test
    fun `given a stored role PIN, when authenticating, then the role and its capabilities are returned`() = runTest {
        // GIVEN
        whenever(store.getPINs(1)).thenReturn(
            Result.success(mapOf(WooPosLocalRole.CASHIER to WooPosPINHash.create("1234")))
        )

        // WHEN
        val role = authenticator.authenticate(1, "1234").getOrThrow()

        // THEN
        assertThat(role).isEqualTo(WooPosLocalRole.CASHIER)
        assertThat(role?.hasCapability(WooPosCapability.VIEW_ORDERS)).isTrue()
        assertThat(role?.hasCapability(WooPosCapability.ISSUE_REFUNDS)).isFalse()
    }

    @Test
    fun `given ambiguous matching PINs, when authenticating, then no role is selected`() = runTest {
        // GIVEN
        val hash = WooPosPINHash.create("1234")
        whenever(store.getPINs(1)).thenReturn(
            Result.success(mapOf(WooPosLocalRole.ADMIN to hash, WooPosLocalRole.CASHIER to hash))
        )

        // THEN
        assertThat(authenticator.authenticate(1, "1234").getOrThrow()).isNull()
    }
}
