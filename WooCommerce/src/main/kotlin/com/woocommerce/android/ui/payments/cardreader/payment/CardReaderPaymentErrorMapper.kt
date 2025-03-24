package com.woocommerce.android.ui.payments.cardreader.payment

import com.woocommerce.android.R
import com.woocommerce.android.cardreader.payments.CardPaymentStatus
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.CardPaymentStatusErrorType.DeclinedByBackendError.AmountTooSmallStripe
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.CardPaymentStatusErrorType.DeclinedByBackendError.AmountTooSmallWooPayments
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.CardPaymentStatusErrorType.DeclinedByBackendError.CardDeclined
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.CardPaymentStatusErrorType.DeclinedByBackendError.Unknown
import com.woocommerce.android.model.UiString.UiStringText
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.ResourceProvider
import javax.inject.Inject

class CardReaderPaymentErrorMapper @Inject constructor(
    private val resources: ResourceProvider,
    private val currencyFormatter: CurrencyFormatter,
) {
    fun mapPaymentErrorToUiError(
        errorType: CardPaymentStatus.CardPaymentStatusErrorType,
        isTapToPayPayment: Boolean,
    ): PaymentFlowError =
        when (errorType) {
            CardPaymentStatus.CardPaymentStatusErrorType.NoNetwork -> PaymentFlowError.NoNetwork
            is CardPaymentStatus.CardPaymentStatusErrorType.DeclinedByBackendError ->
                mapPaymentDeclinedErrorType(errorType, isTapToPayPayment)

            CardPaymentStatus.CardPaymentStatusErrorType.CardReadTimeOut,
            CardPaymentStatus.CardPaymentStatusErrorType.Generic -> PaymentFlowError.Generic

            is CardPaymentStatus.CardPaymentStatusErrorType.Server -> PaymentFlowError.Server
            CardPaymentStatus.CardPaymentStatusErrorType.Canceled -> PaymentFlowError.Canceled
            CardPaymentStatus.CardPaymentStatusErrorType.BuiltInReader.NfcDisabled ->
                PaymentFlowError.BuiltInReader.NfcDisabled

            CardPaymentStatus.CardPaymentStatusErrorType.BuiltInReader.DeviceIsNotSupported ->
                PaymentFlowError.BuiltInReader.DeviceIsNotSupported

            CardPaymentStatus.CardPaymentStatusErrorType.BuiltInReader.InvalidAppSetup ->
                PaymentFlowError.BuiltInReader.InvalidAppSetup
        }

    @Suppress("ComplexMethod")
    private fun mapPaymentDeclinedErrorType(
        cardPaymentStatusErrorType: CardPaymentStatus.CardPaymentStatusErrorType.DeclinedByBackendError,
        isTapToPayPayment: Boolean,
    ) = when (cardPaymentStatusErrorType) {
        is AmountTooSmallWooPayments -> generateAmountToSmallErrorFor(cardPaymentStatusErrorType)
        is AmountTooSmallStripe -> generateAmountToSmallErrorFor(cardPaymentStatusErrorType)

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
        CardDeclined.PinRequired -> {
            if (isTapToPayPayment) {
                PaymentFlowError.BuiltInReader.PinRequired
            } else {
                PaymentFlowError.Declined.PinRequired
            }
        }

        CardDeclined.IncorrectPin -> PaymentFlowError.Declined.IncorrectPin
        CardDeclined.Temporary -> PaymentFlowError.Declined.Temporary
        CardDeclined.TestCard -> PaymentFlowError.Declined.TestCard
        CardDeclined.TestModeLiveCard -> PaymentFlowError.Declined.TestModeLiveCard
        CardDeclined.TooManyPinTries -> PaymentFlowError.Declined.TooManyPinTries
    }

    private fun generateAmountToSmallErrorFor(data: AmountTooSmallWooPayments): PaymentFlowError.AmountTooSmall {
        val minChargeAmountString = currencyFormatter.formatCurrencyGivenInTheSmallestCurrencyUnit(
            data.minAmountInMicroUnits,
            data.currency,
        )
        val message = resources.getString(R.string.card_reader_payment_failed_amount_too_small)
            .format(minChargeAmountString)

        return PaymentFlowError.AmountTooSmall(UiStringText(message))
    }

    private fun generateAmountToSmallErrorFor(data: AmountTooSmallStripe): PaymentFlowError.AmountTooSmall {
        return PaymentFlowError.AmountTooSmall(UiStringText(data.message))
    }
}
