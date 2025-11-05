package com.woocommerce.android.ui.bookings.filter.type

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.woocommerce.android.ui.bookings.filter.SingleChoiceFilterPage
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingsFilterOption

@Composable
fun BookingTypeFilterRoute(
    initialType: BookingsFilterOption.BookingType,
    onTypeFilterChanged: (BookingsFilterOption.BookingType) -> Unit,
) {
    val viewModel = hiltViewModel<BookingTypeFilterViewModel, BookingTypeFilterViewModel.Factory> { factory ->
        factory.create(initialType, onTypeFilterChanged)
    }

    val uiState by viewModel.uiState.collectAsState()
    BookingTypeFilterPage(uiState)
}

@Composable
fun BookingTypeFilterPage(state: BookingTypeFilterUiState) {
    SingleChoiceFilterPage(items = state.items)
}
