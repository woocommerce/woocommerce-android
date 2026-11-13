package com.woocommerce.android.ui.woopos.roles.pin

import com.woocommerce.android.ui.woopos.roles.WooPosCapability
import com.woocommerce.android.ui.woopos.roles.WooPosLocalRole
import com.woocommerce.android.ui.woopos.roles.WooPosStaff
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

    @Test
    fun `given invalid PIN formats, when authenticating, then no hashes are compared`() = runTest {
        // GIVEN
        val hash: WooPosPINHash = mock()
        val source = object : WooPosPINCredentialSource {
            override suspend fun credentials(localSiteId: Int) = Result.success(
                listOf(WooPosPINCredential(WooPosStaff(setOf(WooPosCapability.VIEW_ORDERS)), hash))
            )
        }
        val authenticator = WooPosPINAuthenticator(source, coroutineRule.testDispatchers)

        // WHEN
        listOf("", "123", "12345", "12a4", "１２３４", "١٢٣٤", " 123", "123\n").forEach { pin ->
            assertThat(authenticator.authenticate(1, pin).getOrThrow()).isNull()
        }

        // THEN
        verifyNoInteractions(hash)
    }

    @Test
    fun `given a replacement source, when authenticating, then supplied capabilities are used`() = runTest {
        // GIVEN
        val staff = WooPosStaff(setOf(WooPosCapability.VIEW_ORDERS))
        val source = object : WooPosPINCredentialSource {
            override suspend fun credentials(localSiteId: Int) = Result.success(
                listOf(WooPosPINCredential(staff, WooPosPINHash.create("1234")))
            )
        }

        // WHEN
        val result = WooPosPINAuthenticator(source, coroutineRule.testDispatchers).authenticate(1, "1234")

        // THEN
        assertThat(result.getOrThrow()).isEqualTo(staff)
        assertThat(staff.hasCapability(WooPosCapability.VIEW_ORDERS)).isTrue()
        assertThat(staff.hasCapability(WooPosCapability.ISSUE_REFUNDS)).isFalse()
    }

    @Test
    fun `given ambiguous matching credentials, when authenticating, then no staff is selected`() = runTest {
        // GIVEN
        val hash = WooPosPINHash.create("1234")
        val source = object : WooPosPINCredentialSource {
            override suspend fun credentials(localSiteId: Int) = Result.success(
                listOf(
                    WooPosPINCredential(WooPosStaff(WooPosLocalRole.ADMIN.capabilities), hash),
                    WooPosPINCredential(WooPosStaff(WooPosLocalRole.CASHIER.capabilities), hash),
                )
            )
        }

        // THEN
        assertThat(
            WooPosPINAuthenticator(source, coroutineRule.testDispatchers)
                .authenticate(1, "1234").getOrThrow()
        ).isNull()
    }
}
