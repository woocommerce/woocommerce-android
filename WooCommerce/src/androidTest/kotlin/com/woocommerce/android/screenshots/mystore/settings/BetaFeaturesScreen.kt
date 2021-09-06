package com.woocommerce.android.screenshots.mystore.settings

import com.woocommerce.android.R
import com.woocommerce.android.screenshots.util.Screen

class BetaFeaturesScreen : Screen {
    companion object {
        const val PRODUCT_ADDONS_SWITCH = R.id.switchAddonsToggle
    }

    constructor() : super(PRODUCT_ADDONS_SWITCH)

    fun enableProductAddons(): BetaFeaturesScreen {
        flipSwitchOn(R.id.switchSetting_switch, PRODUCT_ADDONS_SWITCH)
        return BetaFeaturesScreen()
    }

    fun goBackToSettingsScreen(): SettingsScreen {
        pressBack()
        return SettingsScreen()
    }
}
