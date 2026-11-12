package com.woocommerce.android.ui.woopos.roles

import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WooPosPINAuthenticatorTest {
    @get:Rule
    val coroutineRule = WooPosCoroutineTestRule()

    @Test
    fun `given a replacement source, when authenticating, then supplied permissions are used`() = runTest {
        // GIVEN
        val operator = WooPosOperator(setOf(WooPosPermission.VIEW_ORDERS))
        val source = object : WooPosPINCredentialSource {
            override suspend fun credentials(localSiteId: Int) = Result.success(
                listOf(WooPosPINCredential(operator, WooPosPINHash.create("1234")))
            )
        }

        // WHEN
        val result = WooPosPINAuthenticator(source, coroutineRule.testDispatchers).authenticate(1, "1234")

        // THEN
        assertThat(result.getOrThrow()).isEqualTo(operator)
        assertThat(operator.allows(WooPosPermission.VIEW_ORDERS)).isTrue()
        assertThat(operator.allows(WooPosPermission.ISSUE_REFUNDS)).isFalse()
    }

    @Test
    fun `given ambiguous matching credentials, when authenticating, then no operator is selected`() = runTest {
        // GIVEN
        val hash = WooPosPINHash.create("1234")
        val source = object : WooPosPINCredentialSource {
            override suspend fun credentials(localSiteId: Int) = Result.success(
                listOf(
                    WooPosPINCredential(WooPosOperator(WooPosRole.ADMIN.permissions), hash),
                    WooPosPINCredential(WooPosOperator(WooPosRole.CASHIER.permissions), hash),
                )
            )
        }

        // THEN
        assertThat(
            WooPosPINAuthenticator(source, coroutineRule.testDispatchers)
                .authenticate(1, "1234").getOrThrow()
        ).isNull()
    }

    @Test
    fun `when listing permissions, then capability identifiers match the Guarding table`() {
        assertThat(WooPosPermission.entries.map { it.identifier }).containsExactly(
            "woocommerce_pos_process_sales",
            "woocommerce_pos_view_orders",
            "woocommerce_pos_apply_coupons",
            "woocommerce_pos_create_coupons",
            "woocommerce_pos_issue_refunds",
            "woocommerce_pos_exit",
            "woocommerce_pos_manage_store_settings",
        )
    }
}
