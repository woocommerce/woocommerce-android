package com.woocommerce.android.ui.orders.creation.views

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.woocommerce.android.R
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.compose.animations.SkeletonView
import com.woocommerce.android.ui.compose.component.ProductThumbnail
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.orders.creation.OrderCreateEditViewModel
import com.woocommerce.android.ui.orders.creation.OrderCreationProduct
import com.woocommerce.android.ui.orders.creation.ProductInfo
import com.woocommerce.android.ui.products.ProductStockStatus
import com.woocommerce.android.ui.products.ProductType
import com.woocommerce.android.util.getVariationAttributesAndStockText
import java.math.BigDecimal

@Composable
fun ExpandableGroupedProductCard(
    state: State<OrderCreateEditViewModel.ViewState?>,
    product: OrderCreationProduct,
    children: List<OrderCreationProduct.ProductItem>,
    onRemoveProductClicked: () -> Unit,
    onDiscountButtonClicked: () -> Unit,
    onItemAmountChanged: (ProductAmountEvent) -> Unit,
    onEditConfigurationClicked: () -> Unit,
    onProductExpanded: (isExpanded: Boolean, product: OrderCreationProduct) -> Unit,
    onChildProductExpanded: (isExpanded: Boolean, product: OrderCreationProduct) -> Unit,
    modifier: Modifier = Modifier,
    isExpanded: Boolean = false
) {
    Column(
        modifier = Modifier
            .padding(
                horizontal = dimensionResource(id = R.dimen.major_100),
                vertical = dimensionResource(id = R.dimen.minor_50)
            )
            .border(
                1.dp,
                colorResource(id = R.color.divider_color),
                shape = RoundedCornerShape(dimensionResource(id = R.dimen.corner_radius_large))
            )
    ) {
        ExpandableProductCard(
            state = state,
            product = product,
            onRemoveProductClicked = onRemoveProductClicked,
            onDiscountButtonClicked = onDiscountButtonClicked,
            onEditConfigurationClicked = onEditConfigurationClicked,
            onProductExpanded = onProductExpanded,
            onItemAmountChanged = onItemAmountChanged,
            modifier = modifier,
            isExpanded = isExpanded
        )

        if (isExpanded.not()) { Divider() }

        children.forEachIndexed { index, child ->
            var isChildrenExpanded by rememberSaveable { mutableStateOf(false) }

            val childrenModifier = if (isChildrenExpanded) {
                val shape = if (index != children.lastIndex) {
                    RectangleShape
                } else {
                    RoundedCornerShape(
                        bottomStart = dimensionResource(id = R.dimen.corner_radius_large),
                        bottomEnd = dimensionResource(id = R.dimen.corner_radius_large)
                    )
                }
                Modifier.border(
                    1.dp,
                    colorResource(id = R.color.color_on_surface),
                    shape = shape
                )
            } else {
                Modifier
            }

            if (isChildrenExpanded.not() && index != 0) {
                Divider(
                    modifier = Modifier
                        .padding(horizontal = dimensionResource(id = R.dimen.major_100))
                )
            }

            ExpandableChildrenProductCard(
                product = child,
                onProductExpanded = { expanded, childExpanded ->
                    isChildrenExpanded = expanded
                    onChildProductExpanded(isChildrenExpanded, childExpanded)
                },
                isExpanded = isChildrenExpanded,
                modifier = childrenModifier
            )

            if (isChildrenExpanded.not() && index == children.lastIndex) {
                Spacer(Modifier.padding(dimensionResource(id = R.dimen.minor_50)))
            }
        }
    }
}

