package com.woocommerce.android.ui.woopos.home.items

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.ShadowType
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosCard
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosItemImage
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosLazyColumn
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosShimmerText
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosComponentSize
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosCornerRadius
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosElevation
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosIconSize
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.home.items.WooPosItemSelectionViewState.Coupon
import com.woocommerce.android.ui.woopos.home.items.WooPosItemSelectionViewState.Product
import com.woocommerce.android.ui.woopos.util.WooPosTestTags
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

@Composable
fun WooPosItemList(
    modifier: Modifier = Modifier,
    state: WooPosContentViewState,
    listState: LazyListState,
    animateItems: Boolean = true,
    headerContent: (@Composable LazyItemScope.() -> Unit)? = null,
    onItemClicked: (item: WooPosItemSelectionViewState) -> Unit,
    onEndOfProductsListReached: () -> Unit,
    onErrorWhilePaginating: @Composable () -> Unit,
) {
    WooPosLazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(WooPosSpacing.Small.value),
        contentPadding = PaddingValues(top = WooPosSpacing.Small.value, bottom = WooPosSpacing.XXSmall.value),
        state = listState,
        withBottomShadow = true,
    ) {
        if (headerContent != null) {
            item(key = "woopos_item_list_header") {
                Box(modifier = if (animateItems) Modifier.animateItem() else Modifier) {
                    headerContent()
                }
            }
        }

        items(
            state.items,
            key = { item -> item.id }
        ) { posItem ->
            val itemModifier = Modifier.then(if (animateItems) Modifier.animateItem() else Modifier)

            when (posItem) {
                is Product.Simple -> {
                    ProductItem(
                        modifier = itemModifier,
                        item = posItem,
                        onItemClicked = onItemClicked
                    )
                }

                is Product.Variable -> {
                    VariableProductItem(
                        modifier = itemModifier,
                        item = posItem,
                        onItemClicked = onItemClicked
                    )
                }

                is Product.Variation -> {
                    VariationItem(
                        modifier = itemModifier,
                        item = posItem,
                        onItemClicked = onItemClicked
                    )
                }

                is Product.VariationSearchResult -> {
                    VariationSearchResultItem(
                        modifier = itemModifier,
                        item = posItem,
                        onItemClicked = onItemClicked
                    )
                }

                is Coupon -> CouponItem(
                    modifier = itemModifier,
                    item = posItem,
                    onItemClicked = onItemClicked
                )
            }
        }

        when (state.paginationState) {
            WooPosPaginationState.Error -> {
                item {
                    onErrorWhilePaginating()
                }
            }

            WooPosPaginationState.Loading -> {
                item {
                    ItemsLoadingItem()
                }
            }

            WooPosPaginationState.None -> {
            }
        }

        item {
            Spacer(modifier = Modifier.height(WooPosSpacing.Gigantic.value))
        }
    }
    InfiniteListHandler(listState, state, onEndOfProductsListReached)
}

@Composable
private fun ProductItem(
    modifier: Modifier = Modifier,
    item: Product.Simple,
    onItemClicked: (item: WooPosItemSelectionViewState) -> Unit
) {
    val itemContentDescription = stringResource(
        id = R.string.woopos_product_item_content_description,
        item.name,
        item.price
    )
    WooPosProductCard(
        modifier = modifier,
        itemContentDescription = itemContentDescription,
        onItemClicked = onItemClicked,
        item = item
    )
}

@Composable
private fun VariableProductItem(
    modifier: Modifier = Modifier,
    item: Product.Variable,
    onItemClicked: (item: WooPosItemSelectionViewState) -> Unit
) {
    val itemContentDescription = stringResource(
        id = R.string.woopos_variable_product_item_content_description_v2,
        item.name
    )
    WooPosProductCard(
        modifier = modifier,
        itemContentDescription = itemContentDescription,
        clickLabelForAccessibility = stringResource(R.string.woopos_add_variable_product_to_cart_accessibility_label),
        onItemClicked = onItemClicked,
        item = item
    )
}

