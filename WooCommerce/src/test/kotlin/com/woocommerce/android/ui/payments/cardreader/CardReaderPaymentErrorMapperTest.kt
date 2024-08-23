package com.woocommerce.android.ui.payments.cardreader

import com.woocommerce.android.R
import com.woocommerce.android.cardreader.config.CardReaderConfigForCanada
import com.woocommerce.android.cardreader.config.CardReaderConfigForGB
import com.woocommerce.android.cardreader.config.CardReaderConfigForSupportedCountry
import com.woocommerce.android.cardreader.config.CardReaderConfigForUSA
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.CardPaymentStatusErrorType
import com.woocommerce.android.model.UiString.UiStringRes
import com.woocommerce.android.model.UiString.UiStringText
import com.woocommerce.android.ui.payments.cardreader.payment.CardReaderPaymentErrorMapper
import com.woocommerce.android.ui.payments.cardreader.payment.PaymentFlowError
import com.woocommerce.android.util.SiteIndependentCurrencyFormatter
import com.woocommerce.android.viewmodel.ResourceProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class CardReaderPaymentErrorMapperTest {
    private val resources: ResourceProvider = mock {
        on { getString(R.string.card_reader_payment_failed_amount_too_small) }
            .thenReturn("Amount must be at least %1\$s")
    }
    private val currencyFormatter: SiteIndependentCurrencyFormatter = mock()
    private val mapper = CardReaderPaymentErrorMapper(resources, currencyFormatter)
    private var config: CardReaderConfigForSupportedCountry = mock()

    @Test
    fun `given CardReaderTimeOut error, when map to ui error, then Generic error returned`() {
        // GIVEN
        val error = CardPaymentStatusErrorType.CardReadTimeOut

        // WHEN
        val result = mapper.mapPaymentErrorToUiError(error, config, false)

        // THEN
        assertThat(result).isEqualTo(PaymentFlowError.Generic)
        assertThat((result as PaymentFlowError.Generic).message)
            .isEqualTo(UiStringRes(R.string.card_reader_payment_failed_unexpected_error_state))
    }

    @Test
    fun `given NoNetwork error, when map to ui error, then NoNetwork error returned`() {
        // GIVEN
        val error = CardPaymentStatusErrorType.NoNetwork

        // WHEN
        val result = mapper.mapPaymentErrorToUiError(error, config, false)

        // THEN
        assertThat(result).isEqualTo(PaymentFlowError.NoNetwork)
        assertThat((result as PaymentFlowError.NoNetwork).message)
            .isEqualTo(UiStringRes(R.string.card_reader_payment_failed_no_network_state))
    }

    @Test
    fun `given Server error, when map to ui error, then Server error returned`() {
        // GIVEN
        val error = CardPaymentStatusErrorType.Server("")

        // WHEN
        val result = mapper.mapPaymentErrorToUiError(error, config, false)

        // THEN
        assertThat(result).isEqualTo(PaymentFlowError.Server)
        assertThat((result as PaymentFlowError.Server).message)
            .isEqualTo(UiStringRes(R.string.card_reader_payment_failed_server_error_state))
    }

    @Test
    fun `given Generic error, when map to ui error, then Generic error returned`() {
        // GIVEN
        val error = CardPaymentStatusErrorType.Generic

        // WHEN
        val result = mapper.mapPaymentErrorToUiError(error, config, false)

        // THEN
        assertThat(result).isEqualTo(PaymentFlowError.Generic)
        assertThat((result as PaymentFlowError.Generic).message)
            .isEqualTo(UiStringRes(R.string.card_reader_payment_failed_unexpected_error_state))
    }

    @Test
    fun `given UK store and AmountTooSmall error, when map to ui error, then AmountTooSmall error returned`() {
        // GIVEN
        val error = CardPaymentStatusErrorType.DeclinedByBackendError.AmountTooSmall
        config = CardReaderConfigForGB
        whenever(
            currencyFormatter.formatAmountWithCurrency(
                config.minimumAllowedChargeAmount.toDouble(),
                config.currency
            )
        ).thenReturn("£0.30")
        // WHEN
        val result = mapper.mapPaymentErrorToUiError(error, config, false)

        // THEN
        assertThat(result).isEqualTo(
            PaymentFlowError.AmountTooSmall(UiStringText("Amount must be at least £0.30"))
        )
        assertThat((result as PaymentFlowError.AmountTooSmall).message)
            .isEqualTo(UiStringText("Amount must be at least £0.30"))
    }

    @Test
    fun `given US store and AmountTooSmall error, when map to ui error, then AmountTooSmall error returned`() {
        // GIVEN
        val error = CardPaymentStatusErrorType.DeclinedByBackendError.AmountTooSmall
        config = CardReaderConfigForUSA
        whenever(
            currencyFormatter.formatAmountWithCurrency(
                config.minimumAllowedChargeAmount.toDouble(),
                config.currency
            )
        ).thenReturn("US$0.50")

        // WHEN
        val result = mapper.mapPaymentErrorToUiError(error, config, false)

        // THEN
        assertThat(result).isEqualTo(
            PaymentFlowError.AmountTooSmall(UiStringText("Amount must be at least US$0.50"))
        )
        assertThat((result as PaymentFlowError.AmountTooSmall).message)
            .isEqualTo(UiStringText("Amount must be at least US$0.50"))
    }

    @Test
    fun `given CA store and AmountTooSmall error, when map to ui error, then AmountTooSmall error returned`() {
        // GIVEN
        val error = CardPaymentStatusErrorType.DeclinedByBackendError.AmountTooSmall
        config = CardReaderConfigForCanada
        whenever(
            currencyFormatter.formatAmountWithCurrency(
                config.minimumAllowedChargeAmount.toDouble(),
                config.currency
            )
        ).thenReturn("CA$0.50")

        // WHEN
        val result = mapper.mapPaymentErrorToUiError(error, config, false)

        // THEN
        assertThat(result).isEqualTo(
            PaymentFlowError.AmountTooSmall(UiStringText("Amount must be at least CA$0.50"))
        )
        assertThat((result as PaymentFlowError.AmountTooSmall).message)
            .isEqualTo(UiStringText("Amount must be at least CA$0.50"))
    }

    @Test
    fun `given Temporary error, when map to ui error, then Temporary error returned`() {
        // GIVEN
        val error = CardPaymentStatusErrorType.DeclinedByBackendError.CardDeclined.Temporary

        // WHEN
        val result = mapper.mapPaymentErrorToUiError(error, config, false)

        // THEN
        assertThat(result).isEqualTo(PaymentFlowError.Declined.Temporary)
        assertThat((result as PaymentFlowError.Declined.Temporary).message)
            .isEqualTo(UiStringRes(R.string.card_reader_payment_failed_temporary))
    }

    @Test
    fun `given Fraud error, when map to ui error, then Fraud error returned`() {
        // GIVEN
        val error = CardPaymentStatusErrorType.DeclinedByBackendError.CardDeclined.Fraud

        // WHEN
        val result = mapper.mapPaymentErrorToUiError(error, config, false)

        // THEN
        assertThat(result).isEqualTo(PaymentFlowError.Declined.Fraud)
        assertThat((result as PaymentFlowError.Declined.Fraud).message)
            .isEqualTo(UiStringRes(R.string.card_reader_payment_failed_fraud))
    }

    @Test
    fun `given Generic card declined error, when map to ui error, then Generic error returned`() {
        // GIVEN
        val error = CardPaymentStatusErrorType.DeclinedByBackendError.CardDeclined.Generic

        // WHEN
        val result = mapper.mapPaymentErrorToUiError(error, config, false)

        // THEN
        assertThat(result).isEqualTo(PaymentFlowError.Declined.Generic)
        assertThat((result as PaymentFlowError.Declined.Generic).message)
            .isEqualTo(UiStringRes(R.string.card_reader_payment_failed_generic))
    }

    @Test
    fun `given InvalidAccount error, when map to ui error, then InvalidAccount error returned`() {
        // GIVEN
        val error = CardPaymentStatusErrorType.DeclinedByBackendError.CardDeclined.InvalidAccount

        // WHEN
        val result = mapper.mapPaymentErrorToUiError(error, config, false)

        // THEN
        assertThat(result).isEqualTo(PaymentFlowError.Declined.InvalidAccount)
        assertThat((result as PaymentFlowError.Declined.InvalidAccount).message)
            .isEqualTo(UiStringRes(R.string.card_reader_payment_failed_invalid_account))
    }

    @Test
    fun `given CardNotSupported error, when map to ui error, then CardNotSupported error returned`() {
        // GIVEN
        val error = CardPaymentStatusErrorType.DeclinedByBackendError.CardDeclined.CardNotSupported

        // WHEN
        val result = mapper.mapPaymentErrorToUiError(error, config, false)

        // THEN
        assertThat(result).isEqualTo(PaymentFlowError.Declined.CardNotSupported)
        assertThat((result as PaymentFlowError.Declined.CardNotSupported).message)
            .isEqualTo(UiStringRes(R.string.card_reader_payment_failed_card_not_supported))
    }

    @Test
    fun `given CurrencyNotSupported error, when map to ui error, then CurrencyNotSupported error returned`() {
        // GIVEN
        val error = CardPaymentStatusErrorType.DeclinedByBackendError.CardDeclined.CurrencyNotSupported

        // WHEN
        val result = mapper.mapPaymentErrorToUiError(error, config, false)

        // THEN
        assertThat(result).isEqualTo(PaymentFlowError.Declined.CurrencyNotSupported)
        assertThat((result as PaymentFlowError.Declined.CurrencyNotSupported).message)
            .isEqualTo(UiStringRes(R.string.card_reader_payment_failed_currency_not_supported))
    }

    @Test
    fun `given DuplicateTransaction error, when map to ui error, then DuplicateTransaction error returned`() {
        // GIVEN
        val error = CardPaymentStatusErrorType.DeclinedByBackendError.CardDeclined.DuplicateTransaction

        // WHEN
        val result = mapper.mapPaymentErrorToUiError(error, config, false)

        // THEN
        assertThat(result).isEqualTo(PaymentFlowError.Declined.DuplicateTransaction)
        assertThat((result as PaymentFlowError.Declined.DuplicateTransaction).message)
            .isEqualTo(UiStringRes(R.string.card_reader_payment_failed_duplicate_transaction))
    }

    @Test
    fun `given ExpiredCard error, when map to ui error, then ExpiredCard error returned`() {
        // GIVEN
        val error = CardPaymentStatusErrorType.DeclinedByBackendError.CardDeclined.ExpiredCard

        // WHEN
        val result = mapper.mapPaymentErrorToUiError(error, config, false)

        // THEN
        assertThat(result).isEqualTo(PaymentFlowError.Declined.ExpiredCard)
        assertThat((result as PaymentFlowError.Declined.ExpiredCard).message)
            .isEqualTo(UiStringRes(R.string.card_reader_payment_failed_expired_card))
    }

    @Test
    fun `given IncorrectPostalCode error, when map to ui error, then IncorrectPostalCode error returned`() {
        // GIVEN
        val error = CardPaymentStatusErrorType.DeclinedByBackendError.CardDeclined.IncorrectPostalCode

        // WHEN
        val result = mapper.mapPaymentErrorToUiError(error, config, false)

        // THEN
        assertThat(result).isEqualTo(PaymentFlowError.Declined.IncorrectPostalCode)
        assertThat((result as PaymentFlowError.Declined.IncorrectPostalCode).message)
            .isEqualTo(UiStringRes(R.string.card_reader_payment_failed_incorrect_postal_code))
    }

    @Test
    fun `given InvalidAmount error, when map to ui error, then InvalidAmount error returned`() {
        // GIVEN
        val error = CardPaymentStatusErrorType.DeclinedByBackendError.CardDeclined.InvalidAmount

        // WHEN
        val result = mapper.mapPaymentErrorToUiError(error, config, false)

        // THEN
        assertThat(result).isEqualTo(PaymentFlowError.Declined.InvalidAmount)
        assertThat((result as PaymentFlowError.Declined.InvalidAmount).message)
            .isEqualTo(UiStringRes(R.string.card_reader_payment_failed_invalid_amount))
    }

    @Test
    fun `given PinRequired error and hw reader, when map to ui error, then PinRequired error returned`() {
        // GIVEN
        val error = CardPaymentStatusErrorType.DeclinedByBackendError.CardDeclined.PinRequired

        // WHEN
        val result = mapper.mapPaymentErrorToUiError(error, config, false)

        // THEN
        assertThat(result).isEqualTo(PaymentFlowError.Declined.PinRequired)
        assertThat((result as PaymentFlowError.Declined.PinRequired).message)
            .isEqualTo(UiStringRes(R.string.card_reader_payment_failed_pin_required))
    }

    @Test
    fun `given PinRequired error and built in, when map to ui error, then PinRequired error returned`() {
        // GIVEN
        val error = CardPaymentStatusErrorType.DeclinedByBackendError.CardDeclined.PinRequired

        // WHEN
        val result = mapper.mapPaymentErrorToUiError(error, config, true)

        // THEN
        assertThat(result).isEqualTo(PaymentFlowError.BuiltInReader.PinRequired)
        assertThat((result as PaymentFlowError.BuiltInReader.PinRequired).message)
            .isEqualTo(UiStringRes(R.string.card_reader_payment_failed_pin_required_tap_to_pay))
    }

    @Test
    fun `given IncorrectPin error and hw reader, when map to ui error, then IncorrectPin error returned`() {
        // GIVEN
        val error = CardPaymentStatusErrorType.DeclinedByBackendError.CardDeclined.IncorrectPin

        // WHEN
        val result = mapper.mapPaymentErrorToUiError(error, config, false)

        // THEN
        assertThat(result).isEqualTo(PaymentFlowError.Declined.IncorrectPin)
        assertThat((result as PaymentFlowError.Declined.IncorrectPin).message)
            .isEqualTo(UiStringRes(R.string.card_reader_payment_failed_incorrect_pin))
    }

    @Test
    fun `given TooManyPinTries error, when map to ui error, then TooManyPinTries error returned`() {
        // GIVEN
        val error = CardPaymentStatusErrorType.DeclinedByBackendError.CardDeclined.TooManyPinTries

        // WHEN
        val result = mapper.mapPaymentErrorToUiError(error, config, false)

        // THEN
        assertThat(result).isEqualTo(PaymentFlowError.Declined.TooManyPinTries)
        assertThat((result as PaymentFlowError.Declined.TooManyPinTries).message)
            .isEqualTo(UiStringRes(R.string.card_reader_payment_failed_too_many_pin_tries))
    }

    @Test
    fun `given TestCard error, when map to ui error, then TestCard error returned`() {
        // GIVEN
        val error = CardPaymentStatusErrorType.DeclinedByBackendError.CardDeclined.TestCard

        // WHEN
        val result = mapper.mapPaymentErrorToUiError(error, config, false)

        // THEN
        assertThat(result).isEqualTo(PaymentFlowError.Declined.TestCard)
        assertThat((result as PaymentFlowError.Declined.TestCard).message)
            .isEqualTo(UiStringRes(R.string.card_reader_payment_failed_test_card))
    }

    @Test
    fun `given TestModeLiveCard error, when map to ui error, then TestModeLiveCard error returned`() {
        // GIVEN
        val error = CardPaymentStatusErrorType.DeclinedByBackendError.CardDeclined.TestModeLiveCard

        // WHEN
        val result = mapper.mapPaymentErrorToUiError(error, config, false)

        // THEN
        assertThat(result).isEqualTo(PaymentFlowError.Declined.TestModeLiveCard)
        assertThat((result as PaymentFlowError.Declined.TestModeLiveCard).message)
            .isEqualTo(UiStringRes(R.string.card_reader_payment_failed_test_mode_live_card))
    }

    @Test
    fun `given Unknown error, when map to ui error, then Unknown error returned`() {
        // GIVEN
        val error = CardPaymentStatusErrorType.DeclinedByBackendError.Unknown

        // WHEN
        val result = mapper.mapPaymentErrorToUiError(error, config, false)

        // THEN
        assertThat(result).isEqualTo(PaymentFlowError.Unknown)
        assertThat((result as PaymentFlowError.Unknown).message)
            .isEqualTo(UiStringRes(R.string.card_reader_payment_failed_unknown))
    }

    @Test
    fun `given Canceled error, when map to ui error, then Canceled error returned`() {
        // GIVEN
        val error = CardPaymentStatusErrorType.Canceled

        // WHEN
        val result = mapper.mapPaymentErrorToUiError(error, config, false)

        // THEN
        assertThat(result).isEqualTo(PaymentFlowError.Canceled)
        assertThat((result as PaymentFlowError.Canceled).message)
            .isEqualTo(UiStringRes(R.string.card_reader_payment_failed_canceled))
    }

    @Test
    fun `given NfcDisabled error, when map to ui error, then NfcDisable error returned`() {
        // GIVEN
        val error = CardPaymentStatusErrorType.BuiltInReader.NfcDisabled

        // WHEN
        val result = mapper.mapPaymentErrorToUiError(error, config, false)

        // THEN
        assertThat(result).isEqualTo(PaymentFlowError.BuiltInReader.NfcDisabled)
        assertThat((result as PaymentFlowError.BuiltInReader.NfcDisabled).message)
            .isEqualTo(UiStringRes(R.string.card_reader_payment_failed_nfc_disabled))
    }

    @Test
    fun `given DeviceIsNotSupported error, when map to ui error, then DeviceIsNotSupported error returned`() {
        // GIVEN
        val error = CardPaymentStatusErrorType.BuiltInReader.DeviceIsNotSupported

        // WHEN
        val result = mapper.mapPaymentErrorToUiError(error, config, false)

        // THEN
        assertThat(result).isEqualTo(PaymentFlowError.BuiltInReader.DeviceIsNotSupported)
        assertThat((result as PaymentFlowError.BuiltInReader.DeviceIsNotSupported).message)
            .isEqualTo(UiStringRes(R.string.card_reader_payment_failed_device_is_not_supported))
    }

    @Test
    fun `given InvalidAppSetup error, when map to ui error, then InvalidAppSetup error returned`() {
        // GIVEN
        val error = CardPaymentStatusErrorType.BuiltInReader.InvalidAppSetup

        // WHEN
        val result = mapper.mapPaymentErrorToUiError(error, config, false)

        // THEN
        assertThat(result).isEqualTo(PaymentFlowError.BuiltInReader.InvalidAppSetup)
        assertThat((result as PaymentFlowError.BuiltInReader.InvalidAppSetup).message)
            .isEqualTo(UiStringRes(R.string.card_reader_payment_failed_app_setup_is_invalid))
    }
}
