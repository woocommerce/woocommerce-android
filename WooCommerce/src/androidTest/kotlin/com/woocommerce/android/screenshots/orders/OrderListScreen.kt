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
        const val LIST_ITEM = R.id.orderNum
        const val SEARCH_BUTTON = R.id.menu_search
        const val SEARCH_TEXT_FIELD = R.id.search_src_text
        const val SETTINGS_BUTTON_TEXT = R.string.settings
        const val PROCESSING_TAB = "PROCESSING"
    }

    constructor() : super(LIST_VIEW)

    private val tabBar = TabNavComponent()

    // TASKS
    fun searchOrdersByName(): OrderSearchScreen {
        clickOn(SEARCH_BUTTON)
        typeTextInto(SEARCH_TEXT_FIELD, TestDataGenerator.getAllProductsSearchRequest())
        waitForElementToBeDisplayedWithoutFailure(LIST_VIEW)
        return OrderSearchScreen()
    }

    fun selectRandomOrderFromTheList(): SingleOrderScreen {
        selectOrder(0)
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
    private fun selectOrder(index: Int) {
        val correctedIndex = index + TestDataGenerator.getRandomInteger(1, 3) // account for the header
        waitForElementToBeDisplayedWithoutFailure(LIST_ITEM)
        selectItemAtIndexInRecyclerView(correctedIndex, LIST_VIEW, LIST_ITEM)
        return
    }
}
