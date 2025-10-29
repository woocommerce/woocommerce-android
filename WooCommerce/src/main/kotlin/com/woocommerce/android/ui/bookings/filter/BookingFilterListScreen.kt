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
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.preview.LightDarkThemePreviews
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground

@Composable
fun BookingFilterListScreen(state: BookingFilterListUiState) {
    Scaffold(
        topBar = {
            Column {
                Toolbar(
                    title = stringResource(state.currentPage.titleRes),
                    onNavigationButtonClick = state.onClose,
                    navigationIcon = ImageVector.vectorResource(id = state.navigationIcon)
                )
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

        // Make sure this is called after the NavHost to properly receive back events
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
        modifier = modifier
    ) {
        composable(BookingFilterPage.List.route) {
            BookingFilterRootPage(state.items)
        }
        composable(BookingFilterPage.DateTime.route) {
            DateTimeFilterPicker()
        }
        composable(BookingFilterPage.TeamMember.route) {
            TODO()
        }
        composable(BookingFilterPage.AttendanceStatus.route) {
            TODO()
        }
        composable(BookingFilterPage.PaymentStatus.route) {
            TODO()
        }
        composable(BookingFilterPage.BookingType.route) {
            TODO()
        }
        composable(BookingFilterPage.Customer.route) {
            TODO()
        }
        composable(BookingFilterPage.ServiceEvent.route) {
            TODO()
        }
        composable(BookingFilterPage.Location.route) {
            TODO()
        }
    }
}

private val BookingFilterPage.route: String
    get() = this::class.java.simpleName

@LightDarkThemePreviews
@Composable
private fun BookingFilterListScreenPreview() {
    WooThemeWithBackground {
        BookingFilterListScreen(
            state = BookingFilterListUiState(
                initialBookingFilters = null,
            )
        )
    }
}
