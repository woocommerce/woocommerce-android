package com.woocommerce.android.ui.woopos.bookings

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButtonSmall
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosOutlinedButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography

private val selectableAttendanceStatuses = listOf(
    AttendanceStatusUi.Attended,
    AttendanceStatusUi.Unattended,
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
        Row(
            modifier = Modifier.heightIn(min = 56.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            WooPosText(
                text = "#${detail.id}",
                style = WooPosTypography.Heading,
                fontWeight = FontWeight.Bold,
            )

            Spacer(Modifier.weight(1f))

            BookingActions(
                actions = detail.actions,
                cancelInProgress = detail.cancelInProgress,
                onViewOrderClicked = onViewOrderClicked,
                onCancelBookingClicked = onCancelBookingClicked,
            )
        }

        Spacer(modifier = Modifier.height(WooPosSpacing.Small.value))

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

        if (detail.isPayable) {
            Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))

            PaymentSection(
                onPayByCardClicked = onPayByCardClicked,
                onPayByCashClicked = onPayByCashClicked,
            )
        }
    }
}

@Composable
private fun BookingActions(
    actions: List<BookingAction>,
    cancelInProgress: Boolean,
    onViewOrderClicked: () -> Unit,
    onCancelBookingClicked: () -> Unit,
) {
    if (actions.isEmpty()) return

    val onActionClicked: (BookingAction) -> Unit = { action ->
        when (action) {
            BookingAction.ViewOrder -> onViewOrderClicked()
            BookingAction.CancelBooking -> onCancelBookingClicked()
        }
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        if (cancelInProgress) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
            )
        } else {
            when {
                actions.size > 1 -> {
                    val primaryAction = actions.first()
                    val overflowActions = actions.drop(1)

                    BookingActionButton(
                        action = primaryAction,
                        onClick = onActionClicked,
                    )

                    Spacer(Modifier.width(WooPosSpacing.Small.value))

                    BookingOverflowMenu(
                        actions = overflowActions,
                        onClick = onActionClicked,
                    )
                }

                actions.size == 1 -> {
                    BookingActionButton(
                        action = actions.first(),
                        onClick = onActionClicked,
                    )
                }
            }
        }
    }
}

@Composable
private fun BookingActionButton(
    action: BookingAction,
    onClick: (BookingAction) -> Unit,
) {
    val text = actionLabel(action)
    WooPosButtonSmall(
        text = text,
        onClick = { onClick(action) },
    )
}

@Composable
private fun BookingOverflowMenu(
    actions: List<BookingAction>,
    onClick: (BookingAction) -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { showMenu = true }) {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.ic_menu_more_vert),
                contentDescription = stringResource(R.string.more_menu),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        DropdownMenu(
            modifier = Modifier.background(color = MaterialTheme.colorScheme.surfaceContainerLowest),
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            actions.forEach { action ->
                DropdownMenuItem(
                    text = {
                        WooPosText(
                            text = actionLabel(action),
                            style = WooPosTypography.BodyMedium
                        )
                    },
                    onClick = {
                        showMenu = false
                        onClick(action)
                    }
                )
            }
        }
    }
}

@Composable
private fun actionLabel(action: BookingAction): String {
    return when (action) {
        BookingAction.ViewOrder -> stringResource(R.string.woopos_bookings_detail_view_order)
        BookingAction.CancelBooking -> stringResource(R.string.woopos_bookings_detail_cancel)
    }
}

@Composable
private fun BookingSummarySection(detail: BookingDetail) {
    Column {
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

        if (detail.orderTotals != null) {
            BookingTotalsGrid(detail.orderTotals)
        } else {
            WooPosText(
                text = detail.amount,
                style = WooPosTypography.BodyLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun BookingTotalsGrid(totals: BookingOrderTotals) {
    Column(modifier = Modifier.fillMaxWidth()) {
        BookingTotalsRow(
            label = stringResource(R.string.woopos_payment_subtotal_label),
            value = totals.subtotalText,
        )
        Spacer(modifier = Modifier.height(WooPosSpacing.Small.value))
        BookingTotalsRow(
            label = stringResource(R.string.woopos_payment_tax_label),
            value = totals.taxText,
        )
        Spacer(modifier = Modifier.height(WooPosSpacing.Small.value))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(modifier = Modifier.height(WooPosSpacing.Small.value))
        BookingTotalsRow(
            label = stringResource(R.string.woopos_payment_total_label),
            value = totals.totalText,
            style = WooPosTypography.BodyLarge,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun BookingTotalsRow(
    label: String,
    value: String,
    style: WooPosTypography = WooPosTypography.BodyMedium,
    fontWeight: FontWeight = FontWeight.Normal,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        WooPosText(
            text = label,
            style = style,
            fontWeight = fontWeight,
            color = if (fontWeight == FontWeight.Bold) {
                MaterialTheme.colorScheme.onSurface
            } else {
                WooPosTheme.colors.onSurfaceVariantHighest
            },
        )
        WooPosText(
            text = value,
            style = style,
            fontWeight = fontWeight,
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

        WooPosButton(
            text = stringResource(R.string.woopos_bookings_detail_pay_by_card),
            onClick = onPayByCardClicked,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(WooPosSpacing.Small.value))

        WooPosOutlinedButton(
            text = stringResource(R.string.woopos_bookings_detail_pay_by_cash),
            onClick = onPayByCashClicked,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun AttendanceBadge(status: AttendanceStatusUi) {
    val bgColor = when (status) {
        AttendanceStatusUi.Attended -> WooPosTheme.colors.infoLowest
        AttendanceStatusUi.Cancelled -> WooPosTheme.colors.errorLowest
        AttendanceStatusUi.Unattended -> WooPosTheme.colors.default
    }
    val textColor = when (status) {
        AttendanceStatusUi.Attended -> WooPosTheme.colors.onInfoLowest
        AttendanceStatusUi.Cancelled -> WooPosTheme.colors.onErrorLowest
        AttendanceStatusUi.Unattended -> WooPosTheme.colors.onDefault
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
