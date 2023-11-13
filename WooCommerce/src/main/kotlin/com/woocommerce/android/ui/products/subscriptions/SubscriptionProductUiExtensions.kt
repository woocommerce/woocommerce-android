package com.woocommerce.android.ui.products.subscriptions

import com.woocommerce.android.R
import com.woocommerce.android.model.SubscriptionDetails
import com.woocommerce.android.viewmodel.ResourceProvider

fun SubscriptionDetails.expirationDisplayValue(resProvider: ResourceProvider): String {
    val periodString = period.getPeriodString(resProvider, periodInterval)
    return if (length != null) {
        resProvider.getString(R.string.subscription_period, length.toString(), periodString)
    } else {
        resProvider.getString(R.string.subscription_never_expire)
    }
}

fun SubscriptionDetails.expirationDisplayOptions(resources: ResourceProvider): List<String> {
    val periodString = period.getPeriodString(resources, periodInterval)
    val options = mutableListOf(
        resources.getString(R.string.subscription_never_expire)
    )
    period.getRangeForPeriod().forEach { index ->
        options.add(resources.getString(R.string.subscription_period, index, periodString))
    }
    return options
}