@Composable
private fun VariationItem(
    modifier: Modifier = Modifier,
    item: Product.Variation,
    onItemClicked: (item: WooPosItemSelectionViewState) -> Unit
) {
    val itemContentDescription = stringResource(
        id = R.string.woopos_variation_item_content_description,
        item.name,
        item.price
    )
    WooPosProductCard(
        modifier = modifier,
        itemContentDescription = itemContentDescription,
        onItemClicked = onItemClicked,
        item = item
    )
}

@Composable
private fun VariationSearchResultItem(
    modifier: Modifier = Modifier,
    item: Product.VariationSearchResult,
    onItemClicked: (item: WooPosItemSelectionViewState) -> Unit
) {
    val itemContentDescription = stringResource(
        id = R.string.woopos_variation_item_content_description,
        item.parentProductName,
        item.price
    )
    WooPosProductCard(
        modifier = modifier,
        itemContentDescription = itemContentDescription,
        onItemClicked = onItemClicked,
        item = item
    )
}

@Composable
private fun CouponItem(
    modifier: Modifier = Modifier,
    item: Coupon,
    onItemClicked: (item: WooPosItemSelectionViewState) -> Unit
) {
    val itemContentDescription = stringResource(
        id = R.string.woopos_coupon_item_content_description_v2,
        item.name,
        item.summary
    )
    WooPosCouponCard(modifier, itemContentDescription, onItemClicked, item)
}

@Composable
fun WooPosProductCard(
    modifier: Modifier,
    itemContentDescription: String,
    clickLabelForAccessibility: String = stringResource(R.string.woopos_add_product_to_cart_accessibility_label),
    onItemClicked: (item: WooPosItemSelectionViewState) -> Unit,
    item: Product
) {
    WooPosCard(
        modifier = modifier
            .wrapContentHeight()
            .testTag(WooPosTestTags.PRODUCT_ITEM),
        shape = RoundedCornerShape(WooPosCornerRadius.Medium.value),
        backgroundColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        elevation = WooPosElevation.Medium,
        shadowType = ShadowType.Soft,
    ) {
        Row(
            modifier = Modifier
                .clickable(
                    onClickLabel = clickLabelForAccessibility,
                ) { onItemClicked(item) }
                .height(IntrinsicSize.Min)
                .heightIn(min = WooPosComponentSize.Large.value)
                .clearAndSetSemantics { contentDescription = itemContentDescription }
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProductImage(item)

            Spacer(modifier = Modifier.width(WooPosSpacing.Medium.value))

            ProductInfo(modifier = Modifier.weight(1f), item = item)

            if (item is Product.Variable) {
                Image(
                    modifier = Modifier
                        .padding(end = WooPosSpacing.XLarge.value)
                        .size(WooPosIconSize.Small.value),
                    imageVector = ImageVector.vectorResource(R.drawable.ic_chevron_right_24dp),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(WooPosTheme.colors.onSurfaceVariantHighest),
                )
            }
        }
    }
}

@Composable
private fun ProductInfo(modifier: Modifier, item: Product) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .padding(
                top = WooPosSpacing.Medium.value,
                bottom = WooPosSpacing.Medium.value,
                end = WooPosSpacing.Medium.value
            ),
        verticalArrangement = Arrangement.Center
    ) {
        val title = when (item) {
            is Product.VariationSearchResult -> item.parentProductName
            else -> item.name
        }
        WooPosText(
            text = title,
            style = WooPosTypography.BodyLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(WooPosSpacing.XSmall.value))
        when (item) {
            is Product.Simple -> SimpleProductDetails(item = item)
            is Product.Variable -> VariableProductDetails()
            is Product.Variation -> VariationProductDetails(item = item)
            is Product.VariationSearchResult -> VariationSearchResultDetails(item = item)
        }
    }
}

@Composable
private fun ProductImage(item: Product) {
    WooPosItemImage(
        imageUrl = item.imageUrl,
        modifier = Modifier
            .width(WooPosComponentSize.Large.value)
            .fillMaxHeight()
            .heightIn(min = WooPosComponentSize.Large.value),
        placeholderIcon = ImageVector.vectorResource(R.drawable.ic_inventory_2_24dp),
        placeholderIconSize = 44.dp
    )
}

