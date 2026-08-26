package com.woocommerce.android.ui.compose.designsystem.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.woocommerce.android.ui.compose.designsystem.WooTheme
import com.woocommerce.android.ui.compose.designsystem.component.WooDivider
import com.woocommerce.android.ui.compose.designsystem.icons.Box
import com.woocommerce.android.ui.compose.designsystem.icons.Ellipsis
import com.woocommerce.android.ui.compose.designsystem.icons.House
import com.woocommerce.android.ui.compose.designsystem.icons.List
import com.woocommerce.android.ui.compose.designsystem.icons.PointOfSale
import com.woocommerce.android.ui.compose.designsystem.icons.WooIcons

@Composable
internal fun PreviewOnlyTabBarSample(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp),
        color = WooTheme.colors.surface.bright,
    ) {
        Column {
            WooDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = WooTheme.padding.padding7, vertical = WooTheme.padding.padding3),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                PreviewOnlyTabBarItem(
                    icon = WooIcons.Regular.House,
                    label = "Home",
                    selected = true,
                )
                PreviewOnlyTabBarItem(
                    icon = WooIcons.Regular.List,
                    label = "Orders",
                )
                PreviewOnlyTabBarItem(
                    icon = WooIcons.Regular.Box,
                    label = "Products",
                )
                PreviewOnlyTabBarItem(
                    icon = WooIcons.Regular.PointOfSale,
                    label = "Point of sale",
                )
                PreviewOnlyTabBarItem(
                    icon = WooIcons.Regular.Ellipsis,
                    label = "More",
                )
            }
        }
    }
}

@Composable
internal fun PreviewOnlyTableSample(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(WooTheme.colors.surface.bright)
            .border(WooTheme.stroke.thin, WooTheme.colors.outlineVariant),
    ) {
        PreviewOnlyTableRow("Product", "Stock", "Price", isHeader = true)
        WooDivider()
        PreviewOnlyTableRow("Beanie", "24", "\$18")
        WooDivider()
        PreviewOnlyTableRow("Hoodie", "8", "\$48")
    }
}

@Composable
private fun PreviewOnlyTabBarItem(
    icon: ImageVector,
    label: String,
    selected: Boolean = false,
) {
    if (selected) {
        Surface(
            modifier = Modifier.size(48.dp),
            color = WooTheme.colors.primary,
            contentColor = WooTheme.colors.onPrimary,
            shape = RoundedCornerShape(WooTheme.radius.large),
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    } else {
        val contentColor = WooTheme.colors.surface.onVariantLowest

        Column(
            modifier = Modifier.size(48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(24.dp),
            )
            Text(
                text = label,
                color = contentColor,
                style = WooTheme.text.labelSmall.emphasized,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun PreviewOnlyTableRow(
    first: String,
    second: String,
    third: String,
    isHeader: Boolean = false,
) {
    val style = if (isHeader) WooTheme.text.labelMedium.strong else WooTheme.text.bodyMedium.regular
    val fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(WooTheme.padding.padding3),
        horizontalArrangement = Arrangement.spacedBy(WooTheme.spacing.space3),
    ) {
        Text(
            text = first,
            modifier = Modifier.weight(1f),
            color = WooTheme.colors.surface.onDefault,
            style = style.copy(fontWeight = fontWeight),
        )
        Text(
            text = second,
            modifier = Modifier.width(64.dp),
            color = WooTheme.colors.surface.onVariant,
            style = style.copy(fontWeight = fontWeight),
        )
        Text(
            text = third,
            modifier = Modifier.width(64.dp),
            color = WooTheme.colors.surface.onVariant,
            style = style.copy(fontWeight = fontWeight),
        )
    }
}