@Composable
fun ExpandableGroupedProductCardLoading(
    state: State<OrderCreateEditViewModel.ViewState?>,
    product: OrderCreationProduct,
    childrenSize: Int,
    onRemoveProductClicked: () -> Unit,
    onDiscountButtonClicked: () -> Unit,
    onItemAmountChanged: (ProductAmountEvent) -> Unit,
    onEditConfigurationClicked: () -> Unit,
    onProductExpanded: (isExpanded: Boolean, product: OrderCreationProduct) -> Unit,
    modifier: Modifier = Modifier,
    isExpanded: Boolean = false
) {
    Column(
        modifier = Modifier
            .padding(
                horizontal = dimensionResource(id = R.dimen.major_100),
                vertical = dimensionResource(id = R.dimen.minor_50)
            )
            .border(
                1.dp,
                colorResource(id = R.color.divider_color),
                shape = RoundedCornerShape(dimensionResource(id = R.dimen.corner_radius_large))
            )
    ) {
        ExpandableProductCard(
            state = state,
            product = product,
            onRemoveProductClicked = onRemoveProductClicked,
            onDiscountButtonClicked = onDiscountButtonClicked,
            onItemAmountChanged = onItemAmountChanged,
            onEditConfigurationClicked = onEditConfigurationClicked,
            onProductExpanded = onProductExpanded,
            modifier = modifier,
            isExpanded = isExpanded
        )

        if (isExpanded.not()) {
            Divider()
        }

        for (i in 1..childrenSize) {
            if (i != 1) {
                Divider(
                    modifier = Modifier
                        .padding(horizontal = dimensionResource(id = R.dimen.major_100))
                )
            }
            ExpandableChildrenSkeleton()
        }
    }
}

@Composable
fun ExpandableChildrenSkeleton(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(dimensionResource(id = R.dimen.major_100)),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.major_85))
    ) {
        SkeletonView(
            dimensionResource(id = R.dimen.skeleton_image_dimension),
            dimensionResource(id = R.dimen.skeleton_image_dimension)
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.minor_100))
        ) {
            SkeletonView(
                dimensionResource(id = R.dimen.skeleton_text_large_width),
                dimensionResource(id = R.dimen.major_200)
            )
            SkeletonView(
                dimensionResource(id = R.dimen.skeleton_text_extra_large_width),
                dimensionResource(id = R.dimen.major_150)
            )
        }
    }
}

