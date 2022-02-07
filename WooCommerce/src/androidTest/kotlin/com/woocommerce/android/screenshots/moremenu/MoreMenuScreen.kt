package com.woocommerce.android.screenshots.moremenu

import android.content.res.Resources
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.woocommerce.android.R
import com.woocommerce.android.screenshots.mystore.settings.SettingsScreen
import com.woocommerce.android.screenshots.reviews.ReviewsListScreen
import com.woocommerce.android.screenshots.util.Screen

class MoreMenuScreen : Screen(MORE_MENU_VIEW) {
    companion object {
        const val MORE_MENU_VIEW = R.id.menu
    }

    private val resources: Resources = InstrumentationRegistry.getInstrumentation().targetContext.resources

    fun openReviewsListScreen(composeTestRule: ComposeTestRule): ReviewsListScreen {
        composeTestRule.onNodeWithText(
            resources.getString(R.string.more_menu_button_reviews)
        ).performClick()
        return ReviewsListScreen()
    }

    fun openSettings(composeTestRule: ComposeTestRule): SettingsScreen {
        composeTestRule.onNodeWithContentDescription(
            resources.getString(R.string.settings)
        ).performClick()
        return SettingsScreen()
    }
}
