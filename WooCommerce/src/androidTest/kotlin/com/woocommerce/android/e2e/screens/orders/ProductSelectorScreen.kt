package com.woocommerce.android.e2e.screens.orders

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.woocommerce.android.R
import com.woocommerce.android.e2e.helpers.util.Screen
import com.woocommerce.android.ui.login.LoginActivity

class ProductSelectorScreen : Screen(R.id.product_selector_compose_view) {
    fun assertProductsSelectorScreen(
        composeTestRule: AndroidComposeTestRule<ActivityScenarioRule<LoginActivity>, LoginActivity>
    ): ProductSelectorScreen {
        val screenTitle =
            composeTestRule.activity.getString(R.string.coupon_conditions_products_select_products_title)
        Espresso.onView(ViewMatchers.withText(screenTitle)).check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

        val searchHintText = composeTestRule.activity.getString(R.string.product_selector_search_hint)
        composeTestRule.onNodeWithText(searchHintText)
            .assertIsDisplayed()

        return this
    }

    fun selectProduct(
        composeTestRule: AndroidComposeTestRule<ActivityScenarioRule<LoginActivity>, LoginActivity>,
        productName: String
    ): UnifiedOrderScreen {
        composeTestRule.onNodeWithText(productName).performClick()
        val selectButtonText = composeTestRule.activity.getString(R.string.product_selector_select_button_title_one, 1)
        composeTestRule.onNodeWithText(selectButtonText)
            .performClick()
        return UnifiedOrderScreen()
    }
}
