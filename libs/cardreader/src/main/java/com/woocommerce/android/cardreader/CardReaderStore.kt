package com.woocommerce.android.cardreader

interface CardReaderStore {
    suspend fun fetchConnectionToken(): String

    suspend fun capturePaymentIntent(orderId: Long, paymentId: String): CapturePaymentResponse

    sealed class CapturePaymentResponse {
        sealed class Successful : CapturePaymentResponse() {
            object Success : Successful()
            object PaymentAlreadyCaptured : Successful()
        }

        sealed class Error : CapturePaymentResponse() {
            object GenericError : Error()
            object MissingOrder : Error()
            object CaptureError : Error()
            object ServerError : Error()
            object NetworkError : Error()
        }
    }
}
