package com.woocommerce.android.ui.woopos.roles

data class WooPosStaff(val capabilities: Set<WooPosCapability>) {
    fun hasCapability(capability: WooPosCapability): Boolean = capability in capabilities
}
