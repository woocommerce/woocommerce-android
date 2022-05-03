package com.woocommerce.android.ui.cardreader

import com.woocommerce.android.cardreader.payments.CardInteracRefundStatus
import com.woocommerce.android.cardreader.payments.CardInteracRefundStatus.RefundStatusErrorType.DeclinedByBackendError
import com.woocommerce.android.ui.cardreader.payment.CardReaderInteracRefundErrorMapper
import com.woocommerce.android.ui.cardreader.payment.InteracRefundFlowError
import com.woocommerce.android.viewmodel.BaseUnitTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test

class CardReaderInteracRefundErrorMapperTest : BaseUnitTest() {
    private lateinit var cardReaderInteracRefundErrorMapper: CardReaderInteracRefundErrorMapper

    @Before
    fun setup() {
        cardReaderInteracRefundErrorMapper = CardReaderInteracRefundErrorMapper()
    }

    @Test
    fun `given refund status NoNetwork, when map payment error called, then correct ui error is mapped`() {
        val errorType = CardInteracRefundStatus.RefundStatusErrorType.NoNetwork
        val expectedErrorType = InteracRefundFlowError.NoNetwork

        val actualErrorType = cardReaderInteracRefundErrorMapper.mapRefundErrorToUiError(errorType)

        assertThat(actualErrorType).isEqualTo(expectedErrorType)
    }

    @Test
    fun `given refund status Generic, when map payment error called, then correct ui error is mapped`() {
        val errorType = CardInteracRefundStatus.RefundStatusErrorType.Generic
        val expectedErrorType = InteracRefundFlowError.Generic

        val actualErrorType = cardReaderInteracRefundErrorMapper.mapRefundErrorToUiError(errorType)

        assertThat(actualErrorType).isEqualTo(expectedErrorType)
    }

    @Test
    fun `given refund status Server, when map payment error called, then correct ui error is mapped`() {
        val errorType = CardInteracRefundStatus.RefundStatusErrorType.Server
        val expectedErrorType = InteracRefundFlowError.Server

        val actualErrorType = cardReaderInteracRefundErrorMapper.mapRefundErrorToUiError(errorType)

        assertThat(actualErrorType).isEqualTo(expectedErrorType)
    }

    @Test
    fun `given refund status Cancelled, when map payment error called, then correct ui error is mapped`() {
        val errorType = CardInteracRefundStatus.RefundStatusErrorType.Cancelled
        val expectedErrorType = InteracRefundFlowError.Cancelled

        val actualErrorType = cardReaderInteracRefundErrorMapper.mapRefundErrorToUiError(errorType)

        assertThat(actualErrorType).isEqualTo(expectedErrorType)
    }

    @Test
    fun `given refund status NonRetryable, when map payment error called, then correct ui error is mapped`() {
        val errorType = CardInteracRefundStatus.RefundStatusErrorType.NonRetryable
        val expectedErrorType = InteracRefundFlowError.NonRetryableGeneric

        val actualErrorType = cardReaderInteracRefundErrorMapper.mapRefundErrorToUiError(errorType)

        assertThat(actualErrorType).isEqualTo(expectedErrorType)
    }

    @Test
    fun `given refund status UNKNOWN, when map payment error called, then correct ui error is mapped`() {
        val errorType = DeclinedByBackendError.Unknown
        val expectedErrorType = InteracRefundFlowError.Unknown

        val actualErrorType = cardReaderInteracRefundErrorMapper.mapRefundErrorToUiError(errorType)

        assertThat(actualErrorType).isEqualTo(expectedErrorType)
    }

    @Test
    fun `given refund status CARD_NOT_SUPORTED, when map payment error called, then correct ui error is mapped`() {
        val errorType = DeclinedByBackendError.CardDeclined.CardNotSupported
        val expectedErrorType = InteracRefundFlowError.Declined.CardNotSupported

        val actualErrorType = cardReaderInteracRefundErrorMapper.mapRefundErrorToUiError(errorType)

        assertThat(actualErrorType).isEqualTo(expectedErrorType)
    }

    @Test
    fun `given refund status CURRENCY_NOT_SUPORTED, when map payment error called, then correct ui error is mapped`() {
        val errorType = DeclinedByBackendError.CardDeclined.CurrencyNotSupported
        val expectedErrorType = InteracRefundFlowError.Declined.CurrencyNotSupported

        val actualErrorType = cardReaderInteracRefundErrorMapper.mapRefundErrorToUiError(errorType)

        assertThat(actualErrorType).isEqualTo(expectedErrorType)
    }

    @Test
    fun `given refund status DUPLICATE_TRANSACTION, when map payment error called, then correct ui error is mapped`() {
        val errorType = DeclinedByBackendError.CardDeclined.DuplicateTransaction
        val expectedErrorType = InteracRefundFlowError.Declined.DuplicateTransaction

        val actualErrorType = cardReaderInteracRefundErrorMapper.mapRefundErrorToUiError(errorType)

        assertThat(actualErrorType).isEqualTo(expectedErrorType)
    }

    @Test
    fun `given refund status EXPIRED_CARD, when map payment error called, then correct ui error is mapped`() {
        val errorType = DeclinedByBackendError.CardDeclined.ExpiredCard
        val expectedErrorType = InteracRefundFlowError.Declined.ExpiredCard

        val actualErrorType = cardReaderInteracRefundErrorMapper.mapRefundErrorToUiError(errorType)

        assertThat(actualErrorType).isEqualTo(expectedErrorType)
    }

