package com.woocommerce.android.e2e.screens.orders

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import com.woocommerce.android.R
import com.woocommerce.android.e2e.helpers.util.Screen

class ProductSelectorScreen : Screen(R.id.product_selector_compose_view) {
    fun assertProductsSelectorScreen(
        composeTestRule: ComposeContentTestRule
    ): ProductSelectorScreen {
        val screenTitle = getTranslatedString(R.string.coupon_conditions_products_select_products_title)
        Espresso.onView(ViewMatchers.withText(screenTitle)).check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

        val searchHintText = getTranslatedString(R.string.product_selector_search_hint)
        composeTestRule.onNodeWithText(searchHintText).assertIsDisplayed()

        return this
    }

    fun selectProduct(
        composeTestRule: ComposeContentTestRule,
        productName: String
    ): UnifiedOrderScreen {
        composeTestRule.onAllNodesWithText(productName).onFirst().performClick()
        val selectButtonText = String.format(getTranslatedString(R.string.product_selector_select_button_title_one), 1)
        composeTestRule.onNodeWithText(selectButtonText).performClick()

        return UnifiedOrderScreen()
    }
}
