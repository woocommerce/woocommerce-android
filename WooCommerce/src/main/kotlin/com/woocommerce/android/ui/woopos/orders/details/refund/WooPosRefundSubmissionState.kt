package com.woocommerce.android.ui.woopos.orders.details.refund

import androidx.annotation.StringRes

sealed class WooPosRefundSubmissionState {
    data object Processing : WooPosRefundSubmissionState()
    data object PreparingReader : WooPosRefundSubmissionState()
    data object ReaderConnectionRequired : WooPosRefundSubmissionState()
    data class WaitingForCard(
        @StringRes val cardReaderHint: Int? = null,
        val isDismissBlocked: Boolean = false,
    ) : WooPosRefundSubmissionState()
    data object ProcessingReaderRefund : WooPosRefundSubmissionState()
    data object NotifyingStore : WooPosRefundSubmissionState()
    data object Success : WooPosRefundSubmissionState()
    data class Failure(
        val message: String,
        val retryBackendNotificationOnly: Boolean = false,
        val retryCardRefund: Boolean = false,
        val canRetry: Boolean = !retryBackendNotificationOnly,
        val apiErrorCode: String? = null,
    ) : WooPosRefundSubmissionState()
}
