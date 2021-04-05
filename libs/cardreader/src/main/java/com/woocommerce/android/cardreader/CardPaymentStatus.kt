package com.woocommerce.android.cardreader

sealed class CardPaymentStatus {
    object InitializingPayment : CardPaymentStatus()
    object InitializingPaymentFailed : CardPaymentStatus()
    object CollectingPayment : CardPaymentStatus()
    data class CollectingPaymentFailed(val error: CollectingPaymentError) : CardPaymentStatus()
    object WaitingForInput : CardPaymentStatus()
    object ShowAdditionalInfo : CardPaymentStatus()
    object ProcessingPayment : CardPaymentStatus()
    object ProcessingPaymentFailed : CardPaymentStatus()
    object CapturingPayment : CardPaymentStatus()
    object CapturingPaymentFailed : CardPaymentStatus()
    object PaymentCompleted : CardPaymentStatus()
}

enum class CollectingPaymentError {
    CARD_READER_ERROR, TIMED_OUT
}
