package com.woocommerce.android.ui.cardreader

import com.woocommerce.android.R
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.CardPaymentStatusErrorType
import com.woocommerce.android.ui.cardreader.payment.CardReaderPaymentErrorMapper
import com.woocommerce.android.ui.cardreader.payment.PaymentFlowError
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class CardReaderPaymentErrorMapperTest {
    private val mapper = CardReaderPaymentErrorMapper()

    @Test
    fun `given CardReaderTimeOut error, when map to ui error, then Generic error returned`() {
        // GIVEN
        val error = CardPaymentStatusErrorType.CardReadTimeOut

        // WHEN
        val result = mapper.mapPaymentErrorToUiError(error)

        // THEN
        assertThat(result).isEqualTo(PaymentFlowError.Generic)
        assertThat((result as PaymentFlowError.Generic).message)
            .isEqualTo(R.string.card_reader_payment_failed_unexpected_error_state)
    }

    @Test
    fun `given NoNetwork error, when map to ui error, then NoNetwork error returned`() {
        // GIVEN
        val error = CardPaymentStatusErrorType.NoNetwork

        // WHEN
        val result = mapper.mapPaymentErrorToUiError(error)

        // THEN
        assertThat(result).isEqualTo(PaymentFlowError.NoNetwork)
        assertThat((result as PaymentFlowError.NoNetwork).message)
            .isEqualTo(R.string.card_reader_payment_failed_no_network_state)
    }

    @Test
    fun `given Server error, when map to ui error, then Server error returned`() {
        // GIVEN
        val error = CardPaymentStatusErrorType.Server

        // WHEN
        val result = mapper.mapPaymentErrorToUiError(error)

        // THEN
        assertThat(result).isEqualTo(PaymentFlowError.Server)
        assertThat((result as PaymentFlowError.Server).message)
            .isEqualTo(R.string.card_reader_payment_failed_server_error_state)
    }

    @Test
    fun `given Generic error, when map to ui error, then Generic error returned`() {
        // GIVEN
        val error = CardPaymentStatusErrorType.Generic

        // WHEN
        val result = mapper.mapPaymentErrorToUiError(error)

        // THEN
        assertThat(result).isEqualTo(PaymentFlowError.Generic)
        assertThat((result as PaymentFlowError.Generic).message)
            .isEqualTo(R.string.card_reader_payment_failed_unexpected_error_state)
    }

    @Test
    fun `given AmountTooSmall error, when map to ui error, then AmountTooSmall error returned`() {
        // GIVEN
        val error = CardPaymentStatusErrorType.DeclinedByBackendError.AmountTooSmall

        // WHEN
        val result = mapper.mapPaymentErrorToUiError(error)

        // THEN
        assertThat(result).isEqualTo(PaymentFlowError.AmountTooSmall)
        assertThat((result as PaymentFlowError.AmountTooSmall).message)
            .isEqualTo(R.string.card_reader_payment_failed_amount_too_small)
    }

    @Test
    fun `given Temporary error, when map to ui error, then Temporary error returned`() {
        // GIVEN
        val error = CardPaymentStatusErrorType.DeclinedByBackendError.CardDeclined.Temporary

        // WHEN
        val result = mapper.mapPaymentErrorToUiError(error)

        // THEN
        assertThat(result).isEqualTo(PaymentFlowError.Declined.Temporary)
        assertThat((result as PaymentFlowError.Declined.Temporary).message)
            .isEqualTo(R.string.card_reader_payment_failed_temporary)
    }

    @Test
    fun `given Fraud error, when map to ui error, then Fraud error returned`() {
        // GIVEN
        val error = CardPaymentStatusErrorType.DeclinedByBackendError.CardDeclined.Fraud

        // WHEN
        val result = mapper.mapPaymentErrorToUiError(error)

        // THEN
        assertThat(result).isEqualTo(PaymentFlowError.Declined.Fraud)
        assertThat((result as PaymentFlowError.Declined.Fraud).message)
            .isEqualTo(R.string.card_reader_payment_failed_fraud)
    }

    @Test
    fun `given Generic card declined error, when map to ui error, then Generic error returned`() {
        // GIVEN
        val error = CardPaymentStatusErrorType.DeclinedByBackendError.CardDeclined.Generic

        // WHEN
        val result = mapper.mapPaymentErrorToUiError(error)

        // THEN
        assertThat(result).isEqualTo(PaymentFlowError.Declined.Generic)
        assertThat((result as PaymentFlowError.Declined.Generic).message)
            .isEqualTo(R.string.card_reader_payment_failed_generic)
    }

    @Test
    fun `given InvalidAccount error, when map to ui error, then InvalidAccount error returned`() {
        // GIVEN
        val error = CardPaymentStatusErrorType.DeclinedByBackendError.CardDeclined.InvalidAccount

        // WHEN
        val result = mapper.mapPaymentErrorToUiError(error)

        // THEN
        assertThat(result).isEqualTo(PaymentFlowError.Declined.InvalidAccount)
        assertThat((result as PaymentFlowError.Declined.InvalidAccount).message)
            .isEqualTo(R.string.card_reader_payment_failed_invalid_account)
    }

    @Test
    fun `given CardNotSupported error, when map to ui error, then CardNotSupported error returned`() {
        // GIVEN
        val error = CardPaymentStatusErrorType.DeclinedByBackendError.CardDeclined.CardNotSupported

        // WHEN
        val result = mapper.mapPaymentErrorToUiError(error)

        // THEN
        assertThat(result).isEqualTo(PaymentFlowError.Declined.CardNotSupported)
        assertThat((result as PaymentFlowError.Declined.CardNotSupported).message)
            .isEqualTo(R.string.card_reader_payment_failed_card_not_supported)
    }

    @Test
    fun `given CurrencyNotSupported error, when map to ui error, then CurrencyNotSupported error returned`() {
        // GIVEN
        val error = CardPaymentStatusErrorType.DeclinedByBackendError.CardDeclined.CurrencyNotSupported

        // WHEN
        val result = mapper.mapPaymentErrorToUiError(error)

        // THEN
        assertThat(result).isEqualTo(PaymentFlowError.Declined.CurrencyNotSupported)
        assertThat((result as PaymentFlowError.Declined.CurrencyNotSupported).message)
            .isEqualTo(R.string.card_reader_payment_failed_currency_not_supported)
    }

    @Test
    fun `given DuplicateTransaction error, when map to ui error, then DuplicateTransaction error returned`() {
        // GIVEN
        val error = CardPaymentStatusErrorType.DeclinedByBackendError.CardDeclined.DuplicateTransaction

        // WHEN
        val result = mapper.mapPaymentErrorToUiError(error)

        // THEN
        assertThat(result).isEqualTo(PaymentFlowError.Declined.DuplicateTransaction)
        assertThat((result as PaymentFlowError.Declined.DuplicateTransaction).message)
            .isEqualTo(R.string.card_reader_payment_failed_duplicate_transaction)
    }

    @Test
    fun `given ExpiredCard error, when map to ui error, then ExpiredCard error returned`() {
        // GIVEN
        val error = CardPaymentStatusErrorType.DeclinedByBackendError.CardDeclined.ExpiredCard

        // WHEN
        val result = mapper.mapPaymentErrorToUiError(error)

        // THEN
        assertThat(result).isEqualTo(PaymentFlowError.Declined.ExpiredCard)
        assertThat((result as PaymentFlowError.Declined.ExpiredCard).message)
            .isEqualTo(R.string.card_reader_payment_failed_expired_card)
    }

    @Test
    fun `given IncorrectPostalCode error, when map to ui error, then IncorrectPostalCode error returned`() {
        // GIVEN
        val error = CardPaymentStatusErrorType.DeclinedByBackendError.CardDeclined.IncorrectPostalCode

        // WHEN
        val result = mapper.mapPaymentErrorToUiError(error)

        // THEN
        assertThat(result).isEqualTo(PaymentFlowError.Declined.IncorrectPostalCode)
        assertThat((result as PaymentFlowError.Declined.IncorrectPostalCode).message)
            .isEqualTo(R.string.card_reader_payment_failed_incorrect_postal_code)
    }

    @Test
    fun `given InvalidAmount error, when map to ui error, then InvalidAmount error returned`() {
        // GIVEN
        val error = CardPaymentStatusErrorType.DeclinedByBackendError.CardDeclined.InvalidAmount

        // WHEN
        val result = mapper.mapPaymentErrorToUiError(error)

        // THEN
        assertThat(result).isEqualTo(PaymentFlowError.Declined.InvalidAmount)
        assertThat((result as PaymentFlowError.Declined.InvalidAmount).message)
            .isEqualTo(R.string.card_reader_payment_failed_invalid_amount)
    }

    @Test
    fun `given PinRequired error, when map to ui error, then PinRequired error returned`() {
        // GIVEN
        val error = CardPaymentStatusErrorType.DeclinedByBackendError.CardDeclined.PinRequired

        // WHEN
        val result = mapper.mapPaymentErrorToUiError(error)

        // THEN
        assertThat(result).isEqualTo(PaymentFlowError.Declined.PinRequired)
        assertThat((result as PaymentFlowError.Declined.PinRequired).message)
            .isEqualTo(R.string.card_reader_payment_failed_pin_required)
    }

    @Test
    fun `given TooManyPinTries error, when map to ui error, then TooManyPinTries error returned`() {
        // GIVEN
        val error = CardPaymentStatusErrorType.DeclinedByBackendError.CardDeclined.TooManyPinTries

        // WHEN
        val result = mapper.mapPaymentErrorToUiError(error)

        // THEN
        assertThat(result).isEqualTo(PaymentFlowError.Declined.TooManyPinTries)
        assertThat((result as PaymentFlowError.Declined.TooManyPinTries).message)
            .isEqualTo(R.string.card_reader_payment_failed_too_many_pin_tries)
    }

    @Test
    fun `given TestCard error, when map to ui error, then TestCard error returned`() {
        // GIVEN
        val error = CardPaymentStatusErrorType.DeclinedByBackendError.CardDeclined.TestCard

        // WHEN
        val result = mapper.mapPaymentErrorToUiError(error)

        // THEN
        assertThat(result).isEqualTo(PaymentFlowError.Declined.TestCard)
        assertThat((result as PaymentFlowError.Declined.TestCard).message)
            .isEqualTo(R.string.card_reader_payment_failed_test_card)
    }

    @Test
    fun `given TestModeLiveCard error, when map to ui error, then TestModeLiveCard error returned`() {
        // GIVEN
        val error = CardPaymentStatusErrorType.DeclinedByBackendError.CardDeclined.TestModeLiveCard

        // WHEN
        val result = mapper.mapPaymentErrorToUiError(error)

        // THEN
        assertThat(result).isEqualTo(PaymentFlowError.Declined.TestModeLiveCard)
        assertThat((result as PaymentFlowError.Declined.TestModeLiveCard).message)
            .isEqualTo(R.string.card_reader_payment_failed_test_mode_live_card)
    }

    @Test
    fun `given Unknown error, when map to ui error, then Unknown error returned`() {
        // GIVEN
        val error = CardPaymentStatusErrorType.DeclinedByBackendError.Unknown

        // WHEN
        val result = mapper.mapPaymentErrorToUiError(error)

        // THEN
        assertThat(result).isEqualTo(PaymentFlowError.Unknown)
        assertThat((result as PaymentFlowError.Unknown).message)
            .isEqualTo(R.string.card_reader_payment_failed_unknown)
    }
}
