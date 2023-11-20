package com.woocommerce.android.ui.orders.creation.views

import android.annotation.SuppressLint
import android.content.res.Configuration
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Remove
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.woocommerce.android.R
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.compose.component.ProductThumbnail
import com.woocommerce.android.ui.compose.component.WCTextButton
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.orders.creation.OrderCreateEditViewModel
import com.woocommerce.android.ui.orders.creation.OrderCreationProduct
import com.woocommerce.android.ui.orders.creation.ProductInfo
import com.woocommerce.android.ui.products.ProductStockStatus
import com.woocommerce.android.ui.products.ProductType
import com.woocommerce.android.util.getStockText
import java.math.BigDecimal

private const val ANIM_DURATION_MILLIS = 128
private const val MULTIPLICATION_CHAR = "×"

@SuppressLint("UnusedTransitionTargetStateParameter")
@Composable
fun ExpandableProductCard(
    state: State<OrderCreateEditViewModel.ViewState?>,
    product: OrderCreationProduct,
    onRemoveProductClicked: () -> Unit,
    onDiscountButtonClicked: () -> Unit,
    onIncreaseItemAmountClicked: () -> Unit,
    onDecreaseItemAmountClicked: () -> Unit,
    onEditConfigurationClicked: () -> Unit,
    onProductExpanded: (isExpanded: Boolean, product: OrderCreationProduct) -> Unit
) {
    var isExpanded by rememberSaveable { mutableStateOf(false) }
    val transitionState = remember {
        MutableTransitionState(isExpanded).apply { targetState = !isExpanded }
    }
    val transition = updateTransition(transitionState, "expandableProductCard")
    val chevronRotation by transition.animateFloat(
        transitionSpec = { tween(durationMillis = ANIM_DURATION_MILLIS) }, label = "chevronRotation"
    ) {
        if (isExpanded) 180f else 0f
    }
    ConstraintLayout(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = dimensionResource(id = R.dimen.major_100),
                vertical = dimensionResource(id = R.dimen.minor_50)
            )
            .border(
                1.dp,
                colorResource(id = if (isExpanded) R.color.color_on_surface else R.color.divider_color),
                shape = RoundedCornerShape(dimensionResource(id = R.dimen.corner_radius_large))
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                isExpanded = !isExpanded
                onProductExpanded(isExpanded, product)
            }
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
                .size(dimensionResource(R.dimen.major_375))
                .padding(dimensionResource(id = R.dimen.major_100)),
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
                    top = dimensionResource(id = R.dimen.major_100),
                ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colors.onSurface
        )
        Text(
            text = product.getStockText(LocalContext.current),
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
        if (!isExpanded && product.productInfo.hasDiscount) {
            Text(
                modifier = Modifier
                    .constrainAs(discount) {
                        end.linkTo(chevron.start)
                        top.linkTo(stock.top)
                        start.linkTo(stock.end)
                    },
                text = "-${product.productInfo.discountAmount}",
                style = MaterialTheme.typography.body2,
                color = colorResource(id = R.color.woo_green_50)
            )
        }
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
                        bottom = dimensionResource(id = R.dimen.major_100),
                    ),
                style = MaterialTheme.typography.body2,
                text = getQuantityWithTotalText(product),
                color = colorResource(id = R.color.color_on_surface_disabled)
            )
            Text(
                modifier = Modifier.constrainAs(price) {
                    end.linkTo(chevron.start)
                    top.linkTo(quantity.top)
                },
                style = MaterialTheme.typography.body2,
                text = product.productInfo.priceAfterDiscount,
                color = MaterialTheme.colors.onSurface
            )
        }
        IconButton(
            onClick = {
                isExpanded = !isExpanded
                onProductExpanded(isExpanded, product)
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
                tint = MaterialTheme.colors.primary
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
            ExtendedProductCardContent(
                state,
                product,
                onRemoveProductClicked,
                onDiscountButtonClicked,
                onIncreaseItemAmountClicked,
                onDecreaseItemAmountClicked,
                onEditConfigurationClicked
            )
        }
    }
}

