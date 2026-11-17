package com.woocommerce.android.ui.woopos.roles.lock

import android.animation.ValueAnimator
import android.provider.Settings
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.roles.lock.WooPosLockScreenState.Status
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.FileInputStream

@RunWith(AndroidJUnit4::class)
class WooPosLockScreenPresentationTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val incorrect: String get() = context.getString(R.string.woopos_lock_incorrect)

    @Test
    fun statusChangesKeepTheKeypadInPlace() {
        val state = mutableStateOf(WooPosLockScreenState(isLocked = true, enteredDigits = 2))
        setLockScreen(state)
        composeTestRule.waitForIdle()
        val keyTop = composeTestRule.onNodeWithText("1").getUnclippedBoundsInRoot().top

        // WHEN
        state.value = WooPosLockScreenState(isLocked = true, status = Status.Incorrect)
        composeTestRule.waitForIdle()

        // THEN
        composeTestRule.onNodeWithText("1").assertTopPositionInRootIsEqualTo(keyTop)

        // WHEN
        state.value = WooPosLockScreenState(isLocked = true, status = Status.Wait(30))
        composeTestRule.waitForIdle()

        // THEN
        composeTestRule.onNodeWithText("1").assertTopPositionInRootIsEqualTo(keyTop)

        // WHEN
        state.value = WooPosLockScreenState(isLocked = true, enteredDigits = 1)
        composeTestRule.waitForIdle()

        // THEN
        composeTestRule.onNodeWithText("1").assertTopPositionInRootIsEqualTo(keyTop)
    }

    @Test
    fun waitDisablesKeysAndShowsCountdown() {
        setLockScreen(WooPosLockScreenState(isLocked = true, status = Status.Wait(90)))

        composeTestRule.onNodeWithText("1").assertIsNotEnabled()
        composeTestRule.onNodeWithText(context.getString(R.string.woopos_lock_wait, 1L, 30L)).assertIsDisplayed()
    }

    @Test
    fun checkingKeepsEnteredDotsAndDisablesKeysWithoutProgress() {
        setLockScreen(WooPosLockScreenState(isLocked = true, enteredDigits = 4, status = Status.Checking))

        composeTestRule.onNodeWithText("1").assertIsNotEnabled()
        composeTestRule.onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)).assertDoesNotExist()
        composeTestRule.onNode(dots(entered = 4)).assertIsDisplayed()
    }

    @Test
    fun unavailableDisablesKeysAndShowsMessage() {
        setLockScreen(WooPosLockScreenState(isLocked = true, status = Status.StorageUnavailable))

        composeTestRule.onNodeWithText("1").assertIsNotEnabled()
        composeTestRule.onNodeWithText(context.getString(R.string.woopos_lock_unavailable)).assertIsDisplayed()
    }

    @Test
    fun introKeepsKeypadHiddenUntilLockClosesAndContentRises() = withAnimationsEnabled {
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
    fun interruptedIntroDoesNotReplayAfterStateRestoration() = withAnimationsEnabled {
        composeTestRule.mainClock.autoAdvance = false
        val showsScreen = mutableStateOf(true)
        composeTestRule.setContent {
            val stateHolder = rememberSaveableStateHolder()
            if (showsScreen.value) {
                stateHolder.SaveableStateProvider("lock") {
                    WooPosTheme {
                        WooPosLockScreen(WooPosLockScreenState(isLocked = true, enteredDigits = 2), onUIEvent = {})
                    }
                }
            }
        }
        composeTestRule.mainClock.advanceTimeBy(INTRO_HOLD_AND_CLOSE_MILLIS / 2)
        composeTestRule.onNodeWithText("1").assertDoesNotExist()

        // WHEN
        showsScreen.value = false
        composeTestRule.mainClock.advanceTimeByFrame()
        composeTestRule.waitForIdle()
        showsScreen.value = true
        composeTestRule.mainClock.advanceTimeBy(FRAME_MILLIS * 6)

        // THEN
        composeTestRule.onNodeWithText("1").assertIsDisplayed().assertIsEnabled()
        composeTestRule.onNode(dots(entered = 2)).assertIsDisplayed()
    }

    @Test
    fun incorrectPINShakesFourDotsBeforeTheErrorReplacesThem() {
        val state = mutableStateOf(WooPosLockScreenState(isLocked = true, enteredDigits = 4, status = Status.Checking))
        setLockScreen(state)
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.autoAdvance = false

        // WHEN
        state.value = WooPosLockScreenState(isLocked = true, status = Status.Incorrect)
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
    fun waitAfterRejectedPINShakesFourDotsWhileKeysStayDisabled() {
        val state = mutableStateOf(WooPosLockScreenState(isLocked = true, enteredDigits = 4, status = Status.Checking))
        setLockScreen(state)
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.autoAdvance = false

        // WHEN
        state.value = WooPosLockScreenState(isLocked = true, status = Status.Wait(30))
        composeTestRule.mainClock.advanceTimeBy(SHAKE_MILLIS / 2)

        // THEN
        composeTestRule.onNode(dots(entered = 4)).assertIsDisplayed()
        composeTestRule.onNodeWithText("1").assertIsNotEnabled()

        // WHEN
        composeTestRule.mainClock.advanceTimeBy(SHAKE_MILLIS)

        // THEN
        composeTestRule.onNodeWithText(context.getString(R.string.woopos_lock_wait, 0L, 30L)).assertIsDisplayed()
    }

    @Test
    fun checkingAnExistingWaitShowsTheCountdownWithoutRejectedDots() {
        val state = mutableStateOf(WooPosLockScreenState(isLocked = true, status = Status.Checking))
        setLockScreen(state)
        composeTestRule.waitForIdle()

        // WHEN
        state.value = WooPosLockScreenState(isLocked = true, status = Status.Wait(30))
        composeTestRule.waitForIdle()

        // THEN
        composeTestRule.onNodeWithText(context.getString(R.string.woopos_lock_wait, 0L, 30L)).assertIsDisplayed()
        composeTestRule.onNode(dots(entered = 4)).assertDoesNotExist()
    }

    @Test
    fun newInputDuringShakeShowsTheNewDigitWithoutTheError() {
        val state = mutableStateOf(WooPosLockScreenState(isLocked = true, enteredDigits = 4, status = Status.Checking))
        setLockScreen(state)
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.autoAdvance = false
        state.value = WooPosLockScreenState(isLocked = true, status = Status.Incorrect)
        composeTestRule.mainClock.advanceTimeBy(SHAKE_MILLIS / 2)

        // WHEN
        state.value = WooPosLockScreenState(isLocked = true, enteredDigits = 1)
        composeTestRule.mainClock.advanceTimeBy(SHAKE_MILLIS)

        // THEN
        composeTestRule.onNode(dots(entered = 1)).assertIsDisplayed()
        composeTestRule.onNodeWithText(incorrect).assertDoesNotExist()
    }

    private fun withAnimationsEnabled(test: () -> Unit) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val originalScale = Settings.Global.getString(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE)
        fun setScale(command: String) {
            instrumentation.uiAutomation.executeShellCommand(command).use { descriptor ->
                FileInputStream(descriptor.fileDescriptor).use { it.readBytes() }
            }
        }
        try {
            setScale("settings put global animator_duration_scale 1")
            composeTestRule.waitUntil { ValueAnimator.areAnimatorsEnabled() }
            test()
        } finally {
            val command = if (originalScale == null) {
                "settings delete global animator_duration_scale"
            } else {
                "settings put global animator_duration_scale $originalScale"
            }
            setScale(command)
        }
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
