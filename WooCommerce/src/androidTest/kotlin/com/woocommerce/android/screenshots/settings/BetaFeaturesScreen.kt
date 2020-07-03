package com.woocommerce.android.screenshots.settings

import com.woocommerce.android.R
import com.woocommerce.android.screenshots.util.Screen

class BetaFeaturesScreen : Screen(PRODUCT_EDITING_SWITCH) {
    companion object {
        const val PRODUCT_EDITING_SWITCH = R.id.switchProductsUI
    }

    fun enableProductEditing(): BetaFeaturesScreen {
        actionOps.flipSwitchOn(R.id.switchSetting_switch, PRODUCT_EDITING_SWITCH)
        return BetaFeaturesScreen()
    }

    fun goBackToSettingsScreen(): SettingsScreen {
        actionOps.pressBack()
        return SettingsScreen()
    }
}
