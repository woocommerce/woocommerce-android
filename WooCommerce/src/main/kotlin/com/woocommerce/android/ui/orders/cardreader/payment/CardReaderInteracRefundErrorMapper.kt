package com.woocommerce.android.ui.orders.cardreader.payment

import com.woocommerce.android.cardreader.payments.CardInteracRefundStatus
import com.woocommerce.android.cardreader.payments.CardInteracRefundStatus.RefundStatusErrorType.DeclinedByBackendError
import javax.inject.Inject

class CardReaderInteracRefundErrorMapper @Inject constructor() {
    fun mapPaymentErrorToUiError(errorType: CardInteracRefundStatus.RefundStatusErrorType): InteracRefundFlowError =
        when (errorType) {
            CardInteracRefundStatus.RefundStatusErrorType.NoNetwork -> InteracRefundFlowError.NoNetwork
            is DeclinedByBackendError ->
                mapPaymentDeclinedErrorType(errorType)
            CardInteracRefundStatus.RefundStatusErrorType.Generic -> InteracRefundFlowError.Generic
            CardInteracRefundStatus.RefundStatusErrorType.Server -> InteracRefundFlowError.Server
            CardInteracRefundStatus.RefundStatusErrorType.Cancelled -> InteracRefundFlowError.Cancelled
            else -> InteracRefundFlowError.Generic
        }

    @Suppress("ComplexMethod")
    private fun mapPaymentDeclinedErrorType(
        interacRefundStatusErrorType: DeclinedByBackendError
    ) = when (interacRefundStatusErrorType) {
        DeclinedByBackendError.AmountTooSmall -> InteracRefundFlowError.AmountTooSmall
        DeclinedByBackendError.Unknown -> InteracRefundFlowError.Unknown

        DeclinedByBackendError.CardDeclined.CardNotSupported -> InteracRefundFlowError.Declined.CardNotSupported
        DeclinedByBackendError.CardDeclined.CurrencyNotSupported -> InteracRefundFlowError.Declined.CurrencyNotSupported
        DeclinedByBackendError.CardDeclined.DuplicateTransaction -> InteracRefundFlowError.Declined.DuplicateTransaction
        DeclinedByBackendError.CardDeclined.ExpiredCard -> InteracRefundFlowError.Declined.ExpiredCard
        DeclinedByBackendError.CardDeclined.Fraud -> InteracRefundFlowError.Declined.Fraud
        DeclinedByBackendError.CardDeclined.Generic -> InteracRefundFlowError.Declined.Generic
        DeclinedByBackendError.CardDeclined.IncorrectPostalCode -> InteracRefundFlowError.Declined.IncorrectPostalCode
        DeclinedByBackendError.CardDeclined.InsufficientFunds -> InteracRefundFlowError.Declined.InsufficientFunds
        DeclinedByBackendError.CardDeclined.InvalidAccount -> InteracRefundFlowError.Declined.InvalidAccount
        DeclinedByBackendError.CardDeclined.InvalidAmount -> InteracRefundFlowError.Declined.InvalidAmount
        DeclinedByBackendError.CardDeclined.PinRequired -> InteracRefundFlowError.Declined.PinRequired
        DeclinedByBackendError.CardDeclined.Temporary -> InteracRefundFlowError.Declined.Temporary
        DeclinedByBackendError.CardDeclined.TestCard -> InteracRefundFlowError.Declined.TestCard
        DeclinedByBackendError.CardDeclined.TestModeLiveCard -> InteracRefundFlowError.Declined.TestModeLiveCard
        DeclinedByBackendError.CardDeclined.TooManyPinTries -> InteracRefundFlowError.Declined.TooManyPinTries
    }
}
