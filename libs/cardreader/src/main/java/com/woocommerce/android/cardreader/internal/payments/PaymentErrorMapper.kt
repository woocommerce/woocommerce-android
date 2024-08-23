package com.woocommerce.android.cardreader.internal.payments

import com.stripe.stripeterminal.external.models.PaymentIntent
import com.stripe.stripeterminal.external.models.TerminalException
import com.stripe.stripeterminal.external.models.TerminalException.TerminalErrorCode
import com.woocommerce.android.cardreader.CardReaderStore.CapturePaymentResponse
import com.woocommerce.android.cardreader.CardReaderStore.CapturePaymentResponse.Error.NetworkError
import com.woocommerce.android.cardreader.CardReaderStore.CapturePaymentResponse.Error.ServerError
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.CardPaymentStatusErrorType
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.CardPaymentStatusErrorType.BuiltInReader.DeviceIsNotSupported
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.CardPaymentStatusErrorType.BuiltInReader.InvalidAppSetup
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.CardPaymentStatusErrorType.BuiltInReader.NfcDisabled
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.CardPaymentStatusErrorType.Canceled
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.CardPaymentStatusErrorType.CardReadTimeOut
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.CardPaymentStatusErrorType.DeclinedByBackendError
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.CardPaymentStatusErrorType.DeclinedByBackendError.CardDeclined.CardNotSupported
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.CardPaymentStatusErrorType.DeclinedByBackendError.CardDeclined.CurrencyNotSupported
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.CardPaymentStatusErrorType.DeclinedByBackendError.CardDeclined.DuplicateTransaction
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.CardPaymentStatusErrorType.DeclinedByBackendError.CardDeclined.ExpiredCard
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.CardPaymentStatusErrorType.DeclinedByBackendError.CardDeclined.Fraud
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.CardPaymentStatusErrorType.DeclinedByBackendError.CardDeclined.IncorrectPin
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.CardPaymentStatusErrorType.DeclinedByBackendError.CardDeclined.IncorrectPostalCode
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.CardPaymentStatusErrorType.DeclinedByBackendError.CardDeclined.InsufficientFunds
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.CardPaymentStatusErrorType.DeclinedByBackendError.CardDeclined.InvalidAccount
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.CardPaymentStatusErrorType.DeclinedByBackendError.CardDeclined.InvalidAmount
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.CardPaymentStatusErrorType.DeclinedByBackendError.CardDeclined.PinRequired
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.CardPaymentStatusErrorType.DeclinedByBackendError.CardDeclined.Temporary
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.CardPaymentStatusErrorType.DeclinedByBackendError.CardDeclined.TestCard
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.CardPaymentStatusErrorType.DeclinedByBackendError.CardDeclined.TestModeLiveCard
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.CardPaymentStatusErrorType.DeclinedByBackendError.CardDeclined.TooManyPinTries
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.CardPaymentStatusErrorType.Generic
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.CardPaymentStatusErrorType.NoNetwork
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.PaymentFailed

internal class PaymentErrorMapper {
    fun mapTerminalError(
        originalPaymentIntent: PaymentIntent?,
        exception: TerminalException
    ): PaymentFailed {
        val paymentData =
            originalPaymentIntent?.let {
                PaymentDataImpl(exception.paymentIntent ?: originalPaymentIntent)
            }
        val type = when (exception.errorCode) {
            TerminalErrorCode.CARD_READ_TIMED_OUT -> CardReadTimeOut
            TerminalErrorCode.DECLINED_BY_STRIPE_API -> mapDeclinedByStripeApiError(exception)
            TerminalErrorCode.REQUEST_TIMED_OUT -> NoNetwork
            TerminalErrorCode.LOCAL_MOBILE_NFC_DISABLED -> NfcDisabled
            TerminalErrorCode.LOCAL_MOBILE_UNSUPPORTED_DEVICE,
            TerminalErrorCode.LOCAL_MOBILE_DEVICE_TAMPERED,
            TerminalErrorCode.LOCAL_MOBILE_UNSUPPORTED_ANDROID_VERSION -> DeviceIsNotSupported
            TerminalErrorCode.LOCAL_MOBILE_LIBRARY_NOT_INCLUDED -> InvalidAppSetup
            TerminalErrorCode.CANCELED -> Canceled
            else -> Generic
        }
        return PaymentFailed(type, paymentData, exception.errorMessage)
    }

    @Suppress("ComplexMethod")
    private fun mapDeclinedByStripeApiError(exception: TerminalException): DeclinedByBackendError =
        when (exception.apiError?.declineCode) {
            "approve_with_id",
            "issuer_not_available",
            "processing_error",
            "reenter_transaction",
            "try_again_later" -> Temporary

            "call_issuer",
            "card_velocity_exceeded",
            "do_not_honor",
            "do_not_try_again",
            "fraudulent",
            "lost_card",
            "merchant_blacklist",
            "pickup_card",
            "restricted_card",
            "revocation_of_all_authorizations",
            "revocation_of_authorization",
            "security_violation",
            "stolen_card",
            "stop_payment_order" -> Fraud

            "generic_decline",
            "no_action_taken",
            "not_permitted",
            "service_not_allowed",
            "transaction_not_allowed" -> DeclinedByBackendError.CardDeclined.Generic

            "invalid_account",
            "new_account_information_available" -> InvalidAccount

            "card_not_supported" -> CardNotSupported

            "currency_not_supported" -> CurrencyNotSupported

            "duplicate_transaction" -> DuplicateTransaction

            "expired_card" -> ExpiredCard

            "incorrect_zip" -> IncorrectPostalCode

            "insufficient_funds",
            "withdrawal_count_limit_exceeded" -> InsufficientFunds

            "invalid_amount" -> InvalidAmount

            "offline_pin_required",
            "online_or_offline_pin_required" -> PinRequired

            "invalid_pin",
            "incorrect_pin" -> IncorrectPin

            "pin_try_exceeded" -> TooManyPinTries

            "testmode_decline" -> TestCard

            "test_mode_live_card" -> TestModeLiveCard
            else -> when (exception.apiError?.code) {
                "amount_too_small" -> DeclinedByBackendError.AmountTooSmall
                else -> DeclinedByBackendError.Unknown
            }
        }

    fun mapCapturePaymentError(
        originalPaymentIntent: PaymentIntent,
        capturePaymentResponse: CapturePaymentResponse.Error
    ): PaymentFailed {
        val paymentData = PaymentDataImpl(originalPaymentIntent)
        val message = "Capturing payment failed: $capturePaymentResponse"
        val type = when (capturePaymentResponse) {
            is NetworkError -> NoNetwork
            is ServerError -> CardPaymentStatusErrorType.Server(capturePaymentResponse.message)
            else -> Generic
        }
        return PaymentFailed(type, paymentData, message)
    }

    fun mapError(
        originalPaymentIntent: PaymentIntent? = null,
        errorMessage: String
    ): PaymentFailed {
        val paymentData = originalPaymentIntent?.let { PaymentDataImpl(originalPaymentIntent) }
        return PaymentFailed(Generic, paymentData, errorMessage)
    }
}
