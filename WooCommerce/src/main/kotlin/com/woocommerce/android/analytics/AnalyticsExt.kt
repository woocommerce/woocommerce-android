package com.woocommerce.android.analytics

import android.content.Context
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_DEVICE_TYPE_COMPACT
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_DEVICE_TYPE_REGULAR
import com.woocommerce.android.extensions.WindowSizeClass
import com.woocommerce.android.extensions.windowSizeClass

val Context.deviceTypeToAnalyticsString: String
    get() = buildAnalyticsDeviceTypeValue(IsTabletValue(value = windowSizeClass != WindowSizeClass.Compact))

val IsTabletValue.deviceTypeToAnalyticsString: String get() = buildAnalyticsDeviceTypeValue(this)

private fun buildAnalyticsDeviceTypeValue(isTabletValue: IsTabletValue) = if (isTabletValue.value) {
    VALUE_DEVICE_TYPE_REGULAR
} else {
    VALUE_DEVICE_TYPE_COMPACT
}

@JvmInline
value class IsTabletValue(val value: Boolean)
