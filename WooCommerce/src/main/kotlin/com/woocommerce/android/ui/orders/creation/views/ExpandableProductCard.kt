package com.woocommerce.android.ui.orders.creation.views

import android.annotation.SuppressLint
import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Tune
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.lerp
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
import com.woocommerce.android.ui.orders.creation.OrderCreateEditViewModel.Companion.MAX_PRODUCT_QUANTITY
import com.woocommerce.android.ui.orders.creation.OrderCreationProduct
import com.woocommerce.android.ui.orders.creation.ProductInfo
import com.woocommerce.android.ui.orders.creation.isSynced
import com.woocommerce.android.ui.products.ProductStockStatus
import com.woocommerce.android.ui.products.ProductType
import com.woocommerce.android.util.getVariationAttributesAndStockText
import java.math.BigDecimal

const val ANIM_DURATION_MILLIS = 128
const val MULTIPLICATION_CHAR = "×"

@SuppressLint("UnusedTransitionTargetStateParameter")
@Composable
fun ExpandableProductCard(
    state: State<OrderCreateEditViewModel.ViewState?>,
    product: OrderCreationProduct,
    onRemoveProductClicked: () -> Unit,
    onDiscountButtonClicked: () -> Unit,
    onItemAmountChanged: (ProductAmountEvent) -> Unit,
    onEditConfigurationClicked: () -> Unit,
    onProductExpanded: (isExpanded: Boolean, product: OrderCreationProduct) -> Unit,
    modifier: Modifier = Modifier,
    isExpanded: Boolean = false,
) {
    val transitionState = remember {
        MutableTransitionState(isExpanded).apply { targetState = !isExpanded }
    }
    val transition = updateTransition(transitionState, "expandableProductCard")
    val chevronRotation by transition.animateFloat(
        transitionSpec = { tween(durationMillis = ANIM_DURATION_MILLIS) },
        label = "chevronRotation"
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
                .padding(dimensionResource(id = R.dimen.major_100))
                .size(dimensionResource(R.dimen.major_300)),
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
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
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
            if (product.item.isSynced()) {
                Text(
                    modifier = Modifier
                        .constrainAs(quantity) {
                            start.linkTo(name.start)
                            top.linkTo(stock.bottom)
                            end.linkTo(price.start)
                            width = Dimension.fillToConstraints
                        }
                        .padding(
                            start = dimensionResource(id = R.dimen.major_100),
                            end = dimensionResource(id = R.dimen.major_100),
                            bottom = dimensionResource(id = R.dimen.major_100),
                        ),
                    style = MaterialTheme.typography.body2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    text = getQuantityWithTotalText(product),
                    color = colorResource(id = R.color.color_on_surface_disabled)
                )
                Text(
                    modifier = Modifier.constrainAs(price) {
                        end.linkTo(chevron.start)
                        top.linkTo(quantity.top)
                        start.linkTo(quantity.end)
                    },
                    style = MaterialTheme.typography.body2,
                    text = product.productInfo.priceAfterDiscount,
                    color = MaterialTheme.colors.onSurface
                )
            } else {
                // Spacer here because otherwise the layouts will jump in size when the product is synced
                Spacer(
                    modifier = Modifier
                        .constrainAs(quantity) { // Use the same constraints as the quantity Text
                            start.linkTo(name.start)
                            top.linkTo(stock.bottom)
                        }
                        .height(dimensionResource(id = R.dimen.major_200))
                )
            }
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
                onItemAmountChanged,
                onEditConfigurationClicked,
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
    onItemAmountChanged: (ProductAmountEvent) -> Unit,
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

        val editableControlsEnabled = state.value?.isEditable ?: false
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
                text = stringResource(id = R.string.order_creation_products_order_count),
                color = MaterialTheme.colors.onSurface
            )
            val areAmountButtonsEnabled = if (isBundledProduct) {
                editableControlsEnabled
            } else {
                product.item.isSynced() && editableControlsEnabled
            }
            AmountPicker(
                onItemAmountChanged = onItemAmountChanged,
                product = product,
                isAmountChangeable = areAmountButtonsEnabled,
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
                Spacer(modifier = Modifier.size(4.dp))
                Text(
                    text = stringResource(id = R.string.order_creation_add_discount),
                    style = MaterialTheme.typography.body1,
                    modifier = Modifier.align(Alignment.CenterVertically)
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
                    imageVector = Icons.Filled.Tune,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.size(4.dp))
                Text(
                    text = stringResource(id = R.string.extension_configure_button),
                    style = MaterialTheme.typography.body1,
                    modifier = Modifier.align(Alignment.CenterVertically)
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
    onItemAmountChanged: (ProductAmountEvent) -> Unit,
    product: OrderCreationProduct,
    isAmountChangeable: Boolean = true,
) {
    val amount = product.item.quantity.toInt().toString()
    var textFieldValue by remember(amount) { mutableStateOf(TextFieldValue(amount)) }
    val interactionSource = remember { MutableInteractionSource() }
    val isAmountFieldInFocus by interactionSource.collectIsFocusedAsState()

    val focusManager = LocalFocusManager.current

    val elevation = animateDpAsState(
        targetValue = if (isAmountFieldInFocus) 4.dp else 0.dp,
        label = "elevation"
    )

    val fontStyleAnimation = animateFloatAsState(
        targetValue = if (isAmountFieldInFocus) 1.0F else 0.0F,
        label = "fontSize"
    )

    val nonFocusedFontStyle = MaterialTheme.typography.subtitle1
    val focusedFontStyle = MaterialTheme.typography.h4
    val textStyle by remember(fontStyleAnimation.value) {
        derivedStateOf {
            lerp(
                nonFocusedFontStyle,
                focusedFontStyle,
                fontStyleAnimation.value
            )
        }
    }

    Card(
        modifier = modifier,
        elevation = elevation.value,
        shape = RoundedCornerShape(dimensionResource(id = R.dimen.corner_radius_large)),
        border = BorderStroke(
            width = 1.dp,
            color = colorResource(id = R.color.divider_color),
        )
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.minor_100)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val isPlusMinusEnabled = isAmountChangeable && !isAmountFieldInFocus
            val plusButtonTint = if (isPlusMinusEnabled) MaterialTheme.colors.primary else Color.Gray
            val isLastItem = amount == "1"

            val minusButtonTint = when {
                !isPlusMinusEnabled -> Color.Gray
                isLastItem -> MaterialTheme.colors.error
                else -> MaterialTheme.colors.primary
            }

            val decreaseIcon = if (isLastItem) Icons.Filled.DeleteOutline else Icons.Filled.Remove
            IconButton(
                onClick = { onItemAmountChanged(ProductAmountEvent.Decrease) },
                enabled = isPlusMinusEnabled
            ) {
                Icon(
                    imageVector = decreaseIcon,
                    contentDescription =
                    stringResource(id = R.string.order_creation_decrease_item_amount_content_description),
                    tint = minusButtonTint
                )
            }
            BasicTextField(
                value = textFieldValue,
                onValueChange = { value ->
                    try {
                        if (value.text.isNotBlank() && value.text.isNotEmpty()) {
                            // try converting to int to validate that input is a number
                            val intValue = value.text.toInt()
                            if (intValue in 0..MAX_PRODUCT_QUANTITY) {
                                textFieldValue = value
                            }
                        } else {
                            textFieldValue = value
                        }
                    } catch (_: NumberFormatException) {
                        // no-op
                    }
                },
                singleLine = true,
                textStyle = textStyle.copy(color = MaterialTheme.colors.onSurface),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        onItemAmountChanged(ProductAmountEvent.Change(textFieldValue.text))
                        focusManager.clearFocus()
                    }
                ),
                interactionSource = interactionSource,
                cursorBrush = SolidColor(MaterialTheme.colors.primary),
                modifier = Modifier
                    .padding(horizontal = dimensionResource(id = R.dimen.minor_25))
                    .widthIn(min = 12.dp, max = 128.dp)
                    .width(IntrinsicSize.Min),
                enabled = isAmountChangeable,
            )
            IconButton(
                onClick = { onItemAmountChanged(ProductAmountEvent.Increase) },
                enabled = isPlusMinusEnabled
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription =
                    stringResource(id = R.string.order_creation_increase_item_amount_content_description),
                    tint = plusButtonTint
                )
            }
        }
    }
}

