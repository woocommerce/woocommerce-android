package com.woocommerce.android.ui.woopos.home.items.bookings

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.woocommerce.android.ui.woopos.bookings.WooPosBooking
import com.woocommerce.android.ui.woopos.bookings.WooPosBookingsViewModel
import com.woocommerce.android.ui.woopos.bookings.data.BookingSlot
import com.woocommerce.android.ui.woopos.bookings.data.WooPosBookingsDataSource
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButtonState
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosCard
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosOutlinedButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosCornerRadius
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosElevation
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.toAdaptivePadding
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WooPosBookingsTabScreen(
    modifier: Modifier = Modifier,
    viewModel: WooPosBookingsViewModel = hiltViewModel()
) {
    val selectedWeekStart by viewModel.selectedWeekStart.collectAsState()
    var selectedBooking by remember { mutableStateOf<WooPosBooking?>(null) }
    var showBookingDetails by remember { mutableStateOf(false) }
    var isConfirmingBooking by remember { mutableStateOf(false) }
    var confirmationMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Log.d(TAG, "WooPosBookingsTabScreen: Recomposing with selectedWeekStart=$selectedWeekStart")

    val bookingsResult by viewModel.bookingsForWeek.collectAsState(
        initial = WooPosBookingsDataSource.BookingsResult.Loading
    )

    Log.d(TAG, "WooPosBookingsTabScreen: Current result state: ${bookingsResult::class.simpleName}")

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        WeekNavigationHeader(
            weekStart = selectedWeekStart,
            onPreviousWeek = { viewModel.onWeekChanged(selectedWeekStart.minusWeeks(1)) },
            onNextWeek = { viewModel.onWeekChanged(selectedWeekStart.plusWeeks(1)) }
        )

        when (val result = bookingsResult) {
            is WooPosBookingsDataSource.BookingsResult.Loading -> {
                LoadingView()
            }
            is WooPosBookingsDataSource.BookingsResult.Error -> {
                ErrorView(error = result.exception.message ?: "Unknown error")
            }
            is WooPosBookingsDataSource.BookingsResult.Success -> {
                WeekCalendarWithTimeSlots(
                    weekStart = selectedWeekStart,
                    bookings = result.bookings,
                    slots = result.slots,
                    onBookingClick = { booking ->
                        selectedBooking = booking
                        showBookingDetails = true
                    }
                )
            }
        }
    }

    if (showBookingDetails && selectedBooking != null) {
        BookingDetailsDialog(
            booking = selectedBooking!!,
            isConfirmingBooking = isConfirmingBooking,
            confirmationMessage = confirmationMessage,
            onDismiss = {
                showBookingDetails = false
                selectedBooking = null
                confirmationMessage = null
            },
            onConfirmBooking = {
                scope.launch {
                    isConfirmingBooking = true
                    confirmationMessage = null

                    val result = viewModel.confirmBooking(selectedBooking!!.id)
                    isConfirmingBooking = false

                    if (result.isSuccess) {
                        confirmationMessage = "Booking confirmed successfully!"
                        // Close dialog after a short delay to show success message
                        kotlinx.coroutines.delay(1500)
                        showBookingDetails = false
                        confirmationMessage = null
                    } else {
                        confirmationMessage = "Failed to confirm booking. Please try again."
                    }
                }
            },
            onAddToCart = {
                viewModel.onBookingPaymentClick(selectedBooking!!)
                showBookingDetails = false
            }
        )
    }
}

@Composable
private fun WeekNavigationHeader(
    weekStart: LocalDate,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit
) {
    WooPosCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = WooPosSpacing.Medium.value.toAdaptivePadding()),
        elevation = WooPosElevation.Medium,
        backgroundColor = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(WooPosSpacing.Medium.value.toAdaptivePadding()),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPreviousWeek) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Previous week")
            }

            val weekEnd = weekStart.plusDays(6)
            WooPosText(
                text = "${weekStart.format(DateTimeFormatter.ofPattern("MMM d"))} - ${weekEnd.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))}",
                style = WooPosTypography.BodyMedium,
                fontWeight = FontWeight.Bold
            )

            IconButton(onClick = onNextWeek) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Next week")
            }
        }
    }
}

