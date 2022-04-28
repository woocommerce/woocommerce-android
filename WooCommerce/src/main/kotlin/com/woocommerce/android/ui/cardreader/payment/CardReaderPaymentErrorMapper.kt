package com.woocommerce.android.ui.cardreader.payment

import com.woocommerce.android.cardreader.payments.CardPaymentStatus
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.CardPaymentStatusErrorType.DeclinedByBackendError.AmountTooSmall
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.CardPaymentStatusErrorType.DeclinedByBackendError.CardDeclined
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.CardPaymentStatusErrorType.DeclinedByBackendError.Unknown
import javax.inject.Inject

class CardReaderPaymentErrorMapper @Inject constructor() {
    fun mapPaymentErrorToUiError(errorType: CardPaymentStatus.CardPaymentStatusErrorType): PaymentFlowError =
        when (errorType) {
            CardPaymentStatus.CardPaymentStatusErrorType.NoNetwork -> PaymentFlowError.NoNetwork
            is CardPaymentStatus.CardPaymentStatusErrorType.DeclinedByBackendError ->
                mapPaymentDeclinedErrorType(errorType)
            CardPaymentStatus.CardPaymentStatusErrorType.CardReadTimeOut,
            CardPaymentStatus.CardPaymentStatusErrorType.Generic -> PaymentFlowError.Generic
            CardPaymentStatus.CardPaymentStatusErrorType.Server -> PaymentFlowError.Server
            else -> PaymentFlowError.Generic
        }

    @Suppress("ComplexMethod")
    private fun mapPaymentDeclinedErrorType(
        cardPaymentStatusErrorType: CardPaymentStatus.CardPaymentStatusErrorType.DeclinedByBackendError
    ) = when (cardPaymentStatusErrorType) {
        AmountTooSmall -> PaymentFlowError.AmountTooSmall
        Unknown -> PaymentFlowError.Unknown

        CardDeclined.CardNotSupported -> PaymentFlowError.Declined.CardNotSupported
        CardDeclined.CurrencyNotSupported -> PaymentFlowError.Declined.CurrencyNotSupported
        CardDeclined.DuplicateTransaction -> PaymentFlowError.Declined.DuplicateTransaction
        CardDeclined.ExpiredCard -> PaymentFlowError.Declined.ExpiredCard
        CardDeclined.Fraud -> PaymentFlowError.Declined.Fraud
        CardDeclined.Generic -> PaymentFlowError.Declined.Generic
        CardDeclined.IncorrectPostalCode -> PaymentFlowError.Declined.IncorrectPostalCode
        CardDeclined.InsufficientFunds -> PaymentFlowError.Declined.InsufficientFunds
        CardDeclined.InvalidAccount -> PaymentFlowError.Declined.InvalidAccount
        CardDeclined.InvalidAmount -> PaymentFlowError.Declined.InvalidAmount
        CardDeclined.PinRequired -> PaymentFlowError.Declined.PinRequired
        CardDeclined.Temporary -> PaymentFlowError.Declined.Temporary
        CardDeclined.TestCard -> PaymentFlowError.Declined.TestCard
        CardDeclined.TestModeLiveCard -> PaymentFlowError.Declined.TestModeLiveCard
        CardDeclined.TooManyPinTries -> PaymentFlowError.Declined.TooManyPinTries
    }
}
