package com.woocommerce.android.ui.bookings.filter.type

import androidx.compose.runtime.Composable
import com.woocommerce.android.ui.bookings.filter.SingleChoiceFilterPage

@Composable
fun BookingTypeFilterPage(state: BookingTypeFilterUiState) {
    SingleChoiceFilterPage(
        items = state.items,
        selectedValue = state.selectedType.filterValue,
    )
}
