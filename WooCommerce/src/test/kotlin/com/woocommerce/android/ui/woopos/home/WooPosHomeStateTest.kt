package com.woocommerce.android.ui.woopos.home

import com.woocommerce.android.ui.woopos.home.WooPosHomeState.DialogState
import com.woocommerce.android.ui.woopos.home.WooPosHomeState.ScreenPositionState
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class WooPosHomeStateTest {

    @Test
    fun `given Cart position and no dialog, when checking phone cart sheet visibility, then visible`() {
        // GIVEN
        val state = WooPosHomeState(screenPositionState = ScreenPositionState.Cart, dialogState = DialogState.Hidden)

        // WHEN
        val result = state.isPhoneCartSheetVisible

        // THEN
        assertThat(result).isTrue()
    }

    @Test
    fun `given Cart position and scanner setup dialog, when checking phone cart sheet visibility, then hidden`() {
        // GIVEN
        val state = WooPosHomeState(
            screenPositionState = ScreenPositionState.Cart,
            dialogState = DialogState.ScanningSetupDialog,
        )

        // WHEN
        val result = state.isPhoneCartSheetVisible

        // THEN
        assertThat(result).isFalse()
    }
}
