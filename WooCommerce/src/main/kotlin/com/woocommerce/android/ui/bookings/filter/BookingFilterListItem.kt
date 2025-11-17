package com.woocommerce.android.ui.bookings.filter

import com.woocommerce.android.model.UiString

/**
 * UI model simple filter item
 */
data class BookingFilterListItem(
    val title: UiString,
    val value: UiString? = null,
    val selected: Boolean = false,
    val onClick: () -> Unit = {}
)
