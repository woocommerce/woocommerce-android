package com.woocommerce.android.ui.payments.cardreader.payment

import com.woocommerce.android.R
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class RemoteTapToPayPaymentViewStateTest {

    @Test
    fun `given RemoteTapToPayReadyToPair state, when rendered, then device name, fingerprint and cancel action are exposed`() {
        // GIVEN
        val state = RemoteTapToPayReadyToPair(
            deviceName = "Pixel 7",
            fingerprintSuffix = "AB4F",
            siteUrl = "example.com",
            onPrimaryActionClicked = {}
        )

        // WHEN // THEN
        assertThat(state.deviceName).isEqualTo("Pixel 7")
        assertThat(state.fingerprintSuffix).isEqualTo("AB4F")
        assertThat(state.siteUrl).isEqualTo("example.com")
        assertThat(state.primaryActionLabel).isEqualTo(R.string.card_reader_mode_cancel)
    }

    @Test
    fun `given RemoteTapToPayWaitingForPayment state, when rendered, then tablet name and cancel action are exposed`() {
        // GIVEN
        val state = RemoteTapToPayWaitingForPayment(
            tabletName = "Studio iPad",
            onPrimaryActionClicked = {}
        )

        // WHEN // THEN
        assertThat(state.tabletName).isEqualTo("Studio iPad")
        assertThat(state.primaryActionLabel).isEqualTo(R.string.card_reader_mode_cancel)
    }
}