@Composable
fun getQuantityWithTotalText(product: OrderCreationProduct) =
    "${product.item.quantity.toInt()} $MULTIPLICATION_CHAR ${product.productInfo.pricePreDiscount}"

sealed class ProductAmountEvent {
    object Increase : ProductAmountEvent()
    object Decrease : ProductAmountEvent()
    data class Change(val newAmount: String) : ProductAmountEvent()
}

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
        AmountPicker(Modifier, {}, product)
    }
}

@Preview(widthDp = 220)
@Preview
@Preview(name = "Dark mode", uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 220)
@Preview(name = "Dark mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ExpandableProductCardPreview() {
    val item = Order.Item.EMPTY.copy(
        name = "Test Product Long Long Long Long Long Long Name",
        quantity = 3.0f,
        sku = "123",
        itemId = 10L
    )
    val product = OrderCreationProduct.ProductItem(
        item = item,
        productInfo = ProductInfo(
            imageUrl = "",
            isStockManaged = true,
            stockQuantity = 3.0,
            stockStatus = ProductStockStatus.InStock,
            pricePreDiscount = "$1000",
            priceTotal = "$3000",
            priceSubtotal = "$3000",
            discountAmount = "$5",
            priceAfterDiscount = "$2995",
            hasDiscount = true,
            isConfigurable = false,
            productType = ProductType.SIMPLE
        )
    )
    val state = remember { mutableStateOf(OrderCreateEditViewModel.ViewState()) }
    WooThemeWithBackground {
        ExpandableProductCard(state, product, {}, {}, {}, {}, { _, _ -> })
    }
}

@Preview
@Preview(name = "Dark mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ExpandableProductCardUnsyncedPreview() {
    val item = Order.Item.EMPTY.copy(
        name = "Test Product Long Long Long Long Long Long Name",
        quantity = 3.0f,
        sku = "123",
        itemId = 0L
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
        ExpandableProductCard(state, product, {}, {}, {}, {}, { _, _ -> })
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
        ExtendedProductCardContent(state, product, {}, {}, {}) {}
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
        sku = "SKU123",
        itemId = 10L
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
        ExtendedProductCardContent(state, product, {}, {}, {}) {}
    }
}
