package com.woocommerce.android.screenshots.orders

import androidx.test.espresso.Espresso
import androidx.test.espresso.matcher.ViewMatchers
import com.woocommerce.android.R
import com.woocommerce.android.screenshots.TabNavComponent
import com.woocommerce.android.screenshots.mystore.MyStoreScreen
import com.woocommerce.android.screenshots.util.Screen
import org.hamcrest.Matchers

class OrderListScreen : Screen {
    companion object {
        fun navigateToOrders(): OrderListScreen {
            if (!isToolbarTitle("Orders")) {
                MyStoreScreen().tabBar.gotoOrdersScreen()
            }

            Thread.sleep(1000)

            return OrderListScreen()
        }

        const val LIST_VIEW = R.id.ordersList
        const val LIST_ITEM = R.id.linearLayout
        const val SEARCH_BUTTON = R.id.menu_search
        const val SEARCH_TEXT_FIELD = R.id.search_src_text
        const val PROCESSING_TAB = "PROCESSING"
        const val ORDERS_TITLE = "Orders"
    }

    val tabBar = TabNavComponent()

    constructor() : super(LIST_VIEW)

    fun searchOrdersByName(): OrderSearchScreen {
        clickOn(SEARCH_BUTTON)
        typeTextInto(SEARCH_TEXT_FIELD, "123") // TODO replace sting with data generator
        waitForElementToBeDisplayedWithoutFailure(LIST_VIEW)
        return OrderSearchScreen()
    }

    fun goToProcessingOrders(): OrderListScreen {
        clickOn(Espresso.onView(Matchers.allOf(ViewMatchers.withText(PROCESSING_TAB))))
        return OrderListScreen()
    }

    fun goToOrdersView(): OrderListScreen {
        tabBar.gotoOrdersScreen()
        return OrderListScreen()
    }

    fun selectOrder(index: Int): SingleOrderScreen {
        val correctedIndex = index + 1 // account for the header
        selectItemAtIndexInRecyclerView(correctedIndex, LIST_VIEW, LIST_ITEM)
        return SingleOrderScreen()
    }

    fun isTitleVisible(): OrderListScreen {
        isElementCompletelyDisplayed(Espresso.onView(Matchers.allOf(ViewMatchers.withText(ORDERS_TITLE))))
        return OrderListScreen()
    }
}
