package com.woocommerce.android.cardreader

interface CardReaderStore {
    suspend fun fetchConnectionToken(): String

    suspend fun capturePaymentIntent(orderId: Long, paymentId: String): CapturePaymentResponse

    sealed class CapturePaymentResponse {
        sealed class Successful : CapturePaymentResponse() {
            object Success : Successful()
            object PaymentAlreadyCaptured : Successful()
        }

        sealed class Error(val message: String) : CapturePaymentResponse() {
            data class GenericError(val errorMsg: String) : Error(errorMsg)
            data class MissingOrder(val errorMsg: String) : Error(errorMsg)
            data class CaptureError(val errorMsg: String) : Error(errorMsg)
            data class ServerError(val errorMsg: String) : Error(errorMsg)
            data class NetworkError(val errorMsg: String) : Error(errorMsg)
        }
    }
}
