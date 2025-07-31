package com.woocommerce.android.util

import android.view.View

fun View.announceAccessibilityChange(message: String) {
    this.accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
    this.contentDescription = message
}
