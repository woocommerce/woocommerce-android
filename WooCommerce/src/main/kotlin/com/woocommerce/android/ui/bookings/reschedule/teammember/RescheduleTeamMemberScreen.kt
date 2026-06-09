package com.woocommerce.android.ui.bookings.reschedule.teammember

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.woocommerce.android.R
import com.woocommerce.android.ui.bookings.filter.BookingsFilterSelectionPage
import com.woocommerce.android.ui.bookings.filter.teammember.BookingTeamMemberFilterPageLoading
import com.woocommerce.android.ui.bookings.filter.teammember.BookingTeamMemberFilterUiState
import com.woocommerce.android.ui.bookings.filter.teammember.BookingTeamMemberFilterViewModel
import com.woocommerce.android.ui.bookings.filter.teammember.BookingTeamMemberFilterViewModel.SelectionMode
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.model.LocalOrRemoteId
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingsFilterOption.TeamMembers

@Composable
fun RescheduleTeamMemberRoute(
    initialResourceId: Long,
    productId: Long,
    onBack: () -> Unit,
    onTeamMemberSelected: (id: Long) -> Unit,
) {
    val initialMembers = if (initialResourceId != 0L) {
        TeamMembers(setOf(LocalOrRemoteId.RemoteId(initialResourceId)))
    } else {
        null
    }
    val viewModel =
        hiltViewModel<BookingTeamMemberFilterViewModel, BookingTeamMemberFilterViewModel.Factory> { factory ->
            factory.create(
                initial = initialMembers,
                onFilterChanged = {},
                selectionMode = SelectionMode.SINGLE,
                filterByProductId = productId,
            )
        }
    val uiState by viewModel.uiState.observeAsState()

    uiState?.let { state ->
        RescheduleTeamMemberScreen(
            state = state,
            event = viewModel.event,
            onBack = onBack,
            onConfirm = onTeamMemberSelected,
        )
    }
}

@Composable
private fun RescheduleTeamMemberScreen(
    state: BookingTeamMemberFilterUiState,
    event: LiveData<Event>,
    onBack: () -> Unit,
    onConfirm: (Long) -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    HandleEvents(event, snackbarHostState)

    val hasSelection = state.selectedMembers.values.isNotEmpty()

    Scaffold(
        topBar = {
            Toolbar(
                title = stringResource(R.string.booking_appointment_label_team_member),
                onNavigationButtonClick = onBack,
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                HorizontalDivider(thickness = 0.5.dp)
                WCColoredButton(
                    onClick = {
                        val selectedId = state.selectedMembers.values.firstOrNull()
                        if (selectedId != null) {
                            onConfirm(selectedId.value)
                        }
                    },
                    enabled = hasSelection,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Text(stringResource(R.string.booking_reschedule_change_team_member))
                }
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.surface,
    ) { innerPadding ->
        if (state.skeletonVisible) {
            BookingTeamMemberFilterPageLoading(modifier = Modifier.padding(innerPadding))
        } else {
            BookingsFilterSelectionPage(
                items = state.items,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        }
    }
}

@Composable
private fun HandleEvents(event: LiveData<Event>, snackbarHostState: SnackbarHostState) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    DisposableEffect(event, lifecycleOwner) {
        val observer = Observer { event: Event ->
            when (event) {
                is ShowSnackbar -> coroutineScope.launch {
                    snackbarHostState.showSnackbar(context.getString(event.message))
                }
            }
        }

        event.observe(lifecycleOwner, observer)

        onDispose { event.removeObserver(observer) }
    }
}
