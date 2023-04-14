package com.woocommerce.android.ui.payments.cardreader.payment

import com.woocommerce.android.R
import com.woocommerce.android.cardreader.config.CardReaderConfig
import com.woocommerce.android.cardreader.config.CardReaderConfigForSupportedCountry
import com.woocommerce.android.cardreader.payments.CardPaymentStatus
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.CardPaymentStatusErrorType.DeclinedByBackendError.AmountTooSmall
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.CardPaymentStatusErrorType.DeclinedByBackendError.CardDeclined
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.CardPaymentStatusErrorType.DeclinedByBackendError.Unknown
import com.woocommerce.android.model.UiString.UiStringText
import com.woocommerce.android.util.SiteIndependentCurrencyFormatter
import com.woocommerce.android.viewmodel.ResourceProvider
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.util.AppLog
import javax.inject.Inject

class CardReaderPaymentErrorMapper @Inject constructor(
    private val resources: ResourceProvider,
    private val currencyFormatter: SiteIndependentCurrencyFormatter,
    private val logger: AppLogWrapper,
) {
    fun mapPaymentErrorToUiError(
        errorType: CardPaymentStatus.CardPaymentStatusErrorType,
        config: CardReaderConfig,
    ): PaymentFlowError =
        when (errorType) {
            CardPaymentStatus.CardPaymentStatusErrorType.NoNetwork -> PaymentFlowError.NoNetwork
            is CardPaymentStatus.CardPaymentStatusErrorType.DeclinedByBackendError ->
                mapPaymentDeclinedErrorType(errorType, config)
            CardPaymentStatus.CardPaymentStatusErrorType.CardReadTimeOut,
            CardPaymentStatus.CardPaymentStatusErrorType.Generic -> PaymentFlowError.Generic
            CardPaymentStatus.CardPaymentStatusErrorType.Server -> PaymentFlowError.Server
            CardPaymentStatus.CardPaymentStatusErrorType.Canceled -> PaymentFlowError.Canceled
            CardPaymentStatus.CardPaymentStatusErrorType.BuiltInReader.NfcDisabled ->
                PaymentFlowError.BuiltInReader.NfcDisabled
            CardPaymentStatus.CardPaymentStatusErrorType.BuiltInReader.DeviceIsNotSupported ->
                PaymentFlowError.BuiltInReader.DeviceIsNotSupported
            CardPaymentStatus.CardPaymentStatusErrorType.BuiltInReader.InvalidAppSetup ->
                PaymentFlowError.BuiltInReader.InvalidAppSetup
            else -> PaymentFlowError.Generic
        }

    @Suppress("ComplexMethod")
    private fun mapPaymentDeclinedErrorType(
        cardPaymentStatusErrorType: CardPaymentStatus.CardPaymentStatusErrorType.DeclinedByBackendError,
        config: CardReaderConfig,
    ) = when (cardPaymentStatusErrorType) {
        AmountTooSmall -> generateAmountToSmallErrorFor(config)

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

    private fun generateAmountToSmallErrorFor(config: CardReaderConfig) =
        if (config is CardReaderConfigForSupportedCountry) {
            val minChargeAmountString = currencyFormatter.formatAmountWithCurrency(
                config.minimumAllowedChargeAmount.toDouble(),
                config.currency
            )
            val message =
                resources.getString(R.string.card_reader_payment_failed_amount_too_small)
                    .format(minChargeAmountString)

            PaymentFlowError.AmountTooSmall(UiStringText(message))
        } else {
            logger.e(
                AppLog.T.READER,
                "Card reader config should be instance of CardReaderConfigForSupportedCountry"
            )
            PaymentFlowError.Unknown
        }
}
