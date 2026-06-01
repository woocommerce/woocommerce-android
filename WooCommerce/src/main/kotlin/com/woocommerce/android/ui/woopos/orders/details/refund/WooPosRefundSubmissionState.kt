package com.woocommerce.android.ui.woopos.orders.details.refund

sealed class WooPosRefundSubmissionState {
    data object Processing : WooPosRefundSubmissionState()
    data object Success : WooPosRefundSubmissionState()
    data class Failure(val message: String) : WooPosRefundSubmissionState()
}
