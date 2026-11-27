package com.woocommerce.android.ui.woopos.settings.details.security

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.WindowInsets
import androidx.compose.ui.test.WindowSize
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.then
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowInsetsCompat
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.roles.WooPosLocalRole.ADMIN
import com.woocommerce.android.ui.woopos.roles.WooPosLocalRole.CASHIER
import com.woocommerce.android.ui.woopos.settings.details.security.WooPosSettingsSecurityPINSetupState.AccessUnavailable
import com.woocommerce.android.ui.woopos.settings.details.security.WooPosSettingsSecurityPINSetupState.Choose
import com.woocommerce.android.ui.woopos.settings.details.security.WooPosSettingsSecurityPINSetupState.Confirm
import com.woocommerce.android.ui.woopos.settings.details.security.WooPosSettingsSecurityPINSetupState.Error
import com.woocommerce.android.ui.woopos.settings.details.security.WooPosSettingsSecurityPINSetupState.Page
import com.woocommerce.android.ui.woopos.settings.details.security.WooPosSettingsSecurityPINSetupState.Rejected
import com.woocommerce.android.ui.woopos.settings.details.security.WooPosSettingsSecurityPINSetupState.Saved
import com.woocommerce.android.ui.woopos.settings.details.security.WooPosSettingsSecurityPINSetupState.Saving
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.Offset.offset
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs

@RunWith(AndroidJUnit4::class)
class WooPosSettingsSecurityPINSetupDialogTest {
    private val motion = TestMotion()

    @get:Rule
    val composeTestRule = createComposeRule(effectContext = motion)

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val back: String get() = context.getString(R.string.back)
    private val close: String get() = context.getString(R.string.close)
    private val events = mutableListOf<WooPosSettingsSecurityPINSetupUIEvent>()
    private var closeCount = 0

    @Test
    fun confirmationOffersBackWhileChooseOffersOnlyClose() {
        val state = mutableStateOf<WooPosSettingsSecurityPINSetupState>(Choose(ADMIN, enteredDigits = 1))
        setDialog(state)

        composeTestRule.onNodeWithText(context.getString(R.string.woopos_security_choose_pin)).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(back).assertDoesNotExist()

        // WHEN
        state.value = Confirm(ADMIN, enteredDigits = 2)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription(back).performClick()
        Espresso.pressBack()

        // THEN
        composeTestRule.onNodeWithText(context.getString(R.string.woopos_security_confirm_pin)).assertIsDisplayed()
        assertThat(events).containsExactly(
            WooPosSettingsSecurityPINSetupUIEvent.BackTapped,
            WooPosSettingsSecurityPINSetupUIEvent.BackTapped,
        )
        assertThat(closeCount).isZero()
    }

    @Test
    fun heldConfirmationDisablesKeysAndNavigation() {
        setDialog(mutableStateOf(Confirm(ADMIN, enteredDigits = 4)))

        // WHEN
        Espresso.pressBack()

        // THEN
        composeTestRule.onNodeWithContentDescription(back).assertIsNotEnabled()
        composeTestRule.onNodeWithContentDescription(close).assertIsNotEnabled()
        composeTestRule.onNodeWithText("1").assertIsNotEnabled()
        assertThat(events).isEmpty()
        assertThat(closeCount).isZero()
    }

    @Test
    fun savingShowsOnlyProgressAndCannotClose() {
        setDialog(mutableStateOf(Saving(ADMIN)))

        // WHEN
        Espresso.pressBack()

        // THEN
        composeTestRule.onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(close).assertDoesNotExist()
        composeTestRule.onNodeWithText(context.getString(R.string.woopos_security_confirm_pin)).assertDoesNotExist()
        composeTestRule.onNodeWithText("1").assertDoesNotExist()
        assertThat(closeCount).isZero()
    }

    @Test
    fun unavailableAdminAccessClosesTheVisibleFlowOnce() {
        val state = mutableStateOf<WooPosSettingsSecurityPINSetupState>(Saving(ADMIN))
        val isVisible = mutableStateOf(true)
        setDialog(state, isVisible)

        composeTestRule.runOnIdle { state.value = AccessUnavailable }
        composeTestRule.runOnIdle {
            assertThat(closeCount).isEqualTo(1)
            isVisible.value = false
        }
        composeTestRule.runOnIdle { assertThat(closeCount).isEqualTo(1) }
    }

