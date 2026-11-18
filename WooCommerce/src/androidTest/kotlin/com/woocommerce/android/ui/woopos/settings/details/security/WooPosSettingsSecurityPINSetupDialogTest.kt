package com.woocommerce.android.ui.woopos.settings.details.security

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.settings.details.security.WooPosSettingsSecurityPINSetupState.Choose
import com.woocommerce.android.ui.woopos.settings.details.security.WooPosSettingsSecurityPINSetupState.Confirm
import com.woocommerce.android.ui.woopos.settings.details.security.WooPosSettingsSecurityPINSetupState.Error
import com.woocommerce.android.ui.woopos.settings.details.security.WooPosSettingsSecurityPINSetupState.Saved
import com.woocommerce.android.ui.woopos.settings.details.security.WooPosSettingsSecurityPINSetupState.Saving
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WooPosSettingsSecurityPINSetupDialogTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val back: String get() = context.getString(R.string.back)
    private val close: String get() = context.getString(R.string.close)
    private val events = mutableListOf<WooPosSettingsSecurityPINSetupUIEvent>()
    private var closeCount = 0

    @Test
    fun confirmationOffersBackWhileChooseOffersOnlyClose() {
        val state = mutableStateOf<WooPosSettingsSecurityPINSetupState>(Choose(enteredDigits = 1))
        setDialog(state)

        composeTestRule.onNodeWithText(context.getString(R.string.woopos_security_choose_pin)).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(back).assertDoesNotExist()

        // WHEN
        state.value = Confirm(enteredDigits = 2)
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
    fun heldConfirmationDisablesKeysAndIgnoresClose() {
        setDialog(mutableStateOf(Confirm(enteredDigits = 4)))

        // WHEN
        composeTestRule.onNodeWithContentDescription(close).performClick()
        Espresso.pressBack()

        // THEN
        composeTestRule.onNodeWithText("1").assertIsNotEnabled()
        assertThat(events).isEmpty()
        assertThat(closeCount).isZero()
    }

    @Test
    fun savingShowsOnlyProgressAndCannotClose() {
        setDialog(mutableStateOf(Saving))

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
    fun mismatchMessageAppearsAfterTheDotsReturn() {
        val state = mutableStateOf<WooPosSettingsSecurityPINSetupState>(Confirm(enteredDigits = 4))
        setDialog(state)
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.autoAdvance = false

        // WHEN
        state.value = Choose(error = Error.MISMATCH)
        composeTestRule.mainClock.advanceTimeByFrame()

        // THEN
        val mismatch = context.getString(R.string.woopos_security_mismatch)
        composeTestRule.onNodeWithText(mismatch).assertDoesNotExist()

        // WHEN
        composeTestRule.mainClock.advanceTimeBy(MESSAGE_SHOWN_MILLIS)

        // THEN
        composeTestRule.onNodeWithText(mismatch).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.woopos_security_choose_pin)).assertIsDisplayed()
    }

    @Test
    fun successContinueClosesTheFlow() {
        setDialog(mutableStateOf(Saved))

        // WHEN
        composeTestRule.onNodeWithText(context.getString(R.string.continue_button)).assertIsEnabled().performClick()

        // THEN
        composeTestRule.onNodeWithText(context.getString(R.string.woopos_security_success)).assertIsDisplayed()
        assertThat(closeCount).isEqualTo(1)
    }

    @Test
    fun closingKeepsTheLastStepWhileTheFlowResetsBehindIt() {
        val state = mutableStateOf<WooPosSettingsSecurityPINSetupState>(Saved)
        val isVisible = mutableStateOf(true)
        setDialog(state, isVisible)
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.autoAdvance = false

        // WHEN
        isVisible.value = false
        composeTestRule.mainClock.advanceTimeByFrame()
        state.value = Choose()
        composeTestRule.mainClock.advanceTimeByFrame()

        // THEN
        composeTestRule.onNodeWithText(context.getString(R.string.woopos_security_success)).assertExists()
        composeTestRule.onNodeWithText(context.getString(R.string.woopos_security_choose_pin)).assertDoesNotExist()
    }

    @Test
    fun keypadTapsDuringTheClosingAnimationAreIgnored() {
        val isVisible = mutableStateOf(true)
        setDialog(mutableStateOf(Choose(enteredDigits = 1)), isVisible)
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
    fun phoneSuccessPlacesCloseNearTheTopAndContinueNearTheBottom() {
        setPhoneDialog(Saved, DpSize(400.dp, 800.dp))

        val closeBounds = composeTestRule.onNodeWithContentDescription(close).getUnclippedBoundsInRoot()
        val continueBounds = composeTestRule.onNodeWithText(context.getString(R.string.continue_button))
            .assertIsDisplayed()
            .getUnclippedBoundsInRoot()

        assertThat((continueBounds.top - closeBounds.bottom).value).isGreaterThan(580f)
        assertThat((continueBounds.right - continueBounds.left).value).isGreaterThan(320f)
    }

    @Test
    fun phoneLandscapeShowsTheBottomKeypadRowWithoutScrolling() {
        setPhoneDialog(Confirm(), DpSize(800.dp, 380.dp))

        composeTestRule.onNodeWithText("0").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.woopos_lock_delete_digit))
            .assertIsDisplayed()
    }

    private fun setPhoneDialog(state: WooPosSettingsSecurityPINSetupState, size: DpSize) {
        composeTestRule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(size)) {
                val windowInfo = LocalWindowInfo.current
                val windowSize = with(LocalDensity.current) { IntSize(size.width.roundToPx(), size.height.roundToPx()) }
                CompositionLocalProvider(
                    LocalWindowInfo provides object : WindowInfo by windowInfo {
                        override val containerSize = windowSize
                    }
                ) {
                    WooPosTheme {
                        WooPosSettingsSecurityPINSetupDialog(state, isVisible = true, onUIEvent = {}, onClose = {})
                    }
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
                )
            }
        }
    }

    private companion object {
        const val MESSAGE_SHOWN_MILLIS = 500L
    }
}
