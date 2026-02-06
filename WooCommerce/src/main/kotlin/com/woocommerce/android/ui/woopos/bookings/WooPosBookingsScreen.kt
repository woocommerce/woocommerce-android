package com.woocommerce.android.ui.woopos.bookings

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.root.navigation.WooPosNavigationEvent

@Composable
@Suppress("UnusedParameter")
fun WooPosBookingsScreen(onNavigationEvent: (WooPosNavigationEvent) -> Unit) {
    WooPosText(
        modifier = Modifier,
        text = "Bookings screen",
        style = WooPosTypography.BodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
    )
}
