package com.woocommerce.android.ui.bookings.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import com.woocommerce.android.ui.bookings.compose.BookingAppointmentDetails
import com.woocommerce.android.ui.bookings.compose.BookingAppointmentDetailsModel
import com.woocommerce.android.ui.bookings.compose.BookingAttendanceSection
import com.woocommerce.android.ui.bookings.compose.BookingAttendanceStatus
import com.woocommerce.android.ui.bookings.compose.BookingAttendanceStatusBottomSheet
import com.woocommerce.android.ui.bookings.compose.BookingCustomerDetails
import com.woocommerce.android.ui.bookings.compose.BookingCustomerDetailsModel
import com.woocommerce.android.ui.bookings.compose.BookingPaymentDetailsModel
import com.woocommerce.android.ui.bookings.compose.BookingPaymentSection
import com.woocommerce.android.ui.bookings.compose.BookingStaffMemberStatus
import com.woocommerce.android.ui.bookings.compose.BookingStatus
import com.woocommerce.android.ui.bookings.compose.BookingSummary
import com.woocommerce.android.ui.bookings.compose.BookingSummaryModel
import com.woocommerce.android.ui.bookings.compose.CancelBookingDialog
import com.woocommerce.android.ui.compose.animations.SkeletonView
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.compose.component.WCPullToRefreshBox
import com.woocommerce.android.ui.compose.preview.LightDarkThemePreviews
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingDetailsScreen(
    viewModel: BookingDetailsViewModel,
    onBack: () -> Unit,
    onViewOrder: (Long) -> Unit
) {
    val viewState by viewModel.state.observeAsState()

    viewState?.let {
        BookingDetailsScreen(
            viewState = it,
            onBack = onBack,
            onViewOrder = onViewOrder,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingDetailsScreen(
    viewState: BookingDetailsViewState,
    onBack: () -> Unit,
    onViewOrder: (Long) -> Unit,
) {
    val showAttendanceSheet = remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            Toolbar(
                title = viewState.toolbarTitle,
                onNavigationButtonClick = onBack,
                modifier = Modifier.shadow(4.dp)
            )
        }
    ) { innerPadding ->
        WCPullToRefreshBox(
            isRefreshing = viewState.loadingState == BookingDetailsLoadingState.Refreshing,
            onRefresh = viewState.onRefresh,
            state = rememberPullToRefreshState(),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
            ) {
                when {
                    viewState.shouldShowSkeleton -> BookingDetailsLoading()
                    viewState.bookingUiState != null -> {
                        BookingDetailsContent(
                            booking = viewState.bookingUiState,
                            onCancelBooking = viewState.onCancelBooking,
                            onViewOrder = onViewOrder,
                            onAttendanceStatusClicked = { showAttendanceSheet.value = true },
                        )
                    }
                }
            }
        }
        if (showAttendanceSheet.value) {
            BookingAttendanceStatusBottomSheet(
                onSelect = { status ->
                    viewState.onAttendanceStatusSelected(status)
                },
                onDismiss = { showAttendanceSheet.value = false }
            )
        }
        if (viewState.showCancelBookingDialog) {
            CancelBookingDialog(
                message = viewState.cancelDialogMessage,
                onDismiss = viewState.onDismissCancelDialog,
                onConfirmCancel = viewState.onConfirmCancelBooking,
            )
        }
    }
}

@Composable
private fun BookingDetailsContent(
    booking: BookingUiState,
    onCancelBooking: () -> Unit,
    onViewOrder: (Long) -> Unit,
    onAttendanceStatusClicked: () -> Unit,
) {
    BookingSummary(
        model = booking.bookingSummary,
        modifier = Modifier.fillMaxWidth()
    )
    BookingAppointmentDetails(
        model = booking.bookingsAppointmentDetails,
        onCancelBooking = onCancelBooking,
        modifier = Modifier.fillMaxWidth()
    )
    BookingCustomerDetails(
        model = booking.bookingCustomerDetails,
        modifier = Modifier.fillMaxWidth()
    )
    BookingAttendanceSection(
        status = booking.bookingSummary.attendanceStatus,
        onClick = onAttendanceStatusClicked,
        modifier = Modifier.fillMaxWidth()
    )
    booking.bookingPaymentDetails?.let {
        BookingPaymentSection(
            model = it,
            status = booking.bookingSummary.status,
            onMarkAsPaid = { onViewOrder(booking.orderId) },
            onViewOrder = { onViewOrder(booking.orderId) },
            onMarkAsRefunded = { onViewOrder(booking.orderId) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun BookingDetailsLoading() {
    Column {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(16.dp)
        ) {
            SkeletonView(Modifier.size(200.dp, 20.dp))
            Spacer(Modifier.height(4.dp))
            SkeletonView(Modifier.size(250.dp, 15.dp))
            Spacer(Modifier.height(8.dp))
            SkeletonView(Modifier.size(150.dp, 25.dp))
        }
        Spacer(Modifier.height(40.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainer)
        ) {
            repeat(6) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .padding(vertical = 10.dp, horizontal = 16.dp)
                        .fillMaxWidth()
                ) {
                    SkeletonView(
                        modifier = Modifier
                            .height(20.dp)
                            .width(50.dp + 10 * (it % 3).dp)
                    )
                    SkeletonView(
                        modifier = Modifier
                            .height(20.dp)
                            .width(100.dp + 10 * (it % 5).dp)
                    )
                }
                HorizontalDivider(
                    Modifier.padding(start = 16.dp)
                )
            }
            SkeletonView(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(40.dp)
            )
        }
    }
}

@LightDarkThemePreviews
@Composable
private fun BookingDetailsPreview() {
    WooThemeWithBackground {
        BookingDetailsScreen(
            viewState = BookingDetailsViewState(
                toolbarTitle = "Booking #12345",
                bookingUiState = BookingUiState(
                    orderId = 1L,
                    bookingSummary = BookingSummaryModel(
                        date = "05/07/2025, 11:00 AM",
                        name = "Women’s Haircut",
                        customerName = "Margarita Nikolaevna",
                        attendanceStatus = BookingAttendanceStatus.CHECKED_IN,
                        status = BookingStatus.Paid
                    ),
                    bookingsAppointmentDetails = BookingAppointmentDetailsModel(
                        date = "Monday, 05 July 2025",
                        time = "11:00 am - 12:00 pm",
                        staff = BookingStaffMemberStatus.Loaded("Marianne Renoir"),
                        location = "238 Willow Creek Drive, Montgomery AL 36109",
                        duration = "60 min",
                        price = "$55.00",
                        cancelState = CancelState.Idle,
                    ),
                    bookingCustomerDetails = BookingCustomerDetailsModel(
                        name = "Margarita Nikolaevna",
                        email = "margarita@example.com",
                        phone = "+1 555-123-4567",
                        billingAddress = """
                            238 Willow Creek Drive
                            Montgomery AL 36109
                            United States
                        """.trimIndent()
                    ),
                    bookingPaymentDetails = BookingPaymentDetailsModel(
                        service = "$55.00",
                        tax = "$4.50",
                        discount = "-",
                        total = "$59.50"
                    )
                ),
            ),
            onBack = {},
            onViewOrder = {}
        )
    }
}

@LightDarkThemePreviews
@Composable
private fun BookingDetailsLoadingPreview() {
    WooThemeWithBackground {
        BookingDetailsScreen(
            viewState = BookingDetailsViewState(
                toolbarTitle = "",
                bookingUiState = null,
            ),
            onBack = {},
            onViewOrder = {},
        )
    }
}
