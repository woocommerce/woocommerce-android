package com.woocommerce.android.analytics

import android.content.Context
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_DEVICE_TYPE_COMPACT
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_DEVICE_TYPE_REGULAR
import com.woocommerce.android.extensions.isTablet

val Context.deviceTypeToAnalyticsString: String
    get() = buildAnalyticsDeviceTypeValue(IsTabletValue(value = isTablet()))

val IsTabletValue.deviceTypeToAnalyticsString: String get() = buildAnalyticsDeviceTypeValue(this)

private fun buildAnalyticsDeviceTypeValue(isTabletValue: IsTabletValue) = if (isTabletValue.value) {
    VALUE_DEVICE_TYPE_REGULAR
} else {
    VALUE_DEVICE_TYPE_COMPACT
}

@JvmInline
value class IsTabletValue(val value: Boolean)
