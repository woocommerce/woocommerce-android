package com.woocommerce.android.ui.orders.cardreader.payment

import androidx.annotation.StringRes
import com.woocommerce.android.R

sealed class InteracRefund {
    object RefundLoadingDataState : ViewState(
        headerLabel = R.string.card_reader_payment_refund_payment_loading_header,
        hintLabel = R.string.card_reader_payment_collect_payment_loading_hint,
        paymentStateLabel = R.string.card_reader_payment_collect_payment_loading_payment_state,
        isProgressVisible = true
    )

    data class FailedRefundState(
        override val amountWithCurrencyLabel: String?,
        private val primaryLabel: Int? = R.string.try_again,
        override val onPrimaryActionClicked: (() -> Unit)
    ) : ViewState(
        headerLabel = R.string.card_reader_payment_refund_failed_header,
        paymentStateLabel = R.string.card_reader_payment_refund_failed_header,
        paymentStateLabelTopMargin = R.dimen.major_100,
        primaryActionLabel = primaryLabel,
        illustration = R.drawable.img_products_error
    )

    data class CollectRefundState(
        override val amountWithCurrencyLabel: String,
        override val hintLabel: Int = R.string.card_reader_refund_payment_hint,
        override val headerLabel: Int = R.string.card_reader_payment_refund_payment,
    ) : ViewState(
        paymentStateLabel = R.string.card_reader_payment_collect_payment_state,
        illustration = R.drawable.img_card_reader_available
    )

    data class ProcessingRefundState(override val amountWithCurrencyLabel: String) :
        ViewState(
            hintLabel = R.string.card_reader_payment_processing_payment_hint,
            headerLabel = R.string.card_reader_payment_refund_payment,
            paymentStateLabel = R.string.card_reader_payment_processing_refund_state,
            illustration = R.drawable.img_card_reader_available
        )

    data class RefundSuccessfulState(
        override val amountWithCurrencyLabel: String
    ) : ViewState(
        headerLabel = R.string.card_reader_payment_completed_refund_header,
        illustration = R.drawable.img_celebration,
    )
}

sealed class InteracRefundFlowError(@StringRes val message: Int) {
    object FetchingOrderFailed : InteracRefundFlowError(R.string.order_error_fetch_generic)
    object NoNetwork : InteracRefundFlowError(R.string.card_reader_payment_failed_no_network_state)
    object Server : InteracRefundFlowError(R.string.card_reader_payment_failed_server_error_state)
    object Generic : InteracRefundFlowError(R.string.card_reader_refund_failed_unexpected_error_state)
    object Cancelled : InteracRefundFlowError(R.string.card_reader_refund_failed_cancelled)
    object AmountTooSmall : Declined(R.string.card_reader_payment_failed_amount_too_small), NonRetryableError
    object Unknown : Declined(R.string.card_reader_payment_refund_failed_header)
    sealed class Declined(message: Int) : InteracRefundFlowError(message) {
        object Temporary : Declined(R.string.card_reader_payment_failed_temporary)
        object Fraud : Declined(R.string.card_reader_refund_failed_fraud), NonRetryableError
        object Generic : Declined(R.string.card_reader_refund_failed_generic)
        object InvalidAccount : Declined(R.string.card_reader_payment_failed_invalid_account), NonRetryableError
        object CardNotSupported : Declined(R.string.card_reader_refund_failed_card_not_supported)
        object CurrencyNotSupported :
            Declined(R.string.card_reader_payment_failed_currency_not_supported), NonRetryableError

        object DuplicateTransaction :
            Declined(R.string.card_reader_refund_failed_duplicate_transaction), NonRetryableError

        object ExpiredCard : Declined(R.string.card_reader_payment_failed_expired_card)
        object IncorrectPostalCode :
            Declined(R.string.card_reader_payment_failed_incorrect_postal_code), NonRetryableError

        object InsufficientFunds : Declined(R.string.card_reader_refund_failed_insufficient_funds)
        object InvalidAmount : Declined(R.string.card_reader_refund_failed_invalid_amount), NonRetryableError
        object PinRequired : Declined(R.string.card_reader_payment_failed_pin_required)
        object TooManyPinTries : Declined(R.string.card_reader_payment_failed_too_many_pin_tries)
        object TestCard : Declined(R.string.card_reader_refund_failed_test_card)
        object TestModeLiveCard : Declined(R.string.card_reader_payment_failed_test_mode_live_card)
    }

    interface NonRetryableError
}
