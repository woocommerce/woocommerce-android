package com.woocommerce.android.ui.woopos.bookings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButtonSmall
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosOutlinedButtonSmall
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography

private val selectableAttendanceStatuses = listOf(
    AttendanceStatusUi.Booked,
    AttendanceStatusUi.CheckedIn,
    AttendanceStatusUi.NoShow,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WooPosBookingDetailPane(
    detail: BookingDetail,
    onAttendanceStatusSelected: (AttendanceStatusUi) -> Unit,
    onCancelBookingClicked: () -> Unit,
    onPayByCardClicked: () -> Unit,
    onPayByCashClicked: () -> Unit,
    onViewOrderClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(
                start = WooPosSpacing.Medium.value,
                end = WooPosSpacing.Medium.value,
                bottom = WooPosSpacing.XLarge.value
            )
    ) {
        @Suppress("WooPosDesignSystemSpacingUsageRule")
        Spacer(modifier = Modifier.height(56.dp))

        Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))

        BookingSummarySection(detail)

        Spacer(modifier = Modifier.height(WooPosSpacing.Large.value))

        StatusSection(detail)

        if (detail.isAttendanceEditable) {
            Spacer(modifier = Modifier.height(WooPosSpacing.Large.value))

            AttendanceSection(
                currentStatus = detail.attendanceStatus,
                isLoading = detail.attendanceUpdateInProgress,
                onStatusSelected = onAttendanceStatusSelected,
            )
        }

        Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        if (detail.isPayable) {
            Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))

            PaymentSection(
                onPayByCardClicked = onPayByCardClicked,
                onPayByCashClicked = onPayByCashClicked,
            )
        }

        if (detail.hasLinkedOrder) {
            Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))

            WooPosOutlinedButtonSmall(
                text = stringResource(R.string.woopos_bookings_detail_view_order),
                onClick = onViewOrderClicked,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (detail.isCancellable) {
            Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))

            CancelSection(
                isLoading = detail.cancelInProgress,
                onClick = onCancelBookingClicked,
            )
        }
    }
}

@Composable
private fun BookingSummarySection(detail: BookingDetail) {
    Column {
        WooPosText(
            text = "#${detail.id}",
            style = WooPosTypography.BodyMedium,
            color = WooPosTheme.colors.onSurfaceVariantHighest,
        )

        Spacer(modifier = Modifier.height(WooPosSpacing.Small.value))

        WooPosText(
            text = detail.customerName,
            style = WooPosTypography.Heading,
            fontWeight = FontWeight.Bold,
        )

        Spacer(modifier = Modifier.height(WooPosSpacing.Small.value))

        WooPosText(
            text = detail.serviceName,
            style = WooPosTypography.BodyLarge,
            color = WooPosTheme.colors.onSurfaceVariantHighest,
        )

        Spacer(modifier = Modifier.height(WooPosSpacing.Small.value))

        WooPosText(
            text = "${detail.startDate} \u2022 ${detail.startTime} - ${detail.endTime}",
            style = WooPosTypography.BodyMedium,
            color = WooPosTheme.colors.onSurfaceVariantHighest,
        )

        Spacer(modifier = Modifier.height(WooPosSpacing.Small.value))

        WooPosText(
            text = detail.amount,
            style = WooPosTypography.BodyLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun StatusSection(detail: BookingDetail) {
    Row(horizontalArrangement = Arrangement.spacedBy(WooPosSpacing.Small.value)) {
        BookingStatusBadge(detail.bookingStatus)
        detail.attendanceStatus?.let { AttendanceBadge(it) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AttendanceSection(
    currentStatus: AttendanceStatusUi?,
    isLoading: Boolean,
    onStatusSelected: (AttendanceStatusUi) -> Unit,
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(WooPosSpacing.Small.value)
        ) {
            WooPosText(
                text = stringResource(R.string.woopos_bookings_detail_attendance),
                style = WooPosTypography.BodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                )
            }
        }

        Spacer(modifier = Modifier.height(WooPosSpacing.Small.value))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(WooPosSpacing.Small.value),
            verticalArrangement = Arrangement.spacedBy(WooPosSpacing.Small.value),
        ) {
            selectableAttendanceStatuses.forEach { status ->
                FilterChip(
                    selected = status == currentStatus,
                    onClick = { if (!isLoading) onStatusSelected(status) },
                    enabled = !isLoading,
                    label = {
                        WooPosText(
                            text = status.label,
                            style = WooPosTypography.Caption,
                            fontWeight = if (status == currentStatus) {
                                FontWeight.Bold
                            } else {
                                FontWeight.Normal
                            },
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                        labelColor = MaterialTheme.colorScheme.onSurface,
                    ),
                )
            }
        }
    }
}

@Composable
private fun PaymentSection(
    onPayByCardClicked: () -> Unit,
    onPayByCashClicked: () -> Unit,
) {
    Column {
        WooPosText(
            text = stringResource(R.string.woopos_bookings_detail_payment),
            style = WooPosTypography.BodyMedium,
            fontWeight = FontWeight.SemiBold,
        )

        Spacer(modifier = Modifier.height(WooPosSpacing.Small.value))

        WooPosButtonSmall(
            text = stringResource(R.string.woopos_bookings_detail_pay_by_card),
            onClick = onPayByCardClicked,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(WooPosSpacing.Small.value))

        WooPosOutlinedButtonSmall(
            text = stringResource(R.string.woopos_bookings_detail_pay_by_cash),
            onClick = onPayByCashClicked,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun CancelSection(
    isLoading: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(WooPosSpacing.Small.value))
            .clickable(enabled = !isLoading) { onClick() }
            .padding(vertical = WooPosSpacing.Medium.value),
        contentAlignment = Alignment.Center,
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
            )
        } else {
            WooPosText(
                text = stringResource(R.string.woopos_bookings_detail_cancel),
                style = WooPosTypography.BodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun AttendanceBadge(status: AttendanceStatusUi) {
    val bgColor = when (status) {
        AttendanceStatusUi.CheckedIn -> WooPosTheme.colors.infoLowest
        AttendanceStatusUi.NoShow,
        AttendanceStatusUi.Cancelled -> WooPosTheme.colors.errorLowest
        AttendanceStatusUi.Booked -> WooPosTheme.colors.default
    }
    val textColor = when (status) {
        AttendanceStatusUi.CheckedIn -> WooPosTheme.colors.onInfoLowest
        AttendanceStatusUi.NoShow,
        AttendanceStatusUi.Cancelled -> WooPosTheme.colors.onErrorLowest
        AttendanceStatusUi.Booked -> WooPosTheme.colors.onDefault
    }

    WooPosText(
        text = status.label,
        style = WooPosTypography.Caption,
        color = textColor,
        maxLines = 1,
        modifier = Modifier
            .clip(RoundedCornerShape(WooPosSpacing.Small.value))
            .background(bgColor)
            .padding(
                horizontal = WooPosSpacing.Small.value,
                vertical = WooPosSpacing.XSmall.value,
            )
    )
}
