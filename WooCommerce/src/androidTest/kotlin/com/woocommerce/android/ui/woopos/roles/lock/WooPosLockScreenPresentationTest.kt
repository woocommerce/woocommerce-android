package com.woocommerce.android.ui.woopos.roles.lock

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WooPosLockScreenPresentationTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun waitDisablesKeysAndKeepsForgotPINEnabled() {
        val state = WooPosLockScreenState(isLocked = true, remainingSeconds = 30)
        composeTestRule.setContent {
            WooPosTheme {
                WooPosLockScreen(state, onUIEvent = {})
            }
        }

        composeTestRule.onNodeWithText("1").assertIsNotEnabled()
        composeTestRule.onNodeWithText(
            context.getString(R.string.woopos_lock_forgot_pin)
        ).assertIsEnabled()
        composeTestRule.onNodeWithText(
            context.getString(R.string.woopos_lock_wait, 0, 30)
        ).assertIsDisplayed()
    }

    @Test
    fun checkingDisablesKeysAndDisplaysStatus() {
        composeTestRule.setContent {
            WooPosTheme {
                WooPosLockScreen(
                    WooPosLockScreenState(isLocked = true, enteredDigits = 4, isChecking = true),
                    onUIEvent = {},
                )
            }
        }

        composeTestRule.onNodeWithText(context.getString(R.string.woopos_lock_checking)).assertIsDisplayed()
        composeTestRule.onNodeWithText("1").assertIsNotEnabled()
    }
}
