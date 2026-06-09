package com.woocommerce.android.ui.designsystem.compose.preview

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.designsystem.compose.WooTheme
import com.woocommerce.android.ui.designsystem.compose.component.WooDivider
import com.woocommerce.android.ui.designsystem.compose.foundation.DefaultWooStroke

@Composable
internal fun PreviewOnlyCatalogSection() {
    PreviewOnlyCatalogContainer(title = "Preview-only catalog") {
        CatalogItem("Segment control: source in progress") {
            PreviewOnlySegmentControlSample()
        }
        CatalogItem("Sheets: modal and navigation ownership") {
            PreviewOnlySheetSample()
        }
        CatalogItem("Bottom tab bar: app shell and back stack ownership") {
            PreviewOnlyTabBarSample()
        }
        CatalogItem("Table: data, sizing, scrolling, selection, and semantics") {
            PreviewOnlyTableSample()
        }
    }
}

@Composable
internal fun PreviewOnlyCatalogScreenshotSection() {
    PreviewOnlyCatalogContainer(title = "Preview-only catalog snapshots") {
        CatalogItem("Segment control") {
            PreviewOnlySegmentControlSample()
        }
        CatalogItem("Sheets") {
            PreviewOnlySheetSample()
        }
        CatalogItem("Bottom tab bar") {
            PreviewOnlyTabBarSample()
        }
        CatalogItem("Table") {
            PreviewOnlyTableSample()
        }
    }
}

@Composable
private fun PreviewOnlyCatalogContainer(
    title: String,
    content: @Composable () -> Unit,
) {
    Surface(
        color = WooTheme.colors.surface.default,
        contentColor = WooTheme.colors.surface.onDefault,
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(WooTheme.padding.padding5),
            verticalArrangement = Arrangement.spacedBy(WooTheme.spacing.space4),
        ) {
            Text(
                text = title,
                color = WooTheme.colors.surface.onDefault,
                style = WooTheme.text.titleMedium.strong,
            )
            content()
        }
    }
}

@Composable
private fun CatalogItem(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(WooTheme.spacing.space2)) {
        Text(
            text = title,
            color = WooTheme.colors.surface.onVariant,
            style = WooTheme.text.labelMedium.emphasized,
        )
        content()
    }
}

@Composable
private fun PreviewOnlySegmentControlSample() {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .border(DefaultWooStroke.extraThin, WooTheme.colors.outlineVariant, RoundedCornerShape(8.dp)),
    ) {
        PreviewOnlySegmentItem(
            text = "Open",
            selected = true,
            modifier = Modifier.weight(1f),
        )
        PreviewOnlySegmentItem(
            text = "Closed",
            selected = false,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun PreviewOnlySheetSample() {
    Surface(
        color = WooTheme.colors.surface.default,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        border = BorderStroke(DefaultWooStroke.extraThin, WooTheme.colors.outlineVariant),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(WooTheme.padding.padding5),
            verticalArrangement = Arrangement.spacedBy(WooTheme.spacing.space3),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(width = 40.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(WooTheme.colors.outlineVariant),
            )
            Text(
                text = "Sheet preview",
                color = WooTheme.colors.surface.onDefault,
                style = WooTheme.text.titleMedium.emphasized,
            )
            Text(
                text = "Modal behavior remains product-screen owned.",
                color = WooTheme.colors.surface.onVariant,
                style = WooTheme.text.bodyMedium.regular,
            )
        }
    }
}

@Composable
private fun PreviewOnlyTabBarSample() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        color = WooTheme.colors.surface.default,
    ) {
        Column {
            WooDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = WooTheme.padding.padding7, vertical = WooTheme.padding.padding2),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                PreviewOnlyTabBarItem(
                    iconRes = R.drawable.ic_menu_dashboard,
                    label = "Home",
                    selected = true,
                )
                PreviewOnlyTabBarItem(
                    iconRes = R.drawable.ic_menu_orders_list,
                    label = "Orders",
                )
                PreviewOnlyTabBarItem(
                    iconRes = R.drawable.ic_menu_products_list,
                    label = "Products",
                )
                PreviewOnlyTabBarItem(
                    iconRes = R.drawable.ic_more_menu_store,
                    label = "Store",
                )
                PreviewOnlyTabBarItem(
                    iconRes = R.drawable.ic_menu_more,
                    label = "More",
                )
            }
        }
    }
}

@Composable
private fun PreviewOnlyTableSample() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(DefaultWooStroke.extraThin, WooTheme.colors.outlineVariant),
    ) {
        PreviewOnlyTableRow("Product", "Stock", "Price", isHeader = true)
        WooDivider()
        PreviewOnlyTableRow("Beanie", "24", "\$18")
        WooDivider()
        PreviewOnlyTableRow("Hoodie", "8", "\$48")
    }
}

@Composable
private fun PreviewOnlySegmentItem(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
) {
    val containerColor = if (selected) {
        WooTheme.colors.secondary
    } else {
        Color.Transparent
    }
    val contentColor = if (selected) {
        WooTheme.colors.onSecondary
    } else {
        WooTheme.colors.surface.onVariant
    }

    Box(
        modifier = modifier
            .background(containerColor)
            .padding(horizontal = WooTheme.padding.padding4, vertical = WooTheme.padding.padding3),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = contentColor,
            style = WooTheme.text.labelLarge.emphasized,
        )
    }
}

@Composable
private fun PreviewOnlyTabBarItem(
    iconRes: Int,
    label: String,
    selected: Boolean = false,
) {
    if (selected) {
        Surface(
            modifier = Modifier.size(48.dp),
            color = WooTheme.colors.primary,
            contentColor = WooTheme.colors.onPrimary,
            shape = MaterialTheme.shapes.large,
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(iconRes),
                    contentDescription = label,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    } else {
        val contentColor = WooTheme.colors.surface.onVariant

        Column(
            modifier = Modifier.size(48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(iconRes),
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
