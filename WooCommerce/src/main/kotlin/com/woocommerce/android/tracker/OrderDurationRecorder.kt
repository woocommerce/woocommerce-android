package com.woocommerce.android.tracker

import com.woocommerce.android.tracker.OrderDurationRecorder.OrderDurationRecorderException.OrderDurationRecordedType.DURATION_TIMEOUT_EXCEEDED
import com.woocommerce.android.tracker.OrderDurationRecorder.OrderDurationRecorderException.OrderDurationRecordedType.NO_START_RECORDING_TIMESTAMP

private const val TIMEOUT: Long = 60 * 10 * 1000

/**
 * Measures the duration of Order Creation and In-Person Payments flows for analytical purposes.
 */
object OrderDurationRecorder {
    private var orderAddNewTimestamp: Long? = null
    private var cardPaymentStartedTimestamp: Long? = null

    fun startRecording() {
        orderAddNewTimestamp = System.currentTimeMillis()
    }

    fun recordCardPaymentStarted() {
        cardPaymentStartedTimestamp = System.currentTimeMillis()
    }

    fun reset() {
        orderAddNewTimestamp = null
        cardPaymentStartedTimestamp = null
    }

    fun millisecondsSinceOrderAddNew(): Result<Long> {
        return millisecondsSince(orderAddNewTimestamp)
    }

    fun millisecondsSinceCardPaymentStarted(): Result<Long> {
        return millisecondsSince(cardPaymentStartedTimestamp)
    }

    private fun millisecondsSince(origin: Long?): Result<Long> {
        return origin?.let {
            val timestamp = System.currentTimeMillis() - it

            return if (timestamp > TIMEOUT) {
                Result.failure(OrderDurationRecorderException(DURATION_TIMEOUT_EXCEEDED))
            } else {
                Result.success(timestamp)
            }
        } ?: Result.failure(OrderDurationRecorderException(NO_START_RECORDING_TIMESTAMP))
    }

    class OrderDurationRecorderException(
        val errorType: OrderDurationRecordedType
    ) : Exception() {
        enum class OrderDurationRecordedType {
            NO_START_RECORDING_TIMESTAMP,
            DURATION_TIMEOUT_EXCEEDED
        }
    }
}
