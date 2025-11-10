package com.woocommerce.android.e2e.screens.woopos

import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.woocommerce.android.e2e.helpers.util.ComposeScreen
import com.woocommerce.android.ui.woopos.util.WooPosTestTags

class WooPosHomeScreen : ComposeScreen() {
    fun waitForLoad(): WooPosHomeScreen {
        Thread.sleep(3000)
        return this
    }

    fun addProductsToCart(composeTestRule: ComposeTestRule): WooPosHomeScreen {
        println("WooPosTest: Starting addProductsToCart")

        println("WooPosTest: Waiting for product items to appear...")
        composeTestRule.waitUntil(timeoutMillis = 10000) {
            val found = composeTestRule.onAllNodesWithTag(WooPosTestTags.PRODUCT_ITEM).fetchSemanticsNodes().isNotEmpty()
            println("WooPosTest: Wait loop - products found: $found")
            found
        }
        println("WooPosTest: Wait completed - products should be available")

        val productNodes = composeTestRule.onAllNodesWithTag(WooPosTestTags.PRODUCT_ITEM)
        val nodes = productNodes.fetchSemanticsNodes()
        println("WooPosTest: Found ${nodes.size} product nodes")

        require(nodes.isNotEmpty()) { "No clickable products found in the product list" }

        println("WooPosTest: Clicking product at index 0")
        productNodes[0].performClick()
        Thread.sleep(200)

        println("WooPosTest: Clicking product at index 2")
        productNodes[2].performClick()
        Thread.sleep(200)

        println("WooPosTest: Clicking product at index 5")
        productNodes[5].performClick()
        Thread.sleep(500)

        println("WooPosTest: addProductsToCart completed")
        return this
    }

    fun proceedToCheckout(composeTestRule: ComposeTestRule): WooPosTotalsScreen {
        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule.onAllNodesWithTag(WooPosTestTags.CHECKOUT_BUTTON)
                .fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithTag(WooPosTestTags.CHECKOUT_BUTTON)
            .performClick()

        Thread.sleep(1000)
        return WooPosTotalsScreen()
    }
}
