package com.woocommerce.android.ui.woopos.settings.details

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography

@Composable
fun WooPosSettingsDetailsMenuItemInfo(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
) {
    Column(
        modifier = modifier,
    ) {
        WooPosText(
            text = title,
            style = WooPosTypography.BodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(modifier = Modifier.height(WooPosSpacing.XSmall.value))

        WooPosText(
            text = subtitle,
            style = WooPosTypography.BodyMedium,
            color = WooPosTheme.colors.onSurfaceVariantLowest,
        )
    }
}

@WooPosPreview
@Composable
fun WooPosSettingsDetailsMenuItemInfoPreview() {
    WooPosTheme {
        Column {
            WooPosSettingsDetailsMenuItemInfo(
                title = "Store name",
                subtitle = "My WooCommerce Store",
            )

            Spacer(modifier = Modifier.size(WooPosSpacing.Medium.value))

            WooPosSettingsDetailsMenuItemInfo(
                title = "Currency",
                subtitle = "US Dollar (USD)",
            )
        }
    }
}
