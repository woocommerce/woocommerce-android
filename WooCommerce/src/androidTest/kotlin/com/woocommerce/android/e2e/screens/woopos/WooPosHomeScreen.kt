package com.woocommerce.android.e2e.screens.woopos

import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.performClick
import com.woocommerce.android.R
import com.woocommerce.android.e2e.helpers.util.Screen

class WooPosHomeScreen : Screen(R.id.point_of_sale) {
    fun waitForLoad(): WooPosHomeScreen {
        Thread.sleep(3000)
        return this
    }

    fun addProductsToCart(composeTestRule: ComposeTestRule): WooPosHomeScreen {
        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule.onAllNodes(hasClickAction()).fetchSemanticsNodes().isNotEmpty()
        }

        val clickableNodes = composeTestRule.onAllNodes(hasClickAction())
        val nodes = clickableNodes.fetchSemanticsNodes()

        require(nodes.isNotEmpty()) { "No clickable products found in the product list" }

        clickableNodes[0].performClick()
        Thread.sleep(500)

        return this
    }

    fun proceedToCheckout(composeTestRule: ComposeTestRule): WooPosTotalsScreen {
        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule.onAllNodes(hasText("Check out") and hasClickAction())
                .fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNode(hasText("Check out") and hasClickAction())
            .performClick()

        Thread.sleep(1000)
        return WooPosTotalsScreen()
    }
}
