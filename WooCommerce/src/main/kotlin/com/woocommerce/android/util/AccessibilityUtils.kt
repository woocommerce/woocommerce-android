package com.woocommerce.android.util

import android.view.View

fun View.announceAccessibilityChange(message: String, liveRegionMode: Int = View.ACCESSIBILITY_LIVE_REGION_POLITE) {
    this.accessibilityLiveRegion = liveRegionMode
    this.contentDescription = message
}
