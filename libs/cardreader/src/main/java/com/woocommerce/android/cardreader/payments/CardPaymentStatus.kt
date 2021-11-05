package com.woocommerce.android.cardreader.payments

sealed class CardPaymentStatus {
    object InitializingPayment : CardPaymentStatus()
    object CollectingPayment : CardPaymentStatus()
    object WaitingForInput : CardPaymentStatus()
    object ProcessingPayment : CardPaymentStatus()
    object CapturingPayment : CardPaymentStatus()
    data class PaymentCompleted(val receiptUrl: String) : CardPaymentStatus()

    data class PaymentFailed(
        val type: CardPaymentStatusErrorType,
        val paymentDataForRetry: PaymentData?,
        val errorMessage: String
    ) : CardPaymentStatus()

    sealed class CardPaymentStatusErrorType {
        object CardReadTimeOut : CardPaymentStatusErrorType()
        object NoNetwork : CardPaymentStatusErrorType()
        object ServerError : CardPaymentStatusErrorType()
        sealed class PaymentDeclined : CardPaymentStatusErrorType() {
            object AmountTooSmall : PaymentDeclined()
            object Declined : PaymentDeclined()
        }
        object GenericError : CardPaymentStatusErrorType()
    }

    enum class AdditionalInfoType {
        RETRY_CARD,
        INSERT_CARD,
        INSERT_OR_SWIPE_CARD,
        SWIPE_CARD,
        REMOVE_CARD,
        MULTIPLE_CONTACTLESS_CARDS_DETECTED,
        TRY_ANOTHER_READ_METHOD,
        TRY_ANOTHER_CARD,
        CHECK_MOBILE_DEVICE,
    }
}

interface PaymentData
