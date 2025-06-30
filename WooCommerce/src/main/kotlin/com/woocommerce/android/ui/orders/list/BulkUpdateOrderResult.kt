package com.woocommerce.android.ui.orders.list

sealed class BulkUpdateOrderResult {
    data class PartialSuccess(
        val successCount: Int,
        val failureCount: Int
    ) : BulkUpdateOrderResult()

    data object AllSuccess : BulkUpdateOrderResult()

    data object AllFailed : BulkUpdateOrderResult()

    data object NoOrdersUpdated : BulkUpdateOrderResult()

    data class Error(val exception: Exception) : BulkUpdateOrderResult()
}
