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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.ShadowType
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosCard
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosLazyColumn
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosShimmerBox
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosCornerRadius
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosElevation
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.toAdaptivePadding
import com.woocommerce.android.ui.woopos.home.items.WooPosItemSelectionViewState.Coupon
import com.woocommerce.android.ui.woopos.home.items.WooPosItemSelectionViewState.Product
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

@Composable
fun WooPosItemList(
    modifier: Modifier = Modifier,
    state: WooPosContentViewState,
    listState: LazyListState,
    animateItems: Boolean = true,
    onItemClicked: (item: WooPosItemSelectionViewState) -> Unit,
    onEndOfProductsListReached: () -> Unit,
    onErrorWhilePaginating: @Composable () -> Unit,
) {
    WooPosLazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(WooPosSpacing.Small.value),
        contentPadding = PaddingValues(2.dp),
        state = listState,
    ) {
        items(
            state.items,
            key = { product -> product.id }
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
            @Suppress("WooPosDesignSystemSpacingUsageRule")
            Spacer(modifier = Modifier.height(104.dp))
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
    WooPosProductCard(modifier, itemContentDescription, onItemClicked, item)
}

@Composable
private fun VariableProductItem(
    modifier: Modifier = Modifier,
    item: Product.Variable,
    onItemClicked: (item: WooPosItemSelectionViewState) -> Unit
) {
    val itemContentDescription = stringResource(
        id = R.string.woopos_variable_product_item_content_description,
        item.name,
        item.price
    )
    WooPosProductCard(modifier, itemContentDescription, onItemClicked, item)
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
    WooPosProductCard(modifier, itemContentDescription, onItemClicked, item)
}

@Composable
private fun CouponItem(
    modifier: Modifier = Modifier,
    item: Coupon,
    onItemClicked: (item: WooPosItemSelectionViewState) -> Unit
) {
    val itemContentDescription = stringResource(
        id = R.string.woopos_coupon_item_content_description,
        item.name,
    )
    WooPosCouponCard(modifier, itemContentDescription, onItemClicked, item)
}

@Composable
fun WooPosProductCard(
    modifier: Modifier,
    itemContentDescription: String,
    onItemClicked: (item: WooPosItemSelectionViewState) -> Unit,
    item: Product
) {
    WooPosCard(
        modifier = modifier
            .wrapContentHeight()
            .semantics { contentDescription = itemContentDescription },
        shape = RoundedCornerShape(WooPosCornerRadius.Medium.value),
        backgroundColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        elevation = WooPosElevation.Medium,
        shadowType = ShadowType.Soft,
    ) {
        Row(
            modifier = Modifier
                .clickable { onItemClicked(item) }
                .height(IntrinsicSize.Min)
                .heightIn(min = 112.dp)
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
                        .size(24.dp),
                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_chevron),
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
        WooPosText(
            text = item.name,
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
        }
    }
}

@Composable
private fun ProductImage(item: Product) {
    Box(
        modifier = Modifier
            .width(112.dp)
            .fillMaxHeight()
            .heightIn(min = 112.dp)
            .background(MaterialTheme.colorScheme.surfaceDim),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.ic_box),
            contentDescription = null,
            colorFilter = ColorFilter.tint(WooPosTheme.colors.onSurfaceVariantLowest),
            modifier = Modifier.size(36.dp, 36.dp)
        )
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(item.imageUrl)
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop
        )
    }
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
            .wrapContentHeight()
            .semantics { contentDescription = itemContentDescription },
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
                .clickable(enabled = item.expiredState is Coupon.ExpiredState.NotExpired) { onItemClicked(item) }
                .height(IntrinsicSize.Min)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CouponImage(item.expiredState)

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
            .padding(vertical = WooPosSpacing.Small.value.toAdaptivePadding()),
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
                text = stringResource(R.string.woopos_coupon_item_expired_label, expiredState.formattedDate),
                maxLines = 1,
                style = WooPosTypography.BodyLarge,
                color = WooPosTheme.colors.onDisabledContainer
            )
        }
    }
}

