package com.woocommerce.android.cardreader

// TODO cardreader consider refactoring these states
sealed class CardPaymentStatus {
    data class UnexpectedError(val errorCause: String) : CardPaymentStatus()
    object InitializingPayment : CardPaymentStatus()
    object InitializingPaymentFailed : CardPaymentStatus()
    object CollectingPayment : CardPaymentStatus()
    data class CollectingPaymentFailed(val paymentData: PaymentData) : CardPaymentStatus()
    object WaitingForInput : CardPaymentStatus()
    object ShowAdditionalInfo : CardPaymentStatus()
    object ProcessingPayment : CardPaymentStatus()
    data class ProcessingPaymentFailed(val paymentData: PaymentData) : CardPaymentStatus()
    object CapturingPayment : CardPaymentStatus()
    data class CapturingPaymentFailed(val paymentData: PaymentData) : CardPaymentStatus()
    object PaymentCompleted : CardPaymentStatus()
}

interface PaymentData
