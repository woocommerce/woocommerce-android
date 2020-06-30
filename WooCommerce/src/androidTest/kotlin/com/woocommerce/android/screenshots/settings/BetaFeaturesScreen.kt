package com.woocommerce.android.screenshots.settings

import com.woocommerce.android.R
import com.woocommerce.android.screenshots.util.Screen

class BetaFeaturesScreen : Screen {
    companion object {
        const val PRODUCT_EDITING_SWITCH = R.id.switchProductsUI
    }

    constructor() : super(PRODUCT_EDITING_SWITCH)

    fun enableProductEditing(): BetaFeaturesScreen {
        flipSwitchOn(R.id.switchSetting_switch, PRODUCT_EDITING_SWITCH)
        return BetaFeaturesScreen()
    }

    fun goBackToSettingsScreen(): SettingsScreen {
        pressBack()
        return SettingsScreen()
    }
}
