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
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.ProductThumbnail
import com.woocommerce.android.ui.compose.preview.LightDarkThemePreviews
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground

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
    subtitle: @Composable ColumnScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        ProductThumbnail(
            imageUrl = imageUrl.orEmpty(),
            contentDescription = imageContentDescription,
            modifier = Modifier.padding(top = 12.dp),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.padding(bottom = 4.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = if (trailingContent == null) 0.dp else 8.dp),
                    text = title,
                    color = MaterialTheme.colors.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.subtitle1,
                )
                trailingContent?.invoke(this)
            }
            subtitle()
            if (displayDivider) {
                Divider(
                    modifier = Modifier.padding(top = 4.dp),
                    color = colorResource(id = R.color.divider_color),
                )
            }
        }
    }
}

@Composable
fun ProductSummaryRowInfo(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        modifier = modifier,
        text = text,
        color = colorResource(id = R.color.color_on_surface_medium),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = MaterialTheme.typography.caption,
    )
}

@LightDarkThemePreviews
@Composable
private fun ProductSummaryRowPreview() {
    WooThemeWithBackground {
        ProductSummaryRow(
            title = "Woo socks",
            imageUrl = null,
            onClick = {},
        ) {
            ProductSummaryRowInfo("In stock \u2022 \$9.99")
            ProductSummaryRowInfo("SKU: woo-socks")
        }
    }
}
