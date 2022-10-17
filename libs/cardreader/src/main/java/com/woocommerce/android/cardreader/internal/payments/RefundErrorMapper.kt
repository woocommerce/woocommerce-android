package com.woocommerce.android.cardreader.internal.payments

import com.stripe.stripeterminal.external.models.TerminalException
import com.woocommerce.android.cardreader.payments.CardInteracRefundStatus.InteracRefundFailure
import com.woocommerce.android.cardreader.payments.CardInteracRefundStatus.RefundStatusErrorType.Cancelled
import com.woocommerce.android.cardreader.payments.CardInteracRefundStatus.RefundStatusErrorType.DeclinedByBackendError
import com.woocommerce.android.cardreader.payments.CardInteracRefundStatus.RefundStatusErrorType.Generic
import com.woocommerce.android.cardreader.payments.CardInteracRefundStatus.RefundStatusErrorType.NoNetwork
import com.woocommerce.android.cardreader.payments.RefundParams

internal class RefundErrorMapper {
    fun mapTerminalError(
        refundParams: RefundParams,
        exception: TerminalException
    ): InteracRefundFailure {
        val type = when (exception.errorCode) {
            TerminalException.TerminalErrorCode.DECLINED_BY_STRIPE_API -> mapDeclinedByStripeApiError(exception)
            TerminalException.TerminalErrorCode.STRIPE_API_CONNECTION_ERROR -> NoNetwork
            TerminalException.TerminalErrorCode.CANCELED -> Cancelled
            else -> Generic
        }
        return InteracRefundFailure(type, exception.errorMessage, refundParams)
    }

    @Suppress("ComplexMethod")
    private fun mapDeclinedByStripeApiError(exception: TerminalException): DeclinedByBackendError =
        when (exception.apiError?.declineCode) {
            "approve_with_id",
            "issuer_not_available",
            "processing_error",
            "reenter_transaction",
            "try_again_later" -> DeclinedByBackendError.CardDeclined.Temporary

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
            "stop_payment_order" -> DeclinedByBackendError.CardDeclined.Fraud

            "generic_decline",
            "no_action_taken",
            "not_permitted",
            "service_not_allowed",
            "transaction_not_allowed" -> DeclinedByBackendError.CardDeclined.Generic

            "invalid_account",
            "new_account_information_available" -> DeclinedByBackendError.CardDeclined.InvalidAccount

            "card_not_supported" -> DeclinedByBackendError.CardDeclined.CardNotSupported

            "currency_not_supported" -> DeclinedByBackendError.CardDeclined.CurrencyNotSupported

            "duplicate_transaction" -> DeclinedByBackendError.CardDeclined.DuplicateTransaction

            "expired_card" -> DeclinedByBackendError.CardDeclined.ExpiredCard

            "incorrect_zip" -> DeclinedByBackendError.CardDeclined.IncorrectPostalCode

            "insufficient_funds",
            "withdrawal_count_limit_exceeded" -> DeclinedByBackendError.CardDeclined.InsufficientFunds

            "invalid_amount" -> DeclinedByBackendError.CardDeclined.InvalidAmount

            "invalid_pin",
            "offline_pin_required",
            "online_or_offline_pin_required" -> DeclinedByBackendError.CardDeclined.PinRequired

            "pin_try_exceeded" -> DeclinedByBackendError.CardDeclined.TooManyPinTries

            "testmode_decline" -> DeclinedByBackendError.CardDeclined.TestCard

            "test_mode_live_card" -> DeclinedByBackendError.CardDeclined.TestModeLiveCard
            else -> DeclinedByBackendError.Unknown
        }
}
