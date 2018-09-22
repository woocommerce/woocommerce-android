package com.woocommerce.android.util

object AnalyticsUtils {
    /**
     * Returns the proper label for a toggle state property
     * on a track event.
     */
    fun getToggleStateLabel(isSelected: Boolean): String {
        return when (isSelected) {
            true -> "on"
            false -> "off"
        }
    }
}
