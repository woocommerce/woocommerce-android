package com.woocommerce.android.ui.orders.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.designsystem.foundation.WooDesignSystemThemeWithBackground
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OrderSummaryRowTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun givenWideContainer_whenOrderSummaryRowIsRendered_thenDashboardArrangementIsPreserved() {
        val posLabel = InstrumentationRegistry.getInstrumentation()
            .targetContext
            .getString(R.string.pos_badge)

        composeTestRule.setContent {
            WooDesignSystemThemeWithBackground {
                Box(modifier = Modifier.width(380.dp)) {
                    OrderSummaryRow(
                        order = orderSummaryRowModel(isPosOrder = true),
                        onClick = {},
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("#1001").assertIsDisplayed()
        composeTestRule.onNodeWithText("Jane Doe").assertIsDisplayed()
        composeTestRule.onNodeWithText("2026-05-01").assertIsDisplayed()
        composeTestRule.onNodeWithText("processing").assertIsDisplayed()
        composeTestRule.onNodeWithText(posLabel).assertIsDisplayed()
        composeTestRule.onNodeWithText("12.34 USD").assertIsDisplayed()
        assertTextDoesNotExist("Jane Doe  2026-05-01")

        val number = boundsFor("#1001")
        val date = boundsFor("2026-05-01")
        val customer = boundsFor("Jane Doe")
        val status = boundsFor("processing")
        val pos = boundsFor(posLabel)
        val total = boundsFor("12.34 USD")

        assertTrue("Expected date to stay on the same left/top row as the order number", date.left > number.right)
        assertTrue("Expected status tag to stay on the right/top side of the row", status.left > date.right)
        assertTrue("Expected POS tag to stay to the right of the status tag", pos.left > status.right)
        assertTrue("Expected customer name below the order number", customer.top > number.bottom)
        assertTrue("Expected total below the status row", total.top > status.bottom)
        assertTrue("Expected total to stay right-aligned below the status row", total.right >= pos.right)
    }

    @Test
    fun givenNarrowContainer_whenOrderSummaryRowIsRendered_thenMetadataUsesCompactLine() {
        composeTestRule.setContent {
            WooDesignSystemThemeWithBackground {
                Box(modifier = Modifier.width(332.dp)) {
                    OrderSummaryRow(
                        order = orderSummaryRowModel(),
                        onClick = {},
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("#1001").assertIsDisplayed()
        composeTestRule.onNodeWithText("Jane Doe  2026-05-01").assertIsDisplayed()
        assertTextDoesNotExist("Jane Doe")
        assertTextDoesNotExist("2026-05-01")
    }

    private fun boundsFor(text: String): Rect {
        val nodes = composeTestRule
            .onAllNodesWithText(text, useUnmergedTree = true)
            .fetchSemanticsNodes()
        assertTrue("Expected to find node with text $text", nodes.isNotEmpty())
        return nodes.first().boundsInRoot
    }

    private fun assertTextDoesNotExist(text: String) {
        val nodes = composeTestRule
            .onAllNodesWithText(text)
            .fetchSemanticsNodes()
        assertTrue("Expected text not to exist: $text", nodes.isEmpty())
    }

    private fun orderSummaryRowModel(isPosOrder: Boolean = false) = OrderSummaryRowModel(
        number = "#1001",
        date = "2026-05-01",
        customerName = "Jane Doe",
        status = "processing",
        statusColor = R.color.tag_bg_processing,
        totalPrice = "12.34 USD",
        isPosOrder = isPosOrder,
    )
}
