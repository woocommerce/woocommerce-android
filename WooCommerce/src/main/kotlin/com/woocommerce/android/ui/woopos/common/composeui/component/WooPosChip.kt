package com.woocommerce.android.ui.woopos.common.composeui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosCornerRadius
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography

@Composable
fun WooPosChip(
    modifier: Modifier = Modifier,
    text: String,
    onClick: () -> Unit,
    leadingIcon: ImageVector? = null,
    shape: Shape = RoundedCornerShape(WooPosCornerRadius.Large.value),
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceContainerLowest,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    shadowElevation: Dp = 1.dp
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = shape,
        color = backgroundColor,
        shadowElevation = shadowElevation,
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = WooPosSpacing.Medium.value,
                vertical = WooPosSpacing.Small.value
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(WooPosSpacing.Small.value))
            }

            WooPosText(
                text = text,
                style = WooPosTypography.BodyMedium,
                color = contentColor
            )
        }
    }
}

@WooPosPreview
@Composable
fun WooPosChipPreview() {
    WooPosTheme {
        Column(
            modifier = Modifier.padding(WooPosSpacing.Medium.value)
        ) {
            WooPosChip(
                text = "Search chip with icon",
                onClick = {},
                leadingIcon = Icons.Outlined.Search
            )

            Spacer(modifier = Modifier.padding(WooPosSpacing.Medium.value))

            WooPosChip(
                text = "Chip without icon",
                onClick = {}
            )
        }
    }
}
