package com.woocommerce.android.ui.woopos.roles

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class WooPosLocalRole(
    val capabilities: Set<WooPosCapability>,
) {
    @SerialName("admin")
    ADMIN(
        capabilities = WooPosCapability.entries.toSet(),
    ),

    @SerialName("cashier")
    CASHIER(
        capabilities = setOf(
            WooPosCapability.PROCESS_SALES,
            WooPosCapability.VIEW_ORDERS,
            WooPosCapability.APPLY_COUPONS,
        ),
    ),
}
