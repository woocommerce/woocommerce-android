package com.woocommerce.android.ui.bookings.filter

import androidx.annotation.StringRes

/**
 * UI model simple filter item
 */
data class BookingFilterListItem(
    @StringRes val title: Int,
    val subtitle: BookingFilterListItemSubtitle? = null,
    val onClick: () -> Unit = {}
) {
    data class BookingFilterListItemSubtitle(val valueString: String? = null, @StringRes val valueRes: Int? = null)
}
