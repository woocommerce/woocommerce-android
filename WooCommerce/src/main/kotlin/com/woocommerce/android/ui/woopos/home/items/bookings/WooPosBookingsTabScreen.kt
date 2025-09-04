package com.woocommerce.android.ui.woopos.home.items.bookings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.woocommerce.android.ui.woopos.bookings.BookingStatus
import com.woocommerce.android.ui.woopos.bookings.WooPosBooking
import com.woocommerce.android.ui.woopos.bookings.WooPosBookingsViewModel
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosCard
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosElevation
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.toAdaptivePadding
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WooPosBookingsTabScreen(
    modifier: Modifier = Modifier,
    viewModel: WooPosBookingsViewModel = hiltViewModel()
) {
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var selectedBooking by remember { mutableStateOf<WooPosBooking?>(null) }
    var showBookingDetails by remember { mutableStateOf(false) }

    val bookings = remember { generateSampleBookings() }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        CalendarView(
            selectedDate = selectedDate,
            onDateSelected = { selectedDate = it },
            bookings = bookings
        )
        
        BookingsListForDate(
            date = selectedDate,
            bookings = bookings.filter { 
                it.startTime.toLocalDate() == selectedDate 
            },
            onBookingClick = {
                selectedBooking = it
                showBookingDetails = true
            }
        )
    }

    if (showBookingDetails && selectedBooking != null) {
        BookingDetailsDialog(
            booking = selectedBooking!!,
            onDismiss = { 
                showBookingDetails = false
                selectedBooking = null
            },
            onConfirmBooking = {
                showBookingDetails = false
            },
            onAddToCart = {
                viewModel.onBookingPaymentClick(selectedBooking!!)
                showBookingDetails = false
            }
        )
    }
}

@Composable
private fun CalendarView(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    bookings: List<WooPosBooking>
) {
    var currentMonth by remember { mutableStateOf(selectedDate.withDayOfMonth(1)) }
    
    WooPosCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = WooPosSpacing.Medium.value.toAdaptivePadding()),
        elevation = WooPosElevation.Medium,
        backgroundColor = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(WooPosSpacing.Medium.value.toAdaptivePadding())) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Previous month")
                }
                
                Text(
                    text = currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                
                IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Next month")
                }
            }
            
            Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value.toAdaptivePadding()))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEach { day ->
                    Text(
                        text = day,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(WooPosSpacing.Small.value.toAdaptivePadding()))
            
            val daysInMonth = currentMonth.lengthOfMonth()
            val firstDayOfWeek = currentMonth.dayOfWeek.value % 7
            val totalCells = firstDayOfWeek + daysInMonth
            val weeks = (totalCells + 6) / 7
            
            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                modifier = Modifier.height((weeks * 48).dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                items((0 until weeks * 7).toList()) { index ->
                    val dayOfMonth = index - firstDayOfWeek + 1
                    if (dayOfMonth in 1..daysInMonth) {
                        val date = currentMonth.withDayOfMonth(dayOfMonth)
                        val hasBookings = bookings.any { it.startTime.toLocalDate() == date }
                        DayCell(
                            day = dayOfMonth,
                            isSelected = date == selectedDate,
                            hasBookings = hasBookings,
                            isToday = date == LocalDate.now(),
                            onClick = { onDateSelected(date) }
                        )
                    } else {
                        Box(modifier = Modifier.size(48.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    day: Int,
    isSelected: Boolean,
    hasBookings: Boolean,
    isToday: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .padding(WooPosSpacing.XSmall.value.toAdaptivePadding())
            .clip(CircleShape)
            .background(
                when {
                    isSelected -> MaterialTheme.colorScheme.primary
                    isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    else -> Color.Transparent
                }
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = day.toString(),
                color = when {
                    isSelected -> MaterialTheme.colorScheme.onPrimary
                    isToday -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurface
                },
                fontSize = 14.sp,
                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal
            )
            if (hasBookings) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.onPrimary 
                            else MaterialTheme.colorScheme.primary,
                            CircleShape
                        )
                )
            }
        }
    }
}

@Composable
private fun BookingsListForDate(
    date: LocalDate,
    bookings: List<WooPosBooking>,
    onBookingClick: (WooPosBooking) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Bookings for ${date.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = WooPosSpacing.Small.value.toAdaptivePadding())
        )
        
        if (bookings.isEmpty()) {
            WooPosCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
            ) {
                Text(
                    text = "No bookings for this date",
                    modifier = Modifier
                        .padding(WooPosSpacing.Large.value.toAdaptivePadding())
                        .fillMaxWidth(),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            bookings.forEach { booking ->
                BookingCard(booking = booking, onClick = { onBookingClick(booking) })
            }
        }
    }
}

