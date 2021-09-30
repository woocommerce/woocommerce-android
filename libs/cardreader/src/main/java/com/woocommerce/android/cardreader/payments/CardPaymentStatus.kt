package com.woocommerce.android.cardreader

sealed class CardPaymentStatus {
    object InitializingPayment : CardPaymentStatus()
    object CollectingPayment : CardPaymentStatus()
    object WaitingForInput : CardPaymentStatus()
    data class ShowAdditionalInfo(val type: AdditionalInfoType) : CardPaymentStatus()
    object ProcessingPayment : CardPaymentStatus()
    object CapturingPayment : CardPaymentStatus()
    data class PaymentCompleted(val receiptUrl: String) : CardPaymentStatus()

    data class PaymentFailed(
        val type: CardPaymentStatusErrorType,
        val paymentDataForRetry: PaymentData?,
        val errorMessage: String
    ) : CardPaymentStatus()

    enum class CardPaymentStatusErrorType {
        CARD_READ_TIMED_OUT,
        NO_NETWORK,
        SERVER_ERROR,
        PAYMENT_DECLINED,
        GENERIC_ERROR,
        AMOUNT_TOO_SMALL,
    }

    enum class AdditionalInfoType {
        RETRY_CARD,
        INSERT_CARD,
        INSERT_OR_SWIPE_CARD,
        SWIPE_CARD,
        REMOVE_CARD,
        MULTIPLE_CONTACTLESS_CARDS_DETECTED,
        TRY_ANOTHER_READ_METHOD,
        TRY_ANOTHER_CARD;
    }
}

interface PaymentData
