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

    /**
     * Returns the proper label for the confirmation dialog result property on a
     * track event.
     */
    fun getConfirmationResultLabel(isPositive: Boolean): String {
        return when (isPositive) {
            true -> "positive"
            false -> "negative"
        }
    }
}