@Composable
private fun WeekCalendarWithTimeSlots(
    weekStart: LocalDate,
    bookings: List<WooPosBooking>,
    slots: List<BookingSlot>,
    onBookingClick: (WooPosBooking) -> Unit
) {
    val timeSlots = generateTimeSlots()
    val weekDays = (0..6).map { weekStart.plusDays(it.toLong()) }

    WooPosCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = WooPosElevation.Medium,
        backgroundColor = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(WooPosSpacing.Medium.value.toAdaptivePadding())) {
            DayHeaderRow(weekDays)

            Spacer(modifier = Modifier.height(WooPosSpacing.Small.value.toAdaptivePadding()))

            LazyColumn(
                modifier = Modifier.padding(bottom = 80.dp) // Add bottom padding to avoid floating menu overlap
            ) {
                items(timeSlots) { timeSlot ->
                    TimeSlotRow(
                        timeSlot = timeSlot,
                        weekDays = weekDays,
                        bookings = bookings,
                        slots = slots,
                        onBookingClick = onBookingClick
                    )
                }
            }
        }
    }
}

@Composable
private fun DayHeaderRow(weekDays: List<LocalDate>) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.width(60.dp))

        weekDays.forEach { day ->
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    WooPosText(
                        text = day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                        style = WooPosTypography.Caption,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    WooPosText(
                        text = day.dayOfMonth.toString(),
                        style = WooPosTypography.BodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun TimeSlotRow(
    timeSlot: LocalTime,
    weekDays: List<LocalDate>,
    bookings: List<WooPosBooking>,
    slots: List<BookingSlot>,
    onBookingClick: (WooPosBooking) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
    ) {
        Box(
            modifier = Modifier
                .width(60.dp)
                .height(60.dp),
            contentAlignment = Alignment.Center
        ) {
            WooPosText(
                text = timeSlot.format(DateTimeFormatter.ofPattern("HH:mm")),
                style = WooPosTypography.Caption,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }

        weekDays.forEach { day ->
            Box(
                modifier = Modifier.weight(1f).height(60.dp),
                contentAlignment = Alignment.Center
            ) {
                val dayBookings = bookings.filter { booking ->
                    booking.startDateTime.toLocalDate() == day &&
                    booking.startDateTime.toLocalTime().hour == timeSlot.hour
                }

                if (dayBookings.isNotEmpty()) {
                    LazyRow {
                        items(dayBookings) { booking ->
                            BookingSlotCard(
                                booking = booking,
                                onClick = { onBookingClick(booking) }
                            )
                        }
                    }
                } else {
                    val daySlot = slots.find { slot ->
                        slot.date.startsWith(day.toString()) &&
                        slot.date.contains("${String.format("%02d", timeSlot.hour)}:${String.format("%02d", timeSlot.minute)}")
                    }

                    if (daySlot?.isAvailable == true) {
                        AvailableSlot()
                    }
                }
            }
        }
    }
}

@Composable
private fun BookingSlotCard(
    booking: WooPosBooking,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .padding(1.dp) // Minimal padding to fit in cell
            .size(width = 80.dp, height = 45.dp) // Constrained size to fit calendar cell
            .clickable(onClick = onClick),
        color = when (booking.bookingStatus) {
            com.woocommerce.android.ui.woopos.bookings.BookingStatus.CONFIRMED -> MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
            com.woocommerce.android.ui.woopos.bookings.BookingStatus.PENDING -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.9f)
            com.woocommerce.android.ui.woopos.bookings.BookingStatus.COMPLETED -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.9f)
            com.woocommerce.android.ui.woopos.bookings.BookingStatus.CANCELLED -> MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
        },
        shape = RoundedCornerShape(8.dp), // Smaller rounded corners for compact size
        shadowElevation = 1.dp // Smaller shadow
    ) {
        Box {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp), // Minimal internal padding
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                WooPosText(
                    text = "C#${booking.customerId}",
                    style = WooPosTypography.Caption,
                    color = Color.White,
                    maxLines = 1
                )
                WooPosText(
                    text = "$${booking.cost}",
                    style = WooPosTypography.Caption,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }

            // Status badge - smaller size for compact card
            BookingStatusBadge(
                status = booking.bookingStatus,
                modifier = Modifier.align(Alignment.TopEnd)
            )
        }
    }
}

@Composable
private fun AvailableSlot() {
    Box(
        modifier = Modifier
            .size(8.dp)
            .background(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                RoundedCornerShape(WooPosCornerRadius.Small.value)
            )
    )
}

