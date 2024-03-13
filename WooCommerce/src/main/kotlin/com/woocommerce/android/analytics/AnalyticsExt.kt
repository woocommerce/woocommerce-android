package com.woocommerce.android.analytics

import android.content.Context
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_DEVICE_TYPE_COMPACT
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_DEVICE_TYPE_REGULAR
import com.woocommerce.android.extensions.WindowSizeClass
import com.woocommerce.android.extensions.windowSizeClass

val Context.deviceTypeToAnalyticsString: String
    get() = buildAnalyticsDeviceTypeValue(
        IsScreenLargerThanCompactValue(value = windowSizeClass != WindowSizeClass.Compact)
    )

val IsScreenLargerThanCompactValue.deviceTypeToAnalyticsString: String
    get() = buildAnalyticsDeviceTypeValue(this)

private fun buildAnalyticsDeviceTypeValue(isScreenSizeLargerThanCompact: IsScreenLargerThanCompactValue) =
    if (isScreenSizeLargerThanCompact.value) {
        VALUE_DEVICE_TYPE_REGULAR
    } else {
        VALUE_DEVICE_TYPE_COMPACT
    }

@JvmInline
value class IsScreenLargerThanCompactValue(val value: Boolean)