    @Test
    fun fillingADotDrawsBeyondItsRestingBounds() {
        val state = mutableStateOf<WooPosSettingsSecurityPINSetupState>(Choose(ADMIN))
        setDialog(state)
        val layout = composeTestRule.onNodeWithContentDescription(context.getString(R.string.woopos_lock_pin_entry))
        val row = layout.fetchSemanticsNode().boundsInRoot
        val centerX = (row.left + row.height / 2).toInt()
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.runOnUiThread { state.value = Choose(ADMIN, enteredDigits = 1) }
        composeTestRule.mainClock.advanceTimeByFrame()
        composeTestRule.mainClock.advanceTimeByFrame()

        val pixels = composeTestRule.onRoot().captureToImage().toPixelMap()
        val center = pixels[centerX, row.center.y.toInt()]
        val background = pixels[centerX, (row.top - row.height).toInt()]
        val above = pixels[centerX, row.top.toInt() - 1]
        assertThat(abs(center.red - background.red)).isGreaterThan(0.5f)
        assertThat(above.red).isCloseTo(center.red, offset(0.1f))
    }

    @Test
    fun switchingToConfirmKeepsTheDotsInsideTheirTrack() {
        val state = mutableStateOf<WooPosSettingsSecurityPINSetupState>(Choose(ADMIN, enteredDigits = 4))
        setDialog(state)
        val dots = composeTestRule.onNodeWithContentDescription(context.getString(R.string.woopos_lock_pin_entry))
        val row = dots.fetchSemanticsNode().boundsInRoot
        val root = composeTestRule.onRoot()
        val padding = row.height / 2
        val y = row.center.y.toInt()
        val start = (row.left - padding).toInt()
        val end = (row.right + padding).toInt()
        val background = root.captureToImage().toPixelMap()[start - 2, y]
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.runOnUiThread { state.value = Confirm(ADMIN) }

        repeat(5) {
            composeTestRule.mainClock.advanceTimeBy(32)
            val pixels = root.captureToImage().toPixelMap()
            for (x in (start - row.width.toInt()).coerceAtLeast(0) until start - 1) {
                assertThat(pixels[x, y].red).isCloseTo(background.red, offset(0.05f))
            }
            for (x in end + 1 until (end + row.width.toInt()).coerceAtMost(pixels.width)) {
                assertThat(pixels[x, y].red).isCloseTo(background.red, offset(0.05f))
            }
        }
    }

