package com.woocommerce.android.e2e.screens.orders

import android.widget.EditText
import androidx.test.espresso.Espresso
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.UiObject2
import com.woocommerce.android.R
import com.woocommerce.android.e2e.helpers.util.ComposeUiAutomator
import com.woocommerce.android.e2e.helpers.util.OrderData
import com.woocommerce.android.e2e.helpers.util.Screen
import com.woocommerce.android.e2e.helpers.util.allText
import com.woocommerce.android.e2e.helpers.util.composeTestTagWithNumericSuffix
import com.woocommerce.android.e2e.screens.shared.FilterScreen
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

    fun openSearchPane(): OrderListScreen {
        if (composeUi.find(SEARCH_FIELD_SELECTOR) == null) {
            composeUi.waitForTag(OrderListTestTags.SEARCH_ACTION, "Orders search action").click()
        }
        searchTextField()
        return this
    }

    fun enterSearchTerm(term: String): OrderListScreen {
        replaceSearchTerm(term)
        waitForOrderRows(minimumCount = 1)
        return this
    }

    fun enterAbsentSearchTerm(term: String): OrderListScreen {
        replaceSearchTerm(term)
        composeUi.waitForTag(OrderListTestTags.EMPTY, "empty Orders state")
        return this
    }

    fun leaveOrClearSearchMode(): OrderListScreen {
        // to support test on tablets - search bar is displayed on split screen
        // clearing search bar so test can continue in a clean state
        if (Screen.isElementDisplayed(R.id.orderDetail_container)) {
            searchTextField().clear()
            return this
        } // to support test on phones
        else if (composeUi.find(SEARCH_FIELD_SELECTOR) != null) {
            closeSearchMode()
        }
        return this
    }

    fun tapFilters(): FilterScreen {
        composeUi.waitForTag(OrderListTestTags.FILTERS, "Orders filters").click()
        return FilterScreen()
    }

    fun createFABTap(): UnifiedOrderScreen {
        composeUi.waitForTag(OrderListTestTags.CREATE_ORDER_FAB, "Create order button").click()
        return UnifiedOrderScreen()
    }

    fun waitForOrders(): OrderListScreen {
        composeUi.waitForTag(OrderListTestTags.LIST, "Orders list")
        waitForOrderRows(minimumCount = 1)
        return this
    }

    fun waitForEmptyState(): OrderListScreen {
        composeUi.waitForTag(OrderListTestTags.EMPTY, "empty Orders state")
        return this
    }

    fun assertOrderCard(order: OrderData): OrderListScreen {
        scrollToOrder(order.id)
        val row = composeUi.waitFor(By.res(OrderListTestTags.orderRow(order.id.toLong())), "order ${order.id}")
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
        composeUi.waitForCount(
            selector = ORDER_ROW_SELECTOR,
            expectedCount = count,
            description = "order rows",
        )

        return this
    }

    fun assertSearchResultsAbsent(term: String): OrderListScreen {
        val expectedString = "We're sorry, we couldn't find results for \"${term}\""
        composeUi.waitFor(By.textContains(expectedString), "empty search result for $term")
        return this
    }

    private fun replaceSearchTerm(term: String) {
        searchTextField().setText(term)
        Espresso.closeSoftKeyboard()
    }

    private fun searchTextField(): UiObject2 {
        var result: UiObject2? = null
        composeUi.waitUntil(
            condition = {
                result = findSearchTextField()
                result != null
            },
            failureMessage = { "Orders search text field was not found" },
        )
        return requireNotNull(result) { "Orders search text field was not found" }
    }

    private fun findSearchTextField(): UiObject2? {
        return composeUi.find(SEARCH_FIELD_SELECTOR)?.findObject(SEARCH_TEXT_FIELD_SELECTOR)
    }

    private fun closeSearchMode() {
        val searchField = composeUi.waitFor(SEARCH_FIELD_SELECTOR, "Orders search field")
        val cancelText = getInstrumentation().targetContext.getString(R.string.cancel)
        val cancelAction = searchField.findObject(
            By.clickable(true).hasDescendant(By.text(cancelText))
        )
        checkNotNull(cancelAction) { "Orders search cancel action was not found" }.click()
        composeUi.waitForTag(OrderListTestTags.SEARCH_ACTION, "Orders search action")
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
        val SEARCH_FIELD_SELECTOR: BySelector = By.res(OrderListTestTags.SEARCH_FIELD)
        val SEARCH_TEXT_FIELD_SELECTOR: BySelector = By.clazz(EditText::class.java)
        val ORDER_ROW_SELECTOR: BySelector = composeTestTagWithNumericSuffix(OrderListTestTags.ORDER_ROW_PREFIX)
    }
}