@SuppressLint("UnusedTransitionTargetStateParameter")
@Composable
fun ExpandableChildrenProductCard(
    product: OrderCreationProduct,
    onProductExpanded: (isExpanded: Boolean, product: OrderCreationProduct) -> Unit,
    modifier: Modifier = Modifier,
    isExpanded: Boolean = false
) {
    val transitionState = remember {
        MutableTransitionState(isExpanded).apply { targetState = !isExpanded }
    }
    val transition = updateTransition(transitionState, "expandableChildProductCard")
    val chevronRotation by transition.animateFloat(
        transitionSpec = { tween(durationMillis = ANIM_DURATION_MILLIS) },
        label = "childChevronRotation"
    ) {
        if (isExpanded) 180f else 0f
    }
    ConstraintLayout(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                onProductExpanded(!isExpanded, product)
            }
            .then(modifier)
    ) {
        val (img, name, stock, sku, quantity, discount, price, chevron, expandedPart) = createRefs()
        val collapsedStateBottomBarrier = createBottomBarrier(sku, quantity)
        ProductThumbnail(
            modifier = Modifier
                .constrainAs(img) {
                    top.linkTo(name.top)
                    start.linkTo(parent.start)
                    bottom.linkTo(collapsedStateBottomBarrier)
                }
                .padding(start = dimensionResource(id = R.dimen.major_200))
                .size(dimensionResource(R.dimen.major_250)),
            imageUrl = product.productInfo.imageUrl
        )
        Text(
            text = product.item.name,
            modifier = Modifier
                .constrainAs(name) {
                    top.linkTo(parent.top)
                    start.linkTo(img.end)
                    end.linkTo(chevron.start)
                    width = Dimension.fillToConstraints
                }
                .padding(
                    start = dimensionResource(id = R.dimen.major_100),
                    end = dimensionResource(id = R.dimen.major_100),
                    top = dimensionResource(id = R.dimen.minor_100),
                ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colors.onSurface
        )
        Text(
            text = product.getVariationAttributesAndStockText(LocalContext.current),
            modifier = Modifier
                .constrainAs(stock) {
                    start.linkTo(name.start)
                    end.linkTo(discount.start)
                    top.linkTo(name.bottom)
                    width = Dimension.fillToConstraints
                }
                .padding(horizontal = dimensionResource(id = R.dimen.major_100)),
            style = MaterialTheme.typography.body2,
            color = colorResource(id = R.color.color_on_surface_disabled)
        )
        if (isExpanded) {
            Text(
                text = stringResource(
                    id = R.string.orderdetail_product_lineitem_sku_value,
                    product.item.sku
                ),
                modifier = Modifier
                    .constrainAs(sku) {
                        start.linkTo(name.start)
                        top.linkTo(stock.bottom)
                    }
                    .padding(
                        start = dimensionResource(id = R.dimen.major_100),
                        end = dimensionResource(id = R.dimen.major_100),
                        bottom = dimensionResource(id = R.dimen.major_100),
                    ),
                style = MaterialTheme.typography.body2,
                color = colorResource(id = R.color.color_on_surface_disabled)
            )
        } else {
            Text(
                modifier = Modifier
                    .constrainAs(quantity) {
                        start.linkTo(name.start)
                        top.linkTo(stock.bottom)
                    }
                    .padding(
                        start = dimensionResource(id = R.dimen.major_100),
                        end = dimensionResource(id = R.dimen.major_100),
                        bottom = dimensionResource(id = R.dimen.minor_100),
                    ),
                style = MaterialTheme.typography.body2,
                text = getQuantityWithTotalText(product),
                color = colorResource(id = R.color.color_on_surface_disabled)
            )
            val priceColor = if (product.item.price.compareTo(BigDecimal.ZERO) == 1) {
                MaterialTheme.colors.onSurface
            } else {
                colorResource(id = R.color.color_on_surface_disabled)
            }

            Text(
                modifier = Modifier.constrainAs(price) {
                    end.linkTo(chevron.start)
                    top.linkTo(quantity.top)
                },
                style = MaterialTheme.typography.body2,
                text = product.productInfo.priceAfterDiscount,
                color = priceColor
            )
        }
        IconButton(
            onClick = {
                onProductExpanded(!isExpanded, product)
            },
            modifier = Modifier.constrainAs(chevron) {
                top.linkTo(parent.top)
                end.linkTo(parent.end)
            }
        ) {
            Icon(
                modifier = Modifier.rotate(chevronRotation),
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription =
                stringResource(R.string.order_creation_collapse_expand_product_card_content_description),
                tint = colorResource(id = R.color.color_on_surface_disabled)
            )
        }
        AnimatedVisibility(
            visible = isExpanded,
            modifier = Modifier
                .constrainAs(expandedPart) {
                    bottom.linkTo(parent.bottom)
                    top.linkTo(collapsedStateBottomBarrier)
                }
                .fillMaxWidth(),
            enter = slideInVertically() + expandVertically(expandFrom = Alignment.Top) +
                fadeIn(initialAlpha = 0.3f),
            exit = fadeOut() + shrinkVertically()
        ) {
            Divider(
                modifier = Modifier
                    .padding(horizontal = dimensionResource(id = R.dimen.major_100))
            )

            Row(modifier = Modifier.padding(dimensionResource(id = R.dimen.major_100))) {
                Text(
                    text = stringResource(id = R.string.product_price),
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colors.onSurface
                )
                Text(
                    modifier = Modifier.padding(end = dimensionResource(id = R.dimen.major_100)),
                    color = colorResource(id = R.color.color_on_surface_disabled),
                    text = getQuantityWithTotalText(product)
                )
                val totalAmountStyle = if (product.productInfo.hasDiscount) {
                    MaterialTheme.typography.body1.copy(
                        textDecoration = TextDecoration.LineThrough,
                        color = colorResource(id = R.color.color_on_surface_disabled)
                    )
                } else {
                    MaterialTheme.typography.body1.copy(color = MaterialTheme.colors.onSurface)
                }
                Text(text = product.productInfo.priceSubtotal, style = totalAmountStyle)
            }
        }
    }
}

@Preview
@Composable
fun ExpandableChildrenProductCardPreview() {
    val item = Order.Item.EMPTY.copy(
        name = "Test Product",
        quantity = 3.0f,
        total = 23.toBigDecimal(),
        subtotal = 30.toBigDecimal(),
        sku = "SKU123"
    )
    val product = OrderCreationProduct.ProductItem(
        item = item,
        productInfo = ProductInfo(
            imageUrl = "",
            isStockManaged = true,
            stockQuantity = 3.0,
            stockStatus = ProductStockStatus.InStock,
            pricePreDiscount = "$10",
            priceTotal = "$30",
            priceSubtotal = "$25",
            discountAmount = "$5",
            priceAfterDiscount = "$25",
            isConfigurable = true,
            productType = ProductType.SIMPLE,
            hasDiscount = true
        )
    )
    WooThemeWithBackground {
        ExpandableChildrenProductCard(product, { _, _ -> })
    }
}
