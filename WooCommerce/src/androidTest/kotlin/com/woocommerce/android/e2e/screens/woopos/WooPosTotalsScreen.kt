package com.woocommerce.android.e2e.screens.woopos

import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.woocommerce.android.e2e.helpers.util.ComposeScreen
import com.woocommerce.android.ui.woopos.util.WooPosTestTags

class WooPosTotalsScreen : ComposeScreen() {
    fun waitForLoad(composeTestRule: ComposeTestRule): WooPosTotalsScreen {
        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule.onAllNodesWithTag(WooPosTestTags.CASH_PAYMENT_BUTTON)
                .fetchSemanticsNodes().isNotEmpty()
        }
        return this
    }

    fun selectCashPayment(composeTestRule: ComposeTestRule): WooPosCashPaymentScreen {
        composeTestRule.onNodeWithTag(WooPosTestTags.CASH_PAYMENT_BUTTON)
            .performClick()
        return WooPosCashPaymentScreen()
    }
}
