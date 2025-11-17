package com.woocommerce.android.ui.bookings.filter.teammember

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.woocommerce.android.ui.bookings.filter.BookingsFilterSelectionPage
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingsFilterOption

@Composable
fun BookingTeamMemberFilterRoute(
    initialTeamMembers: BookingsFilterOption.TeamMembers?,
    onTeamMembersFilterChanged: (BookingsFilterOption.TeamMembers) -> Unit
) {
    val viewModel =
        hiltViewModel<BookingTeamMemberFilterViewModel, BookingTeamMemberFilterViewModel.Factory> { factory ->
            factory.create(initialTeamMembers, onTeamMembersFilterChanged)
        }
    val uiState by viewModel.uiState.collectAsState()
    BookingTeamMemberFilterPage(uiState)
}

@Composable
fun BookingTeamMemberFilterPage(state: BookingTeamMemberFilterUiState) {
    BookingsFilterSelectionPage(items = state.items)
}
