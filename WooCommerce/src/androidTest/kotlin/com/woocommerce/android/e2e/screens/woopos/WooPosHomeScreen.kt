package com.woocommerce.android.e2e.screens.woopos

import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.woocommerce.android.e2e.helpers.util.ComposeScreen
import com.woocommerce.android.ui.woopos.util.WooPosTestTags

class WooPosHomeScreen : ComposeScreen() {
    fun waitForLoad(): WooPosHomeScreen {
        Thread.sleep(1000)
        return this
    }

    fun addProductsToCart(composeTestRule: ComposeTestRule): WooPosHomeScreen {
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag(WooPosTestTags.PRODUCT_ITEM).fetchSemanticsNodes().isNotEmpty()
        }

        val productNodes = composeTestRule.onAllNodesWithTag(WooPosTestTags.PRODUCT_ITEM)
        val nodes = productNodes.fetchSemanticsNodes()

        require(nodes.isNotEmpty()) { "No clickable products found in the product list" }

        productNodes[0].performClick()
        Thread.sleep(200)

        productNodes[2].performClick()
        Thread.sleep(200)

        return this
    }

    fun proceedToCheckout(composeTestRule: ComposeTestRule): WooPosTotalsScreen {
        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule.onAllNodesWithTag(WooPosTestTags.CHECKOUT_BUTTON)
                .fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithTag(WooPosTestTags.CHECKOUT_BUTTON)
            .performClick()

        Thread.sleep(2000)
        return WooPosTotalsScreen()
    }
}
