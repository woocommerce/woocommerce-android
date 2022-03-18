package com.woocommerce.android.ui.orders.cardreader

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
        paymentStateLabel = R.string.order_error_fetch_generic,
        paymentStateLabelTopMargin = R.dimen.major_100,
        primaryActionLabel = primaryLabel,
        illustration = R.drawable.img_products_error
    )

    data class CollectRefundState(
        override val amountWithCurrencyLabel: String,
        override val hintLabel: Int = R.string.card_reader_payment_collect_payment_hint,
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