@Composable
fun ExtendedProductCardContent(
    state: State<OrderCreateEditViewModel.ViewState?>,
    product: OrderCreationProduct,
    onRemoveProductClicked: () -> Unit,
    onDiscountButtonClicked: () -> Unit,
    onIncreaseItemAmountClicked: () -> Unit,
    onDecreaseItemAmountClicked: () -> Unit,
    onEditConfigurationClicked: () -> Unit
) {
    ConstraintLayout(
        modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.minor_100))
    ) {
        val (
            topDivider,
            bottomDivider,
            orderCount,
            price,
            discountButton,
            discountAmount,
            priceAfterDiscountLabel,
            priceAfterDiscountValue,
            removeButton,
            configurationButton,
            configurationDivider
        ) = createRefs()

        val buttonBarrier = createTopBarrier(removeButton, configurationButton)

        val editableControlsEnabled = state.value?.isIdle == true
        // The logic to update bundled products quantity is complex so we need to prevent any change while we are
        // updating the bundle and inner products quantity
        val isBundledProduct = product.productInfo.productType == ProductType.BUNDLE

        Divider(
            modifier = Modifier
                .constrainAs(topDivider) {
                    top.linkTo(parent.top)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
                .padding(horizontal = dimensionResource(id = R.dimen.minor_100))
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = dimensionResource(id = R.dimen.major_100),
                    start = dimensionResource(id = R.dimen.minor_100),
                    end = dimensionResource(id = R.dimen.minor_100),
                )
                .constrainAs(orderCount) {
                    top.linkTo(topDivider.bottom)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Order count",
                color = MaterialTheme.colors.onSurface
            )
            AmountPicker(
                onIncreaseClicked = onIncreaseItemAmountClicked,
                onDecreaseClicked = onDecreaseItemAmountClicked,
                product = product,
                isDecreaseButtonEnabled = (editableControlsEnabled && isBundledProduct) || isBundledProduct.not(),
                isIncreaseButtonEnabled = (editableControlsEnabled && isBundledProduct) || isBundledProduct.not()
            )
        }
        Row(
            modifier = Modifier
                .constrainAs(price) {
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    top.linkTo(orderCount.bottom)
                }
                .padding(
                    start = dimensionResource(id = R.dimen.minor_100),
                    end = dimensionResource(id = R.dimen.minor_100),
                    top = dimensionResource(id = R.dimen.major_100),
                    bottom = dimensionResource(id = R.dimen.minor_100)
                ),
        ) {
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
        if (product.productInfo.hasDiscount) {
            WCTextButton(
                modifier = Modifier.constrainAs(discountButton) {
                    top.linkTo(price.bottom)
                },
                onClick = onDiscountButtonClicked,
                enabled = editableControlsEnabled
            ) {
                Text(
                    text = stringResource(id = R.string.discount),
                    style = MaterialTheme.typography.body1,
                )
                Spacer(Modifier.width(dimensionResource(id = R.dimen.minor_100)))
                Icon(
                    modifier = Modifier.size(dimensionResource(id = R.dimen.image_minor_40)),
                    imageVector = Icons.Filled.Edit,
                    contentDescription = null
                )
            }
            Text(
                modifier = Modifier
                    .padding(horizontal = dimensionResource(id = R.dimen.minor_100))
                    .constrainAs(discountAmount) {
                        end.linkTo(parent.end)
                        top.linkTo(discountButton.top)
                        bottom.linkTo(discountButton.bottom)
                    },
                text = "-${product.productInfo.discountAmount}",
                color = colorResource(id = R.color.woo_green_50)
            )
            Text(
                modifier = Modifier
                    .constrainAs(priceAfterDiscountLabel) {
                        top.linkTo(discountButton.bottom)
                        start.linkTo(parent.start)
                        bottom.linkTo(bottomDivider.top)
                    }
                    .padding(dimensionResource(id = R.dimen.minor_100)),
                text = stringResource(R.string.order_creation_price_after_discount),
                color = MaterialTheme.colors.onSurface
            )
            Text(
                modifier = Modifier
                    .padding(horizontal = dimensionResource(id = R.dimen.minor_100))
                    .constrainAs(priceAfterDiscountValue) {
                        top.linkTo(priceAfterDiscountLabel.top)
                        bottom.linkTo(priceAfterDiscountLabel.bottom)
                        end.linkTo(parent.end)
                    },
                text = product.productInfo.priceAfterDiscount,
                color = MaterialTheme.colors.onSurface
            )
        } else {
            WCTextButton(
                modifier = Modifier.constrainAs(discountButton) {
                    top.linkTo(price.bottom)
                    bottom.linkTo(bottomDivider.top)
                },
                onClick = onDiscountButtonClicked,
                enabled = editableControlsEnabled
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_add),
                    contentDescription = null
                )
                Text(
                    text = stringResource(id = R.string.order_creation_add_discount),
                    style = MaterialTheme.typography.body1
                )
            }
        }
        Divider(
            modifier = Modifier
                .constrainAs(bottomDivider) {
                    bottom.linkTo(buttonBarrier)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
                .padding(horizontal = dimensionResource(id = R.dimen.minor_100))
        )
        if (product.productInfo.isConfigurable) {
            WCTextButton(
                modifier = Modifier.constrainAs(configurationButton) {
                    top.linkTo(bottomDivider.bottom)
                    bottom.linkTo(configurationDivider.top)
                },
                onClick = onEditConfigurationClicked,
                enabled = editableControlsEnabled
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_configuration),
                    contentDescription = null
                )
                Text(
                    text = stringResource(id = R.string.extension_configure_button),
                    style = MaterialTheme.typography.body1
                )
            }

            Divider(
                modifier = Modifier
                    .constrainAs(configurationDivider) {
                        bottom.linkTo(removeButton.top)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                    }
                    .padding(horizontal = dimensionResource(id = R.dimen.minor_100))
            )
        }
        WCTextButton(
            modifier = Modifier.constrainAs(removeButton) {
                bottom.linkTo(parent.bottom)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
            },
            onClick = onRemoveProductClicked,
            enabled = editableControlsEnabled
        ) {
            Text(
                text = stringResource(id = R.string.order_creation_remove_product),
                color = if (editableControlsEnabled) {
                    colorResource(id = R.color.woo_red_60)
                } else {
                    colorResource(id = R.color.color_on_surface_disabled)
                }
            )
        }
    }
}

