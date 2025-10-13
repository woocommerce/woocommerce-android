package com.woocommerce.android.ui.bookings.details

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.bookings.compose.BookingAppointmentDetails
import com.woocommerce.android.ui.bookings.compose.BookingAppointmentDetailsModel
import com.woocommerce.android.ui.bookings.compose.BookingAttendanceSection
import com.woocommerce.android.ui.bookings.compose.BookingAttendanceStatus
import com.woocommerce.android.ui.bookings.compose.BookingAttendanceStatusBottomSheet
import com.woocommerce.android.ui.bookings.compose.BookingCustomerDetails
import com.woocommerce.android.ui.bookings.compose.BookingCustomerDetailsModel
import com.woocommerce.android.ui.bookings.compose.BookingPaymentDetailsModel
import com.woocommerce.android.ui.bookings.compose.BookingPaymentSection
import com.woocommerce.android.ui.bookings.compose.BookingStatus
import com.woocommerce.android.ui.bookings.compose.BookingSummary
import com.woocommerce.android.ui.bookings.compose.BookingSummaryModel
import com.woocommerce.android.ui.compose.component.Toolbar
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
        Surface(
            color = colorResource(R.color.default_window_background),
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(innerPadding)
            ) {
                viewState.bookingUiState?.let {
                    BookingSummary(
                        model = viewState.bookingUiState.bookingSummary,
                        modifier = Modifier.fillMaxWidth()
                    )
                    BookingAppointmentDetails(
                        model = viewState.bookingUiState.bookingsAppointmentDetails,
                        onCancelBooking = viewState.onCancelBooking,
                        modifier = Modifier.fillMaxWidth()
                    )
                    BookingCustomerDetails(
                        model = viewState.bookingUiState.bookingCustomerDetails,
                        modifier = Modifier.fillMaxWidth()
                    )
                    BookingAttendanceSection(
                        status = viewState.bookingUiState.bookingSummary.attendanceStatus,
                        onClick = { showAttendanceSheet.value = true },
                        modifier = Modifier.fillMaxWidth()
                    )
                    BookingPaymentSection(
                        model = viewState.bookingUiState.bookingPaymentDetails,
                        status = viewState.bookingUiState.bookingSummary.status,
                        onMarkAsPaid = { onViewOrder(viewState.orderId) },
                        onViewOrder = { onViewOrder(viewState.orderId) },
                        onMarkAsRefunded = { onViewOrder(viewState.orderId) },
                        modifier = Modifier.fillMaxWidth()
                    )
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
    }
}

@Preview(showBackground = true)
@Composable
private fun BookingDetailsPreview() {
    WooThemeWithBackground {
        BookingDetailsScreen(
            viewState = BookingDetailsViewState(
                toolbarTitle = "Booking #12345",
                bookingUiState = BookingUiState(
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
                        staff = "Marianne Renoir",
                        location = "238 Willow Creek Drive, Montgomery AL 36109",
                        duration = "60 min",
                        price = "$55.00"
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
