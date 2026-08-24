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

    /**
     * [apiErrorCode] is the store's REST error code, when it returned one. Kept raw rather than
     * pre-mapped so unrecognised codes survive for analytics too.
     *
     * A code that [WooPosRefundApiError] recognises makes the failure non-retryable by default:
     * those are deterministic validation rejections, so resubmitting the same request produces the
     * same rejection. Recovering needs the order data refreshed or the selection changed.
     */
    data class Failure(
        val message: String,
        val retryBackendNotificationOnly: Boolean = false,
        val retryCardRefund: Boolean = false,
        val apiErrorCode: String? = null,
        val canRetry: Boolean = !retryBackendNotificationOnly && WooPosRefundApiError.fromCode(apiErrorCode) == null,
    ) : WooPosRefundSubmissionState()
}
