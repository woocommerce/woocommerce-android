package com.woocommerce.android.ui.woopos.settings.details.security

import com.woocommerce.android.ui.woopos.roles.lock.WooPosLockPreferences

sealed interface WooPosSettingsSecurityState {
    data object Loading : WooPosSettingsSecurityState
    data object Unavailable : WooPosSettingsSecurityState
    data object Restricted : WooPosSettingsSecurityState
    data object Off : WooPosSettingsSecurityState
    data class Protected(
        val hasCashierPIN: Boolean,
        val removalFailed: Boolean,
        val lockPreferences: WooPosLockPreferences,
    ) : WooPosSettingsSecurityState
}
