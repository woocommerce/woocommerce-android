package com.woocommerce.android.ui.orders.wooshippinglabels.servicestatus

data class ServiceStatusNotice(
    val id: String,
    val severity: Severity,
    val message: String,
    val isDismissible: Boolean,
    val actionUrl: String?,
) {
    enum class Severity {
        INFO,
        WARNING,
        ERROR
    }
}
