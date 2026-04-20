package com.woocommerce.android.ui.woopos.common.composeui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosCornerRadius
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosElevation
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.toAdaptiveIconSize

@Composable
fun WooPosChip(
    modifier: Modifier = Modifier,
    text: String,
    onClick: () -> Unit,
    leadingIcon: ImageVector? = null,
    shape: Shape = RoundedCornerShape(WooPosCornerRadius.Medium.value),
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    iconTint: Color = MaterialTheme.colorScheme.onSurface,
) {
    WooPosCard(
        modifier = modifier,
        shape = shape,
        backgroundColor = backgroundColor,
        shadowType = ShadowType.Soft,
        elevation = WooPosElevation.Medium,
    ) {
        @Suppress("WooPosDesignSystemSpacingUsageRule")
        Row(
            modifier = Modifier
                .clickable(onClick = onClick)
                .padding(
                    start = WooPosSpacing.Medium.value,
                    top = 12.dp,
                    end = WooPosSpacing.Medium.value,
                    bottom = 12.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(26.dp.toAdaptiveIconSize())
                )

                Spacer(modifier = Modifier.width(WooPosSpacing.Small.value))
            }

            WooPosText(
                text = text,
                style = WooPosTypography.BodyLarge,
                color = contentColor
            )
        }
    }
}

@WooPosPreview
@Composable
fun WooPosChipPreview() {
    WooPosTheme {
        Box(modifier = Modifier.padding(WooPosSpacing.Medium.value)) {
            Column(
                modifier = Modifier
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(WooPosSpacing.Small.value, Alignment.Start),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    WooPosChip(
                        text = "Search chip",
                        onClick = {},
                        leadingIcon = ImageVector.vectorResource(R.drawable.ic_search_24dp)
                    )

                    WooPosChip(
                        text = "No icon chip",
                        onClick = {}
                    )
                }
            }
        }
    }
}
