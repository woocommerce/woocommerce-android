package com.woocommerce.android.screenshots.products

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

class ProductListScreen : Screen(LIST_VIEW) {
    companion object {
        fun navigateToProducts(): ProductListScreen {
            if (!isToolbarTitle("Products")) {
                MyStoreScreen().tabBar.gotoProductsScreen()
            }

            return ProductListScreen()
        }

        const val LIST_VIEW = R.id.productsRecycler
        const val LIST_ITEM = R.id.linearLayout
        const val FILTERS_BUTTON = R.id.btn_product_filter
        const val FILTER_ITEM = R.id.filterItemName
        const val FILTER_OPTION_LIST = R.id.filterOptionList
        const val CLEAR_FILTERS = R.id.menu_clear
        const val RADIO_BTN = R.id.filterOptionItem_tick
        const val SHOW_PRODUCTS_ON_STATUS_BTN = R.id.filterOptionList_btnShowProducts
        const val SHOW_PRODUCTS_ON_FILTER_BTN = R.id.filterList_btnShowProducts
        const val SETTINGS_BUTTON_TEXT = R.string.settings
        const val SORT_PRODUCTS_BUTTON = R.id.btn_product_sorting
        const val SORTING_LIST = R.id.sorting_optionsList
        const val SORTING_ITEM_MARK = R.id.sortingItem_tick
    }

    val tabBar = TabNavComponent()

    // TASKS
    fun filterOutProductsBy(filter: String): ProductListScreen {
        clickOps.clickOn(FILTERS_BUTTON)
        selectFilter(FILTER_ITEM, filter)
        waitOps.waitForElementToBeDisplayed(FILTER_OPTION_LIST)
        selectRandomItem(TestDataGenerator.getRandomInteger(1, 3), FILTER_OPTION_LIST, RADIO_BTN)
        clickOps.clickOn(SHOW_PRODUCTS_ON_STATUS_BTN)
        return ProductListScreen()
    }

    fun sortProducts(): ProductListScreen {
        clickOps.clickOn(SORT_PRODUCTS_BUTTON)
        selectOps.selectItemAtIndexInRecyclerView1(TestDataGenerator.getRandomInteger(0, 3), SORTING_LIST)
        return ProductListScreen()
    }

    private fun selectFilter(elementID: Int, stockStatus: String) {
        clickOps.clickOn(elementID, stockStatus)
    }

    fun selectRandomProductFromTheList(): SingleProductScreen {
        selectRandomItem(TestDataGenerator.getRandomInteger(0, 5), LIST_VIEW, LIST_ITEM)
        return SingleProductScreen()
    }

    fun logOut(): WelcomeScreen {
        openSettingsPane().logOut()
        return WelcomeScreen()
    }

    fun cancelFilters() {
        clickOps.clickOn(FILTERS_BUTTON)
        clickOps.clickOn(CLEAR_FILTERS)
        clickOps.clickOn(SHOW_PRODUCTS_ON_FILTER_BTN)
    }

    // CHECKS
    fun isTitle(title: String): ProductListScreen {
        isToolbarTitle(title)
        return ProductListScreen()
    }

    // HELPERS

    private fun selectRandomItem(randomInteger: Int, recyclerViewID: Int, elementID: Int) {
        selectOps.selectItemAtIndexInRecyclerView(randomInteger, recyclerViewID, elementID)
    }

    private fun openSettingsPane(): SettingsScreen {
        actionOps.openToolbarActionMenu()
        Espresso.onView(ViewMatchers.withText(SETTINGS_BUTTON_TEXT)).perform(ViewActions.click())
        return SettingsScreen()
    }
}
