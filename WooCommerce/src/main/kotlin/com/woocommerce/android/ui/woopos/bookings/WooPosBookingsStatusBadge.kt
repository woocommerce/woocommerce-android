package com.woocommerce.android.ui.woopos.bookings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography

@Composable
fun WooPosBookingsStatusBadge(status: PosBookingStatus) {
    val bgColor = when (status.colorKey) {
        BookingStatusColorKey.COMPLETED -> WooPosTheme.colors.infoLowest
        BookingStatusColorKey.FAILED -> WooPosTheme.colors.errorLowest
        BookingStatusColorKey.PROCESSING,
        BookingStatusColorKey.ON_HOLD,
        BookingStatusColorKey.OTHER -> WooPosTheme.colors.default
    }

    val textColor = when (status.colorKey) {
        BookingStatusColorKey.COMPLETED -> WooPosTheme.colors.onInfoLowest
        BookingStatusColorKey.FAILED -> WooPosTheme.colors.onErrorLowest
        BookingStatusColorKey.PROCESSING,
        BookingStatusColorKey.ON_HOLD,
        BookingStatusColorKey.OTHER -> WooPosTheme.colors.onDefault
    }

    WooPosText(
        text = status.text,
        style = WooPosTypography.BodySmall,
        color = textColor,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .clip(RoundedCornerShape(WooPosSpacing.Small.value))
            .background(bgColor)
            .padding(WooPosSpacing.Small.value)
    )
}
