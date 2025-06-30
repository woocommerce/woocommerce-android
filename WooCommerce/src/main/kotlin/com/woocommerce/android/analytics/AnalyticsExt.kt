package com.woocommerce.android.analytics

import android.content.Context
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_DEVICE_TYPE_COMPACT
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_DEVICE_TYPE_REGULAR
import com.woocommerce.android.extensions.isTwoPanesShouldBeUsed

val Context.deviceTypeToAnalyticsString: String
    get() = buildAnalyticsDeviceTypeValue(
        IsScreenInTwoPaneLayout(value = isTwoPanesShouldBeUsed)
    )

val IsScreenInTwoPaneLayout.deviceTypeToAnalyticsString: String
    get() = buildAnalyticsDeviceTypeValue(this)

private fun buildAnalyticsDeviceTypeValue(isScreenInTwoPaneLayout: IsScreenInTwoPaneLayout) =
    // Keeping the value as it is to maintain backward compatibility with the existing analytics data
    if (isScreenInTwoPaneLayout.value) {
        VALUE_DEVICE_TYPE_REGULAR // 2 pane layout
    } else {
        VALUE_DEVICE_TYPE_COMPACT // Single pane layout
    }

@JvmInline
value class IsScreenInTwoPaneLayout(val value: Boolean)
