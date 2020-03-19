package com.woocommerce.android.screenshots.mystore.settings

import com.woocommerce.android.R
import com.woocommerce.android.screenshots.mystore.MyStoreScreen
import com.woocommerce.android.screenshots.util.Screen

class SettingsScreen : Screen {
    companion object {
        const val BETA_FEATURES_BUTTON = R.id.textBetaFeatures
        const val LOG_OUT_BUTTON = R.id.buttonLogout
    }

    constructor(): super(LOG_OUT_BUTTON)

    fun openBetaFeatures(): BetaFeaturesScreen {
        clickOn(BETA_FEATURES_BUTTON)
        return BetaFeaturesScreen()
    }

    fun goBackToMyStoreScreen(): MyStoreScreen {
        pressBack()
        return MyStoreScreen()
    }

    fun logOut() {
        if (!isElementCompletelyDisplayed(LOG_OUT_BUTTON)) {
            scrollTo(LOG_OUT_BUTTON)
        }

        clickOn(LOG_OUT_BUTTON)

        // Confirm Log Out
        clickButtonInDialogWithTitle(R.string.signout)
    }
}
