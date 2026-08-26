package com.woocommerce.android.ui.woopos.orders.details.refund

import com.woocommerce.android.R
import com.woocommerce.android.viewmodel.ResourceProvider
import javax.inject.Inject

class WooPosMapRefundFailure @Inject constructor(
    private val resourceProvider: ResourceProvider,
) {
    operator fun invoke(failure: WooPosRefundSubmissionState.Failure): WooPosRefundState {
        // On a backend-only notification the card is already reversed and only the store call
        // failed, so the mapped code is read out of the way first: every recovery it offers leads
        // back into the flow, and walking it again would refund the card a second time. Its message
        // describes the order rather than the refund that went through, so the cashier is told to
        // record it instead.
        val backendOnly = failure.retryBackendNotificationOnly
        val apiError = WooPosRefundApiError.fromCode(failure.apiErrorCode).takeIf { !backendOnly }
        if (apiError == WooPosRefundApiError.OrderNotRefundable) {
            return WooPosRefundState.NoRefundableItems
        }

        return WooPosRefundState.Error(
            message = if (backendOnly) {
                resourceProvider.getString(R.string.woopos_refund_error_store_not_updated)
            } else {
                failure.message
            },
            errorType = WooPosRefundState.Error.ErrorType.Processing,
            recovery = when {
                backendOnly -> WooPosRefundState.Recovery.None
                apiError != null -> apiError.recovery
                failure.canRetry -> WooPosRefundState.Recovery.Retry
                else -> WooPosRefundState.Recovery.None
            },
        )
    }
}
