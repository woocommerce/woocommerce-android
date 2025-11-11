package com.woocommerce.android.e2e.screens.woopos

import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.woocommerce.android.e2e.helpers.util.ComposeScreen
import com.woocommerce.android.ui.woopos.util.WooPosTestTags

class WooPosCashPaymentScreen : ComposeScreen() {
    fun completePayment(composeTestRule: ComposeTestRule): WooPosPaymentSuccessScreen {
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag(WooPosTestTags.COMPLETE_PAYMENT_BUTTON)
                .fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithTag(WooPosTestTags.COMPLETE_PAYMENT_BUTTON)
            .performClick()

        Thread.sleep(8000)
        return WooPosPaymentSuccessScreen()
    }
}
