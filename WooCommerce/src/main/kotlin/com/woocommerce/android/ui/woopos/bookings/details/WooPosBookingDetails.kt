package com.woocommerce.android.ui.woopos.bookings.details

import androidx.compose.foundation.background
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import com.woocommerce.android.ui.woopos.bookings.WOO_POS_BOOKINGS_TOOLBAR_HEIGHT
import com.woocommerce.android.ui.woopos.bookings.WooPosAttendanceBadge
import com.woocommerce.android.ui.woopos.bookings.WooPosBookingStatus
import com.woocommerce.android.ui.woopos.bookings.WooPosBookingStatusColorKey
import com.woocommerce.android.ui.woopos.bookings.WooPosBookingsState
import com.woocommerce.android.ui.woopos.bookings.WooPosBookingsStatusBadge
import com.woocommerce.android.ui.woopos.bookings.WooPosBookingsUIEvent
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.ShadowType
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosCard
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosOutlinedButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosOutlinedButtonSmall
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosShimmerBox
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosToggleButton
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosCornerRadius
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography

@Composable
fun WooPosBookingDetails(
    modifier: Modifier = Modifier,
    details: WooPosBookingsState.BookingDetailsViewState,
    onUIEvent: (WooPosBookingsUIEvent) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(
                start = WooPosSpacing.Medium.value,
                end = WooPosSpacing.Medium.value,
                bottom = WooPosSpacing.Large.value
            )
    ) {
        BookingHeader(details = details, onUIEvent = onUIEvent)

        Spacer(Modifier.height(WooPosSpacing.Large.value))

        BookingDetailsCard(details = details)

        details.customerSection?.let { section ->
            Spacer(Modifier.height(WooPosSpacing.Medium.value))
            BookingCustomerCard(customerSection = section, onUIEvent = onUIEvent)
        }

        details.attendanceSection?.let { section ->
            Spacer(Modifier.height(WooPosSpacing.Large.value))
            BookingAttendanceSection(attendanceSection = section, onUIEvent = onUIEvent)
        }

        Spacer(Modifier.height(WooPosSpacing.Large.value))

        BookingPaymentCard(paymentSection = details.paymentSection)

        if (details.paymentSection.showPayButtons) {
            Spacer(Modifier.height(WooPosSpacing.Large.value))
            BookingPayButtons(onUIEvent = onUIEvent)
        }

        Spacer(Modifier.height(WooPosSpacing.Large.value))

        BookingNoteSection(bookingNote = details.bookingNote, onUIEvent = onUIEvent)
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
        color = WooPosTheme.colors.onSurfaceVariantHighest
    )

    Spacer(Modifier.height(WooPosSpacing.Small.value))

    Row {
        details.attendanceBadge?.let { badge ->
            WooPosAttendanceBadge(attendanceState = badge)
            Spacer(Modifier.width(WooPosSpacing.Small.value))
        }
        WooPosBookingsStatusBadge(status = details.status)
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
                    .height(40.dp)
                    .width(160.dp)
                    .clip(RoundedCornerShape(WooPosCornerRadius.Medium.value))
            )
        }

        is WooPosBookingsState.BookingActionsState.Loaded -> {
            BookingOverflowMenu(
                actions = actionsState.actions,
                onClick = { onUIEvent(WooPosBookingsUIEvent.BookingActionClicked(it)) }
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
                style = WooPosTypography.BodyLarge,
                fontWeight = FontWeight.Bold,
            )

            Spacer(Modifier.height(WooPosSpacing.Medium.value))

            DetailRowLine(
                label = stringResource(R.string.woopos_bookings_details_service_name_label),
                value = details.bookingName,
            )

            Spacer(Modifier.height(WooPosSpacing.Medium.value))

            DetailRowLine(
                label = stringResource(R.string.woopos_bookings_details_date_label),
                value = details.appointmentDate,
            )

            Spacer(Modifier.height(WooPosSpacing.Medium.value))

            DetailRowLine(
                label = stringResource(R.string.woopos_bookings_details_time_label),
                value = details.appointmentTime,
            )

            details.teamMember?.let {
                Spacer(Modifier.height(WooPosSpacing.Medium.value))
                DetailRowLine(
                    label = stringResource(R.string.woopos_bookings_details_team_member_label),
                    value = it,
                )
            }

            details.location?.let {
                Spacer(Modifier.height(WooPosSpacing.Medium.value))
                DetailRowLine(
                    label = stringResource(R.string.woopos_bookings_details_location_label),
                    value = it,
                )
            }

            Spacer(Modifier.height(WooPosSpacing.Medium.value))

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
        Column(Modifier.padding(WooPosSpacing.Medium.value)) {
            WooPosText(
                text = stringResource(R.string.woopos_bookings_details_customer_title),
                style = WooPosTypography.BodyLarge,
                fontWeight = FontWeight.Bold,
            )

            Spacer(Modifier.height(WooPosSpacing.Medium.value))

            customerSection.name?.let {
                WooPosText(
                    text = it,
                    style = WooPosTypography.BodyMedium,
                )
                Spacer(Modifier.height(WooPosSpacing.Medium.value))
            }

            customerSection.email?.let { email ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    WooPosText(
                        text = email,
                        style = WooPosTypography.BodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { onUIEvent(WooPosBookingsUIEvent.CopyEmailClicked(email)) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_copy_white_24dp),
                            contentDescription = stringResource(R.string.woopos_bookings_details_copy_email),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(Modifier.height(WooPosSpacing.Medium.value))
            }

            customerSection.phone?.let { phone ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    WooPosText(
                        text = phone,
                        style = WooPosTypography.BodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_menu_more_vert),
                            contentDescription = stringResource(R.string.more_menu),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            customerSection.billingAddress?.let {
                DividerWithSpacing()
                WooPosText(
                    text = stringResource(R.string.woopos_bookings_details_billing_address_label),
                    style = WooPosTypography.BodyMedium,
                    color = WooPosTheme.colors.onSurfaceVariantHighest,
                )
                Spacer(Modifier.height(WooPosSpacing.Small.value))
                WooPosText(
                    text = it,
                    style = WooPosTypography.BodyMedium,
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
    attendanceSection: WooPosBookingsState.AttendanceSection,
    onUIEvent: (WooPosBookingsUIEvent) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        WooPosCard(shadowType = ShadowType.Soft) {
            Column(Modifier.padding(WooPosSpacing.Medium.value)) {
                WooPosText(
                    text = stringResource(R.string.woopos_bookings_details_attendance_title),
                    style = WooPosTypography.BodyLarge,
                    fontWeight = FontWeight.Bold,
                )

                Spacer(Modifier.height(WooPosSpacing.Medium.value))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(WooPosSpacing.Medium.value)
                ) {
                    WooPosToggleButton(
                        text = stringResource(R.string.woopos_bookings_details_attendance_attended),
                        isSelected = attendanceSection.selection == WooPosBookingsState.AttendanceState.ATTENDED,
                        onClick = { onUIEvent(WooPosBookingsUIEvent.AttendanceToggled(true)) },
                        modifier = Modifier.weight(1f)
                    )

                    WooPosToggleButton(
                        text = stringResource(R.string.woopos_bookings_details_attendance_unattended),
                        isSelected = attendanceSection.selection == WooPosBookingsState.AttendanceState.UNATTENDED,
                        onClick = { onUIEvent(WooPosBookingsUIEvent.AttendanceToggled(false)) },
                        modifier = Modifier.weight(1f)
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
                style = WooPosTypography.BodyLarge,
                fontWeight = FontWeight.Bold,
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
private fun BookingPayButtons(onUIEvent: (WooPosBookingsUIEvent) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        WooPosButton(
            text = stringResource(R.string.woopos_bookings_details_pay_by_card),
            modifier = Modifier.fillMaxWidth(),
            onClick = { onUIEvent(WooPosBookingsUIEvent.PayByCardClicked) }
        )

        Spacer(Modifier.height(WooPosSpacing.Medium.value))

        WooPosOutlinedButton(
            text = stringResource(R.string.woopos_bookings_details_pay_by_cash),
            modifier = Modifier.fillMaxWidth(),
            onClick = { onUIEvent(WooPosBookingsUIEvent.PayByCashClicked) }
        )
    }
}

@Composable
private fun BookingNoteSection(
    bookingNote: String?,
    onUIEvent: (WooPosBookingsUIEvent) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        WooPosCard(shadowType = ShadowType.Soft) {
            Row(
                modifier = Modifier.padding(WooPosSpacing.Medium.value),
                verticalAlignment = Alignment.CenterVertically
            ) {
                WooPosText(
                    text = stringResource(R.string.woopos_bookings_details_booking_note_title),
                    style = WooPosTypography.BodyLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                WooPosOutlinedButtonSmall(
                    text = stringResource(R.string.woopos_bookings_details_add_note),
                    onClick = { onUIEvent(WooPosBookingsUIEvent.AddBookingNoteClicked) }
                )
            }
        }

        Spacer(Modifier.height(WooPosSpacing.Small.value))

        bookingNote?.let {
            WooPosText(
                text = it,
                style = WooPosTypography.BodyMedium,
            )
            Spacer(Modifier.height(WooPosSpacing.Small.value))
        }

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
            style = WooPosTypography.BodyLarge,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.weight(1f))
        WooPosText(
            text = value,
            style = WooPosTypography.BodyLarge,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun DividerWithSpacing() {
    Spacer(Modifier.height(WooPosSpacing.Medium.value))
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    Spacer(Modifier.height(WooPosSpacing.Medium.value))
}

@Composable
private fun BookingOverflowMenu(
    actions: List<WooPosBookingsState.BookingAction>,
    onClick: (WooPosBookingsState.BookingAction) -> Unit
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
                        val text = when (action) {
                            is WooPosBookingsState.BookingAction.EmailReceipt -> stringResource(
                                R.string.woopos_orders_email_receipt
                            )
                        }
                        WooPosText(
                            text = text,
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

@WooPosPreview
@Composable
fun WooPosBookingDetailsPreview() {
    val bookingDetails = WooPosBookingsState.BookingDetailsViewState(
        id = 333L,
        orderId = 3330L,
        number = "#333",
        status = WooPosBookingStatus(text = "Unpaid", colorKey = WooPosBookingStatusColorKey.FAILED),
        actionsState = WooPosBookingsState.BookingActionsState.Loaded(
            listOf(WooPosBookingsState.BookingAction.EmailReceipt(1L))
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
        ),
        attendanceSection = WooPosBookingsState.AttendanceSection(
            selection = WooPosBookingsState.AttendanceState.UNATTENDED,
        ),
        paymentSection = WooPosBookingsState.PaymentSection(
            serviceAmount = "$55.00",
            taxAmount = "$0.00",
            discountAmount = "-",
            totalAmount = "$55.00",
            paidWithLabel = null,
            showPayButtons = true,
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
