package com.woocommerce.android.ui.bookings.reschedule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.bookings.compose.BookingLabel
import com.woocommerce.android.ui.compose.animations.SkeletonView
import com.woocommerce.android.ui.compose.component.Toolbar

@Composable
fun BookingRescheduleScreen(
    viewModel: BookingRescheduleViewModel,
) {
    val state by viewModel.state.observeAsState()
    RescheduleContent(
        state = state,
        onBackPressed = viewModel::onBackPressed,
    )
}

@Composable
fun RescheduleContent(
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
                modifier = Modifier.shadow(4.dp),
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            if (state != null && state.teamMemberId != 0L) {
                TeamMemberSelectorRow(
                    teamMemberName = state.teamMemberName,
                )
            }
            Box(
                modifier = Modifier.fillMaxSize(),
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
}

@Composable
private fun TeamMemberSelectorRow(
    teamMemberName: String?,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                BookingLabel(R.string.booking_appointment_label_team_member)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (teamMemberName != null) {
                        Text(
                            text = teamMemberName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    } else {
                        SkeletonView(
                            width = 80.dp,
                            height = with(LocalDensity.current) {
                                MaterialTheme.typography.bodyMedium.fontSize.toDp()
                            },
                        )
                    }
                    Icon(
                        painter = painterResource(id = R.drawable.ic_arrow_right),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
            HorizontalDivider(
                modifier = Modifier.padding(start = 16.dp),
                thickness = 0.5.dp,
            )
        }
    }
}
