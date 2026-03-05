package com.woocommerce.android.ui.bookings.compose

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.bookings.PaymentStatus
import com.woocommerce.android.ui.bookings.details.AttendanceUpdateStatus
import com.woocommerce.android.ui.compose.animations.SkeletonView
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground

@Composable
fun BookingSummary(
    model: BookingSummaryModel,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = model.date,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
        )
        Text(
            text = model.name,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            text = model.customerName ?: stringResource(R.string.orderdetail_customer_name_default),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 6.dp)
        ) {
            if (model.isCancelled) {
                BookingCancelledTag()
            }
            model.attendanceStatus?.let {
                BookingAttendanceStatusTag(
                    state = it,
                    attendanceUpdateStatus = model.attendanceUpdateStatus,
                )
            }
            BookingPaymentStatusTag(paymentStatus = model.paymentStatus)
        }
    }
}

@Composable
fun BookingSummaryLoading() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        SkeletonView(Modifier.size(173.dp, 18.dp))
        SkeletonView(Modifier.size(131.dp, 18.dp))
        SkeletonView(Modifier.size(161.dp, 18.dp))
        Spacer(Modifier.height(6.dp))
        SkeletonView(Modifier.size(138.dp, 22.dp))
    }
}

data class BookingSummaryModel(
    val date: String,
    val name: String,
    val customerName: String?,
    val attendanceStatus: BookingAttendanceStatus?,
    val paymentStatus: PaymentStatus,
    val isCancelled: Boolean,
    val attendanceUpdateStatus: AttendanceUpdateStatus,
)

@Preview
@Composable
private fun BookingSummaryPreview() {
    WooThemeWithBackground {
        BookingSummary(
            model = BookingSummaryModel(
                date = "05/07/2025, 11:00 AM",
                name = "Women's Haircut",
                customerName = "Margarita Nikolaevna",
                attendanceStatus = BookingAttendanceStatus.Attended,
                paymentStatus = PaymentStatus.PAID,
                isCancelled = false,
                attendanceUpdateStatus = AttendanceUpdateStatus.Idle,
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun BookingSummaryDarkPreview() {
    WooThemeWithBackground {
        BookingSummary(
            model = BookingSummaryModel(
                date = "05/07/2025, 11:00 AM",
                name = "Women's Haircut",
                customerName = "Margarita Nikolaevna",
                attendanceStatus = BookingAttendanceStatus.Unattended,
                paymentStatus = PaymentStatus.UNPAID,
                isCancelled = false,
                attendanceUpdateStatus = AttendanceUpdateStatus.Idle,
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Preview
@Composable
private fun BookingSummaryAttendanceUpdatingPreview() {
    WooThemeWithBackground {
        BookingSummary(
            model = BookingSummaryModel(
                date = "05/07/2025, 11:00 AM",
                name = "Women's Haircut",
                customerName = "Margarita Nikolaevna",
                attendanceStatus = BookingAttendanceStatus.Attended,
                paymentStatus = PaymentStatus.PAID,
                isCancelled = false,
                attendanceUpdateStatus = AttendanceUpdateStatus.InProgress,
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Preview
@Composable
private fun BookingSummaryCancelledPreview() {
    WooThemeWithBackground {
        BookingSummary(
            model = BookingSummaryModel(
                date = "05/07/2025, 11:00 AM",
                name = "Women's Haircut",
                customerName = "Margarita Nikolaevna",
                attendanceStatus = BookingAttendanceStatus.Unattended,
                paymentStatus = PaymentStatus.UNPAID,
                isCancelled = true,
                attendanceUpdateStatus = AttendanceUpdateStatus.Idle,
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