@Composable
private fun AmountPicker(
    modifier: Modifier = Modifier,
    onIncreaseClicked: () -> Unit,
    onDecreaseClicked: () -> Unit,
    product: OrderCreationProduct,
    isDecreaseButtonEnabled: Boolean = true,
    isIncreaseButtonEnabled: Boolean = true
) {
    Row(
        modifier = modifier
            .border(
                1.dp,
                colorResource(id = R.color.divider_color),
                shape = RoundedCornerShape(dimensionResource(id = R.dimen.corner_radius_large))
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.minor_100))
    ) {
        val decreaseButtonTint = if (isDecreaseButtonEnabled) MaterialTheme.colors.primary else Color.Gray
        val increaseButtonTint = if (isIncreaseButtonEnabled) MaterialTheme.colors.primary else Color.Gray
        IconButton(onClick = onDecreaseClicked, enabled = isDecreaseButtonEnabled) {
            Icon(
                imageVector = Icons.Filled.Remove,
                contentDescription =
                stringResource(id = R.string.order_creation_decrease_item_amount_content_description),
                tint = decreaseButtonTint
            )
        }
        Text(text = product.item.quantity.toInt().toString(), color = MaterialTheme.colors.onSurface)
        IconButton(onClick = onIncreaseClicked, enabled = isIncreaseButtonEnabled) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription =
                stringResource(id = R.string.order_creation_increase_item_amount_content_description),
                tint = increaseButtonTint
            )
        }
    }
}

@Composable
private fun getQuantityWithTotalText(product: OrderCreationProduct) =
    "${product.item.quantity.toInt()} $MULTIPLICATION_CHAR ${product.productInfo.pricePreDiscount}"

@Preview
@Preview(name = "Dark mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun AmountPickerPreview() {
    val item = Order.Item.EMPTY.copy(name = "Test Product", quantity = 3.0f, sku = "123")
    val product = OrderCreationProduct.ProductItem(
        item = item,
        productInfo = ProductInfo(
            imageUrl = "",
            isStockManaged = true,
            stockQuantity = 3.0,
            stockStatus = ProductStockStatus.InStock,
            pricePreDiscount = "$10",
            priceTotal = "$30",
            priceSubtotal = "$30",
            discountAmount = "$5",
            priceAfterDiscount = "$25",
            isConfigurable = false,
            productType = ProductType.SIMPLE,
            hasDiscount = item.discount > BigDecimal.ZERO
        )
    )
    WooThemeWithBackground {
        AmountPicker(Modifier, {}, {}, product)
    }
}

@Preview
@Preview(name = "Dark mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ExpandableProductCardPreview() {
    val item = Order.Item.EMPTY.copy(
        name = "Test Product Long Long Long Long Long Long Name",
        quantity = 3.0f,
        sku = "123"
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
            priceSubtotal = "$30",
            discountAmount = "$5",
            priceAfterDiscount = "$25",
            hasDiscount = true,
            isConfigurable = false,
            productType = ProductType.SIMPLE
        )
    )
    val state = remember { mutableStateOf(OrderCreateEditViewModel.ViewState()) }
    WooThemeWithBackground {
        ExpandableProductCard(state, product, {}, {}, {}, {}, {}, { _, _ -> })
    }
}

@Preview
@Preview(name = "Dark mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ExtendedProductCardContentPreview() {
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
            isConfigurable = false,
            productType = ProductType.SIMPLE,
            hasDiscount = true
        )
    )
    val state = remember { mutableStateOf(OrderCreateEditViewModel.ViewState()) }
    WooThemeWithBackground {
        ExtendedProductCardContent(state, product, {}, {}, {}, {}) {}
    }
}

@Preview
@Preview(name = "Dark mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ExtendedConfigurableProductCardContentPreview() {
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
    val state = remember { mutableStateOf(OrderCreateEditViewModel.ViewState()) }
    WooThemeWithBackground {
        ExtendedProductCardContent(state, product, {}, {}, {}, {}) {}
    }
}