@Composable
fun WooPosCouponCard(
    modifier: Modifier,
    itemContentDescription: String,
    onItemClicked: (item: WooPosItemSelectionViewState) -> Unit,
    item: Coupon
) {
    WooPosCard(
        modifier = modifier
            .wrapContentHeight(),
        shape = RoundedCornerShape(WooPosCornerRadius.Medium.value),
        backgroundColor = if (item.expiredState is Coupon.ExpiredState.Expired) {
            WooPosTheme.colors.disabledContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerLowest
        },
        elevation = WooPosElevation.Medium,
        shadowType = ShadowType.Soft,
    ) {
        Row(
            modifier = Modifier
                .clickable(
                    enabled = item.expiredState is Coupon.ExpiredState.NotExpired,
                    onClickLabel = stringResource(R.string.woopos_add_coupon_to_cart_accessibility_label)
                ) { onItemClicked(item) }
                .clearAndSetSemantics { contentDescription = itemContentDescription }
                .testTag(WooPosTestTags.COUPON_ADD_TO_CART_BUTTON)
                .height(IntrinsicSize.Min)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CouponImage()

            Spacer(modifier = Modifier.width(WooPosSpacing.Medium.value))

            CouponInfo(item.name, item.summary, item.expiredState)
        }
    }
}

@Composable
private fun CouponInfo(name: String, summary: String, expiredState: Coupon.ExpiredState) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .padding(
                end = WooPosSpacing.Medium.value,
            )
            .padding(vertical = WooPosSpacing.Medium.value),
        verticalArrangement = Arrangement.Center
    ) {
        WooPosText(
            text = name,
            style = WooPosTypography.BodyLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = if (expiredState is Coupon.ExpiredState.Expired) {
                WooPosTheme.colors.onDisabledContainer
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )

        Spacer(modifier = Modifier.height(WooPosSpacing.XSmall.value))

        WooPosText(
            text = summary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = WooPosTypography.BodyLarge,
            color = if (expiredState is Coupon.ExpiredState.Expired) {
                WooPosTheme.colors.onDisabledContainer
            } else {
                WooPosTheme.colors.onSurfaceVariantHighest
            },
        )

        if (expiredState is Coupon.ExpiredState.Expired) {
            Spacer(modifier = Modifier.height(WooPosSpacing.XSmall.value))
            WooPosText(
                text = expiredState.formattedDate?.let {
                    stringResource(R.string.woopos_coupon_item_expired_label, it)
                } ?: stringResource(R.string.coupon_list_item_label_expired),
                maxLines = 1,
                style = WooPosTypography.BodyLarge,
                color = WooPosTheme.colors.onDisabledContainer
            )
        }
    }
}

@Composable
private fun CouponImage() {
    WooPosItemImage(
        imageUrl = null,
        modifier = Modifier
            .width(WooPosComponentSize.Large.value)
            .fillMaxHeight()
            .heightIn(min = WooPosComponentSize.Large.value),
        placeholderIcon = ImageVector.vectorResource(R.drawable.ic_sell_24dp),
        placeholderIconSize = 36.dp
    )
}

@Composable
private fun SimpleProductDetails(item: Product.Simple) {
    WooPosText(
        text = item.price,
        style = WooPosTypography.BodyLarge,
        fontWeight = FontWeight.Normal,
        color = WooPosTheme.colors.onSurfaceVariantHighest,
    )
}

@Composable
private fun VariableProductDetails() {
    WooPosText(
        text = stringResource(id = R.string.woopos_variations_options_available_text),
        style = WooPosTypography.BodyLarge,
        color = WooPosTheme.colors.onSurfaceVariantHighest,
    )
}

@Composable
fun VariationProductDetails(item: Product.Variation) {
    WooPosText(
        text = item.price,
        style = WooPosTypography.BodyLarge,
        color = WooPosTheme.colors.onSurfaceVariantHighest,
    )
}

