package com.woocommerce.android.ui.bookings.filter

import androidx.annotation.StringRes
import com.woocommerce.android.model.UiString

/**
 * UI model simple filter item
 */
data class BookingFilterListItem(
    @StringRes val title: Int,
    val subtitle: UiString? = null,
    val selected: Boolean = false,
    val onClick: () -> Unit = {}
)
