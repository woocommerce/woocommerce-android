package com.woocommerce.android.aiassistant.appfunctions

import android.content.Intent

fun interface AppFunctionsAccessPolicy {
    fun isAllowed(): Boolean
}

fun interface AppFunctionsHostNavigation {
    fun orderDetailsIntent(orderId: Long): Intent
}
