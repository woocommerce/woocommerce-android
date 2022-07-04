package com.woocommerce.android.screenshots.moremenu

import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.woocommerce.android.R
import com.woocommerce.android.screenshots.mystore.settings.SettingsScreen
import com.woocommerce.android.screenshots.reviews.ReviewsListScreen
import com.woocommerce.android.screenshots.util.Screen

class MoreMenuScreen : Screen(MORE_MENU_VIEW) {
    companion object {
        const val MORE_MENU_VIEW = R.id.more_menu_compose_view
    }

    fun openReviewsListScreen(composeTestRule: ComposeTestRule): ReviewsListScreen {
        composeTestRule.onNodeWithText(
            getTranslatedString(R.string.more_menu_button_reviews)
        ).performClick()
        return ReviewsListScreen()
    }

    fun openSettings(composeTestRule: ComposeTestRule): SettingsScreen {
        composeTestRule.onNodeWithContentDescription(
            getTranslatedString(R.string.settings)
        ).performClick()
        return SettingsScreen()
    }
}
