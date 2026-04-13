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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosComponentSize
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosCornerRadius
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosElevation
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosIcons
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.home.items.WooPosItemList
import com.woocommerce.android.ui.woopos.home.items.WooPosItemSelectionViewState.Product
import com.woocommerce.android.ui.woopos.home.items.WooPosPaginationState
import com.woocommerce.android.ui.woopos.home.items.WooPosProductsViewState
import com.woocommerce.android.ui.woopos.home.items.WooPosPullToRefreshState

@Composable
fun WooPosPaginationErrorIndicator(
    modifier: Modifier = Modifier,
    icon: ImageVector? = WooPosIcons.ErrorX,
    message: String,
    description: String,
    primaryButton: WooPosErrorScreenButtonState,
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
    icon: ImageVector? = null,
    message: String,
    description: String,
    primaryButton: WooPosErrorScreenButtonState
) {
    val itemContentDescription =
        stringResource(id = R.string.woopos_items_pagination_error_content_description)

    WooPosCard(
        modifier = modifier.semantics { contentDescription = itemContentDescription },
        shape = RoundedCornerShape(WooPosCornerRadius.Medium.value),
        elevation = WooPosElevation.Medium,
        shadowType = ShadowType.Soft,
    ) {
        Row(
            modifier = Modifier
                .height(WooPosComponentSize.Large.value)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(WooPosSpacing.Medium.value)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(WooPosSpacing.Medium.value),
                modifier = Modifier.weight(1f)
            ) {
                if (icon != null) {
                    Box(
                        modifier = Modifier.size(WooPosComponentSize.Large.value)
                    ) {
                        Icon(
                            modifier = Modifier
                                .size(WooPosComponentSize.XSmall.value)
                                .align(Alignment.Center),
                            imageVector = icon,
                            contentDescription = stringResource(R.string.woopos_error_icon_content_description),
                            tint = WooPosTheme.colors.unspecified,
                        )
                    }
                }

                Column(
                    modifier = if (icon == null) {
                        Modifier.padding(start = WooPosSpacing.Medium.value)
                    } else {
                        Modifier
                    }
                ) {
                    WooPosText(
                        text = message,
                        style = WooPosTypography.BodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    WooPosText(
                        text = description,
                        style = WooPosTypography.BodyMedium,
                        modifier = Modifier.padding(top = WooPosSpacing.Small.value)
                    )
                }
            }

            WooPosOutlinedButton(
                text = primaryButton.text,
                onClick = primaryButton.click,
                modifier = Modifier
                    .padding(end = WooPosSpacing.Medium.value)
            )
        }
    }
}

@Composable
@WooPosPreview
fun WooPosPaginationErrorScreenPreview() {
    val itemsState =
        WooPosProductsViewState.Content(
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
            paginationState = WooPosPaginationState.Error,
            pullToRefreshState = WooPosPullToRefreshState.Refreshing,
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
                primaryButton = WooPosErrorScreenButtonState(
                    text = stringResource(id = R.string.woopos_items_pagination_try_again_label),
                    click = {}
                )
            )
        }
    }
}
