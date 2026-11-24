package com.woocommerce.android.ui.woopos.roles.lock

data class WooPosAutoLockSettings(
    val onLaunch: Boolean = true,
    val onBackground: Boolean = true,
    val afterSale: Boolean = false,
    val inactivityMinutes: Int = 5,
) {
    init {
        require(inactivityMinutes in INACTIVITY_OPTIONS)
    }

    companion object {
        val INACTIVITY_OPTIONS = listOf(0, 1, 2, 5, 10, 15)
    }
}
