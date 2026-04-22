package com.woocommerce.android.ui.bookings.reschedule

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.woocommerce.android.R
import com.woocommerce.android.ui.bookings.compose.BookingLabel
import com.woocommerce.android.ui.bookings.reschedule.teammember.RescheduleTeamMemberRoute
import com.woocommerce.android.ui.compose.animations.SkeletonView
import com.woocommerce.android.ui.compose.animations.slideInNavTransition
import com.woocommerce.android.ui.compose.animations.slideOutNavTransition
import com.woocommerce.android.ui.compose.component.DatePickerDialog
import com.woocommerce.android.ui.compose.component.Toolbar
import java.util.Calendar
import java.util.TimeZone

@Composable
fun BookingRescheduleScreen(
    viewModel: BookingRescheduleViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.observeAsState()
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = ReschedulePage.Reschedule.route,
        modifier = modifier,
        enterTransition = { slideInNavTransition(forward = true) },
        exitTransition = { slideOutNavTransition(forward = true) },
        popEnterTransition = { slideInNavTransition(forward = false) },
        popExitTransition = { slideOutNavTransition(forward = false) },
    ) {
        composable(ReschedulePage.Reschedule.route) {
            RescheduleContent(
                state = state,
                onBackPressed = viewModel::onBackPressed,
                onTeamMemberRowClicked = { navController.navigate(ReschedulePage.TeamMemberSelector.route) },
                onDateRowClicked = viewModel::onDateRowClicked,
            )
        }
        composable(ReschedulePage.TeamMemberSelector.route) {
            val currentState = state
            if (currentState != null) {
                RescheduleTeamMemberRoute(
                    initialResourceId = currentState.teamMemberId,
                    productId = currentState.productId,
                    onBack = { navController.popBackStack() },
                    onTeamMemberSelected = { id ->
                        viewModel.onTeamMemberChanged(id)
                        navController.popBackStack()
                    },
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
private fun RescheduleContent(
    state: BookingRescheduleState?,
    onBackPressed: () -> Unit,
    onTeamMemberRowClicked: () -> Unit,
    onDateRowClicked: () -> Unit,
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
                    onClick = onTeamMemberRowClicked,
                )
            }
            if (state != null && state.formattedDate.isNotEmpty()) {
                RescheduleRow(
                    labelRes = R.string.booking_appointment_label_date,
                    onClick = onDateRowClicked,
                ) {
                    RescheduleRowValue(state.formattedDate)
                }
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

    state?.datePickerState?.let { pickerState ->
        val utcTimeZone = TimeZone.getTimeZone("UTC")
        DatePickerDialog(
            currentDate = pickerState.currentDateMillis?.let { millis ->
                Calendar.getInstance(utcTimeZone).apply { timeInMillis = millis }
            },
            minDate = pickerState.minDateMillis?.let { millis ->
                Calendar.getInstance(utcTimeZone).apply { timeInMillis = millis }
            },
            onDateSelected = { calendar ->
                pickerState.onDateSelected(calendar.timeInMillis)
            },
            onDismissRequest = pickerState.onDismiss,
        )
    }
}

@Composable
private fun TeamMemberSelectorRow(
    teamMemberName: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    RescheduleRow(
        labelRes = R.string.booking_appointment_label_team_member,
        onClick = onClick,
        modifier = modifier,
    ) {
        if (teamMemberName != null) {
            RescheduleRowValue(teamMemberName)
        } else {
            SkeletonView(
                width = 80.dp,
                height = with(LocalDensity.current) {
                    MaterialTheme.typography.bodyMedium.fontSize.toDp()
                },
            )
        }
    }
}

@Composable
private fun RescheduleRowValue(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun RescheduleRow(
    @StringRes labelRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    value: @Composable () -> Unit,
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
                    .clickable(onClick = onClick)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                BookingLabel(labelRes)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    value()
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

private enum class ReschedulePage {
    Reschedule,
    TeamMemberSelector,
}

private val ReschedulePage.route: String
    get() = name
