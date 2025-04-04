package com.woocommerce.android.cardreader

interface CardReaderStore {
    suspend fun fetchConnectionToken(): String

    suspend fun capturePaymentIntent(orderId: Long, paymentId: String): CapturePaymentResponse

    sealed class CapturePaymentResponse {
        sealed class Successful : CapturePaymentResponse() {
            object Success : Successful()
            object PaymentAlreadyCaptured : Successful()
        }

        sealed class Error(open val message: String) : CapturePaymentResponse() {
            data class GenericError(override val message: String) : Error(message)
            data class MissingOrder(override val message: String) : Error(message)
            sealed class CaptureError(override val message: String) : Error(message) {
                data class AmountTooSmall(
                    override val message: String,
                    val minAmountInMicroUnits: Long,
                    val currency: String,
                ) : CaptureError(message)

                data class Generic(override val message: String) : CaptureError(message)
            }
            data class ServerError(override val message: String) : Error(message)
            data class NetworkError(override val message: String) : Error(message)
        }
    }
}
