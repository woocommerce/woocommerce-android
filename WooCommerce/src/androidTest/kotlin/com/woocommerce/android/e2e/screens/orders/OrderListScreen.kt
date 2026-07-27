package com.woocommerce.android.e2e.screens.orders

import androidx.test.espresso.Espresso
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.Condition
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.UiScrollable
import androidx.test.uiautomator.UiSelector
import com.woocommerce.android.R
import com.woocommerce.android.e2e.helpers.util.OrderData
import com.woocommerce.android.e2e.helpers.util.Screen
import com.woocommerce.android.e2e.screens.shared.FilterScreen
import com.woocommerce.android.ui.orders.list.OrderListTestTags
import java.util.regex.Pattern

class OrderListScreen : Screen(R.id.order_list_compose_container) {
    private val device = UiDevice.getInstance(getInstrumentation())

    fun selectOrder(index: Int): SingleOrderScreen {
        waitForOrderRows(minimumCount = index + 1)[index].click()
        return SingleOrderScreen()
    }

    fun selectOrderById(orderId: Int): SingleOrderScreen {
        scrollToOrder(orderId)
        waitFor(By.res(OrderListTestTags.orderRow(orderId.toLong())), "order $orderId").click()
        return SingleOrderScreen()
    }

    fun openSearchPane(): OrderListScreen {
        if (!Screen.isElementFocused(androidx.appcompat.R.id.search_src_text)) {
            clickOn(R.id.menu_search)
        }
        return this
    }

    fun enterSearchTerm(term: String): OrderListScreen {
        typeTextInto(androidx.appcompat.R.id.search_src_text, term)
        waitForOrderRows(minimumCount = 1)
        return this
    }

    fun enterAbsentSearchTerm(term: String): OrderListScreen {
        typeTextInto(androidx.appcompat.R.id.search_src_text, term)
        waitFor(By.res(OrderListTestTags.EMPTY), "empty Orders state")
        return this
    }

    fun leaveOrClearSearchMode(): OrderListScreen {
        // to support test on tablets - search bar is displayed on split screen
        // clearing search bar so test can continue in a clean state
        if (Screen.isElementDisplayed(R.id.orderDetail_container)) {
            clearSearchBar(androidx.appcompat.R.id.search_src_text)
            return this
        } // to support test on phones
        else if (Screen.isElementDisplayed(androidx.appcompat.R.id.search_src_text)) {
            // Double pressBack is needed because first one only removes the focus
            // from search field, while the second one leaves the search mode.
            Espresso.pressBack()
            Espresso.pressBack()
        }
        return this
    }

    fun tapFilters(): FilterScreen {
        clickOn(R.id.btn_order_filter)
        return FilterScreen()
    }

    fun createFABTap(): UnifiedOrderScreen {
        clickOn(R.id.createOrderButton)
        return UnifiedOrderScreen()
    }

    fun waitForOrders(): OrderListScreen {
        waitFor(By.res(OrderListTestTags.LIST), "Orders list")
        waitForOrderRows(minimumCount = 1)
        return this
    }

    fun waitForEmptyState(): OrderListScreen {
        waitFor(By.res(OrderListTestTags.EMPTY), "empty Orders state")
        return this
    }

    fun assertOrderCard(order: OrderData): OrderListScreen {
        scrollToOrder(order.id)
        val row = waitFor(By.res(OrderListTestTags.orderRow(order.id.toLong())), "order ${order.id}")
        val rowText = row.allText().joinToString(" ")
        check(rowText.contains("#${order.id}")) { "Order row did not contain '#${order.id}': $rowText" }
        check(rowText.contains(order.customerName)) {
            "Order row did not contain customer '${order.customerName}': $rowText"
        }
        check(rowText.contains(order.total)) { "Order row did not contain total '${order.total}': $rowText" }
        check(rowText.contains(order.status)) { "Order row did not contain status '${order.status}': $rowText" }

        return this
    }

    fun assertOrdersCount(count: Int): OrderListScreen {
        waitUntil(
            condition = { orderRows().size == count },
            failureMessage = { "Expected $count order rows, found ${orderRows().size}" },
        )

        return this
    }

    fun assertSearchResultsAbsent(term: String): OrderListScreen {
        val expectedString = "We're sorry, we couldn't find results for \"${term}\""
        waitFor(By.textContains(expectedString), "empty search result for $term")
        return this
    }

    private fun scrollToOrder(orderId: Int) {
        waitFor(By.res(OrderListTestTags.LIST), "Orders list")
        UiScrollable(UiSelector().resourceId(OrderListTestTags.LIST)).apply {
            setAsVerticalList()
            scrollTextIntoView("#$orderId")
        }
    }

    private fun waitForOrderRows(minimumCount: Int): List<UiObject2> {
        waitUntil(
            condition = { orderRows().size >= minimumCount },
            failureMessage = {
                "Expected at least $minimumCount order rows, found ${orderRows().size}"
            },
        )
        return orderRows()
    }

    private fun orderRows(): List<UiObject2> = device.findObjects(ORDER_ROW_SELECTOR)

    private fun waitFor(selector: BySelector, description: String): UiObject2 {
        waitUntil(
            condition = { device.findObject(selector) != null },
            failureMessage = { "$description was not found" },
        )
        return requireNotNull(device.findObject(selector)) { "$description was not found" }
    }

    private fun waitUntil(
        condition: () -> Boolean,
        failureMessage: () -> String,
    ) {
        check(device.wait(Condition<UiDevice, Boolean> { condition() }, NODE_TIMEOUT_MS)) {
            failureMessage()
        }
    }

    private fun UiObject2.allText(): List<String> = buildList {
        text?.takeIf(String::isNotEmpty)?.let(::add)
        contentDescription?.takeIf(String::isNotEmpty)?.let(::add)
        children.forEach { addAll(it.allText()) }
    }

    private companion object {
        const val NODE_TIMEOUT_MS = 10_000L
        val ORDER_ROW_SELECTOR: BySelector = By.res(
            Pattern.compile("${OrderListTestTags.ORDER_ROW_PREFIX}[0-9]+")
        )
    }
}
