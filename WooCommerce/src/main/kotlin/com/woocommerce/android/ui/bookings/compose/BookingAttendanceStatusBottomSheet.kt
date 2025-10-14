package com.woocommerce.android.ui.bookings.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.WCModalBottomSheet
import com.woocommerce.android.ui.compose.preview.LightDarkThemePreviews
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingAttendanceStatusBottomSheet(
    onSelect: (BookingAttendanceStatus) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    WCModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        BookingAttendanceStatusSelection(
            onSelect = { status ->
                scope.launch { sheetState.hide() }
                onSelect(status)
                onDismiss()
            },
            modifier = modifier
        )
    }
}

@Composable
private fun BookingAttendanceStatusSelection(
    onSelect: (BookingAttendanceStatus) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
    ) {
        Text(
            text = stringResource(R.string.booking_attendance_update_title),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 22.dp)
        )
        listOf(
            BookingAttendanceStatus.BOOKED,
            BookingAttendanceStatus.CHECKED_IN,
            BookingAttendanceStatus.NO_SHOW,
        ).forEachIndexed { index, status ->
            AttendanceStatusRow(
                status = status,
                onClick = { onSelect(status) }
            )
        }
    }
}

@Composable
private fun AttendanceStatusRow(
    status: BookingAttendanceStatus,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        status.iconRes?.let { iconRes ->
            Icon(
                painter = painterResource(iconRes),
                contentDescription = status.text(),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = status.text(),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.size(2.dp))
            Text(
                text = status.description(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BookingAttendanceStatus.description(): String = when (this) {
    BookingAttendanceStatus.BOOKED -> R.string.booking_attendance_status_booked_desc
    BookingAttendanceStatus.CHECKED_IN -> R.string.booking_attendance_status_checked_in_desc
    BookingAttendanceStatus.NO_SHOW -> R.string.booking_attendance_status_no_show_desc
    else -> null
}.let { it?.let { id -> stringResource(id) } ?: "" }

private val BookingAttendanceStatus.iconRes: Int?
    get() = when (this) {
        BookingAttendanceStatus.BOOKED -> R.drawable.ic_attendance_booked
        BookingAttendanceStatus.CHECKED_IN -> R.drawable.ic_attendance_checked_in
        BookingAttendanceStatus.NO_SHOW -> R.drawable.ic_attendance_no_show
        else -> null
    }

@LightDarkThemePreviews
@Composable
private fun BookingAttendanceStatusBottomSheetPreview() {
    WooThemeWithBackground {
        BookingAttendanceStatusSelection(
            onSelect = {},
        )
    }
}

@LightDarkThemePreviews
@Composable
private fun AttendanceStatusRowPreview() {
    WooThemeWithBackground {
        AttendanceStatusRow(
            status = BookingAttendanceStatus.CHECKED_IN,
            onClick = {}
        )
    }
}
