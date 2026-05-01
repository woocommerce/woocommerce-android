package com.woocommerce.android.ui.woopos.bookings.details

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.bookings.PaymentStatus
import com.woocommerce.android.ui.woopos.bookings.WOO_POS_BOOKINGS_TOOLBAR_HEIGHT
import com.woocommerce.android.ui.woopos.bookings.WooPosAttendanceBadge
import com.woocommerce.android.ui.woopos.bookings.WooPosBookingsState
import com.woocommerce.android.ui.woopos.bookings.WooPosBookingsUIEvent
import com.woocommerce.android.ui.woopos.bookings.WooPosCancelledBadge
import com.woocommerce.android.ui.woopos.bookings.WooPosGuestBadge
import com.woocommerce.android.ui.woopos.bookings.WooPosPaymentStatusBadge
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.ShadowType
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosCard
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosOutlinedButtonSmall
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosOverflowMenu
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosOverflowMenuItem
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosOverflowPrimaryAction
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosShimmerBox
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosToggleButton
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosComponentSize
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosCornerRadius
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosIconSize
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.toAdaptiveIconSize

private const val SHOW_PAYMENT_SECTION = false

@Composable
fun WooPosBookingDetails(
    modifier: Modifier = Modifier,
    details: WooPosBookingsState.BookingDetailsViewState,
    onUIEvent: (WooPosBookingsUIEvent) -> Unit
) {
    val hasCollectButton = details.paymentSection.collectPaymentLabel != null
    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(
                    start = WooPosSpacing.Medium.value,
                    end = WooPosSpacing.Medium.value,
                    bottom = if (hasCollectButton) {
                        92.dp + WooPosSpacing.Large.value + WooPosSpacing.Medium.value
                    } else {
                        WooPosSpacing.XLarge.value
                    }
                )
        ) {
            BookingHeader(details = details, onUIEvent = onUIEvent)

            Spacer(Modifier.height(WooPosSpacing.Large.value))

            BookingDetailsCard(details = details)

            val attendanceSection = details.attendanceSection
            if (attendanceSection is WooPosBookingsState.AttendanceSection.Visible) {
                Spacer(Modifier.height(WooPosSpacing.Large.value))
                BookingAttendanceSection(attendanceSection = attendanceSection, onUIEvent = onUIEvent)
            }

            details.customerSection?.let { section ->
                Spacer(Modifier.height(WooPosSpacing.Large.value))
                BookingCustomerCard(customerSection = section, onUIEvent = onUIEvent)
            }

            Spacer(Modifier.height(WooPosSpacing.Large.value))

            @Suppress("KotlinConstantConditions")
            if (SHOW_PAYMENT_SECTION) {
                BookingPaymentCard(paymentSection = details.paymentSection)
                Spacer(Modifier.height(WooPosSpacing.Large.value))
            }

            BookingNoteSection(bookingNote = details.bookingNote, onUIEvent = onUIEvent)
        }

        if (hasCollectButton) {
            val panelFadeDistance = WooPosSpacing.Huge.value.value
            val panelAlpha by remember {
                derivedStateOf {
                    val maxScroll = scrollState.maxValue
                    if (maxScroll == 0) return@derivedStateOf 0f
                    val distanceFromBottom = maxScroll - scrollState.value
                    (distanceFromBottom / panelFadeDistance).coerceIn(0f, 1f)
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = panelAlpha)
                    )
            ) {
                HorizontalDivider(
                    color = WooPosTheme.colors.outlineVariant.copy(alpha = panelAlpha),
                    thickness = 0.5.dp,
                )
                WooPosButton(
                    text = stringResource(R.string.card_reader_collect_payment),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            top = WooPosSpacing.Medium.value,
                            bottom = WooPosSpacing.Large.value,
                            start = WooPosSpacing.Medium.value,
                            end = WooPosSpacing.Medium.value,
                        ),
                    onClick = { onUIEvent(WooPosBookingsUIEvent.CollectPaymentClicked) }
                )
            }
        }
    }
}

@Composable
private fun BookingHeader(
    details: WooPosBookingsState.BookingDetailsViewState,
    onUIEvent: (WooPosBookingsUIEvent) -> Unit
) {
    Row(
        modifier = Modifier.heightIn(min = WOO_POS_BOOKINGS_TOOLBAR_HEIGHT),
        verticalAlignment = Alignment.CenterVertically
    ) {
        WooPosText(
            text = details.headerTitle,
            style = WooPosTypography.Heading,
            fontWeight = FontWeight.Bold,
        )

        Spacer(Modifier.weight(1f))

        BookingActions(details.actionsState, onUIEvent)
    }

    WooPosText(
        text = details.headerSubtitle,
        style = WooPosTypography.BodyMedium,
        color = WooPosTheme.colors.onSurfaceVariantLowest
    )

    Spacer(Modifier.height(WooPosSpacing.Small.value))

    Row(
        horizontalArrangement = Arrangement.spacedBy(WooPosSpacing.Small.value),
    ) {
        if (details.isCancelled) {
            WooPosCancelledBadge()
        }
        details.attendanceBadge?.let { badge ->
            WooPosAttendanceBadge(attendanceState = badge)
        }
        WooPosPaymentStatusBadge(paymentStatus = details.paymentStatus)
    }
}

