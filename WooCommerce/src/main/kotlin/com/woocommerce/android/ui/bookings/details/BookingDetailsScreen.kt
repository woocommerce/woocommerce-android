package com.woocommerce.android.ui.bookings.details

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import com.woocommerce.android.R
import com.woocommerce.android.extensions.isTwoPanesShouldBeUsed
import com.woocommerce.android.ui.bookings.PaymentStatus
import com.woocommerce.android.ui.bookings.compose.BookingAppointmentDetails
import com.woocommerce.android.ui.bookings.compose.BookingAppointmentDetailsModel
import com.woocommerce.android.ui.bookings.compose.BookingAttendanceStatus
import com.woocommerce.android.ui.bookings.compose.BookingCustomerDetails
import com.woocommerce.android.ui.bookings.compose.BookingCustomerDetailsUiModel
import com.woocommerce.android.ui.bookings.compose.BookingLocationStatus
import com.woocommerce.android.ui.bookings.compose.BookingNoteSection
import com.woocommerce.android.ui.bookings.compose.BookingPaymentDetailsModel
import com.woocommerce.android.ui.bookings.compose.BookingPaymentSection
import com.woocommerce.android.ui.bookings.compose.BookingStaffMemberStatus
import com.woocommerce.android.ui.bookings.compose.BookingSummary
import com.woocommerce.android.ui.bookings.compose.BookingSummaryLoading
import com.woocommerce.android.ui.bookings.compose.BookingSummaryModel
import com.woocommerce.android.ui.compose.Render
import com.woocommerce.android.ui.compose.animations.SkeletonView
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.compose.component.WCPullToRefreshBox
import com.woocommerce.android.ui.compose.modifier.detailsPanePadding
import com.woocommerce.android.ui.compose.preview.LightDarkThemePreviews
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingDetailsScreen(
    viewModel: BookingDetailsViewModel,
    onBack: () -> Unit,
) {
    val viewState by viewModel.state.observeAsState()

    viewState?.let {
        BookingDetailsScreen(
            viewState = it,
            onBack = onBack,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingDetailsScreen(
    viewState: BookingDetailsViewState,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    Scaffold(
        topBar = {
            Toolbar(
                title = viewState.toolbarTitle,
                navigationIcon = if (context.isTwoPanesShouldBeUsed) {
                    null
                } else {
                    ImageVector.vectorResource(R.drawable.ic_back_24dp)
                },
                onNavigationButtonClick = onBack,
                modifier = Modifier.shadow(4.dp)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier.padding(top = innerPadding.calculateTopPadding())
        ) {
            when (viewState) {
                BookingDetailsViewState.Empty -> BookingDetailsEmptyScreen()
                is BookingDetailsViewState.ShowBooking -> {
                    val state = rememberPullToRefreshState()
                    WCPullToRefreshBox(
                        isRefreshing = viewState.loadingState == BookingDetailsLoadingState.Refreshing,
                        onRefresh = viewState.onRefresh,
                        state = state,
                        modifier = Modifier
                            .fillMaxSize()
                            .detailsPanePadding(),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        ) {
                            when {
                                viewState.shouldShowSkeleton -> BookingDetailsLoading()
                                viewState.bookingUiState != null -> {
                                    BookingDetailsContent(
                                        booking = viewState.bookingUiState,
                                        onCancelBooking = viewState.bookingUiState.onCancelBooking,
                                        onRescheduleBooking = viewState.bookingUiState.onRescheduleBooking,
                                    )
                                }
                            }
                        }
                    }
                    viewState.dialogState?.Render()
                }
            }
        }
    }
}

@Composable
private fun BookingDetailsContent(
    booking: BookingUiState,
    onCancelBooking: () -> Unit,
    onRescheduleBooking: () -> Unit,
) {
    BookingSummary(
        model = booking.bookingSummary,
        modifier = Modifier
            .fillMaxWidth()
            .background(color = MaterialTheme.colorScheme.surfaceContainer)
    )
    BookingAppointmentDetails(
        model = booking.bookingsAppointmentDetails,
        onCancelBooking = onCancelBooking,
        onAttendanceToggle = booking.onAttendanceToggle,
        onRescheduleBooking = onRescheduleBooking,
        modifier = Modifier.fillMaxWidth()
    )
    BookingCustomerDetails(
        model = booking.bookingCustomerDetails,
        modifier = Modifier.fillMaxWidth()
    )
    booking.bookingPaymentDetails?.let {
        BookingPaymentSection(
            model = it,
            onViewOrder = booking.onViewOrderClicked,
            onIssueRefund = booking.onIssueRefundClicked,
            modifier = Modifier.fillMaxWidth(),
        )
    }
    BookingNoteSection(
        note = booking.note,
        onClick = booking.onNoteClicked,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun BookingDetailsLoading() {
    Column {
        BookingSummaryLoading()
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

@Composable
fun BookingDetailsEmptyScreen(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_woo_generic_error),
                contentDescription = null
            )
            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_200)))
            Text(
                text = stringResource(R.string.booking_not_selected),
                style = MaterialTheme.typography.titleLarge
            )
        }
    }
}

