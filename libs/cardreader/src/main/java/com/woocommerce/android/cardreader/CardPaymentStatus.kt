package com.woocommerce.android.cardreader

sealed class CardPaymentStatus {
    object InitializingPayment : CardPaymentStatus()
    object InitializingPaymentFailed : CardPaymentStatus()
    object CollectingPayment : CardPaymentStatus()
    object CollectingPaymentFailed : CardPaymentStatus()
    object WaitingForInput : CardPaymentStatus()
    object ShowAdditionalInfo : CardPaymentStatus()
    object ProcessingPayment : CardPaymentStatus()
    object ProcessingPaymentFailed : CardPaymentStatus()
    object CapturingPayment : CardPaymentStatus()
    object CapturingPaymentFailed : CardPaymentStatus()
    object PaymentCompleted : CardPaymentStatus()
}
