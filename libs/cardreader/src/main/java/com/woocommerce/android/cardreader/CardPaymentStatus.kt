package com.woocommerce.android.cardreader

import com.woocommerce.android.cardreader.receipts.ReceiptPaymentInfo

sealed class CardPaymentStatus {
    object InitializingPayment : CardPaymentStatus()
    object CollectingPayment : CardPaymentStatus()
    object WaitingForInput : CardPaymentStatus()
    object ShowAdditionalInfo : CardPaymentStatus()
    object ProcessingPayment : CardPaymentStatus()
    object CapturingPayment : CardPaymentStatus()
    data class PaymentCompleted(val receiptPaymentInfo: ReceiptPaymentInfo) : CardPaymentStatus()

    data class PaymentFailed(
        val type: CardPaymentStatusErrorType,
        val paymentDataForRetry: PaymentData?,
        val errorMessage: String
    ) : CardPaymentStatus()

    enum class CardPaymentStatusErrorType {
        CARD_READ_TIMED_OUT,
        NO_NETWORK,
        PAYMENT_DECLINED,
        GENERIC_ERROR
    }
}

interface PaymentData
