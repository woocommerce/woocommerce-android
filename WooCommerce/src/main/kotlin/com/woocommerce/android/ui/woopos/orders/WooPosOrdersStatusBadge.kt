package com.woocommerce.android.ui.woopos.orders

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
fun WooPosOrdersStatusBadge(status: PosOrderStatus) {
    val bgColor = when (status.colorKey) {
        OrderStatusColorKey.COMPLETED -> WooPosTheme.colors.infoLowest
        OrderStatusColorKey.FAILED -> WooPosTheme.colors.errorLowest
        OrderStatusColorKey.PROCESSING,
        OrderStatusColorKey.ON_HOLD,
        OrderStatusColorKey.OTHER -> WooPosTheme.colors.default
    }

    val textColor = when (status.colorKey) {
        OrderStatusColorKey.COMPLETED -> WooPosTheme.colors.onInfoLowest
        OrderStatusColorKey.FAILED -> WooPosTheme.colors.onErrorLowest
        OrderStatusColorKey.PROCESSING,
        OrderStatusColorKey.ON_HOLD,
        OrderStatusColorKey.OTHER -> WooPosTheme.colors.onDefault
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
