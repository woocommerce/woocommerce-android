package com.woocommerce.android.ui.bookings.reschedule

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.Toolbar

@Composable
fun BookingRescheduleScreen(
    viewModel: BookingRescheduleViewModel,
) {
    val state by viewModel.state.observeAsState()
    BookingRescheduleScreen(
        state = state,
        onBackPressed = viewModel::onBackPressed,
    )
}

@Composable
fun BookingRescheduleScreen(
    state: BookingRescheduleState?,
    onBackPressed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            Toolbar(
                title = stringResource(R.string.booking_reschedule_screen_title),
                onNavigationButtonClick = onBackPressed,
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center,
        ) {
            when (state?.availabilityState) {
                is BookingRescheduleState.AvailabilityState.Loading, null -> {
                    CircularProgressIndicator()
                }
                is BookingRescheduleState.AvailabilityState.Error -> {
                    // Empty for now — error is shown via snackbar
                }
                is BookingRescheduleState.AvailabilityState.Loaded -> {
                    // Availability data loaded — calendar UI will be added here
                }
            }
        }
    }
}
