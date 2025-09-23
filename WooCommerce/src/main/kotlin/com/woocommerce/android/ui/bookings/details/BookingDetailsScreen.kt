package com.woocommerce.android.ui.bookings.details

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.bookings.compose.AttendanceStatus
import com.woocommerce.android.ui.bookings.compose.BookingAppointmentDetails
import com.woocommerce.android.ui.bookings.compose.BookingAppointmentDetailsModel
import com.woocommerce.android.ui.bookings.compose.BookingPaymentStatus
import com.woocommerce.android.ui.bookings.compose.BookingSummary
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingDetailsScreen(
    viewModel: BookingDetailsViewModel,
    onBack: () -> Unit
) {
    val viewState by viewModel.state.observeAsState()

    viewState?.let {
        BookingDetailsScreen(
            viewState = it,
            onBack = onBack,
            onCancelBooking = viewModel::onCancelBooking
        )
    }
}

@Composable
fun BookingDetailsScreen(
    viewState: BookingDetailsViewState,
    onBack: () -> Unit,
    onCancelBooking: () -> Unit
) {
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
                modifier = Modifier.padding(innerPadding)
            ) {
                BookingSummary(
                    model = viewState.bookingSummary,
                    modifier = Modifier.fillMaxWidth()
                )
                BookingAppointmentDetails(
                    model = viewState.bookingsAppointmentDetails,
                    onCancelBooking = onCancelBooking,
                    modifier = Modifier.fillMaxWidth()
                )
            }
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
                bookingSummary = BookingSummary(
                    date = "05/07/2025, 11:00 AM",
                    name = "Women’s Haircut",
                    customerName = "Margarita Nikolaevna",
                    attendanceStatus = AttendanceStatus.CHECKED_IN,
                    paymentStatus = BookingPaymentStatus.PAID
                ),
                bookingsAppointmentDetails = BookingAppointmentDetailsModel(
                    date = "Monday, 05 July 2025",
                    time = "11:00 am - 12:00 pm",
                    staff = "Marianne Renoir",
                    location = "238 Willow Creek Drive, Montgomery AL 36109",
                    duration = "60 min",
                    price = "$55.00"
                )
            ),
            onBack = {},
            onCancelBooking = {}
        )
    }
}
