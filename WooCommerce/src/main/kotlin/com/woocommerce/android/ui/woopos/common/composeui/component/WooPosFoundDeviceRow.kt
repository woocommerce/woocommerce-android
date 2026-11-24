package com.woocommerce.android.ui.woopos.common.composeui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosComponentSize
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosCornerRadius
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosIconSize
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography

@Composable
fun WooPosFoundDeviceRow(
    name: String,
    suffix: String?,
    icon: Painter,
    iconContentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick)
            .height(WooPosComponentSize.Small.value),
        shape = RoundedCornerShape(WooPosCornerRadius.Medium.value),
        color = WooPosTheme.colors.transparent,
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.inverseSurface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    PaddingValues(
                        horizontal = WooPosSpacing.Medium.value,
                        vertical = WooPosSpacing.Small.value,
                    )
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = icon,
                contentDescription = iconContentDescription,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(WooPosIconSize.Small.value),
            )
            Spacer(modifier = Modifier.size(WooPosSpacing.Small.value))
            WooPosText(
                text = name,
                style = WooPosTypography.BodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                fontWeight = FontWeight.Bold,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            suffix?.let {
                WooPosText(
                    text = it,
                    style = WooPosTypography.BodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
