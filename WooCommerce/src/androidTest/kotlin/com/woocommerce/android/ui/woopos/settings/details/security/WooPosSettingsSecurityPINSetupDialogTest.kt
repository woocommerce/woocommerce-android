package com.woocommerce.android.ui.woopos.settings.details.security

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.roles.WooPosLocalRole.ADMIN
import com.woocommerce.android.ui.woopos.roles.WooPosLocalRole.CASHIER
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
    fun heldConfirmationDisablesKeysAndIgnoresClose() {
        setDialog(mutableStateOf(Confirm(ADMIN, enteredDigits = 4)))

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
    fun mismatchMessageAppearsAfterTheDotsReturn() {
        val state = mutableStateOf<WooPosSettingsSecurityPINSetupState>(Confirm(ADMIN, enteredDigits = 4))
        setDialog(state)
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.autoAdvance = false

        // WHEN
        state.value = Choose(ADMIN, error = Error.MISMATCH)
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
    fun takenCashierPINShowsCashierTitleAndTakenMessage() {
        setDialog(mutableStateOf(Choose(CASHIER, error = Error.PIN_TAKEN)))

        // THEN
        composeTestRule.onNodeWithText(context.getString(R.string.woopos_security_choose_cashier_pin))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.woopos_security_pin_taken)).assertIsDisplayed()
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