@Composable
private fun BookingActions(
    actionsState: WooPosBookingsState.BookingActionsState,
    onUIEvent: (WooPosBookingsUIEvent) -> Unit
) {
    when (actionsState) {
        is WooPosBookingsState.BookingActionsState.Loading -> {
            WooPosShimmerBox(
                modifier = Modifier
                    .height(WooPosIconSize.Large.value)
                    .width(WooPosComponentSize.XLarge.value)
                    .clip(RoundedCornerShape(WooPosCornerRadius.Medium.value))
            )
        }

        is WooPosBookingsState.BookingActionsState.Loaded -> {
            val viewOrder = actionsState.actions
                .filterIsInstance<WooPosBookingsState.BookingAction.ViewOrder>()
                .firstOrNull()
            val otherActions = actionsState.actions
                .filter { it !is WooPosBookingsState.BookingAction.ViewOrder }

            WooPosOverflowMenu(
                primaryAction = viewOrder?.let { action ->
                    WooPosOverflowPrimaryAction(
                        label = stringResource(R.string.booking_payment_view_order),
                        onClick = { onUIEvent(WooPosBookingsUIEvent.BookingMenuActionClicked(action)) }
                    )
                },
                items = otherActions.map { action ->
                    bookingActionToMenuItem(action) {
                        onUIEvent(WooPosBookingsUIEvent.BookingMenuActionClicked(it))
                    }
                }
            )
        }
    }
}

@Composable
private fun BookingDetailsCard(
    details: WooPosBookingsState.BookingDetailsViewState
) {
    WooPosCard(shadowType = ShadowType.Soft) {
        Column(Modifier.padding(WooPosSpacing.Medium.value)) {
            WooPosText(
                text = stringResource(R.string.woopos_bookings_details_title, details.number.removePrefix("#")),
                style = WooPosTypography.BodyXLarge,
                fontWeight = FontWeight.Bold,
            )

            Spacer(Modifier.height(WooPosSpacing.Medium.value))

            details.teamMember?.let {
                DetailRowLine(
                    label = stringResource(R.string.woopos_bookings_details_team_member_label),
                    value = it,
                )
                DividerWithSpacing()
            }

            details.location?.let {
                DetailRowLine(
                    label = stringResource(R.string.woopos_bookings_details_location_label),
                    value = it,
                )
                DividerWithSpacing()
            }

            DetailRowLine(
                label = stringResource(R.string.woopos_bookings_details_duration_label),
                value = details.duration,
            )
        }
    }
}

@Composable
private fun BookingCustomerCard(
    customerSection: WooPosBookingsState.CustomerSection,
    onUIEvent: (WooPosBookingsUIEvent) -> Unit
) {
    WooPosCard(shadowType = ShadowType.Soft) {
        Column(Modifier.fillMaxWidth().padding(WooPosSpacing.Medium.value)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(WooPosSpacing.Small.value),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                WooPosText(
                    text = stringResource(R.string.woopos_bookings_details_customer_title),
                    style = WooPosTypography.BodyXLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                if (customerSection.isGuest) {
                    WooPosGuestBadge()
                }
            }

            customerSection.email?.let { email ->
                Spacer(Modifier.height(WooPosSpacing.Medium.value))
                CopyableRow(
                    text = email,
                    contentDescription = stringResource(R.string.woopos_bookings_details_copy_email),
                    onClick = { onUIEvent(WooPosBookingsUIEvent.CopyEmailClicked(email)) }
                )
            }

            customerSection.phone?.let { phone ->
                DividerWithSpacing()
                CopyableRow(
                    text = phone,
                    contentDescription = stringResource(R.string.woopos_bookings_details_copy_phone),
                    onClick = { onUIEvent(WooPosBookingsUIEvent.CopyPhoneClicked(phone)) }
                )
            }

            customerSection.note?.let {
                DividerWithSpacing()
                WooPosText(
                    text = stringResource(R.string.woopos_bookings_details_customer_note_label),
                    style = WooPosTypography.BodyMedium,
                    color = WooPosTheme.colors.onSurfaceVariantHighest,
                )
                Spacer(Modifier.height(WooPosSpacing.Small.value))
                WooPosText(
                    text = it,
                    style = WooPosTypography.BodyMedium,
                )
            }
        }
    }
}

