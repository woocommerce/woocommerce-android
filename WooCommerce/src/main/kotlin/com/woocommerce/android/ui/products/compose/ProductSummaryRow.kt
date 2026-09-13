package com.woocommerce.android.ui.products.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.ProductThumbnail
import com.woocommerce.android.ui.compose.designsystem.WooTheme
import com.woocommerce.android.ui.compose.designsystem.component.WooDivider
import com.woocommerce.android.ui.compose.designsystem.foundation.WooDesignSystemThemeWithBackground

@Composable
fun ProductSummaryRow(
    title: String,
    imageUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    displayDivider: Boolean = false,
    imageContentDescription: String = stringResource(id = R.string.product_image_content_description),
    trailingContent: (@Composable RowScope.() -> Unit)? = null,
    supportingContent: @Composable ColumnScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = WooTheme.padding.padding5),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProductThumbnail(
            imageUrl = imageUrl.orEmpty(),
            contentDescription = imageContentDescription,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = WooTheme.padding.padding3),
            verticalArrangement = Arrangement.Center,
        ) {
            Spacer(modifier = Modifier.height(WooTheme.spacing.space4))
            Row(
                modifier = Modifier.padding(bottom = WooTheme.padding.padding2),
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = if (trailingContent == null) 0.dp else WooTheme.padding.padding3),
                    text = title,
                    color = WooTheme.colors.surface.onDefault,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = WooTheme.text.titleMedium.regular,
                )
                trailingContent?.invoke(this)
            }
            supportingContent()
            Spacer(modifier = Modifier.height(WooTheme.spacing.space4))
            if (displayDivider) {
                WooDivider()
            }
        }
    }
}

@Composable
fun ProductSummaryRowInfo(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = WooTheme.colors.surface.onDefault,
    maxLines: Int = 1,
    overflow: TextOverflow = TextOverflow.Ellipsis,
    style: TextStyle = WooTheme.text.bodyMedium.regular,
) {
    Text(
        modifier = modifier,
        text = text,
        color = color,
        maxLines = maxLines,
        overflow = overflow,
        style = style,
    )
}

@PreviewLightDark
@Composable
private fun ProductSummaryRowPreview() {
    WooDesignSystemThemeWithBackground {
        ProductSummaryRow(
            title = "Woo socks",
            imageUrl = "https://example.com/socks.png",
            onClick = {},
        ) {
            ProductSummaryRowInfo("In stock \u2022 \$9.99")
            ProductSummaryRowInfo("SKU: woo-socks")
        }
    }
}
