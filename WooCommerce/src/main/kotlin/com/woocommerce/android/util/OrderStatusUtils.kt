package com.woocommerce.android.util

import com.woocommerce.android.R
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.CoreOrderStatus

object OrderStatusUtils {
    fun getLabelForOrderStatus(orderStatus: CoreOrderStatus, getString: (Int) -> (String)): String {
        return getString(when (orderStatus) {
            CoreOrderStatus.PENDING -> R.string.orderstatus_pending
            CoreOrderStatus.PROCESSING -> R.string.orderstatus_processing
            CoreOrderStatus.ON_HOLD -> R.string.orderstatus_hold
            CoreOrderStatus.COMPLETED -> R.string.orderstatus_completed
            CoreOrderStatus.CANCELLED -> R.string.orderstatus_cancelled
            CoreOrderStatus.REFUNDED -> R.string.orderstatus_refunded
            CoreOrderStatus.FAILED -> R.string.orderstatus_failed
        })
    }
}