@Composable
private fun VariationSearchResultDetails(item: Product.VariationSearchResult) {
    WooPosText(
        text = item.name,
        style = WooPosTypography.BodyLarge,
        color = WooPosTheme.colors.onSurfaceVariantHighest,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
    Spacer(modifier = Modifier.height(WooPosSpacing.XSmall.value))
    WooPosText(
        text = item.price,
        style = WooPosTypography.BodyLarge,
        color = WooPosTheme.colors.onSurfaceVariantHighest,
    )
}

@Composable
fun WooPosItemsLoadingIndicator(
    modifier: Modifier = Modifier,
    itemsCount: Int = 10
) {
    WooPosLazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(WooPosSpacing.Small.value),
        contentPadding = PaddingValues(WooPosSpacing.XXSmall.value),
    ) {
        items(itemsCount) {
            ItemsLoadingItem()
        }

        item {
            Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))
        }
    }
}

@Composable
private fun ItemsLoadingItem() {
    WooPosCard(
        shape = RoundedCornerShape(WooPosCornerRadius.Medium.value),
        backgroundColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        elevation = WooPosElevation.Medium,
        shadowType = ShadowType.Soft,
    ) {
        Row(
            modifier = Modifier
                .height(WooPosComponentSize.Large.value)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(WooPosComponentSize.Large.value)
                    .background(WooPosTheme.colors.onSurfaceVariantLowest.copy(alpha = 0.35f))
            )

            Spacer(modifier = Modifier.width(WooPosSpacing.Medium.value))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.Center,
            ) {
                WooPosShimmerText(
                    text = "Product Name",
                    style = WooPosTypography.BodyLarge.style,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.size(WooPosSpacing.XSmall.value))

                WooPosShimmerText(
                    text = "$10.00",
                    style = WooPosTypography.BodyLarge.style
                )
            }

            Spacer(modifier = Modifier.weight(0.5f))
        }
    }
}

@Composable
private fun InfiniteListHandler(
    listState: LazyListState,
    state: WooPosContentViewState,
    onEndOfProductsListReached: () -> Unit
) {
    val buffer = 5 // must be smaller than the page size, otherwise won't call onEndOfProductsListReached
    val loadMore = remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val totalItemsNumber = layoutInfo.totalItemsCount
            val lastVisibleItemIndex = (layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) + 1

            lastVisibleItemIndex > (totalItemsNumber - buffer)
        }
    }

    LaunchedEffect(state.pullToRefreshState) {
        snapshotFlow { loadMore.value }
            .distinctUntilChanged()
            .filter { it }
            .collect {
                onEndOfProductsListReached()
            }
    }
}

@WooPosPreview
@Composable
fun ItemListPreview() {
    WooPosTheme {
        WooPosItemList(
            Modifier,
            WooPosProductsViewState.Content(
                listOf(
                    Product.Simple(
                        id = 1,
                        name = "Simple Product Simple Product Simple" +
                            " Product Simple Product Simple Product Simple Product Simple Product",
                        price = "$10.00",
                        imageUrl = ""
                    ),
                    Product.Variable(
                        id = 2,
                        name = "Variable Product with very loooooooooooooooooooooooooooooooooong",
                        price = "$10.00",
                        "",
                        1,
                        listOf()
                    ),
                    Product.Variation(3, "Variation", "$10", "", 0),
                    Product.VariationSearchResult(
                        id = 6,
                        name = "Blue - Medium",
                        price = "$15.00",
                        imageUrl = "",
                        productId = 1,
                        parentProductName = "Premium T-Shirt",
                    ),
                    Coupon(
                        id = 4,
                        name = "Coupon",
                        summary = "10% off everything",
                        expiredState = Coupon.ExpiredState.NotExpired
                    ),
                    Coupon(
                        id = 5,
                        name = "Expired Coupon",
                        summary = "10% off everything",
                        expiredState = Coupon.ExpiredState.Expired("24 Apr 2025")
                    ),
                ),
            ),
            listState = LazyListState(),
            onItemClicked = {},
            onEndOfProductsListReached = {},
            onErrorWhilePaginating = {}
        )
    }
}

@WooPosPreview
@Composable
fun LoadingListPreview() {
    WooPosTheme {
        WooPosItemsLoadingIndicator(itemsCount = 10)
    }
}
