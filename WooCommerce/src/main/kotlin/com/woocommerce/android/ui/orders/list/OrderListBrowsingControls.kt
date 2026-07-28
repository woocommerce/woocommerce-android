package com.woocommerce.android.ui.orders.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.designsystem.WooTheme
import com.woocommerce.android.ui.compose.designsystem.component.WooButtonSize
import com.woocommerce.android.ui.compose.designsystem.component.WooDivider
import com.woocommerce.android.ui.compose.designsystem.component.WooFilledButton
import com.woocommerce.android.ui.compose.designsystem.component.WooOutlinedButton

@Composable
internal fun OrderListBrowsingControls(
    filterCount: Int,
    lastUpdate: String?,
    onFiltersClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(WooTheme.colors.surface.default)
            .testTag(OrderListScreenTestTags.BROWSING_CONTROLS),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = WooTheme.padding.padding5,
                    vertical = WooTheme.padding.padding3,
                ),
            horizontalArrangement = Arrangement.spacedBy(WooTheme.spacing.space3),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(WooTheme.spacing.space1),
            ) {
                Text(
                    text = stringResource(
                        if (filterCount > 0) {
                            R.string.orderfilters_filter_card_title_filtered_orders
                        } else {
                            R.string.orderfilters_filter_card_title_all_orders
                        }
                    ),
                    modifier = Modifier.semantics { heading() },
                    color = WooTheme.colors.surface.onDefault,
                    style = WooTheme.text.titleSmall.strong,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                lastUpdate?.takeIf(String::isNotBlank)?.let { value ->
                    Text(
                        text = value,
                        color = colorResource(R.color.color_on_surface_medium),
                        style = WooTheme.text.bodySmall.regular,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (filterCount > 0) {
                WooFilledButton(
                    text = stringResource(R.string.product_list_filters_selected, filterCount),
                    onClick = onFiltersClicked,
                    size = WooButtonSize.Small,
                    modifier = Modifier.testTag(OrderListScreenTestTags.FILTERS),
                )
            } else {
                WooOutlinedButton(
                    text = stringResource(R.string.product_list_filters),
                    onClick = onFiltersClicked,
                    size = WooButtonSize.Small,
                    modifier = Modifier.testTag(OrderListScreenTestTags.FILTERS),
                )
            }
        }
        WooDivider()
    }
}
