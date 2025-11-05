package com.woocommerce.android.ui.bookings.filter

import androidx.activity.compose.BackHandler
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import com.woocommerce.android.ui.bookings.filter.customer.BookingCustomerFilterPage
import com.woocommerce.android.ui.bookings.filter.type.BookingTypeFilterRoute
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.preview.LightDarkThemePreviews
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingsFilterOption

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
        enterTransition = { slideIn(popNavigation = true) },
        exitTransition = { slideOut(popNavigation = true) },
        popEnterTransition = { slideIn(popNavigation = false) },
        popExitTransition = { slideOut(popNavigation = false) },
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
            BookingTypeFilterRoute(initialType = state.currentBookingType) { type ->
                state.onUpdateFilterOption(type)
            }
        }
        composable(BookingFilterPage.Customer.route) {
            BookingCustomerFilterPage { customer ->
                state.onUpdateFilterOption(
                    BookingsFilterOption.Customer(
                        customerId = customer.customerId ?: 0L,
                        customerName = "${customer.firstName} ${customer.lastName}".trim()
                            .ifBlank { customer.email }.orEmpty()
                    )
                )
                state.onClose()
            }
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
    get() = name

private fun slideIn(popNavigation: Boolean): EnterTransition {
    return slideInHorizontally(animationSpec = tween(durationMillis = TRANSITION_DURATION)) { fullWidth ->
        if (popNavigation) fullWidth else -fullWidth
    } + fadeIn(animationSpec = tween(durationMillis = TRANSITION_DURATION))
}

private fun slideOut(popNavigation: Boolean): ExitTransition {
    return slideOutHorizontally(animationSpec = tween(durationMillis = TRANSITION_DURATION)) { fullWidth ->
        if (popNavigation) -fullWidth else fullWidth
    } + fadeOut(animationSpec = tween(durationMillis = TRANSITION_DURATION))
}

private const val TRANSITION_DURATION = 250

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
