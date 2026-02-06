package com.woocommerce.android.ui.woopos.bookings.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.bookings.WooPosBookingsState
import com.woocommerce.android.ui.woopos.bookings.WooPosBookingsUIEvent
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography

@Composable
@Suppress("UnusedParameter")
fun WooPosBookingDetails(
    modifier: Modifier = Modifier,
    details: WooPosBookingsState.BookingDetailsViewState.Computed.Details,
    onUIEvent: (WooPosBookingsUIEvent) -> Unit
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        WooPosText(
            text = "Booking Detail",
            style = WooPosTypography.Heading
        )
        Spacer(modifier = Modifier.size(WooPosSpacing.Medium.value))
        WooPosButton(
            text = stringResource(R.string.woopos_bookings_pay_by_card),
            onClick = { onUIEvent(WooPosBookingsUIEvent.PayByCardClicked) }
        )
    }
}