@Composable
private fun BookingAttendanceSection(
    attendanceSection: WooPosBookingsState.AttendanceSection.Visible,
    onUIEvent: (WooPosBookingsUIEvent) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        WooPosCard(shadowType = ShadowType.Soft) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(WooPosSpacing.Medium.value),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                WooPosText(
                    text = stringResource(R.string.woopos_bookings_details_attendance_title),
                    style = WooPosTypography.BodyXLarge,
                    fontWeight = FontWeight.SemiBold,
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(WooPosSpacing.Small.value)
                ) {
                    WooPosToggleButton(
                        text = stringResource(R.string.woopos_bookings_details_attendance_attended),
                        isSelected = attendanceSection.selection == WooPosBookingsState.AttendanceState.ATTENDED,
                        onClick = { onUIEvent(WooPosBookingsUIEvent.AttendanceToggled(true)) },
                    )

                    WooPosToggleButton(
                        text = stringResource(R.string.woopos_bookings_details_attendance_unattended),
                        isSelected = attendanceSection.selection == WooPosBookingsState.AttendanceState.UNATTENDED,
                        onClick = { onUIEvent(WooPosBookingsUIEvent.AttendanceToggled(false)) },
                    )
                }
            }
        }

        Spacer(Modifier.height(WooPosSpacing.Small.value))

        WooPosText(
            text = stringResource(R.string.woopos_bookings_details_attendance_hint),
            style = WooPosTypography.BodySmall,
            color = WooPosTheme.colors.onSurfaceVariantHighest,
        )
    }
}

@Composable
private fun BookingPaymentCard(
    paymentSection: WooPosBookingsState.PaymentSection
) {
    WooPosCard(shadowType = ShadowType.Soft) {
        Column(Modifier.padding(WooPosSpacing.Medium.value)) {
            WooPosText(
                text = stringResource(R.string.woopos_bookings_details_payment_title),
                style = WooPosTypography.BodyXLarge,
                fontWeight = FontWeight.SemiBold,
            )

            Spacer(Modifier.height(WooPosSpacing.Medium.value))

            DetailRowLine(
                label = stringResource(R.string.woopos_bookings_details_service_label),
                value = paymentSection.serviceAmount,
            )

            Spacer(Modifier.height(WooPosSpacing.Small.value))

            DetailRowLine(
                label = stringResource(R.string.woopos_bookings_details_tax_label),
                value = paymentSection.taxAmount,
            )

            Spacer(Modifier.height(WooPosSpacing.Small.value))

            DetailRowLine(
                label = stringResource(R.string.woopos_bookings_details_discount_label),
                value = paymentSection.discountAmount,
            )

            DividerWithSpacing()

            TotalRowLine(
                label = stringResource(R.string.woopos_bookings_details_total_label),
                value = paymentSection.totalAmount,
            )

            paymentSection.paidWithLabel?.let {
                Spacer(Modifier.height(WooPosSpacing.XSmall.value))
                WooPosText(
                    text = it,
                    style = WooPosTypography.BodyMedium,
                    color = WooPosTheme.colors.onSurfaceVariantHighest,
                )
            }
        }
    }
}

@Composable
private fun BookingNoteSection(
    bookingNote: String?,
    onUIEvent: (WooPosBookingsUIEvent) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        WooPosCard(shadowType = ShadowType.Soft) {
            Column(modifier = Modifier.padding(WooPosSpacing.Medium.value)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    WooPosText(
                        text = stringResource(R.string.woopos_bookings_details_booking_note_title),
                        style = WooPosTypography.BodyXLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    WooPosOutlinedButtonSmall(
                        text = stringResource(
                            if (bookingNote.isNullOrBlank()) {
                                R.string.woopos_bookings_details_add_note
                            } else {
                                R.string.woopos_bookings_details_edit_note
                            }
                        ),
                        onClick = { onUIEvent(WooPosBookingsUIEvent.AddBookingNoteClicked) }
                    )
                }
                bookingNote?.takeIf { it.isNotBlank() }?.let {
                    Spacer(Modifier.height(WooPosSpacing.Small.value))
                    WooPosText(
                        text = it,
                        style = WooPosTypography.BodyMedium,
                    )
                }
            }
        }

        Spacer(Modifier.height(WooPosSpacing.Small.value))

        WooPosText(
            text = stringResource(R.string.woopos_bookings_details_booking_note_hint),
            style = WooPosTypography.BodySmall,
            color = WooPosTheme.colors.onSurfaceVariantHighest,
        )
    }
}