@Composable
private fun CouponImage(expiredState: Coupon.ExpiredState) {
    Box(
        modifier = Modifier
            .width(112.dp)
            .fillMaxHeight()
            .heightIn(min = 112.dp)
            .background(MaterialTheme.colorScheme.surfaceDim),
        contentAlignment = Alignment.Center
    ) {
        Image(
            imageVector = Icons.Outlined.LocalOffer,
            contentDescription = null,
            colorFilter = if (expiredState is Coupon.ExpiredState.Expired) {
                ColorFilter.tint(WooPosTheme.colors.disabledContainer)
            } else {
                ColorFilter.tint(WooPosTheme.colors.onSurfaceVariantLowest)
            },
            modifier = Modifier.size(36.dp, 36.dp)
        )
    }
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
fun WooPosItemsLoadingIndicator(
    modifier: Modifier = Modifier,
    itemsCount: Int = 10
) {
    WooPosLazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(WooPosSpacing.Small.value),
        contentPadding = PaddingValues(2.dp),
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
                .height(112.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(112.dp)
                    .background(WooPosTheme.colors.onSurfaceVariantLowest.copy(alpha = 0.35f))
            )

            Spacer(modifier = Modifier.width(WooPosSpacing.Medium.value))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.Center,
            ) {
                WooPosShimmerBox(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                        .clip(RoundedCornerShape(WooPosCornerRadius.Medium.value))
                )

                Spacer(modifier = Modifier.size(WooPosSpacing.XSmall.value))

                WooPosShimmerBox(
                    modifier = Modifier
                        .height(32.dp)
                        .width(57.dp)
                        .clip(RoundedCornerShape(WooPosCornerRadius.Medium.value))
                )
            }

            Spacer(modifier = Modifier.weight(0.5f))
        }
    }
}

@Composable
fun WooPosItemsEmptyList(
    modifier: Modifier = Modifier,
    title: String,
    message: String,
    contentDescription: String,
) {
    WooPosItemsEmptyListInternal(
        modifier = modifier,
        title = title,
        message = message,
        contentDescription = contentDescription,
        actionLabel = null,
        onActionClicked = null
    )
}

@Composable
fun WooPosItemsEmptyList(
    modifier: Modifier = Modifier,
    title: String,
    message: String,
    contentDescription: String,
    actionLabel: String,
    onActionClicked: (() -> Unit),
) {
    WooPosItemsEmptyListInternal(
        modifier = modifier,
        title = title,
        message = message,
        contentDescription = contentDescription,
        actionLabel = actionLabel,
        onActionClicked = onActionClicked
    )
}

@Composable
private fun WooPosItemsEmptyListInternal(
    modifier: Modifier = Modifier,
    title: String,
    message: String,
    contentDescription: String,
    actionLabel: String?,
    onActionClicked: (() -> Unit)? = null,
) {
    Box(
        modifier = modifier.verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                modifier = Modifier.size(104.dp),
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_not_found),
                contentDescription = contentDescription,
            )

            Spacer(modifier = Modifier.height(WooPosSpacing.XLarge.value.toAdaptivePadding()))

            WooPosText(
                text = title,
                style = WooPosTypography.Heading,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value.toAdaptivePadding()))

            WooPosText(
                text = message,
                style = WooPosTypography.BodyLarge,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(WooPosSpacing.XLarge.value.toAdaptivePadding()))

            if (onActionClicked != null && actionLabel != null) {
                WooPosButton(
                    text = actionLabel,
                    onClick = onActionClicked,
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(80.dp)
                )
            }
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
                    Coupon(
                        id = 4,
                        name = "Coupon",
                        summary = "10% off everything",
                        expiredState = Coupon.ExpiredState.NotExpired
                    ),
                    Coupon(
                        id = 4,
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
fun EmptyListPreview() {
    WooPosTheme {
        WooPosItemsEmptyList(
            modifier = Modifier.fillMaxSize(),
            title = "Empty List",
            message = "This list is empty",
            contentDescription = ""
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
