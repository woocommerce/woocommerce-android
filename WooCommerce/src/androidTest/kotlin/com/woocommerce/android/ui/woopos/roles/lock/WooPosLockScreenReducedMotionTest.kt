package com.woocommerce.android.ui.woopos.roles.lock

import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WooPosLockScreenReducedMotionTest {
    @get:Rule
    val composeTestRule = createComposeRule(effectContext = DisabledMotion)

    @Test
    fun disabledAnimationsSkipTheIntroAndShowTheKeypadImmediately() {
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            WooPosTheme {
                WooPosLockScreen(WooPosLockScreenState(isLocked = true), onUIEvent = {})
            }
        }

        // WHEN
        composeTestRule.mainClock.advanceTimeByFrame()
        composeTestRule.mainClock.advanceTimeByFrame()

        // THEN
        composeTestRule.onNodeWithText("1").assertIsDisplayed().assertIsEnabled()
    }

    private object DisabledMotion : MotionDurationScale {
        override val scaleFactor: Float = 0f
    }
}
