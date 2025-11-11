package com.woocommerce.android.e2e.screens.woopos

import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import com.woocommerce.android.e2e.helpers.util.ComposeScreen
import com.woocommerce.android.ui.woopos.util.WooPosTestTags

class WooPosPaymentSuccessScreen : ComposeScreen() {
    fun waitForScreen(composeTestRule: ComposeTestRule): WooPosPaymentSuccessScreen {
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag(WooPosTestTags.NEW_ORDER_BUTTON)
                .fetchSemanticsNodes().isNotEmpty()
        }
        Thread.sleep(1000)
        return this
    }
}
