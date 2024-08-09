package com.woocommerce.android

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.woocommerce.android.ui.woopos.common.composeui.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.component.Button
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosErrorScreen
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class WooPosErrorStateTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createComposeRule()

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun testErrorStateDisplaysCorrectly() {
        val testMessage = "Test error message"
        val testReason = "Test error reason"

        composeTestRule.setContent {
            WooPosTheme {
                WooPosErrorScreen(
                    message = testMessage,
                    reason = testReason
                )
            }
        }

        composeTestRule.onNodeWithText(testMessage).assertIsDisplayed()
        composeTestRule.onNodeWithText(testReason).assertIsDisplayed()
    }

    @Test
    fun testButtonsAreDisplayedAndClickable() {
        val primaryButtonText = "Retry"
        val secondaryButtonText = "Cancel"
        var primaryClicked = false
        var secondaryClicked = false

        val primaryButton = Button(primaryButtonText) { primaryClicked = true }
        val secondaryButton = Button(secondaryButtonText) { secondaryClicked = true }

        composeTestRule.setContent {
            WooPosTheme {
                WooPosErrorScreen(
                    message = "Test Message",
                    reason = "Test Reason",
                    primaryButton = primaryButton,
                    secondaryButton = secondaryButton
                )
            }
        }

        composeTestRule.onNodeWithText(primaryButtonText).assertIsDisplayed().performClick()
        composeTestRule.onNodeWithText(secondaryButtonText).assertIsDisplayed().performClick()

        assert(primaryClicked) { "Primary button click should set primaryClicked to true" }
        assert(secondaryClicked) { "Secondary button click should set secondaryClicked to true" }
    }
}
