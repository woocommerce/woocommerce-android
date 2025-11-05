package com.woocommerce.android.ui.bookings.filter

import androidx.annotation.StringRes

/**
 * UI model simple filter item
 */
data class BookingFilterListItem(
    @StringRes val title: Int,
    val value: String? = null,
    val onClick: () -> Unit = {}
)
