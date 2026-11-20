package com.woocommerce.android.e2e.screens.orders

import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.UiObject2
import com.woocommerce.android.R
import com.woocommerce.android.e2e.helpers.util.ComposeUiAutomator
import com.woocommerce.android.e2e.helpers.util.Screen
import com.woocommerce.android.e2e.helpers.util.composeTestTagWithNumericSuffix
import com.woocommerce.android.ui.orders.list.OrderListTestTags

class OrderListScreen : Screen(R.id.order_list_compose_container) {
    private val composeUi = ComposeUiAutomator()

    init {
        composeUi.waitForTag(OrderListTestTags.SCREEN, "Orders screen")
    }

    fun selectOrder(index: Int): SingleOrderScreen {
        waitForOrderRows(minimumCount = index + 1)[index].click()
        return SingleOrderScreen()
    }

    fun selectOrderById(orderId: Int): SingleOrderScreen {
        scrollToOrder(orderId)
        composeUi.waitFor(By.res(OrderListTestTags.orderRow(orderId.toLong())), "order $orderId").click()
        return SingleOrderScreen()
    }

    fun createFABTap(): UnifiedOrderScreen {
        composeUi.waitForTag(OrderListTestTags.CREATE_ORDER_FAB, "Create order button").click()
        return UnifiedOrderScreen()
    }

    private fun scrollToOrder(orderId: Int) {
        composeUi.scrollTextIntoView(OrderListTestTags.LIST, "#$orderId")
    }

    private fun waitForOrderRows(minimumCount: Int): List<UiObject2> = composeUi.waitForAtLeast(
        selector = ORDER_ROW_SELECTOR,
        minimumCount = minimumCount,
        description = "order rows",
    )

    private companion object {
        val ORDER_ROW_SELECTOR: BySelector = composeTestTagWithNumericSuffix(OrderListTestTags.ORDER_ROW_PREFIX)
    }
}
