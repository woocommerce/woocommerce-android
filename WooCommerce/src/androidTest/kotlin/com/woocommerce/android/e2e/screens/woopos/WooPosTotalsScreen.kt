package com.woocommerce.android.e2e.screens.woopos

import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.performClick
import com.woocommerce.android.e2e.helpers.util.ComposeScreen

class WooPosTotalsScreen : ComposeScreen() {
    fun selectCashPayment(composeTestRule: ComposeTestRule): WooPosTotalsScreen {
        composeTestRule.waitUntil(timeoutMillis = 15000) {
            composeTestRule.onAllNodes(hasText("Cash payment") and hasClickAction())
                .fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNode(hasText("Cash payment") and hasClickAction())
            .performClick()

        Thread.sleep(1000)
        return this
    }

    fun completePayment(composeTestRule: ComposeTestRule): WooPosPaymentSuccessScreen {
        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule.onAllNodes(hasText("Mark payment as complete") and hasClickAction())
                .fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNode(hasText("Mark payment as complete") and hasClickAction())
            .performClick()

        Thread.sleep(2000)
        return WooPosPaymentSuccessScreen()
    }
}
