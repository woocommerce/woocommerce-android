package com.woocommerce.android.ui.woopos.common.composeui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosCornerRadius
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosElevation
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.toAdaptivePadding
import com.woocommerce.android.ui.woopos.home.items.PaginationState
import com.woocommerce.android.ui.woopos.home.items.WooPosItem.Product
import com.woocommerce.android.ui.woopos.home.items.WooPosItemList
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsViewState

@Composable
fun WooPosPaginationErrorIndicator(
    modifier: Modifier = Modifier,
    icon: Painter = painterResource(id = R.drawable.ic_woo_pos_error),
    message: String,
    description: String,
    primaryButton: Button,
) {
    WooPosPaginationErrorIndicatorContent(
        modifier = modifier,
        icon = icon,
        message = message,
        description = description,
        primaryButton = primaryButton
    )
}

@Composable
private fun WooPosPaginationErrorIndicatorContent(
    modifier: Modifier,
    icon: Painter,
    message: String,
    description: String,
    primaryButton: Button
) {
    val itemContentDescription = stringResource(id = R.string.woopos_items_pagination_error_content_description)
    WooPosCard(
        modifier = modifier
            .semantics { contentDescription = itemContentDescription },
        shape = RoundedCornerShape(WooPosCornerRadius.Medium.value),
        elevation = WooPosElevation.Medium,
        shadowType = ShadowType.Soft,
    ) {
        Row(
            modifier = Modifier
                .height(112.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(WooPosSpacing.Medium.value)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(WooPosSpacing.Medium.value),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(112.dp)
                ) {
                    Icon(
                        modifier = Modifier
                            .size(54.dp)
                            .align(Alignment.Center),
                        painter = icon,
                        contentDescription = stringResource(R.string.woopos_error_icon_content_description),
                        tint = WooPosTheme.colors.unspecified,
                    )
                }
                Column {
                    WooPosText(
                        text = message,
                        style = WooPosTypography.BodyLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                    WooPosText(
                        text = description,
                        style = WooPosTypography.BodyMedium,
                        modifier = Modifier.padding(top = WooPosSpacing.Small.value.toAdaptivePadding()),
                        maxLines = 1,
                    )
                }
            }

            WooPosOutlinedButton(
                text = primaryButton.text,
                onClick = primaryButton.click,
                modifier = Modifier
                    .padding(end = WooPosSpacing.Medium.value.toAdaptivePadding())
            )
        }
    }
}

@Composable
@WooPosPreview
fun WooPosPaginationErrorScreenPreview() {
    val itemsState =
        WooPosItemsViewState.Content(
            items = listOf(
                Product.Simple(
                    1,
                    name = "Product 1, Product 1, Product 1, " +
                        "Product 1, Product 1, Product 1, Product 1, Product 1" +
                        "Product 1, Product 1, Product 1, Product 1, Product 1",
                    price = "10.0$",
                    imageUrl = null,
                ),
                Product.Simple(
                    2,
                    name = "Product 2",
                    price = "2000.00$",
                    imageUrl = null,
                ),
                Product.Variable(
                    3,
                    name = "Product 3",
                    price = "2000.00$",
                    imageUrl = null,
                    numOfVariations = 20,
                    variationIds = listOf()
                ),
            ),
            paginationState = PaginationState.Error,
            reloadingProductsWithPullToRefresh = true,
            bannerState = WooPosItemsViewState.Content.BannerState(
                isBannerHiddenByUser = true,
                title = R.string.woopos_banner_simple_products_only_title,
                message = R.string.woopos_banner_simple_products_only_message,
                icon = R.drawable.info,
            ),
            search = WooPosItemsViewState.Content.SearchState.Hidden
        )
    WooPosTheme {
        WooPosItemList(
            state = itemsState,
            listState = rememberLazyListState(),
            onItemClicked = {},
            onEndOfProductsListReached = {}
        ) {
            WooPosPaginationErrorIndicator(
                message = stringResource(id = R.string.woopos_items_pagination_error_title),
                description = stringResource(id = R.string.woopos_items_pagination_error_description),
                primaryButton = Button(
                    text = stringResource(id = R.string.woopos_items_pagination_try_again_label),
                    click = {}
                )
            )
        }
    }
}
