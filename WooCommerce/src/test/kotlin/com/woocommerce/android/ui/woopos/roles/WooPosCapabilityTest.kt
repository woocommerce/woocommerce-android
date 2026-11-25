package com.woocommerce.android.ui.woopos.roles

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class WooPosCapabilityTest {
    @Test
    fun `when reading capability identifiers, then they match the POS Roles spec`() {
        assertThat(WooPosCapability.entries.associateWith { it.identifier }).containsExactlyInAnyOrderEntriesOf(
            mapOf(
                WooPosCapability.PROCESS_SALES to "woocommerce_pos_process_sales",
                WooPosCapability.VIEW_ORDERS to "woocommerce_pos_view_orders",
                WooPosCapability.APPLY_COUPONS to "woocommerce_pos_apply_coupons",
                WooPosCapability.CREATE_COUPONS to "woocommerce_pos_create_coupons",
                WooPosCapability.ISSUE_REFUNDS to "woocommerce_pos_issue_refunds",
                WooPosCapability.EXIT_POS to "woocommerce_pos_exit",
                WooPosCapability.MANAGE_STORE_SETTINGS to "woocommerce_pos_manage_store_settings",
            )
        )
    }
}
