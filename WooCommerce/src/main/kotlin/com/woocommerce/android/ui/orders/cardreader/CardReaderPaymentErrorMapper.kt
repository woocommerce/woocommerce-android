package com.woocommerce.android.ui.orders.cardreader

import com.woocommerce.android.cardreader.payments.CardPaymentStatus
import javax.inject.Inject

class CardReaderPaymentErrorMapper @Inject constructor() {
    fun mapToUiError(errorType: CardPaymentStatus.CardPaymentStatusErrorType): PaymentFlowError =
        when (errorType) {
            CardPaymentStatus.CardPaymentStatusErrorType.NoNetwork -> PaymentFlowError.NoNetwork
            is CardPaymentStatus.CardPaymentStatusErrorType.PaymentDeclined ->
                mapPaymentDeclinedErrorType(errorType)
            CardPaymentStatus.CardPaymentStatusErrorType.CardReadTimeOut,
            CardPaymentStatus.CardPaymentStatusErrorType.GenericError -> PaymentFlowError.GenericError
            CardPaymentStatus.CardPaymentStatusErrorType.ServerError -> PaymentFlowError.ServerError
            else -> PaymentFlowError.GenericError
        }

    @Suppress("ComplexMethod")
    private fun mapPaymentDeclinedErrorType(
        cardPaymentStatusErrorType: CardPaymentStatus.CardPaymentStatusErrorType.PaymentDeclined
    ) = when (cardPaymentStatusErrorType) {
        CardPaymentStatus.CardPaymentStatusErrorType.PaymentDeclined.AmountTooSmall ->
            PaymentFlowError.Declined.AmountTooSmall
        CardPaymentStatus.CardPaymentStatusErrorType.PaymentDeclined.CardNotSupported ->
            PaymentFlowError.Declined.CardNotSupported
        CardPaymentStatus.CardPaymentStatusErrorType.PaymentDeclined.CurrencyNotSupported ->
            PaymentFlowError.Declined.CurrencyNotSupported
        CardPaymentStatus.CardPaymentStatusErrorType.PaymentDeclined.DuplicateTransaction ->
            PaymentFlowError.Declined.DuplicateTransaction
        CardPaymentStatus.CardPaymentStatusErrorType.PaymentDeclined.ExpiredCard ->
            PaymentFlowError.Declined.ExpiredCard
        CardPaymentStatus.CardPaymentStatusErrorType.PaymentDeclined.Fraud ->
            PaymentFlowError.Declined.Fraud
        CardPaymentStatus.CardPaymentStatusErrorType.PaymentDeclined.Generic ->
            PaymentFlowError.Declined.Generic
        CardPaymentStatus.CardPaymentStatusErrorType.PaymentDeclined.IncorrectPostalCode ->
            PaymentFlowError.Declined.IncorrectPostalCode
        CardPaymentStatus.CardPaymentStatusErrorType.PaymentDeclined.InsufficientFunds ->
            PaymentFlowError.Declined.InsufficientFunds
        CardPaymentStatus.CardPaymentStatusErrorType.PaymentDeclined.InvalidAccount ->
            PaymentFlowError.Declined.InvalidAccount
        CardPaymentStatus.CardPaymentStatusErrorType.PaymentDeclined.InvalidAmount ->
            PaymentFlowError.Declined.InvalidAmount
        CardPaymentStatus.CardPaymentStatusErrorType.PaymentDeclined.PinRequired ->
            PaymentFlowError.Declined.PinRequired
        CardPaymentStatus.CardPaymentStatusErrorType.PaymentDeclined.Temporary ->
            PaymentFlowError.Declined.Temporary
        CardPaymentStatus.CardPaymentStatusErrorType.PaymentDeclined.TestCard ->
            PaymentFlowError.Declined.TestCard
        CardPaymentStatus.CardPaymentStatusErrorType.PaymentDeclined.TestModeLiveCard ->
            PaymentFlowError.Declined.TestModeLiveCard
        CardPaymentStatus.CardPaymentStatusErrorType.PaymentDeclined.TooManyPinTries ->
            PaymentFlowError.Declined.TooManyPinTries
        CardPaymentStatus.CardPaymentStatusErrorType.PaymentDeclined.Unknown ->
            PaymentFlowError.Declined.Unknown
    }
}
