package com.woocommerce.android.ui.woopos.roles.lock

import android.animation.ValueAnimator
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WooPosLockScreenPresentationTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val incorrect: String get() = context.getString(R.string.woopos_lock_incorrect)

    @Test
    fun waitDisablesKeysAndShowsCountdown() {
        setLockScreen(WooPosLockScreenState(isLocked = true, remainingSeconds = 90))

        composeTestRule.onNodeWithText("1").assertIsNotEnabled()
        composeTestRule.onNodeWithText(context.getString(R.string.woopos_lock_wait, 1L, 30L)).assertIsDisplayed()
    }

    @Test
    fun checkingKeepsEnteredDotsAndDisablesKeysWithoutProgress() {
        setLockScreen(WooPosLockScreenState(isLocked = true, enteredDigits = 4, isChecking = true))

        composeTestRule.onNodeWithText("1").assertIsNotEnabled()
        composeTestRule.onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)).assertDoesNotExist()
        composeTestRule.onNode(dots(entered = 4)).assertIsDisplayed()
    }

    @Test
    fun unavailableDisablesKeysAndShowsMessage() {
        setLockScreen(WooPosLockScreenState(isLocked = true, error = WooPosLockError.STORAGE))

        composeTestRule.onNodeWithText("1").assertIsNotEnabled()
        composeTestRule.onNodeWithText(context.getString(R.string.woopos_lock_unavailable)).assertIsDisplayed()
    }

    @Test
    fun introKeepsKeypadHiddenUntilLockClosesAndContentRises() {
        assumeTrue(ValueAnimator.areAnimatorsEnabled())
        composeTestRule.mainClock.autoAdvance = false
        setLockScreen(WooPosLockScreenState(isLocked = true))

        // WHEN the lock is open and then closing
        composeTestRule.mainClock.advanceTimeBy(INTRO_HOLD_AND_CLOSE_MILLIS - FRAME_MILLIS)

        // THEN
        composeTestRule.onNodeWithText("1").assertDoesNotExist()

        // WHEN the content has risen into place
        composeTestRule.mainClock.advanceTimeBy(INTRO_REVEAL_MILLIS + FRAME_MILLIS * 6)

        // THEN
        composeTestRule.onNodeWithText("1").assertIsDisplayed().assertIsEnabled()
    }

    @Test
    fun incorrectPINShakesFourDotsBeforeTheErrorReplacesThem() {
        val state = mutableStateOf(WooPosLockScreenState(isLocked = true, enteredDigits = 4, isChecking = true))
        setLockScreen(state)
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.autoAdvance = false

        // WHEN
        state.value = WooPosLockScreenState(isLocked = true, error = WooPosLockError.INCORRECT)
        composeTestRule.mainClock.advanceTimeBy(SHAKE_MILLIS / 2)

        // THEN
        composeTestRule.onNode(dots(entered = 4)).assertIsDisplayed()
        composeTestRule.onNodeWithText(incorrect).assertDoesNotExist()

        // WHEN
        composeTestRule.mainClock.advanceTimeBy(SHAKE_MILLIS)

        // THEN
        composeTestRule.onNodeWithText(incorrect).assertIsDisplayed()
    }

    @Test
    fun newInputDuringShakeShowsTheNewDigitWithoutTheError() {
        val state = mutableStateOf(WooPosLockScreenState(isLocked = true, enteredDigits = 4, isChecking = true))
        setLockScreen(state)
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.autoAdvance = false
        state.value = WooPosLockScreenState(isLocked = true, error = WooPosLockError.INCORRECT)
        composeTestRule.mainClock.advanceTimeBy(SHAKE_MILLIS / 2)

        // WHEN
        state.value = WooPosLockScreenState(isLocked = true, enteredDigits = 1)
        composeTestRule.mainClock.advanceTimeBy(SHAKE_MILLIS)

        // THEN
        composeTestRule.onNode(dots(entered = 1)).assertIsDisplayed()
        composeTestRule.onNodeWithText(incorrect).assertDoesNotExist()
    }

    private fun setLockScreen(state: WooPosLockScreenState) = setLockScreen(mutableStateOf(state))

    private fun setLockScreen(state: State<WooPosLockScreenState>) {
        composeTestRule.setContent {
            WooPosTheme {
                WooPosLockScreen(state.value, onUIEvent = {})
            }
        }
    }

    private fun dots(entered: Int): SemanticsMatcher =
        hasContentDescription(context.getString(R.string.woopos_lock_pin_entry)) and SemanticsMatcher.expectValue(
            SemanticsProperties.StateDescription,
            context.getString(R.string.woopos_lock_digits_entered, entered, 4),
        )

    private companion object {
        const val FRAME_MILLIS = 16L
        const val INTRO_HOLD_AND_CLOSE_MILLIS = 600L
        const val INTRO_REVEAL_MILLIS = 280L
        const val SHAKE_MILLIS = 400L
    }
}
