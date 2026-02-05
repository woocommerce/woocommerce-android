package com.woocommerce.android.ui.woopos.bookings.details

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.woocommerce.android.ui.woopos.bookings.WooPosBookingsState
import com.woocommerce.android.ui.woopos.bookings.WooPosBookingsUIEvent
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography

@Composable
fun WooPosBookingDetails(
    modifier: Modifier = Modifier,
    details: WooPosBookingsState.BookingDetailsViewState.Computed.Details,
    onUIEvent: (WooPosBookingsUIEvent) -> Unit
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        WooPosText(
            text = "Booking Detail",
            style = WooPosTypography.Heading
        )
    }
}
