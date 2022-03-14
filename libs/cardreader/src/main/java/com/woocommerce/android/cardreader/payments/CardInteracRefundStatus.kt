package com.woocommerce.android.cardreader.payments

import com.stripe.stripeterminal.external.models.TerminalException

sealed class CardInteracRefundStatus {
    object InitializingInteracRefund : CardInteracRefundStatus()
    object CollectingInteracRefund : CardInteracRefundStatus()
    object WaitingForInput : CardInteracRefundStatus()
    object ProcessingInteracRefund : CardInteracRefundStatus()
    object InteracRefundSuccess : CardInteracRefundStatus()
    data class InteracRefundFailure(val errorMessage: TerminalException) : CardInteracRefundStatus()
}
