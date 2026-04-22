package com.woocommerce.android.ui.payments.cardreader.readermode

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.woocommerce.android.ui.payments.cardreader.payment.RemoteTapToPayReadyToPair
import com.woocommerce.android.ui.payments.cardreader.payment.ViewState
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Rule
import org.junit.Test

class CardReaderModeStateBridgeTest {

    @Rule
    @JvmField
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Test
    fun `given non-remote state, when pushed, then require throws`() {
        // GIVEN
        val bridge = CardReaderModeStateBridge()

        // WHEN // THEN
        assertThatThrownBy { bridge.push(ViewState.ReFetchingOrderState) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `given override pushed, when cleared, then stateOverride is null`() {
        // GIVEN
        val bridge = CardReaderModeStateBridge()
        bridge.push(
            RemoteTapToPayReadyToPair(deviceName = "Pixel 7", fingerprintSuffix = "AB4F", onPrimaryActionClicked = {})
        )

        // WHEN
        bridge.clear()

        // THEN
        assertThat(bridge.stateOverride.value).isNull()
    }
}
