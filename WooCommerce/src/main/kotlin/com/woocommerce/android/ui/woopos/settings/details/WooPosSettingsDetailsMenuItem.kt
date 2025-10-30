package com.woocommerce.android.ui.woopos.settings.details

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosCard
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography

@Composable
fun WooPosSettingsDetailsMenuItem(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    onClick: (() -> Unit),
) {
    WooPosCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple()
            ) { onClick() },
    ) {
        Column(
            modifier = Modifier
                .padding(WooPosSpacing.Medium.value)
        ) {
            WooPosText(
                text = title,
                style = WooPosTypography.BodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
            )
            WooPosText(
                text = subtitle,
                style = WooPosTypography.BodySmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(top = WooPosSpacing.XSmall.value)
            )
        }
    }
}

@WooPosPreview
@Composable
fun WooPosSettingsDetailsMenuItemPreview() {
    WooPosTheme {
        Column {
            WooPosSettingsDetailsMenuItem(
                title = "Store name",
                subtitle = "My WooCommerce Store",
                onClick = {}
            )

            Spacer(modifier = Modifier.size(WooPosSpacing.Large.value))

            WooPosSettingsDetailsMenuItem(
                title = "Currency",
                subtitle = "US Dollar (USD)",
                onClick = {}
            )
        }
    }
}
