package com.woocommerce.android.ui.bookings.filter.teammember

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.woocommerce.android.ui.bookings.filter.BookingsFilterSelectionPage
import com.woocommerce.android.ui.compose.animations.SkeletonView
import com.woocommerce.android.viewmodel.MultiLiveEvent
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
    val uiState by viewModel.uiState.observeAsState()
    val event by viewModel.event.observeAsState()
    uiState?.let { BookingTeamMemberFilterPage(it, event) }
}

@Composable
fun BookingTeamMemberFilterPage(state: BookingTeamMemberFilterUiState, event: MultiLiveEvent.Event?) {
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(event) {
        if (event is MultiLiveEvent.Event.ShowSnackbar) {
            snackbarHostState.showSnackbar(context.getString(event.message))
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        if (state.isLoading) {
            BookingTeamMemberFilterPageLoading(modifier = Modifier.padding(paddingValues))
        } else {
            BookingsFilterSelectionPage(items = state.items, modifier = Modifier.padding(paddingValues))
        }
    }
}

@Composable
fun BookingTeamMemberFilterPageLoading(modifier: Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        SkeletonView(
            modifier = Modifier
                .size(62.dp, 64.dp)
                .padding(horizontal = 16.dp, vertical = 24.dp)
        )
        HorizontalDivider(thickness = 0.5.dp)
        SkeletonView(
            modifier = Modifier
                .size(175.dp, 64.dp)
                .padding(horizontal = 16.dp, vertical = 24.dp)
        )
        HorizontalDivider(thickness = 0.5.dp)
        SkeletonView(
            modifier = Modifier
                .size(130.dp, 64.dp)
                .padding(horizontal = 16.dp, vertical = 24.dp)
        )
        HorizontalDivider(thickness = 0.5.dp)
        SkeletonView(
            modifier = Modifier
                .size(167.dp, 64.dp)
                .padding(horizontal = 16.dp, vertical = 24.dp)
        )
        HorizontalDivider(thickness = 0.5.dp)
        SkeletonView(
            modifier = Modifier
                .size(166.dp, 64.dp)
                .padding(horizontal = 16.dp, vertical = 24.dp)
        )
        HorizontalDivider(thickness = 0.5.dp)
    }
}
