package com.woocommerce.android.e2e.screens.moremenu

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.woocommerce.android.R
import com.woocommerce.android.e2e.helpers.util.Screen
import com.woocommerce.android.e2e.screens.mystore.settings.SettingsScreen
import com.woocommerce.android.e2e.screens.reviews.ReviewsListScreen

class MoreMenuScreen : Screen(R.id.more_menu_compose_view) {
    fun openReviewsListScreen(composeTestRule: ComposeTestRule): ReviewsListScreen {
        composeTestRule.onNodeWithText(
            getTranslatedString(R.string.more_menu_button_reviews)
        )
            .performScrollTo()
            .performClick()
        return ReviewsListScreen()
    }

    fun openSettings(composeTestRule: ComposeTestRule): SettingsScreen {
        composeTestRule.onNodeWithContentDescription(
            getTranslatedString(R.string.settings)
        ).performClick()
        return SettingsScreen()
    }

    fun assertStoreTitle(composeTestRule: ComposeTestRule, storeTitle: String): MoreMenuScreen {
        composeTestRule.onNodeWithText(storeTitle)
            .assertIsDisplayed()
        return MoreMenuScreen()
    }
}
