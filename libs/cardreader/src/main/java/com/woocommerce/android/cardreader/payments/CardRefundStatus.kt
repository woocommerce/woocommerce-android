package com.woocommerce.android.cardreader.payments

import com.stripe.stripeterminal.external.models.TerminalException

sealed class CardRefundStatus {
    object InitializingRefund : CardRefundStatus()
    object CollectingRefund : CardRefundStatus()
    object WaitingForInput : CardRefundStatus()
    object ProcessingRefund : CardRefundStatus()
    object RefundSuccess : CardRefundStatus()
    data class RefundFailure(val errorMessage: TerminalException) : CardRefundStatus()
}
