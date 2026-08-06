package com.woocommerce.android.e2e.screens.shared

import androidx.test.espresso.Espresso
import androidx.test.espresso.matcher.ViewMatchers
import com.woocommerce.android.R
import com.woocommerce.android.e2e.helpers.util.Screen
import com.woocommerce.android.e2e.screens.orders.OrderListScreen
import com.woocommerce.android.e2e.screens.products.ProductListScreen
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.Matchers

class FilterScreen : Screen {
    constructor() : super(R.id.toolbar)

    fun filterByPropertyAndValue(property: String, value: String): FilterScreen {
        clickByTextAndId(property, R.id.filterItemName)

        val valueCell = Espresso.onView(
            Matchers.allOf(
                ViewMatchers.withText(containsString(value)),
                Matchers.anyOf(
                    ViewMatchers.withId(R.id.filterOptionItem_name),
                    ViewMatchers.withId(R.id.filterOptionNameTextView),
                )
            )
        )

        clickOn(valueCell)
        return this
    }

    fun clearFilters(): FilterScreen {
        clickOn(R.id.menu_clear)
        return this
    }

    fun showProducts(): ProductListScreen {
        val showProductButton = Espresso.onView(
            Matchers.allOf(
                Matchers.anyOf(
                    ViewMatchers.withId(R.id.filterList_btnShowProducts),
                    ViewMatchers.withId(R.id.filterOptionList_btnShowProducts)
                ),
                ViewMatchers.isCompletelyDisplayed(),
            )
        )

        clickOn(showProductButton)

        return ProductListScreen()
    }

    fun showOrders(expectResults: Boolean): OrderListScreen {
        clickOn(R.id.showOrdersButton)

        val orderListScreen = OrderListScreen()
        return if (expectResults) {
            orderListScreen.waitForOrders()
        } else {
            orderListScreen.waitForEmptyState()
        }
    }

    fun leaveFilterScreenToProducts(): ProductListScreen {
        if (isElementDisplayed(R.id.filterList) || isElementDisplayed(R.id.filterOptionList)) {
            showProducts()
        }

        return ProductListScreen()
    }

    fun leaveFilterScreenToOrders(): OrderListScreen {
        if (isElementDisplayed(R.id.filterList)) {
            showOrders(false)
        }

        return OrderListScreen()
    }
}
