package com.woocommerce.android.ui.payments.cardreader.payment

import com.woocommerce.android.R
import com.woocommerce.android.model.UiString.UiStringRes
import com.woocommerce.android.model.UiString.UiStringText
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderType
import com.woocommerce.android.ui.payments.cardreader.payment.ViewState.BuiltInReaderCapturingPaymentState
import com.woocommerce.android.ui.payments.cardreader.payment.ViewState.BuiltInReaderCollectPaymentState
import com.woocommerce.android.ui.payments.cardreader.payment.ViewState.BuiltInReaderFailedPaymentState
import com.woocommerce.android.ui.payments.cardreader.payment.ViewState.BuiltInReaderPaymentSuccessfulReceiptSentAutomaticallyState
import com.woocommerce.android.ui.payments.cardreader.payment.ViewState.BuiltInReaderPaymentSuccessfulState
import com.woocommerce.android.ui.payments.cardreader.payment.ViewState.BuiltInReaderProcessingPaymentState
import com.woocommerce.android.ui.payments.cardreader.payment.ViewState.ExternalReaderCapturingPaymentState
import com.woocommerce.android.ui.payments.cardreader.payment.ViewState.ExternalReaderCollectPaymentState
import com.woocommerce.android.ui.payments.cardreader.payment.ViewState.ExternalReaderFailedPaymentState
import com.woocommerce.android.ui.payments.cardreader.payment.ViewState.ExternalReaderPaymentSuccessfulReceiptSentAutomaticallyState
import com.woocommerce.android.ui.payments.cardreader.payment.ViewState.ExternalReaderPaymentSuccessfulState
import com.woocommerce.android.ui.payments.cardreader.payment.ViewState.ExternalReaderProcessingPaymentState
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class CardReaderPaymentReaderTypeStateProviderTest {
    private val provider = CardReaderPaymentReaderTypeStateProvider()

    @Test
    fun `given built in card reader type, when provideCollectPaymentState, then return BuiltInReaderCollectPaymentState`() {
        // GIVEN
        val cardReaderType = CardReaderType.BUILT_IN

        // WHEN
        val result = provider.provideCollectPaymentState(
            cardReaderType,
            "amountLabel"
        ) {}

        // THEN
        assertThat(result).isInstanceOf(BuiltInReaderCollectPaymentState::class.java)
        assertThat(result.amountWithCurrencyLabel).isEqualTo("amountLabel")
    }

    @Test
    fun `given external card reader type, when provideCollectPaymentState, then return ExternalReaderCollectPaymentState`() {
        // GIVEN
        val cardReaderType = CardReaderType.EXTERNAL

        // WHEN
        val result = provider.provideCollectPaymentState(
            cardReaderType,
            "amountLabel",
            {}
        )

        // THEN
        assertThat(result).isInstanceOf(ExternalReaderCollectPaymentState::class.java)
        assertThat(result.amountWithCurrencyLabel).isEqualTo("amountLabel")
    }

    @Test
    fun `given built in card reader type, when provideProcessingPaymentState, then return BuiltInReaderProcessingPaymentState`() {
        // GIVEN
        val cardReaderType = CardReaderType.BUILT_IN

        // WHEN
        val result = provider.provideProcessingPaymentState(
            cardReaderType,
            "amountLabel"
        ) {}

        // THEN
        assertThat(result).isInstanceOf(BuiltInReaderProcessingPaymentState::class.java)
        assertThat(result.amountWithCurrencyLabel).isEqualTo("amountLabel")
    }

    @Test
    fun `given external card reader type, when provideProcessingPaymentState, then return ExternalReaderProcessingPaymentState`() {
        // GIVEN
        val cardReaderType = CardReaderType.EXTERNAL

        // WHEN
        val result = provider.provideProcessingPaymentState(
            cardReaderType,
            "amountLabel"
        ) {}

        // THEN
        assertThat(result).isInstanceOf(ExternalReaderProcessingPaymentState::class.java)
        assertThat(result.amountWithCurrencyLabel).isEqualTo("amountLabel")
    }

    @Test
    fun `given built in card reader type, when providePaymentSuccessState, then return BuiltInReaderPaymentSuccessfulState`() {
        // GIVEN
        val cardReaderType = CardReaderType.BUILT_IN

        // WHEN
        val result = provider.providePaymentSuccessState(
            cardReaderType,
            "amountLabel",
            {},
            {},
            {},
        )

        // THEN
        assertThat(result).isInstanceOf(BuiltInReaderPaymentSuccessfulState::class.java)
        assertThat(result.amountWithCurrencyLabel).isEqualTo("amountLabel")
    }

    @Test
    fun `given external card reader type, when providePaymentSuccessState, then return ExternalReaderPaymentSuccessfulState`() {
        // GIVEN
        val cardReaderType = CardReaderType.EXTERNAL

        // WHEN
        val result = provider.providePaymentSuccessState(
            cardReaderType,
            "amountLabel",
            {},
            {},
            {},
        )

        // THEN
        assertThat(result).isInstanceOf(ExternalReaderPaymentSuccessfulState::class.java)
        assertThat(result.amountWithCurrencyLabel).isEqualTo("amountLabel")
    }

    @Test
    fun `given built in card reader type, when providePaymentSuccessfulReceiptSentAutomaticallyState, then return BuiltInReaderPaymentSuccessfulState`() {
        // GIVEN
        val cardReaderType = CardReaderType.BUILT_IN

        // WHEN
        val result = provider.providePaymentSuccessfulReceiptSentAutomaticallyState(
            cardReaderType,
            "amountLabel",
            UiStringRes(R.string.all),
            {},
            {},
        )

        // THEN
        assertThat(result).isInstanceOf(BuiltInReaderPaymentSuccessfulReceiptSentAutomaticallyState::class.java)
        assertThat(result.amountWithCurrencyLabel).isEqualTo("amountLabel")
        assertThat(result.receiptSentAutomaticallyHint).isEqualTo(UiStringRes(R.string.all))
    }

    @Test
    fun `given external card reader type, when providePaymentSuccessfulReceiptSentAutomaticallyState, then return ExternalReaderPaymentSuccessfulReceiptSentAutomaticallyState`() {
        // GIVEN
        val cardReaderType = CardReaderType.EXTERNAL

        // WHEN
        val result = provider.providePaymentSuccessfulReceiptSentAutomaticallyState(
            cardReaderType,
            "amountLabel",
            UiStringRes(android.R.string.ok),
            {},
            {},
        )

        // THEN
        assertThat(result).isInstanceOf(ExternalReaderPaymentSuccessfulReceiptSentAutomaticallyState::class.java)
        assertThat(result.amountWithCurrencyLabel).isEqualTo("amountLabel")
        assertThat(result.receiptSentAutomaticallyHint).isEqualTo(
            UiStringRes(
                android.R.string.ok
            )
        )
    }

    @Test
    fun `given built in card reader type, when provideFailedPaymentState, then return BuiltInReaderPaymentFailedState`() {
        // GIVEN
        val cardReaderType = CardReaderType.BUILT_IN
        val error = PaymentFlowError.AmountTooSmall(UiStringText("Amount must be at least US$0.50"))

        // WHEN
        val result = provider.provideFailedPaymentState(
            cardReaderType,
            errorType = error,
            amountLabel = "amountLabel",
            primaryLabel = android.R.string.ok,
            {},
        )

        // THEN
        assertThat(result).isInstanceOf(BuiltInReaderFailedPaymentState::class.java)
        assertThat(result.amountWithCurrencyLabel).isEqualTo("amountLabel")
        assertThat(result.primaryActionLabel).isEqualTo(android.R.string.ok)
        assertThat(result.paymentStateLabel).isEqualTo(error.message)
    }

    @Test
    fun `given external card reader type, when provideFailedPaymentState, then return ExternalReaderPaymentFailedState`() {
        // GIVEN
        val cardReaderType = CardReaderType.EXTERNAL
        val error = PaymentFlowError.AmountTooSmall(UiStringText("Amount must be at least US$0.50"))

        // WHEN
        val result = provider.provideFailedPaymentState(
            cardReaderType,
            errorType = error,
            amountLabel = "amountLabel",
            primaryLabel = android.R.string.ok,
            {},
        )

        // THEN
        assertThat(result).isInstanceOf(ExternalReaderFailedPaymentState::class.java)
        assertThat(result.amountWithCurrencyLabel).isEqualTo("amountLabel")
        assertThat(result.primaryActionLabel).isEqualTo(android.R.string.ok)
        assertThat(result.paymentStateLabel).isEqualTo(error.message)
    }

    @Test
    fun `given built in card reader type, when provideCapturingPaymentState, then return BuiltInReaderCapturingPaymentState`() {
        // GIVEN
        val cardReaderType = CardReaderType.BUILT_IN

        // WHEN
        val result = provider.provideCapturingPaymentState(
            cardReaderType,
            "amountLabel",
        )

        // THEN
        assertThat(result).isInstanceOf(BuiltInReaderCapturingPaymentState::class.java)
        assertThat(result.amountWithCurrencyLabel).isEqualTo("amountLabel")
    }

    @Test
    fun `given external card reader type, when provideCapturingPaymentState, then return ExternalReaderCapturingPaymentState`() {
        // GIVEN
        val cardReaderType = CardReaderType.EXTERNAL

        // WHEN
        val result = provider.provideCapturingPaymentState(
            cardReaderType,
            "amountLabel",
        )

        // THEN
        assertThat(result).isInstanceOf(ExternalReaderCapturingPaymentState::class.java)
        assertThat(result.amountWithCurrencyLabel).isEqualTo("amountLabel")
    }
}