@Composable
private fun BookingCard(
    booking: WooPosBooking,
    onClick: () -> Unit
) {
    WooPosCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = WooPosSpacing.Small.value.toAdaptivePadding())
            .clickable(onClick = onClick),
        elevation = WooPosElevation.Medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(WooPosSpacing.Medium.value.toAdaptivePadding()),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = booking.customerName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = booking.serviceName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Text(
                    text = "${booking.startTime.format(DateTimeFormatter.ofPattern("h:mm a"))} - ${booking.endTime.format(DateTimeFormatter.ofPattern("h:mm a"))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            
            Column(horizontalAlignment = Alignment.End) {
                StatusChip(status = booking.status)
                Spacer(modifier = Modifier.height(WooPosSpacing.XSmall.value.toAdaptivePadding()))
                Text(
                    text = "$${booking.price}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun StatusChip(status: BookingStatus) {
    val backgroundColor = when (status) {
        BookingStatus.CONFIRMED -> Color(0xFF4CAF50)
        BookingStatus.PENDING -> Color(0xFFFFA726)
        BookingStatus.COMPLETED -> Color(0xFF2196F3)
        BookingStatus.CANCELLED -> Color(0xFFE91E63)
    }
    
    Surface(
        color = backgroundColor.copy(alpha = 0.2f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = status.name.lowercase().replaceFirstChar { it.uppercase() },
            modifier = Modifier.padding(
                horizontal = WooPosSpacing.Small.value.toAdaptivePadding(), 
                vertical = WooPosSpacing.XSmall.value.toAdaptivePadding()
            ),
            color = backgroundColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookingDetailsDialog(
    booking: WooPosBooking,
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
                    Text(
                        text = "Booking Details",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = WooPosSpacing.Medium.value.toAdaptivePadding())
                    )
                    
                    DetailRow("Customer", booking.customerName)
                    DetailRow("Service", booking.serviceName)
                    DetailRow("Date", booking.startTime.format(DateTimeFormatter.ofPattern("MMMM d, yyyy")))
                    DetailRow("Time", "${booking.startTime.format(DateTimeFormatter.ofPattern("h:mm a"))} - ${booking.endTime.format(DateTimeFormatter.ofPattern("h:mm a"))}")
                    DetailRow("Status", booking.status.name.lowercase().replaceFirstChar { it.uppercase() })
                    DetailRow("Amount", "$${booking.price}")
                    
                    Spacer(modifier = Modifier.height(WooPosSpacing.Large.value.toAdaptivePadding()))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(WooPosSpacing.Small.value.toAdaptivePadding())
                    ) {
                        if (booking.status == BookingStatus.PENDING) {
                            Button(
                                onClick = onConfirmBooking,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF4CAF50)
                                )
                            ) {
                                Text("Confirm", modifier = Modifier.padding(vertical = WooPosSpacing.Small.value.toAdaptivePadding()))
                            }
                        }
                        
                        if (!booking.isPaid) {
                            Button(
                                onClick = onAddToCart,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text("Add to Cart", modifier = Modifier.padding(vertical = WooPosSpacing.Small.value.toAdaptivePadding()))
                            }
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
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun generateSampleBookings(): List<WooPosBooking> {
    val today = LocalDate.now()
    return listOf(
        WooPosBooking(
            id = 1,
            customerName = "John Smith",
            serviceName = "Hair Cut & Style",
            startTime = today.atTime(9, 0),
            endTime = today.atTime(10, 0),
            status = BookingStatus.CONFIRMED,
            price = BigDecimal("45.00"),
            isPaid = false
        ),
        WooPosBooking(
            id = 2,
            customerName = "Emma Wilson",
            serviceName = "Color Treatment",
            startTime = today.atTime(10, 30),
            endTime = today.atTime(12, 0),
            status = BookingStatus.CONFIRMED,
            price = BigDecimal("120.00"),
            isPaid = true
        ),
        WooPosBooking(
            id = 3,
            customerName = "Michael Brown",
            serviceName = "Beard Trim",
            startTime = today.atTime(14, 0),
            endTime = today.atTime(14, 30),
            status = BookingStatus.PENDING,
            price = BigDecimal("25.00"),
            isPaid = false
        ),
        WooPosBooking(
            id = 4,
            customerName = "Sarah Davis",
            serviceName = "Full Service Package",
            startTime = today.plusDays(1).atTime(11, 0),
            endTime = today.plusDays(1).atTime(13, 0),
            status = BookingStatus.CONFIRMED,
            price = BigDecimal("180.00"),
            isPaid = false
        ),
        WooPosBooking(
            id = 5,
            customerName = "Tom Johnson",
            serviceName = "Hair Cut",
            startTime = today.plusDays(2).atTime(15, 0),
            endTime = today.plusDays(2).atTime(15, 45),
            status = BookingStatus.CONFIRMED,
            price = BigDecimal("35.00"),
            isPaid = false
        )
    )
}

@WooPosPreview
@Composable
private fun PreviewWooPosBookingsTabScreen() {
    WooPosTheme {
        WooPosBookingsTabScreen()
    }
}