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
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.woocommerce.android.R
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.compose.component.WCTextButton
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.orders.creation.ProductUIModel
import com.woocommerce.android.ui.products.ProductStockStatus

private const val ANIM_DURATION_MILLIS = 128

@SuppressLint("UnusedTransitionTargetStateParameter")
@Composable
fun ExpandableProductCard(
    item: ProductUIModel,
    onRemoveProductClicked: () -> Unit,
    onDiscountButtonClicked: () -> Unit,
) {
    var isExpanded by rememberSaveable { mutableStateOf(false) }
    val transitionState = remember {
        MutableTransitionState(isExpanded).apply { targetState = !isExpanded }
    }
    val transition = updateTransition(transitionState, "expandableProductCard")
    val chevronRotation by transition.animateFloat(
        transitionSpec = { tween(durationMillis = ANIM_DURATION_MILLIS) }, label = "chevronRotation"
    ) {
        if (isExpanded) 0f else 180f
    }
    Surface(modifier = Modifier.background(MaterialTheme.colors.surface)) {
        ConstraintLayout(
            modifier = Modifier
                .fillMaxWidth()
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
            val (img, name, chevron, expandedPart) = createRefs()
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current).data(item.imageUrl).crossfade(true).build(),
                contentDescription = stringResource(R.string.product_image_content_description),
                contentScale = ContentScale.Fit,
                placeholder = painterResource(R.drawable.ic_product),
                error = painterResource(R.drawable.ic_product),
                modifier = Modifier
                    .constrainAs(img) {
                        start.linkTo(parent.start)
                        top.linkTo(parent.top)
                        bottom.linkTo(expandedPart.top)
                    }
                    .size(dimensionResource(R.dimen.major_375))
                    .padding(dimensionResource(id = R.dimen.major_100))
                    .clip(RoundedCornerShape(dimensionResource(id = R.dimen.corner_radius_image)))
            )
            Text(
                text = item.item.name,
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
                overflow = TextOverflow.Ellipsis
            )
            IconButton(
                onClick = { isExpanded = !isExpanded },
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
                        top.linkTo(img.bottom)
                    }
                    .fillMaxWidth(),
                enter = slideInVertically() + expandVertically(expandFrom = Alignment.Top) +
                        fadeIn(initialAlpha = 0.3f),
                exit = fadeOut() + shrinkVertically()
            ) {
                ExtendedProductCardContent(
                    item,
                    onRemoveProductClicked,
                    onDiscountButtonClicked,
                )
            }
        }
    }
}

@Composable
fun ExtendedProductCardContent(
    item: ProductUIModel,
    onRemoveProductClicked: () -> Unit,
    onDiscountButtonClicked: () -> Unit,
) {
    ConstraintLayout(
        modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.minor_100))
    ) {
        val (
            topDivider,
            bottomDivider,
            price,
            discountButton,
            discountAmount,
            priceAfterDiscountLabel,
            priceAfterDiscountValue,
            removeButton
        ) = createRefs()
        Divider(
            modifier = Modifier.constrainAs(topDivider) {
                top.linkTo(parent.top)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
            }
        )
        Row(
            modifier = Modifier
                .constrainAs(price) {
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    top.linkTo(topDivider.bottom)
                }
                .padding(dimensionResource(id = R.dimen.minor_100)),
        ) {
            Text(text = stringResource(id = R.string.product_price), modifier = Modifier.weight(1f))
            Text(
                modifier = Modifier.padding(end = dimensionResource(id = R.dimen.major_100)),
                color = colorResource(id = R.color.color_on_surface_disabled),
                text = "${item.item.quantity.toInt()} x ${item.item.pricePreDiscount}"
            )
            val totalAmountStyle = if (item.hasDiscount) {
                MaterialTheme.typography.body1.copy(
                    textDecoration = TextDecoration.LineThrough,
                    color = colorResource(id = R.color.color_on_surface_disabled)
                )
            } else {
                MaterialTheme.typography.body1
            }
            Text(text = "${item.item.subtotal}", style = totalAmountStyle)
        }
        if (item.hasDiscount) {
            WCTextButton(
                modifier = Modifier.constrainAs(discountButton) {
                    top.linkTo(price.bottom)
                },
                onClick = onDiscountButtonClicked
            ) {
                Text(
                    text = stringResource(id = R.string.discount),
                    style = MaterialTheme.typography.body1,
                )
                Spacer(Modifier.width(dimensionResource(id = R.dimen.minor_50)))
                Icon(
                    modifier = Modifier.size(dimensionResource(id = R.dimen.image_minor_40)),
                    imageVector = ImageVector.vectorResource(R.drawable.ic_edit),
                    contentDescription = null
                )
            }
            Text(
                modifier = Modifier.constrainAs(discountAmount) {
                    end.linkTo(parent.end)
                    top.linkTo(discountButton.top)
                    bottom.linkTo(discountButton.bottom)
                },
                text = "-${item.discountAmount}",
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
                text = stringResource(R.string.order_creation_price_after_discount)
            )
            Text(
                modifier = Modifier.constrainAs(priceAfterDiscountValue) {
                    top.linkTo(discountButton.bottom)
                    end.linkTo(parent.end)
                },
                text = item.priceAfterDiscount
            )
        } else {
            WCTextButton(
                modifier = Modifier.constrainAs(discountButton) {
                    top.linkTo(price.bottom)
                    bottom.linkTo(bottomDivider.top)
                },
                onClick = onDiscountButtonClicked
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
            modifier = Modifier.constrainAs(bottomDivider) {
                bottom.linkTo(removeButton.top)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
            }
        )
        WCTextButton(
            modifier = Modifier.constrainAs(removeButton) {
                bottom.linkTo(parent.bottom)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
            },
            onClick = onRemoveProductClicked
        ) {
            Text(
                text = stringResource(id = R.string.order_creation_remove_product),
                color = colorResource(
                    id = R.color.woo_red_60
                )
            )
        }
    }
}

@Preview
@Preview(name = "Dark mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ExpandableProductCardPreview() {
    val item = Order.Item.EMPTY.copy(name = "Test Product Long Long Long Long Long Long Name", quantity = 3.0f)
    val product = ProductUIModel(
        item = item,
        imageUrl = "",
        isStockManaged = true,
        stockQuantity = 3.0,
        stockStatus = ProductStockStatus.InStock,
        pricePreDiscount = "$10",
        priceTotal = "$30",
        priceSubtotal = "$30",
        discountAmount = "-$5",
        priceAfterDiscount = "$25"
    )
    WooThemeWithBackground {
        ExpandableProductCard(product, {}, {})
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
        subtotal = 30.toBigDecimal()
    )
    val product = ProductUIModel(
        item = item,
        imageUrl = "",
        isStockManaged = true,
        stockQuantity = 3.0,
        stockStatus = ProductStockStatus.InStock,
        pricePreDiscount = "$10",
        priceTotal = "$30",
        priceSubtotal = "$25",
        discountAmount = "$5",
        priceAfterDiscount = "$25"
    )
    WooThemeWithBackground {
        ExtendedProductCardContent(product, {}, {})
    }
}
