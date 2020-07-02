package com.woocommerce.android.screenshots.orders

import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers
import com.woocommerce.android.R
import com.woocommerce.android.screenshots.TabNavComponent
import com.woocommerce.android.screenshots.login.WelcomeScreen
import com.woocommerce.android.screenshots.mystore.MyStoreScreen
import com.woocommerce.android.screenshots.settings.SettingsScreen
import com.woocommerce.android.screenshots.util.Screen
import com.woocommerce.android.screenshots.util.TestDataGenerator
import org.hamcrest.Matchers

class OrderListScreen() : Screen(ORDER_LIST_VIEW) {
    companion object {
        fun navigateToOrders(): OrderListScreen {
            if (!isToolbarTitle("Orders")) {
                MyStoreScreen().tabBar.gotoOrdersScreen()
            }

            return OrderListScreen()
        }

        const val ORDER_LIST_VIEW = R.id.ordersList
        const val ORDER_LIST_ITEM = R.id.orderNum
        const val SEARCH_BUTTON = R.id.menu_search
        const val SEARCH_TEXT_FIELD = R.id.search_src_text
        const val SETTINGS_BUTTON_TEXT = R.string.settings
        const val PROCESSING_TAB = "PROCESSING"
    }

    private val tabBar = TabNavComponent()

    // TASKS
    fun searchOrdersByName(): OrderSearchScreen {
        clickOn(SEARCH_BUTTON)
        typeTextInto(SEARCH_TEXT_FIELD, TestDataGenerator.getAllProductsSearchRequest())
        waitForElementToBeDisplayedWithoutFailure(ORDER_LIST_VIEW)
        return OrderSearchScreen()
    }

    fun selectRandomOrderFromTheList(): SingleOrderScreen {
        selectOrder(TestDataGenerator.getRandomInteger(1, 3))
        return SingleOrderScreen()
    }

    private fun openSettingsPane(): SettingsScreen {
        openToolbarActionMenu()
        Espresso.onView(ViewMatchers.withText(SETTINGS_BUTTON_TEXT)).perform(ViewActions.click())
        return SettingsScreen()
    }

    // NAVIGATION
    fun goToProcessingOrders(): OrderListScreen {
        clickOn(Espresso.onView(Matchers.allOf(ViewMatchers.withText(PROCESSING_TAB))))
        return OrderListScreen()
    }

    fun logOut(): WelcomeScreen {
        openSettingsPane().logOut()
        return WelcomeScreen()
    }

    // CHECKS
    fun isTitle(title: String): OrderListScreen {
        isToolbarTitle(title)
        return OrderListScreen()
    }

    // HELPERS
    private fun selectOrder(correctedIndex: Int) {
        waitForElementToBeDisplayedWithoutFailure(ORDER_LIST_ITEM)
        selectItemAtIndexInRecyclerView(correctedIndex, ORDER_LIST_VIEW, ORDER_LIST_ITEM)
        return
    }
}
