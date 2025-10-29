package com.woocommerce.android.ui.bookings.filter

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.bookings.filter.type.BookingTypeFilterPage
import com.woocommerce.android.ui.bookings.filter.type.DUMMY_BOOKING_TYPE_FILTER_UI_STATE
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.preview.LightDarkThemePreviews
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground

@Composable
fun BookingFilterListScreen(state: BookingFilterListUiState) {
    BackHandler {
        state.onClose()
    }

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
        AnimatedContent(
            targetState = state.currentPage,
            transitionSpec = {
                if (targetState is BookingFilterPage.List) {
                    slideOut()
                } else {
                    slideIn()
                }
            },
            label = "BookingFiltersAnimatedContent",
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) { page ->
            when (page) {
                is BookingFilterPage.List -> {
                    BookingFilterRootPage(state.items)
                }

                BookingFilterPage.BookingType -> BookingTypeFilterPage(DUMMY_BOOKING_TYPE_FILTER_UI_STATE)
                BookingFilterPage.AttendanceStatus,
                BookingFilterPage.Customer,
                BookingFilterPage.Location,
                BookingFilterPage.PaymentStatus,
                BookingFilterPage.ServiceEvent,
                BookingFilterPage.TeamMember,
                is BookingFilterPage.DateTime -> {
                    DateTimeFilterPicker()
                }
            }
        }
    }
}

private const val TRANSITION_DURATION = 250

private fun slideIn(duration: Int = TRANSITION_DURATION): ContentTransform {
    return (
        slideInHorizontally(animationSpec = tween(durationMillis = duration)) { fullWidth -> fullWidth } +
            fadeIn(animationSpec = tween(durationMillis = duration))
        ) togetherWith (
        slideOutHorizontally(animationSpec = tween(durationMillis = duration)) { fullWidth -> -fullWidth } +
            fadeOut(animationSpec = tween(durationMillis = duration))
        )
}

private fun slideOut(duration: Int = TRANSITION_DURATION): ContentTransform {
    return (
        slideInHorizontally(animationSpec = tween(durationMillis = duration)) { fullWidth -> -fullWidth } +
            fadeIn(animationSpec = tween(durationMillis = duration))
        ) togetherWith (
        slideOutHorizontally(animationSpec = tween(durationMillis = duration)) { fullWidth -> fullWidth } +
            fadeOut(animationSpec = tween(durationMillis = duration))
        )
}

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
