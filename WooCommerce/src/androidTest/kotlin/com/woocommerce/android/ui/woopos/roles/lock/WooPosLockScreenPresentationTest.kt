package com.woocommerce.android.ui.woopos.roles.lock

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.roles.pin.WooPosPINEntryStatus
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.Offset.offset
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WooPosLockScreenPresentationTest {
    @get:Rule
    val composeTestRule = createComposeRule(effectContext = EnabledMotion)

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val incorrect: String get() = context.getString(R.string.woopos_lock_incorrect)

    @Test
    fun filledDotsCoverTheirOutline() {
        setLockScreen(WooPosLockScreenState(isLocked = true, enteredDigits = 2))
        composeTestRule.waitForIdle()

        val pixels = composeTestRule.onNode(dots(entered = 2)).captureToImage().toPixelMap()
        val edge = pixels[(pixels.height / 40f).toInt(), pixels.height / 2]
        val center = pixels[pixels.height / 2, pixels.height / 2]

        assertThat(edge.red).isCloseTo(center.red, offset(0.08f))
        assertThat(edge.green).isCloseTo(center.green, offset(0.08f))
        assertThat(edge.blue).isCloseTo(center.blue, offset(0.08f))
    }

    @Test
    fun startupShowsRegularLoadingUntilProtectionIsKnown() {
        val state = mutableStateOf(WooPosLockScreenState(isLocked = true, status = WooPosPINEntryStatus.Initializing))
        setLockScreen(state)

        composeTestRule.onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.woopos_lock_enter_pin)).assertDoesNotExist()
        composeTestRule.onNodeWithText("1").assertDoesNotExist()

        // WHEN
        composeTestRule.runOnIdle { state.value = WooPosLockScreenState(isLocked = true) }

        // THEN
        composeTestRule.onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)).assertDoesNotExist()
        composeTestRule.onNodeWithText(context.getString(R.string.woopos_lock_enter_pin)).assertIsDisplayed()
        composeTestRule.onNodeWithText("1").assertIsDisplayed()
    }

    @Test
    fun statusChangesKeepTheKeypadInPlace() {
        val state = mutableStateOf(WooPosLockScreenState(isLocked = true, enteredDigits = 2))
        setLockScreen(state)
        composeTestRule.waitForIdle()
        val keyTop = composeTestRule.onNodeWithText("1").getUnclippedBoundsInRoot().top

        // WHEN
        state.value = WooPosLockScreenState(isLocked = true, status = WooPosPINEntryStatus.Incorrect)
        composeTestRule.waitForIdle()

        // THEN
        composeTestRule.onNodeWithText("1").assertTopPositionInRootIsEqualTo(keyTop)

        // WHEN
        state.value = WooPosLockScreenState(isLocked = true, status = WooPosPINEntryStatus.Wait(30))
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
        setLockScreen(WooPosLockScreenState(isLocked = true, status = WooPosPINEntryStatus.Wait(90)))

        composeTestRule.onNodeWithText("1").assertIsNotEnabled()
        composeTestRule.onNodeWithText(context.getString(R.string.woopos_lock_wait, "1:30")).assertIsDisplayed()
    }

    @Test
    fun checkingKeepsEnteredDotsAndDisablesKeysWithoutProgress() {
        setLockScreen(WooPosLockScreenState(isLocked = true, enteredDigits = 4, status = WooPosPINEntryStatus.Checking))

        composeTestRule.onNodeWithText("1").assertIsNotEnabled()
        composeTestRule.onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)).assertDoesNotExist()
        composeTestRule.onNode(dots(entered = 4)).assertIsDisplayed()
    }

    @Test
    fun unavailableDisablesKeysAndShowsMessage() {
        setLockScreen(WooPosLockScreenState(isLocked = true, status = WooPosPINEntryStatus.StorageUnavailable))

        composeTestRule.onNodeWithText("1").assertIsNotEnabled()
        composeTestRule.onNodeWithText(context.getString(R.string.woopos_lock_unavailable)).assertIsDisplayed()
    }

    @Test
    fun introKeepsKeypadHiddenUntilLockClosesAndContentRises() {
        composeTestRule.mainClock.autoAdvance = false
        setLockScreen(WooPosLockScreenState(isLocked = true), fixedSize = true)

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
    fun introKeepsTheCenteredLockVisibleBeforeContentIsRevealed() {
        composeTestRule.mainClock.autoAdvance = false
        setLockScreen(WooPosLockScreenState(isLocked = true), fixedSize = true)
        composeTestRule.mainClock.advanceTimeBy(FRAME_MILLIS * 3)

        // THEN
        val pixels = composeTestRule.onRoot().captureToImage().toPixelMap()
        var lockPixels = 0
        for (y in 0 until pixels.height) {
            for (x in 0 until pixels.width) {
                val color = pixels[x, y]
                if (color.blue > 0.45f && color.red > 0.25f && color.blue > color.green * 1.25f) lockPixels++
            }
        }
        assertThat(lockPixels).isGreaterThan(100)
        composeTestRule.onNodeWithText("1").assertDoesNotExist()
    }

    @Test
    fun interruptedIntroDoesNotReplayAfterStateRestoration() {
        composeTestRule.mainClock.autoAdvance = false
        val showsScreen = mutableStateOf(true)
        composeTestRule.setContent {
            val stateHolder = rememberSaveableStateHolder()
            if (showsScreen.value) {
                stateHolder.SaveableStateProvider("lock") {
                    DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(INTRO_SIZE)) {
                        WooPosTheme {
                            WooPosLockScreen(WooPosLockScreenState(isLocked = true, enteredDigits = 2), onUIEvent = {})
                        }
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
        val state = mutableStateOf(
            WooPosLockScreenState(isLocked = true, enteredDigits = 4, status = WooPosPINEntryStatus.Checking),
        )
        setLockScreen(state)
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.autoAdvance = false

        // WHEN
        state.value = WooPosLockScreenState(isLocked = true, status = WooPosPINEntryStatus.Incorrect)
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
        val state = mutableStateOf(
            WooPosLockScreenState(isLocked = true, enteredDigits = 4, status = WooPosPINEntryStatus.Checking),
        )
        setLockScreen(state)
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.autoAdvance = false

        // WHEN
        state.value = WooPosLockScreenState(isLocked = true, status = WooPosPINEntryStatus.Wait(30))
        composeTestRule.mainClock.advanceTimeBy(SHAKE_MILLIS / 2)

        // THEN
        composeTestRule.onNode(dots(entered = 4)).assertIsDisplayed()
        composeTestRule.onNodeWithText("1").assertIsNotEnabled()

        // WHEN
        composeTestRule.mainClock.advanceTimeBy(SHAKE_MILLIS)

        // THEN
        composeTestRule.onNodeWithText(context.getString(R.string.woopos_lock_wait, "0:30")).assertIsDisplayed()
    }

    @Test
    fun checkingAnExistingWaitShowsTheCountdownWithoutRejectedDots() {
        val state = mutableStateOf(WooPosLockScreenState(isLocked = true, status = WooPosPINEntryStatus.Checking))
        setLockScreen(state)
        composeTestRule.waitForIdle()

        // WHEN
        state.value = WooPosLockScreenState(isLocked = true, status = WooPosPINEntryStatus.Wait(30))
        composeTestRule.waitForIdle()

        // THEN
        composeTestRule.onNodeWithText(context.getString(R.string.woopos_lock_wait, "0:30")).assertIsDisplayed()
        composeTestRule.onNode(dots(entered = 4)).assertDoesNotExist()
    }

    @Test
    fun newInputDuringShakeShowsTheNewDigitWithoutTheError() {
        val state = mutableStateOf(
            WooPosLockScreenState(isLocked = true, enteredDigits = 4, status = WooPosPINEntryStatus.Checking),
        )
        setLockScreen(state)
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.autoAdvance = false
        state.value = WooPosLockScreenState(isLocked = true, status = WooPosPINEntryStatus.Incorrect)
        composeTestRule.mainClock.advanceTimeBy(SHAKE_MILLIS / 2)

        // WHEN
        state.value = WooPosLockScreenState(isLocked = true, enteredDigits = 1)
        composeTestRule.mainClock.advanceTimeBy(SHAKE_MILLIS)

        // THEN
        composeTestRule.onNode(dots(entered = 1)).assertIsDisplayed()
        composeTestRule.onNodeWithText(incorrect).assertDoesNotExist()
    }

    private fun setLockScreen(state: WooPosLockScreenState, fixedSize: Boolean = false) =
        setLockScreen(mutableStateOf(state), fixedSize)

    private fun setLockScreen(state: State<WooPosLockScreenState>, fixedSize: Boolean = false) {
        composeTestRule.setContent {
            if (fixedSize) {
                DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(INTRO_SIZE)) {
                    WooPosTheme { WooPosLockScreen(state.value, onUIEvent = {}) }
                }
            } else {
                WooPosTheme { WooPosLockScreen(state.value, onUIEvent = {}) }
            }
        }
    }

    private object EnabledMotion : MotionDurationScale {
        override val scaleFactor: Float = 1f
    }

    private fun dots(entered: Int): SemanticsMatcher =
        hasContentDescription(context.getString(R.string.woopos_lock_pin_entry)) and SemanticsMatcher.expectValue(
            SemanticsProperties.StateDescription,
            context.getString(R.string.woopos_lock_digits_entered, entered, 4),
        )

    private companion object {
        val INTRO_SIZE = DpSize(800.dp, 1000.dp)
        const val FRAME_MILLIS = 16L
        const val INTRO_HOLD_AND_CLOSE_MILLIS = 600L
        const val INTRO_REVEAL_MILLIS = 280L
        const val SHAKE_MILLIS = 400L
    }
}
