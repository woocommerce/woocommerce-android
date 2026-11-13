package com.woocommerce.android.ui.woopos.settings.details.security

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class WooPosMockPinSetupTest {
    private val fixture = WooPosMockPinSetup()
    private val sut: WooPosPinSetup = fixture

    @Test
    fun `given four key presses, when choosing a PIN, then confirmation starts with empty dots`() {
        // WHEN
        repeat(4) { sut.onUIEvent(WooPosPinSetupUIEvent.DigitTapped(it)) }

        // THEN
        assertThat(sut.state.value).isEqualTo(WooPosPinSetupState(stage = WooPosPinSetupState.Stage.CONFIRM))
    }

    @Test
    fun `given confirmation, when Back is tapped, then entry count and stage reset`() {
        // GIVEN
        fixture.show(WooPosPinSetupState(stage = WooPosPinSetupState.Stage.CONFIRM, enteredDigits = 2))

        // WHEN
        sut.onUIEvent(WooPosPinSetupUIEvent.BackTapped)

        // THEN
        assertThat(sut.state.value).isEqualTo(WooPosPinSetupState())
    }

    @Test
    fun `given mismatch, when a digit is entered, then mismatch clears`() {
        // GIVEN
        fixture.show(WooPosPinSetupState(mismatch = true))

        // WHEN
        sut.onUIEvent(WooPosPinSetupUIEvent.DigitTapped(1))

        // THEN
        assertThat(sut.state.value).isEqualTo(WooPosPinSetupState(enteredDigits = 1))
    }

    @Test
    fun `given checking, when keys are pressed, then input does not change`() {
        // GIVEN
        val checking = WooPosPinSetupState(stage = WooPosPinSetupState.Stage.CHECKING, enteredDigits = 4)
        fixture.show(checking)

        // WHEN
        sut.onUIEvent(WooPosPinSetupUIEvent.DigitTapped(1))
        sut.onUIEvent(WooPosPinSetupUIEvent.DeleteTapped)

        // THEN
        assertThat(sut.state.value).isEqualTo(checking)
    }

    @Test
    fun `given confirmation, when four arbitrary digits are entered, then mock stops at checking without saving`() {
        // GIVEN
        fixture.show(WooPosPinSetupState(stage = WooPosPinSetupState.Stage.CONFIRM))

        // WHEN
        repeat(4) { sut.onUIEvent(WooPosPinSetupUIEvent.DigitTapped(9)) }

        // THEN
        assertThat(sut.state.value.stage).isEqualTo(WooPosPinSetupState.Stage.CHECKING)
        assertThat(sut.state.value.enteredDigits).isEqualTo(4)
    }
}
