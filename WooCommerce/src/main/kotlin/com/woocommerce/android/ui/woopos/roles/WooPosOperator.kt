package com.woocommerce.android.ui.woopos.roles

data class WooPosOperator(val permissions: Set<WooPosPermission>) {
    fun allows(permission: WooPosPermission): Boolean = permission in permissions
}
