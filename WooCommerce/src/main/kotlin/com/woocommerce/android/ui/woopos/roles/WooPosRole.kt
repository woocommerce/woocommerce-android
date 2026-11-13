package com.woocommerce.android.ui.woopos.roles

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class WooPosRole(
    val permissions: Set<WooPosPermission>,
) {
    @SerialName("admin")
    ADMIN(
        permissions = WooPosPermission.entries.toSet(),
    ),

    @SerialName("cashier")
    CASHIER(
        permissions = setOf(
            WooPosPermission.PROCESS_SALES,
            WooPosPermission.VIEW_ORDERS,
            WooPosPermission.APPLY_COUPONS,
        ),
    ),
}
