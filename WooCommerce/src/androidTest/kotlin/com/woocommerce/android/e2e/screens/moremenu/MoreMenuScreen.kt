package com.woocommerce.android.e2e.screens.moremenu

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.woocommerce.android.R
import com.woocommerce.android.e2e.helpers.util.Screen
import com.woocommerce.android.e2e.screens.mystore.settings.SettingsScreen
import com.woocommerce.android.e2e.screens.pos.POSModeScreen
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

    fun openPOSScreen(composeTestRule: ComposeTestRule): POSModeScreen {
        composeTestRule.onNodeWithText(
            getTranslatedString(R.string.more_menu_button_payments)
        )
            .performScrollTo()
            .performClick()
        return POSModeScreen()
    }

    fun openSettings(composeTestRule: ComposeTestRule): SettingsScreen {
        // Tests are failing randomly here depending on how fast this screen is displayed after tapping on the More button
        // Adding this retry to wait for the settings icon to be displayed, retrying up to a max time
        val maxAttempts = 25
        var currentAttempt = 0
        var lastError: AssertionError? = null

        while (currentAttempt < maxAttempts) {
            try {
                composeTestRule.onNodeWithContentDescription(getTranslatedString(R.string.settings))
                    .assertIsDisplayed()
                    .assertHasClickAction()
                break // Exit loop if node is displayed and clickable
            } catch (e: AssertionError) {
                lastError = e // Capture the last AssertionError
                Thread.sleep(100) // Wait 100ms before retrying
                currentAttempt++
            }
        }

        if (currentAttempt == maxAttempts && lastError != null) {
            throw IllegalStateException("Failed to open settings due to: ${lastError.message}", lastError)
        }

        composeTestRule.onNodeWithContentDescription(
            getTranslatedString(R.string.settings)
        ).assertHasClickAction().performClick()

        return SettingsScreen()
    }

    fun assertStoreTitle(composeTestRule: ComposeTestRule, storeTitle: String): MoreMenuScreen {
        composeTestRule.onNodeWithText(storeTitle)
            .assertIsDisplayed()
        return MoreMenuScreen()
    }
}
