package com.woocommerce.android.ui.woopos.roles.lock

data class WooPosLockPreferences(
    val locksWhenPOSOpens: Boolean = true,
    val locksInBackground: Boolean = true,
    val locksAfterSale: Boolean = false,
    val inactivityTimeout: WooPosInactivityLockTimeout = WooPosInactivityLockTimeout.FIVE_MINUTES,
)