    @Test
    fun `given refund status FRAUD, when map payment error called, then correct ui error is mapped`() {
        val errorType = DeclinedByBackendError.CardDeclined.Fraud
        val expectedErrorType = InteracRefundFlowError.Declined.Fraud

        val actualErrorType = cardReaderInteracRefundErrorMapper.mapRefundErrorToUiError(errorType)

        assertThat(actualErrorType).isEqualTo(expectedErrorType)
    }

    @Test
    fun `given refund status Declined GENERIC, when map payment error called, then correct ui error is mapped`() {
        val errorType = DeclinedByBackendError.CardDeclined.Generic
        val expectedErrorType = InteracRefundFlowError.Declined.Generic

        val actualErrorType = cardReaderInteracRefundErrorMapper.mapRefundErrorToUiError(errorType)

        assertThat(actualErrorType).isEqualTo(expectedErrorType)
    }

    @Test
    fun `given refund status INCORRECT_POSTAL_CODE, when map payment error called, then correct ui error is mapped`() {
        val errorType = DeclinedByBackendError.CardDeclined.IncorrectPostalCode
        val expectedErrorType = InteracRefundFlowError.Declined.IncorrectPostalCode

        val actualErrorType = cardReaderInteracRefundErrorMapper.mapRefundErrorToUiError(errorType)

        assertThat(actualErrorType).isEqualTo(expectedErrorType)
    }

    @Test
    fun `given refund status INSUFFICIENT_FUNDS, when map payment error called, then correct ui error is mapped`() {
        val errorType = DeclinedByBackendError.CardDeclined.InsufficientFunds
        val expectedErrorType = InteracRefundFlowError.Declined.InsufficientFunds

        val actualErrorType = cardReaderInteracRefundErrorMapper.mapRefundErrorToUiError(errorType)

        assertThat(actualErrorType).isEqualTo(expectedErrorType)
    }

    @Test
    fun `given refund status INVALID_ACCOUNT, when map payment error called, then correct ui error is mapped`() {
        val errorType = DeclinedByBackendError.CardDeclined.InvalidAccount
        val expectedErrorType = InteracRefundFlowError.Declined.InvalidAccount

        val actualErrorType = cardReaderInteracRefundErrorMapper.mapRefundErrorToUiError(errorType)

        assertThat(actualErrorType).isEqualTo(expectedErrorType)
    }

    @Test
    fun `given refund status INVALID_AMOUNT, when map payment error called, then correct ui error is mapped`() {
        val errorType = DeclinedByBackendError.CardDeclined.InvalidAmount
        val expectedErrorType = InteracRefundFlowError.Declined.InvalidAmount

        val actualErrorType = cardReaderInteracRefundErrorMapper.mapRefundErrorToUiError(errorType)

        assertThat(actualErrorType).isEqualTo(expectedErrorType)
    }

    @Test
    fun `given refund status PIN_REQUIRED, when map payment error called, then correct ui error is mapped`() {
        val errorType = DeclinedByBackendError.CardDeclined.PinRequired
        val expectedErrorType = InteracRefundFlowError.Declined.PinRequired

        val actualErrorType = cardReaderInteracRefundErrorMapper.mapRefundErrorToUiError(errorType)

        assertThat(actualErrorType).isEqualTo(expectedErrorType)
    }

    @Test
    fun `given refund status TEMPORARY, when map payment error called, then correct ui error is mapped`() {
        val errorType = DeclinedByBackendError.CardDeclined.Temporary
        val expectedErrorType = InteracRefundFlowError.Declined.Temporary

        val actualErrorType = cardReaderInteracRefundErrorMapper.mapRefundErrorToUiError(errorType)

        assertThat(actualErrorType).isEqualTo(expectedErrorType)
    }

    @Test
    fun `given refund status TEST_CARD, when map payment error called, then correct ui error is mapped`() {
        val errorType = DeclinedByBackendError.CardDeclined.TestCard
        val expectedErrorType = InteracRefundFlowError.Declined.TestCard

        val actualErrorType = cardReaderInteracRefundErrorMapper.mapRefundErrorToUiError(errorType)

        assertThat(actualErrorType).isEqualTo(expectedErrorType)
    }

    @Test
    fun `given refund status TEST_MODE_LIVE_CARD, when map payment error called, then correct ui error is mapped`() {
        val errorType = DeclinedByBackendError.CardDeclined.TestModeLiveCard
        val expectedErrorType = InteracRefundFlowError.Declined.TestModeLiveCard

        val actualErrorType = cardReaderInteracRefundErrorMapper.mapRefundErrorToUiError(errorType)

        assertThat(actualErrorType).isEqualTo(expectedErrorType)
    }

    @Test
    fun `given refund status TOO_MANY_PIN_TRIES, when map payment error called, then correct ui error is mapped`() {
        val errorType = DeclinedByBackendError.CardDeclined.TooManyPinTries
        val expectedErrorType = InteracRefundFlowError.Declined.TooManyPinTries

        val actualErrorType = cardReaderInteracRefundErrorMapper.mapRefundErrorToUiError(errorType)

        assertThat(actualErrorType).isEqualTo(expectedErrorType)
    }
}