@LightDarkThemePreviews
@Composable
private fun BookingDetailsPreview() {
    WooThemeWithBackground {
        BookingDetailsScreen(
            viewState = BookingDetailsViewState.ShowBooking(
                toolbarTitle = "Booking #12345",
                bookingUiState = BookingUiState(
                    orderId = 1L,
                    bookingSummary = BookingSummaryModel(
                        date = "05/07/2025, 11:00 AM",
                        name = "Women’s Haircut",
                        customerName = "Margarita Nikolaevna",
                        attendanceStatus = BookingAttendanceStatus.Attended,
                        paymentStatus = PaymentStatus.PAID,
                        isCancelled = false,
                        attendanceUpdateStatus = AttendanceUpdateStatus.Idle,
                    ),
                    bookingsAppointmentDetails = BookingAppointmentDetailsModel(
                        date = "Monday, 05 July 2025",
                        time = "11:00 am - 12:00 pm",
                        staff = BookingStaffMemberStatus.Loaded("Marianne Renoir"),
                        location = BookingLocationStatus.Loaded("238 Willow Creek Drive, Montgomery AL 36109"),
                        duration = "60 min",
                        cancelButtonVisible = true,
                        cancelStatus = CancelStatus.Idle,
                    ),
                    bookingCustomerDetails = BookingCustomerDetailsUiModel(
                        name = "Margarita Nikolaevna",
                        email = "margarita@example.com",
                        phone = "+1 555-123-4567",
                        billingAddress = """
                            238 Willow Creek Drive
                            Montgomery AL 36109
                            United States
                        """.trimIndent(),
                        customerNote = "Customer note"
                    ),
                    bookingPaymentDetails = BookingPaymentDetailsModel(
                        service = "$55.00",
                        tax = "$4.50",
                        discount = "-",
                        total = "$59.50"
                    ),
                    note = "",
                    isAttendanceStatusEditable = true
                ),
            ),
            onBack = {},
        )
    }
}

@LightDarkThemePreviews
@Composable
private fun BookingDetailsLoadingPreview() {
    WooThemeWithBackground {
        BookingDetailsScreen(
            viewState = BookingDetailsViewState.ShowBooking(
                toolbarTitle = "",
                bookingUiState = null,
                loadingState = BookingDetailsLoadingState.Refreshing,
            ),
            onBack = {},
        )
    }
}

@LightDarkThemePreviews
@Composable
private fun BookingDetailsEmptyPreview() {
    WooThemeWithBackground {
        BookingDetailsScreen(
            viewState = BookingDetailsViewState.Empty,
            onBack = {},
        )
    }
}