@Composable
private fun DetailRowLine(
    label: String,
    value: String,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        WooPosText(
            text = label,
            style = WooPosTypography.BodyMedium,
            color = WooPosTheme.colors.onSurfaceVariantHighest,
        )
        Spacer(Modifier.weight(1f))
        WooPosText(
            text = value,
            style = WooPosTypography.BodyMedium,
        )
    }
}

@Composable
private fun TotalRowLine(
    label: String,
    value: String,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        WooPosText(
            text = label,
            style = WooPosTypography.BodyMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.weight(1f))
        WooPosText(
            text = value,
            style = WooPosTypography.BodyMedium,
        )
    }
}

@Composable
private fun CopyableRow(
    text: String,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        WooPosText(
            text = text,
            style = WooPosTypography.BodyMedium,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = ImageVector.vectorResource(R.drawable.ic_copy_white_24dp),
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp.toAdaptiveIconSize())
        )
    }
}

@Composable
private fun DividerWithSpacing() {
    Spacer(Modifier.height(WooPosSpacing.Medium.value))
    HorizontalDivider(
        color = WooPosTheme.colors.outlineVariant,
        thickness = 0.5.dp,
    )
    Spacer(Modifier.height(WooPosSpacing.Medium.value))
}

@Composable
private fun bookingActionToMenuItem(
    action: WooPosBookingsState.BookingAction,
    onClick: (WooPosBookingsState.BookingAction) -> Unit
): WooPosOverflowMenuItem {
    val (label, color) = when (action) {
        is WooPosBookingsState.BookingAction.ViewOrder -> {
            stringResource(R.string.booking_payment_view_order) to
                MaterialTheme.colorScheme.onSurface
        }
        is WooPosBookingsState.BookingAction.EmailReceipt -> {
            stringResource(R.string.woopos_orders_email_receipt) to
                MaterialTheme.colorScheme.onSurface
        }
        is WooPosBookingsState.BookingAction.IssueRefund -> {
            stringResource(R.string.booking_overflow_refund) to
                MaterialTheme.colorScheme.onSurface
        }
        is WooPosBookingsState.BookingAction.CancelBooking -> {
            stringResource(R.string.woopos_bookings_cancel_menu_item) to
                MaterialTheme.colorScheme.error
        }
    }
    return WooPosOverflowMenuItem(
        label = label,
        color = color,
        onClick = { onClick(action) }
    )
}

@WooPosPreview
@Composable
fun WooPosBookingDetailsPreview() {
    val bookingDetails = WooPosBookingsState.BookingDetailsViewState(
        id = 333L,
        orderId = 3330L,
        number = "#333",
        paymentStatus = PaymentStatus.UNPAID,
        isCancelled = false,
        actionsState = WooPosBookingsState.BookingActionsState.Loaded(
            listOf(
                WooPosBookingsState.BookingAction.ViewOrder(1L),
                WooPosBookingsState.BookingAction.EmailReceipt(3330L),
                WooPosBookingsState.BookingAction.CancelBooking(bookingId = 333L, orderId = 3330L)
            )
        ),
        headerTitle = "10:30-11:30 AM",
        headerSubtitle = "Women's Haircut \u00B7 Margarita Nikolaevna",
        attendanceBadge = WooPosBookingsState.AttendanceState.UNATTENDED,
        bookingName = "Women's Haircut",
        appointmentDate = "Monday, 05 July 2025",
        appointmentTime = "10:30-11:30 AM",
        duration = "60 min",
        teamMember = "Marianne Renoir",
        location = "238 Willow Creek Drive, Montgomery, AL 36109",
        customerSection = WooPosBookingsState.CustomerSection(
            name = "Margarita Nikolaevna",
            email = "margarita.n@gmail.com",
            phone = "+1 742582943798",
            billingAddress = "238 Willow Creek Drive, Montgomery, AL 36109",
            note = "Prefers eco-friendly products, shorter length cuts",
            isGuest = false,
        ),
        attendanceSection = WooPosBookingsState.AttendanceSection.Visible(
            selection = WooPosBookingsState.AttendanceState.UNATTENDED,
        ),
        paymentSection = WooPosBookingsState.PaymentSection(
            serviceAmount = "$55.00",
            taxAmount = "$0.00",
            discountAmount = "-",
            totalAmount = "$55.00",
            paidWithLabel = null,
            collectPaymentLabel = "$55.00",
        ),
        bookingNote = null,
    )

    WooPosTheme {
        WooPosBookingDetails(
            details = bookingDetails,
            onUIEvent = {}
        )
    }
}
