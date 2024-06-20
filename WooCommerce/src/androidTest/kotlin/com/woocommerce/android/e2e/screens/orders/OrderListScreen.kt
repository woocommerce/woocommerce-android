package com.woocommerce.android.e2e.screens.orders

import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import com.google.android.material.card.MaterialCardView
import com.woocommerce.android.R
import com.woocommerce.android.e2e.helpers.util.CustomMatchers
import com.woocommerce.android.e2e.helpers.util.OrderData
import com.woocommerce.android.e2e.helpers.util.Screen
import com.woocommerce.android.e2e.screens.shared.FilterScreen
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.Matchers

class OrderListScreen : Screen(R.id.ordersList) {
    fun selectOrder(index: Int): SingleOrderScreen {
        val correctedIndex = index + 1 // account for the header
        selectItemAtIndexInRecyclerView(correctedIndex, R.id.ordersList, R.id.linearLayout)
        return SingleOrderScreen()
    }

    fun selectOrderById(orderId: Int): SingleOrderScreen {
        val orderToOpen = Espresso.onView(
            Matchers.allOf(
                ViewMatchers.withId(R.id.orderNum),
                ViewMatchers.withText("#$orderId")
            )
        )

        waitForElementToBeDisplayed(orderToOpen)
        orderToOpen.perform(ViewActions.click())
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
        // If we expect for results, we wait for the list header
        waitForElementToBeDisplayed(R.id.orderListHeader)
        return this
    }

    fun enterAbsentSearchTerm(term: String): OrderListScreen {
        typeTextInto(androidx.appcompat.R.id.search_src_text, term)
        // We don't expect results, so we wait for "no results" situation
        waitForElementToBeDisplayed(
            Espresso.onView(ViewMatchers.withText(containsString("couldn't find")))
        )
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

    fun assertOrderCard(order: OrderData): OrderListScreen {
        // Using quite a complex matcher to make sure that all expected
        // order details belong to the same order card.
        Espresso.onView(
            Matchers.allOf(
                // We "start" with making sure that there's and order ID on screen
                Matchers.allOf(
                    ViewMatchers.withId(R.id.orderNum),
                    ViewMatchers.withText("#${order.id}")
                ),
                // And that this ID has a sibling with expected Customer Name
                ViewMatchers.hasSibling(
                    Matchers.allOf(
                        ViewMatchers.withId(R.id.orderName),
                        ViewMatchers.withText(order.customerName)
                    )
                ),
                // And that this ID has a sibling with expected price
                ViewMatchers.hasSibling(
                    Matchers.allOf(
                        ViewMatchers.withId(R.id.orderTotal),
                        ViewMatchers.withText(order.total)
                    )
                ),
                // And that this ID has a sibling with a child containing expected status
                ViewMatchers.hasSibling(
                    Matchers.allOf(
                        ViewMatchers.withId(R.id.orderTags),
                        ViewMatchers.withChild(ViewMatchers.withText(order.status))
                    )
                ),
                // And that all of them are children of Orders List
                ViewMatchers.isDescendantOfA(ViewMatchers.withId(R.id.ordersList)),
            )
        )
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

        return this
    }

    fun assertOrdersCount(count: Int): OrderListScreen {
        Espresso.onView(ViewMatchers.withId(R.id.ordersList))
            .check(
                ViewAssertions.matches(
                    CustomMatchers()
                        .withViewCount(Matchers.instanceOf(MaterialCardView::class.java), count)
                )
            )

        return this
    }

    fun assertSearchResultsAbsent(term: String): OrderListScreen {
        val expectedString = "We're sorry, we couldn't find results for \"${term}\""
        Espresso.onView(
            Matchers.allOf(
                ViewMatchers.withId(R.id.empty_view_title),
                ViewMatchers.withText(containsString(expectedString))
            )
        )
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        return this
    }
}
