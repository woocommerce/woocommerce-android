package com.woocommerce.android.ui.bookings.filter

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.woocommerce.android.R
import com.woocommerce.android.ui.bookings.filter.attendancestatus.BookingAttendanceStatusFilterRoute
import com.woocommerce.android.ui.bookings.filter.customer.BookingCustomerFilterPage
import com.woocommerce.android.ui.bookings.filter.datetime.DateTimeFilterRoute
import com.woocommerce.android.ui.bookings.filter.productname.BookingServiceEventFilterRoute
import com.woocommerce.android.ui.bookings.filter.teammember.BookingTeamMemberFilterRoute
import com.woocommerce.android.ui.bookings.filter.type.BookingTypeFilterRoute
import com.woocommerce.android.ui.compose.Render
import com.woocommerce.android.ui.compose.animations.slideInNavTransition
import com.woocommerce.android.ui.compose.animations.slideOutNavTransition
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.getText
import com.woocommerce.android.ui.compose.preview.LightDarkThemePreviews
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingFilters
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingsFilterOption
import java.util.Locale

@Composable
fun BookingFilterListScreen(state: BookingFilterListUiState) {
    Scaffold(
        topBar = {
            Column {
                if (state.showClearButton) {
                    Toolbar(
                        title = state.title.getText(),
                        onNavigationButtonClick = state.onClose,
                        navigationIcon = ImageVector.vectorResource(id = state.navigationIcon),
                        onActionButtonClick = state.onClear,
                        actionButtonText = stringResource(id = R.string.clear).uppercase(Locale.getDefault())
                    )
                } else {
                    Toolbar(
                        title = state.title.getText(),
                        onNavigationButtonClick = state.onClose,
                        navigationIcon = ImageVector.vectorResource(id = state.navigationIcon)
                    )
                }
                HorizontalDivider(thickness = 0.5.dp)
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                HorizontalDivider(thickness = 0.5.dp)
                WCColoredButton(
                    onClick = state.onShowBookings,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Text(stringResource(id = R.string.bookings_filters_show_bookings))
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
    ) { innerPadding ->
        FiltersNavHost(
            state = state,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        )

        state.dialogState?.Render()

        // The navigation is driven by the state, so we handle back navigation by calling onClose
        // We need to ensure that this called after NavHost to make sure we receive back events
        BackHandler {
            state.onClose()
        }
    }
}

@Composable
private fun FiltersNavHost(
    state: BookingFilterListUiState,
    modifier: Modifier
) {
    val navController = rememberNavController()

    LaunchedEffect(state.currentPage) {
        val currentRoute = navController.currentDestination?.route
        if (currentRoute == state.currentPage.route) return@LaunchedEffect

        if (state.currentPage != BookingFilterPage.List) {
            navController.navigate(state.currentPage.route) {
                popUpTo(BookingFilterPage.List.route)
            }
        } else {
            navController.popBackStack(BookingFilterPage.List.route, false)
        }
    }

    NavHost(
        navController = navController,
        startDestination = BookingFilterPage.List.route,
        enterTransition = { slideInNavTransition(forward = true) },
        exitTransition = { slideOutNavTransition(forward = true) },
        popEnterTransition = { slideInNavTransition(forward = false) },
        popExitTransition = { slideOutNavTransition(forward = false) },
        modifier = modifier
    ) {
        composable(BookingFilterPage.List.route) {
            BookingFilterRootPage(state.items)
        }
        composable(BookingFilterPage.DateTime.route) {
            DateTimeFilterRoute(
                initialRange = state.updatedBookingFilters.dateRange
            ) { dateRange ->
                state.onUpdateFilterOption(dateRange)
            }
        }
        composable(BookingFilterPage.TeamMember.route) {
            BookingTeamMemberFilterRoute(initialTeamMembers = state.updatedBookingFilters.teamMembers) { teamMember ->
                state.onUpdateFilterOption(teamMember)
            }
        }
        composable(BookingFilterPage.BookingType.route) {
            BookingTypeFilterRoute(initialType = state.updatedBookingFilters.bookingType) { type ->
                state.onUpdateFilterOption(type)
            }
        }
        composable(BookingFilterPage.ServiceEvent.route) {
            BookingServiceEventFilterRoute(
                initialServiceEvents = state.updatedBookingFilters.serviceEvents,
                onServiceEventsFilterChanged = { serviceEvents ->
                    state.onUpdateFilterOption(serviceEvents)
                }
            )
        }
        composable(BookingFilterPage.AttendanceStatus.route) {
            BookingAttendanceStatusFilterRoute(
                initialAttendanceStatus = state.updatedBookingFilters.attendanceStatus
            ) { attendanceStatus -> state.onUpdateFilterOption(attendanceStatus) }
        }
        composable(BookingFilterPage.PaymentStatus.route) {
        }
        composable(BookingFilterPage.Customer.route) {
            BookingCustomerFilterPage { customer ->
                customer.customerId?.let { id ->
                    state.onUpdateFilterOption(
                        BookingsFilterOption.Customer(
                            userId = id,
                            customerName = "${customer.firstName} ${customer.lastName}".trim()
                                .ifBlank { customer.email }.orEmpty()
                        )
                    )
                }
                state.onClose()
            }
        }
        composable(BookingFilterPage.Location.route) {
        }
    }
}

private val BookingFilterPage.route: String
    get() = name

@LightDarkThemePreviews
@Composable
private fun BookingFilterListScreenPreview() {
    WooThemeWithBackground {
        BookingFilterListScreen(
            state = BookingFilterListUiState(
                initialBookingFilters = BookingFilters(),
            )
        )
    }
}