    @Test
    fun mismatchMessageAppearsAfterTheDotsReturn() {
        val state = mutableStateOf<WooPosSettingsSecurityPINSetupState>(Rejected(ADMIN, Page.CONFIRM))
        setDialog(state)
        composeTestRule.waitForIdle()
        val keyBounds = composeTestRule.onNodeWithText("1").getUnclippedBoundsInRoot()
        composeTestRule.mainClock.autoAdvance = false

        // WHEN
        state.value = Choose(ADMIN, error = Error.MISMATCH)
        composeTestRule.mainClock.advanceTimeByFrame()

        // THEN
        val mismatch = context.getString(R.string.woopos_security_mismatch)
        composeTestRule.onNodeWithText(mismatch).assertDoesNotExist()

        // WHEN
        composeTestRule.mainClock.advanceTimeBy(PAGE_SETTLED_MILLIS)
        composeTestRule.onNodeWithText(mismatch).assertDoesNotExist()
        composeTestRule.mainClock.advanceTimeBy(MESSAGE_REVEAL_MILLIS)

        // THEN
        composeTestRule.onNodeWithText(mismatch).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.woopos_security_choose_pin)).assertIsDisplayed()
        assertThat(composeTestRule.onNodeWithText("1").getUnclippedBoundsInRoot()).isEqualTo(keyBounds)
    }

    @Test
    fun duplicateClearsTheSameRowBeforeShowingTheError() {
        val state = mutableStateOf<WooPosSettingsSecurityPINSetupState>(Rejected(CASHIER, Page.CHOOSE))
        setDialog(state)
        val key = composeTestRule.onNodeWithText("1")
        val keyBounds = key.getUnclippedBoundsInRoot()
        key.assertIsNotEnabled()
        val dots = hasContentDescription(context.getString(R.string.woopos_lock_pin_entry))
        val message = context.getString(R.string.woopos_security_pin_taken)
        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.runOnUiThread { state.value = Choose(CASHIER, error = Error.PIN_TAKEN) }
        composeTestRule.mainClock.advanceTimeByFrame()

        composeTestRule.onAllNodes(dots).assertCountEquals(1)
        composeTestRule.onNodeWithText(message).assertDoesNotExist()
        composeTestRule.mainClock.advanceTimeBy(PAGE_SETTLED_MILLIS)

        composeTestRule.onNodeWithText(message).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.woopos_security_choose_cashier_pin))
            .assertIsDisplayed()
        assertThat(key.getUnclippedBoundsInRoot()).isEqualTo(keyBounds)
    }

    @Test
    fun typingBeforeTheMismatchMessagePreventsItFromAppearing() {
        val state = mutableStateOf<WooPosSettingsSecurityPINSetupState>(Rejected(ADMIN, Page.CONFIRM))
        setDialog(state)
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.runOnUiThread { state.value = Choose(ADMIN, error = Error.MISMATCH) }
        composeTestRule.mainClock.advanceTimeBy(PAGE_SETTLED_MILLIS)

        composeTestRule.runOnUiThread { state.value = Choose(ADMIN, enteredDigits = 1) }
        composeTestRule.mainClock.advanceTimeBy(MESSAGE_REVEAL_MILLIS)

        composeTestRule.onNodeWithText(context.getString(R.string.woopos_security_mismatch)).assertDoesNotExist()
    }

    @Test
    fun reducedMotionShowsTheErrorWithoutWaitingForTheSlide() {
        motion.scaleFactor = 0f
        val state = mutableStateOf<WooPosSettingsSecurityPINSetupState>(Rejected(ADMIN, Page.CONFIRM))
        setDialog(state)

        composeTestRule.runOnIdle { state.value = Choose(ADMIN, error = Error.MISMATCH) }

        composeTestRule.onNodeWithText(context.getString(R.string.woopos_security_mismatch)).assertIsDisplayed()
        composeTestRule.onAllNodes(hasContentDescription(context.getString(R.string.woopos_lock_pin_entry)))
            .assertCountEquals(1)
    }

    @Test
    fun cashierSuccessExplainsTheCashierPIN() {
        setDialog(mutableStateOf(Saved(CASHIER)))

        // THEN
        composeTestRule.onNodeWithText(context.getString(R.string.woopos_security_cashier_success)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.woopos_security_cashier_success_message))
            .assertIsDisplayed()
    }

    @Test
    fun successContinueClosesTheFlow() {
        setDialog(mutableStateOf(Saved(ADMIN)))

        // WHEN
        composeTestRule.onNodeWithText(context.getString(R.string.continue_button)).assertIsEnabled().performClick()

        // THEN
        composeTestRule.onNodeWithText(context.getString(R.string.woopos_security_success)).assertIsDisplayed()
        assertThat(closeCount).isEqualTo(1)
    }

    @Test
    fun closingKeepsTheLastStepWhileTheFlowResetsBehindIt() {
        val state = mutableStateOf<WooPosSettingsSecurityPINSetupState>(Saved(ADMIN))
        val isVisible = mutableStateOf(true)
        setDialog(state, isVisible)
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.autoAdvance = false

        // WHEN
        isVisible.value = false
        composeTestRule.mainClock.advanceTimeByFrame()
        state.value = Choose(ADMIN)
        composeTestRule.mainClock.advanceTimeByFrame()

        // THEN
        composeTestRule.onNodeWithText(context.getString(R.string.woopos_security_success)).assertExists()
        composeTestRule.onNodeWithText(context.getString(R.string.woopos_security_choose_pin)).assertDoesNotExist()
    }

    @Test
    fun keypadTapsDuringTheClosingAnimationAreIgnored() {
        val isVisible = mutableStateOf(true)
        setDialog(mutableStateOf(Choose(ADMIN, enteredDigits = 1)), isVisible)
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.autoAdvance = false

        // WHEN
        isVisible.value = false
        composeTestRule.mainClock.advanceTimeByFrame()
        composeTestRule.onNodeWithText("2").performClick()

        // THEN
        assertThat(events).isEmpty()
    }

    @Test
    fun phoneSuccessOffersOnlyContinueNearTheBottom() {
        setPhoneDialog(Saved(CASHIER), DpSize(400.dp, 800.dp))

        composeTestRule.onNodeWithContentDescription(close).assertDoesNotExist()
        val continueBounds = composeTestRule.onNodeWithText(context.getString(R.string.continue_button))
            .assertIsDisplayed()
            .getUnclippedBoundsInRoot()

        assertThat(continueBounds.top.value).isGreaterThan(700f)
        assertThat((continueBounds.right - continueBounds.left).value).isGreaterThan(320f)
    }

    @Test
    fun phoneKeepsTheKeypadInPlaceWhenTheConfirmationTitleWraps() {
        val state = mutableStateOf<WooPosSettingsSecurityPINSetupState>(Choose(ADMIN))
        val chooseTitle = "Choose a PIN"
        val confirmTitle = "Confirm the PIN you just entered for this account"
        setPhoneDialog(state, DpSize(360.dp, 780.dp), 1.5f, chooseTitle, confirmTitle)

        val key = composeTestRule.onNodeWithText("1")
        val dots = composeTestRule.onNodeWithContentDescription(context.getString(R.string.woopos_lock_pin_entry))
        val chooseBounds = composeTestRule.onNodeWithText(chooseTitle).getUnclippedBoundsInRoot()
        val keypadBounds = key.getUnclippedBoundsInRoot()
        assertThat(chooseBounds.top)
            .isGreaterThan(composeTestRule.onNodeWithContentDescription(close).getUnclippedBoundsInRoot().bottom)

        composeTestRule.runOnIdle { state.value = Confirm(ADMIN) }

        val confirmBounds = composeTestRule.onNodeWithText(confirmTitle)
            .assertIsDisplayed()
            .getUnclippedBoundsInRoot()
        assertThat(confirmBounds.top).isLessThan(chooseBounds.top)
        assertThat(confirmBounds.bottom).isLessThan(dots.getUnclippedBoundsInRoot().top)
        assertThat(key.getUnclippedBoundsInRoot()).isEqualTo(keypadBounds)
    }

    @Test
    fun phoneLandscapeShowsTheBottomKeypadRowWithoutScrolling() {
        setPhoneDialog(Confirm(CASHIER), DpSize(800.dp, 340.dp))

        composeTestRule.onNodeWithText("0").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.woopos_lock_delete_digit))
            .assertIsDisplayed()
    }

    @Test
    fun shortPhoneWithLargeTextShowsTheBottomKeypadRowWithoutScrolling() {
        setPhoneDialog(Confirm(CASHIER), DpSize(800.dp, 320.dp), fontScale = 2f)

        composeTestRule.onNodeWithText("0").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.woopos_lock_delete_digit))
            .assertIsDisplayed()
    }

    @Test
    fun tabletSuccessKeepsItsHeightAfterStateRestoration() {
        val restorationTester = StateRestorationTester(composeTestRule)
        val state = mutableStateOf<WooPosSettingsSecurityPINSetupState>(Confirm(ADMIN))
        restorationTester.setContent {
            WooPosTheme {
                WooPosSettingsSecurityPINSetupDialog(
                    state.value,
                    isVisible = true,
                    isFullScreen = false,
                    onUIEvent = {},
                    onClose = {},
                    savedActionLabel = context.getString(R.string.continue_button),
                )
            }
        }
        composeTestRule.runOnIdle { state.value = Saved(ADMIN) }
        val continueNode = composeTestRule.onNodeWithText(context.getString(R.string.continue_button))
        val originalBounds = continueNode.getUnclippedBoundsInRoot()

        restorationTester.emulateSavedInstanceStateRestore()

        assertThat(continueNode.getUnclippedBoundsInRoot()).isEqualTo(originalBounds)
    }

    private fun setPhoneDialog(state: WooPosSettingsSecurityPINSetupState, size: DpSize, fontScale: Float = 1f) {
        setPhoneDialog(
            mutableStateOf(state),
            size,
            fontScale,
        )
    }

    private fun setPhoneDialog(
        state: State<WooPosSettingsSecurityPINSetupState>,
        size: DpSize,
        fontScale: Float,
        chooseTitle: String? = null,
        confirmTitle: String? = null,
    ) {
        composeTestRule.setContent {
            val configuration = DeviceConfigurationOverride.ForcedSize(size) then
                DeviceConfigurationOverride.WindowSize(size) then
                DeviceConfigurationOverride.WindowInsets(WindowInsetsCompat.CONSUMED) then
                DeviceConfigurationOverride.FontScale(fontScale)
            DeviceConfigurationOverride(configuration) {
                WooPosTheme {
                    WooPosSettingsSecurityPINSetupDialog(
                        state.value,
                        isVisible = true,
                        isFullScreen = true,
                        chooseTitle = chooseTitle,
                        confirmTitle = confirmTitle,
                        onUIEvent = {},
                        onClose = {},
                        savedActionLabel = context.getString(R.string.continue_button),
                    )
                }
            }
        }
    }

    private fun setDialog(
        state: State<WooPosSettingsSecurityPINSetupState>,
        isVisible: State<Boolean> = mutableStateOf(true),
    ) {
        composeTestRule.setContent {
            WooPosTheme {
                WooPosSettingsSecurityPINSetupDialog(
                    state = state.value,
                    isVisible = isVisible.value,
                    onUIEvent = { events += it },
                    onClose = { closeCount++ },
                    savedActionLabel = context.getString(R.string.continue_button),
                )
            }
        }
    }

    private class TestMotion : MotionDurationScale {
        override var scaleFactor: Float = 1f
    }

    private companion object {
        const val PAGE_SETTLED_MILLIS = 340L
        const val MESSAGE_REVEAL_MILLIS = 300L
    }
}
