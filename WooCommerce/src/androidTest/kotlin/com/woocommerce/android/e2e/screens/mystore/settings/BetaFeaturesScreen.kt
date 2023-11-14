package com.woocommerce.android.e2e.screens.mystore.settings

import com.woocommerce.android.R
import com.woocommerce.android.e2e.helpers.util.Screen

class BetaFeaturesScreen : Screen {
    constructor() : super(R.id.switchAddonsToggle)

    fun enableProductAddons(): BetaFeaturesScreen {
        flipSwitchOn(R.id.switchSetting_switch, R.id.switchAddonsToggle)
        return BetaFeaturesScreen()
    }

    fun goBackToSettingsScreen(): SettingsScreen {
        pressBack()
        return SettingsScreen()
    }
}
