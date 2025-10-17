package com.woocommerce.android.ui.bookings.compose

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground

@Composable
fun BookingSummary(
    model: BookingSummaryModel,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.Start,
        modifier = modifier
            .background(color = MaterialTheme.colorScheme.surfaceContainer)
            .padding(16.dp)
    ) {
        Text(
            text = model.date,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
        )
        FlowRow(
            modifier = Modifier.padding(top = 2.dp),
        ) {
            Text(
                text = "${model.name} • ",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp),
            )
            Text(
                text = model.customerName ?: stringResource(R.string.orderdetail_customer_name_default),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp),
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .padding(top = 8.dp)
        ) {
            BookingAttendanceStatusTag(
                state = model.attendanceStatus
            )
            BookingStatusTag(
                state = model.status
            )
        }
    }
}

data class BookingSummaryModel(
    val date: String,
    val name: String,
    val customerName: String?,
    val attendanceStatus: BookingAttendanceStatus,
    val status: BookingStatus,
)

@Preview
@Composable
private fun BookingSummaryPreview() {
    WooThemeWithBackground {
        BookingSummary(
            model = BookingSummaryModel(
                date = "05/07/2025, 11:00 AM",
                name = "Women’s Haircut",
                customerName = "Margarita Nikolaevna",
                attendanceStatus = BookingAttendanceStatus.CheckedIn,
                status = BookingStatus.Paid
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
                name = "Women’s Haircut",
                customerName = "Margarita Nikolaevna",
                attendanceStatus = BookingAttendanceStatus.Booked,
                status = BookingStatus.PendingConfirmation
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