@Composable
private fun LoadingView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorView(error: String) {
    WooPosCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
    ) {
        WooPosText(
            text = "Error loading bookings: $error",
            modifier = Modifier
                .padding(WooPosSpacing.Large.value.toAdaptivePadding())
                .fillMaxWidth(),
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
            style = WooPosTypography.BodyMedium
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookingDetailsDialog(
    booking: WooPosBooking,
    isConfirmingBooking: Boolean = false,
    confirmationMessage: String? = null,
    onDismiss: () -> Unit,
    onConfirmBooking: () -> Unit,
    onAddToCart: () -> Unit
) {
    AnimatedVisibility(
        visible = true,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            WooPosCard(
                modifier = Modifier
                    .padding(WooPosSpacing.Large.value.toAdaptivePadding())
                    .clickable(enabled = false) { },
                backgroundColor = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .padding(WooPosSpacing.Large.value.toAdaptivePadding())
                        .width(400.dp)
                ) {
                    WooPosText(
                        text = "Booking Details",
                        style = WooPosTypography.BodyLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = WooPosSpacing.Medium.value.toAdaptivePadding())
                    )

                    DetailRow("Customer", booking.customerName)
                    DetailRow("Service", booking.serviceName)
                    DetailRow("Date", booking.startDateTime.format(DateTimeFormatter.ofPattern("MMMM d, yyyy")))
                    DetailRow("Time", "${booking.startDateTime.format(DateTimeFormatter.ofPattern("h:mm a"))} - ${booking.endDateTime.format(DateTimeFormatter.ofPattern("h:mm a"))}")
                    DetailRow("Status", booking.status)
                    DetailRow("Amount", "$${booking.cost}")

                    // Show confirmation message or loading state
                    if (isConfirmingBooking || confirmationMessage != null) {
                        Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value.toAdaptivePadding()))

                        if (isConfirmingBooking) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                WooPosText(
                                    text = "Confirming booking...",
                                    style = WooPosTypography.BodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        } else if (confirmationMessage != null) {
                            WooPosText(
                                text = confirmationMessage,
                                style = WooPosTypography.BodySmall,
                                color = if (confirmationMessage.contains("success"))
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(WooPosSpacing.Large.value.toAdaptivePadding()))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(WooPosSpacing.Small.value.toAdaptivePadding())
                    ) {
                        // Show confirm button for bookings that need confirmation
                        val needsConfirmation = booking.status.equals("unpaid", ignoreCase = true) ||
                                               booking.status.equals("pending-confirmation", ignoreCase = true)

                        if (needsConfirmation) {
                            WooPosOutlinedButton(
                                text = if (isConfirmingBooking) "Confirming..." else "Confirm",
                                state = if (isConfirmingBooking) WooPosButtonState.LOADING else WooPosButtonState.ENABLED,
                                onClick = onConfirmBooking,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        if (!booking.isPaid) {
                            WooPosButton(
                                text = "Add to art",
                                state = WooPosButtonState.ENABLED,
                                onClick = onAddToCart,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = WooPosSpacing.Small.value.toAdaptivePadding()),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        WooPosText(
            text = label,
            style = WooPosTypography.BodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        WooPosText(
            text = value,
            style = WooPosTypography.BodySmall,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun generateTimeSlots(): List<LocalTime> {
    val slots = mutableListOf<LocalTime>()
    var time = LocalTime.of(8, 0)

    while (time.isBefore(LocalTime.of(20, 0))) {
        slots.add(time)
        time = time.plusHours(1)
    }

    return slots
}

@Composable
private fun BookingStatusBadge(
    status: com.woocommerce.android.ui.woopos.bookings.BookingStatus,
    modifier: Modifier = Modifier
) {
    val (text, backgroundColor) = when (status) {
        com.woocommerce.android.ui.woopos.bookings.BookingStatus.CONFIRMED -> "C" to Color(0xFF4CAF50)
        com.woocommerce.android.ui.woopos.bookings.BookingStatus.PENDING -> "P" to Color(0xFFFF9800)
        com.woocommerce.android.ui.woopos.bookings.BookingStatus.COMPLETED -> "✓" to Color(0xFF2196F3)
        com.woocommerce.android.ui.woopos.bookings.BookingStatus.CANCELLED -> "X" to Color(0xFFF44336)
    }

    Box(
        modifier = modifier
            .size(12.dp) // Smaller badge for compact card
            .background(
                backgroundColor,
                RoundedCornerShape(50) // Circular using rounded corners
            )
            .padding(1.dp),
        contentAlignment = Alignment.Center
    ) {
        WooPosText(
            text = text,
            style = WooPosTypography.Caption,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}

private const val TAG = "WooPosBookingsTabScreen"

@WooPosPreview
@Composable
private fun PreviewWooPosBookingsTabScreen() {
    WooPosTheme {
        WooPosBookingsTabScreen()
    }
}
