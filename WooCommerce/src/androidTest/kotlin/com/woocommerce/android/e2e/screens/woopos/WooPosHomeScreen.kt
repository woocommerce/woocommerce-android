package com.woocommerce.android.e2e.screens.woopos

import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.woocommerce.android.e2e.helpers.util.ComposeScreen
import com.woocommerce.android.ui.woopos.util.WooPosTestTags

class WooPosHomeScreen : ComposeScreen() {
    fun waitForLoad(composeTestRule: ComposeTestRule): WooPosHomeScreen {
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag(WooPosTestTags.PRODUCT_ITEM).fetchSemanticsNodes().isNotEmpty()
        }
        return this
    }

    fun addProductsToCart(composeTestRule: ComposeTestRule): WooPosHomeScreen {
        val firstProductNodes = composeTestRule.onAllNodesWithTag(WooPosTestTags.PRODUCT_ITEM)
        val firstNodes = firstProductNodes.fetchSemanticsNodes()
        require(firstNodes.isNotEmpty()) { "No clickable products found in the product list" }
        firstProductNodes[0].performClick()

        val secondProductNodes = composeTestRule.onAllNodesWithTag(WooPosTestTags.PRODUCT_ITEM)
        val secondNodes = secondProductNodes.fetchSemanticsNodes()
        require(secondNodes.size > 2) { "Not enough products in the list" }
        secondProductNodes[2].performClick()

        composeTestRule.waitForIdle()

        return this
    }

    fun proceedToCheckout(composeTestRule: ComposeTestRule): WooPosTotalsScreen {
        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule.onAllNodesWithTag(WooPosTestTags.CHECKOUT_BUTTON)
                .fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithTag(WooPosTestTags.CHECKOUT_BUTTON)
            .performClick()

        return WooPosTotalsScreen()
    }
}
