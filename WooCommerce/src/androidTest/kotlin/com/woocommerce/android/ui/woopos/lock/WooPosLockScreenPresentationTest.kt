package com.woocommerce.android.ui.woopos.lock

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class WooPosLockScreenPresentationTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createComposeRule()

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun waitDisablesKeysAndKeepsRecoveryAvailable() {
        val state = mutableStateOf(WooPosLockScreenState(isLocked = true, remainingSeconds = 30))
        composeTestRule.setContent {
            WooPosTheme {
                WooPosLockScreen(state.value, onUIEvent = { event ->
                    if (event == WooPosLockScreenUIEvent.ForgotPINTapped) {
                        state.value = state.value.copy(error = WooPosLockError.RECOVERY_UNAVAILABLE)
                    }
                })
            }
        }

        composeTestRule.onNodeWithText("1").assertIsNotEnabled()
        composeTestRule.onNodeWithText("Forgot PIN?").assertIsEnabled().performClick()
        composeTestRule.onNodeWithText("Too many incorrect attempts.\nTry again in 0:30.").assertIsDisplayed()
        composeTestRule.onNodeWithText("PIN recovery is not available yet.").assertIsDisplayed()
        composeTestRule.onNodeWithText("1").assertIsNotEnabled()
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

        composeTestRule.onNodeWithText("Checking PIN…").assertIsDisplayed()
        composeTestRule.onNodeWithText("1").assertIsNotEnabled()
    }
}
