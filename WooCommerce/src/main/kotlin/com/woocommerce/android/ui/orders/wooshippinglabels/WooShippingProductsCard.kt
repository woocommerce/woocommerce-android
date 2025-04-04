package com.woocommerce.android.ui.orders.wooshippinglabels

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.woocommerce.android.R
import com.woocommerce.android.extensions.formatToString
import com.woocommerce.android.ui.compose.component.SelectionCheck
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.util.StringUtils

@Composable
fun ShippingProductsCard(
    shippableItems: ShippableItemsUI,
    modifier: Modifier = Modifier,
    iconColor: Color = MaterialTheme.colors.primary,
    isExpanded: Boolean = false,
    onExpand: (Boolean) -> Unit = {}
) {
    Column(
        modifier.animateContentSize()
    ) {
        ShippingProductsCardHeader(
            shippableItems = shippableItems,
            isExpanded = isExpanded,
            iconColor = iconColor,
            modifier = Modifier
                .clip(RoundedCornerShape(dimensionResource(R.dimen.corner_radius_large)))
                .clickable { onExpand(!isExpanded) }
                .padding(
                    start = dimensionResource(R.dimen.major_100),
                    end = dimensionResource(R.dimen.minor_100),
                    top = dimensionResource(R.dimen.minor_100),
                    bottom = dimensionResource(R.dimen.minor_100)
                )
        )
        if (isExpanded) {
            ShippingProductsList(
                shippableItemUI = shippableItems.shippableItems,
            )
        }
    }
}

@Preview
@Composable
private fun ShippingProductsCardPreview(@PreviewParameter(IsExpandedProvider::class) isExpanded: Boolean) {
    WooThemeWithBackground {
        Box(modifier = Modifier.padding(dimensionResource(R.dimen.major_100))) {
            ShippingProductsCard(
                shippableItems = ShippableItemsUI(
                    shippableItems = generateItems(6),
                    formattedTotalWeight = "8.5kg",
                    formattedTotalPrice = "$92.78"
                ),
                isExpanded = isExpanded
            )
        }
    }
}

@Composable
private fun ShippingProductsCardHeader(
    shippableItems: ShippableItemsUI,
    iconColor: Color,
    modifier: Modifier = Modifier,
    isExpanded: Boolean = false
) {
    val boxModifier = if (isExpanded) {
        Modifier
            .background(
                color = MaterialTheme.colors.surface,
                shape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_large))
            )
            .then(modifier)
    } else {
        Modifier
            .background(
                color = MaterialTheme.colors.surface,
                shape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_large))
            )
            .border(
                width = dimensionResource(R.dimen.minor_10),
                color = colorResource(R.color.divider_color),
                shape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_large))
            )
            .then(modifier)
    }

    val rotationAnimation = animateFloatAsState(targetValue = if (isExpanded) 180f else 0f, label = "rotationAnimation")

    Box(
        modifier = boxModifier
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProductsSummary(
                totalItems = shippableItems.shippableItems.size,
                totalWeight = shippableItems.formattedTotalWeight,
                totalPrice = shippableItems.formattedTotalPrice,
                modifier = Modifier
                    .weight(1f)
                    .padding(dimensionResource(R.dimen.minor_100))
            )
            Icon(
                painter = painterResource(R.drawable.ic_arrow_down),
                tint = iconColor,
                contentDescription =
                stringResource(id = R.string.shipping_label_package_details_items_expand_content_description),
                modifier = Modifier
                    .size(dimensionResource(R.dimen.image_minor_100))
                    .rotate(rotationAnimation.value)
            )
        }
    }
}

@Preview
@Composable
private fun ShippingProductsCardHeaderPreview() {
    val shippableItems = ShippableItemsUI(
        shippableItems = generateItems(4),
        formattedTotalWeight = "8.5kg",
        formattedTotalPrice = "$92.78"
    )
    val isExpanded = remember { mutableStateOf(false) }

    WooThemeWithBackground {
        Box(modifier = Modifier.padding(dimensionResource(R.dimen.major_100))) {
            ShippingProductsCardHeader(
                shippableItems = shippableItems,
                isExpanded = isExpanded.value,
                iconColor = MaterialTheme.colors.primary,
                modifier = Modifier
                    .clickable { isExpanded.value = !isExpanded.value }
                    .padding(
                        horizontal = dimensionResource(R.dimen.major_100),
                        vertical = dimensionResource(R.dimen.minor_100)
                    )
            )
        }
    }
}

@Composable
private fun ShippingProductsList(
    shippableItemUI: List<ShippableItemUI>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        shippableItemUI.forEach {
            ShippingProduct(
                title = it.title,
                description = it.formattedSize,
                weight = it.formattedWeight,
                price = it.formattedPrice,
                quantity = it.quantity,
                imageUrl = it.imageUrl
            )
            Spacer(modifier = Modifier.size(dimensionResource(R.dimen.minor_100)))
        }
    }
}

@Composable
private fun ShippingProduct(
    title: String,
    description: String,
    weight: String,
    price: String,
    quantity: Float,
    modifier: Modifier = Modifier,
    imageUrl: String? = null
) {
    RoundedCornerBoxWithBorder(
        innerModifier = modifier.padding(dimensionResource(R.dimen.major_100))
    ) {
        ShippingProductDetails(
            title = title,
            description = description,
            weight = weight,
            imageUrl = imageUrl,
            quantity = quantity
        )

        Text(
            text = price,
            style = MaterialTheme.typography.body2,
            color = MaterialTheme.colors.onSurface,
            modifier = Modifier.align(Alignment.BottomEnd)
        )
    }
}

@Preview
@Composable
internal fun ShippingProductPreview() {
    WooThemeWithBackground {
        Box(modifier = Modifier.padding(dimensionResource(R.dimen.major_100))) {
            ShippingProduct(
                title = "Title",
                description = "23 x 23 x 52 cm",
                weight = "0.6kg",
                price = "$12.99",
                quantity = 1f
            )
        }
    }
}

@Composable
private fun ShippingProductDetails(
    title: String,
    description: String,
    weight: String,
    quantity: Float,
    modifier: Modifier = Modifier,
    imageUrl: String? = null,
    imageSize: Dp = dimensionResource(R.dimen.image_major_56),
    displayQuantity: Boolean = true
) {
    Row(modifier = modifier) {
        Box(modifier = Modifier.align(Alignment.CenterVertically)) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageUrl)
                    .crossfade(true)
                    .build(),
                placeholder = painterResource(R.drawable.ic_product),
                error = painterResource(R.drawable.ic_product),
                contentDescription = stringResource(R.string.product_image_content_description),
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .size(imageSize)
                    .clip(RoundedCornerShape(3.dp))
            )
            if (displayQuantity) {
                val quantityPadding = dimensionResource(R.dimen.image_minor_40) / 2
                QuantityBadge(
                    quantity = quantity,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .graphicsLayer {
                            translationY = -quantityPadding.toPx()
                            translationX = quantityPadding.toPx()
                        }
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = dimensionResource(R.dimen.major_100))
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.subtitle1,
                color = MaterialTheme.colors.onSurface,
            )

            if (description.isNotEmpty()) {
                ShippingProductInfo(
                    summary = description,
                    modifier = Modifier.padding(top = dimensionResource(R.dimen.minor_50))
                )
            }

            if (weight.isNotEmpty()) {
                ShippingProductInfo(
                    summary = weight,
                    modifier = Modifier.padding(top = dimensionResource(R.dimen.minor_50))
                )
            }
        }
    }
}

@Preview
@Composable
internal fun ShippingProductDetailsPreview() {
    WooThemeWithBackground {
        ShippingProductDetails(
            title = "Title",
            description = "23 x 23 x 52 cm",
            weight = "0.6kg",
            quantity = 1f
        )
    }
}

@Composable
private fun ShippingProductInfo(
    summary: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = summary,
        style = MaterialTheme.typography.body2,
        color = colorResource(id = R.color.color_on_surface_medium),
        modifier = modifier
    )
}

@Composable
private fun QuantityBadge(
    quantity: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                color = MaterialTheme.colors.onSurface,
                shape = CircleShape
            )
            .border(
                width = dimensionResource(R.dimen.minor_10),
                color = MaterialTheme.colors.surface,
                shape = CircleShape
            )
            .sizeIn(
                minWidth = dimensionResource(R.dimen.image_minor_50),
                minHeight = dimensionResource(R.dimen.image_minor_50)
            )
            .padding(dimensionResource(R.dimen.minor_50))
    ) {
        Text(
            text = quantity.formatToString(),
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.surface,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Preview
@Composable
internal fun QuantityBadgePreview() {
    WooThemeWithBackground {
        Column(modifier = Modifier.background(Color.DarkGray)) {
            QuantityBadge(quantity = 1f, modifier = Modifier.padding(dimensionResource(R.dimen.major_100)))
            QuantityBadge(quantity = 10f, modifier = Modifier.padding(dimensionResource(R.dimen.major_100)))
            QuantityBadge(quantity = 45.56f, modifier = Modifier.padding(dimensionResource(R.dimen.major_100)))
            QuantityBadge(quantity = 100f, modifier = Modifier.padding(dimensionResource(R.dimen.major_100)))
        }
    }
}

@Composable
fun RoundedCornerBoxWithBorder(
    modifier: Modifier = Modifier,
    innerModifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colors.surface,
    borderColor: Color = colorResource(R.color.divider_color),
    borderWidth: Dp = dimensionResource(R.dimen.minor_10),
    content: @Composable BoxScope.() -> Unit = {},
) {
    Box(
        modifier = modifier
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_large))
            )
            .border(
                width = borderWidth,
                color = borderColor,
                shape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_large))
            )
            .clip(RoundedCornerShape(dimensionResource(R.dimen.corner_radius_large)))
            .then(innerModifier)

    ) {
        content()
    }
}

@Composable
fun ProductsSummary(
    totalItems: Int,
    totalWeight: String,
    totalPrice: String,
    modifier: Modifier = Modifier
) {
    val items = StringUtils.getQuantityString(
        context = LocalContext.current,
        quantity = totalItems,
        default = R.string.shipping_label_package_details_items_count_many,
        one = R.string.shipping_label_package_details_items_count_one,
    )
    Row(modifier = modifier) {
        Text(
            text = items,
            style = MaterialTheme.typography.subtitle1,
            color = MaterialTheme.colors.onSurface,
            modifier = Modifier
                .weight(1f)
                .align(Alignment.CenterVertically),
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = stringResource(
                R.string.shipping_label_package_details_items_weight_price,
                totalWeight,
                totalPrice
            ),
            modifier = Modifier.align(Alignment.CenterVertically),
            style = MaterialTheme.typography.subtitle1,
            color = colorResource(id = R.color.color_on_surface_medium)
        )
    }
}

@Composable
fun SelectableShippingProduct(
    title: String,
    description: String,
    weight: String,
    price: String,
    quantity: Float,
    isSelected: Boolean,
    onSelectionChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    imageUrl: String? = null,
) {
    RoundedCornerBoxWithBorder(
        modifier = modifier,
        innerModifier = Modifier
            .clickable { onSelectionChange(!isSelected) }
            .padding(
                top = 16.dp,
                start = 8.dp,
                end = 16.dp,
                bottom = 16.dp
            )
    ) {
        SelectableShippingProductDetails(
            title = title,
            description = description,
            weight = weight,
            price = price,
            quantity = quantity,
            isSelected = isSelected,
            onSelectionChange = onSelectionChange,
            imageUrl = imageUrl
        )
    }
}

@Composable
fun ExpandableSelectableShippingProduct(
    title: String,
    description: String,
    weight: String,
    price: String,
    quantity: Float,
    isSelected: Boolean,
    onSelectionChange: (Boolean) -> Unit,
    onInnerSelectionChange: (Boolean, Int) -> Unit,
    selectedIndexes: Set<Int>,
    isExpanded: Boolean,
    onExpand: (Boolean) -> Unit,
    singleWeight: String,
    singlePrice: String,
    modifier: Modifier = Modifier,
    imageUrl: String? = null,
) {
    val rotationAnimation = animateFloatAsState(targetValue = if (isExpanded) 180f else 0f, label = "rotationAnimation")
    RoundedCornerBoxWithBorder(
        modifier = modifier,
        innerModifier = Modifier.clickable { onSelectionChange(!isSelected) }
    ) {
        Column(modifier = Modifier.animateContentSize()) {
            Row(
                Modifier
                    .height(IntrinsicSize.Min)
                    .padding(start = 8.dp)
            ) {
                SelectionCheck(
                    isSelected = isSelected,
                    onSelectionChange = onSelectionChange,
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .padding(8.dp)
                )
                ShippingProductDetails(
                    title = title,
                    description = description,
                    weight = weight,
                    imageUrl = imageUrl,
                    quantity = quantity,
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .padding(vertical = 16.dp)
                        .weight(1f)
                )

                Column(
                    modifier = Modifier
                        .padding(end = 8.dp, bottom = 16.dp)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.Bottom,
                    horizontalAlignment = Alignment.End
                ) {
                    IconButton(onClick = { onExpand(!isExpanded) }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_down),
                            tint = MaterialTheme.colors.primary,
                            contentDescription = stringResource(
                                id = R.string.shipping_label_package_details_items_expand_content_description
                            ),
                            modifier = Modifier
                                .size(dimensionResource(R.dimen.image_minor_100))
                                .rotate(rotationAnimation.value)
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = price,
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.onSurface,
                        modifier = modifier.padding(end = 8.dp)
                    )
                }
            }
            if (isExpanded) {
                for (index in 0 until quantity.toInt()) {
                    Divider()
                    val isInnerItemSelected = index in selectedIndexes
                    SelectableShippingProductDetails(
                        title = title,
                        description = description,
                        weight = singleWeight,
                        price = singlePrice,
                        quantity = quantity,
                        isSelected = isInnerItemSelected,
                        onSelectionChange = {
                            onInnerSelectionChange(isInnerItemSelected, index)
                        },
                        imageUrl = imageUrl,
                        modifier = Modifier
                            .clickable { onInnerSelectionChange(isInnerItemSelected, index) }
                            .padding(start = 24.dp, top = 16.dp, end = 16.dp, bottom = 16.dp),
                        imageSize = dimensionResource(R.dimen.image_minor_100),
                        displayQuantity = false
                    )
                }
            }
        }
    }
}

@Composable
fun SelectableShippingProductDetails(
    title: String,
    description: String,
    weight: String,
    price: String,
    quantity: Float,
    isSelected: Boolean,
    onSelectionChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    imageUrl: String? = null,
    imageSize: Dp = dimensionResource(R.dimen.image_major_50),
    displayQuantity: Boolean = true
) {
    Row(modifier) {
        SelectionCheck(
            isSelected = isSelected,
            onSelectionChange = onSelectionChange,
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .padding(8.dp)
        )
        ShippingProductDetails(
            title = title,
            description = description,
            weight = weight,
            imageUrl = imageUrl,
            quantity = quantity,
            imageSize = imageSize,
            displayQuantity = displayQuantity,
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .weight(1f)
        )

        Text(
            text = price,
            style = MaterialTheme.typography.body2,
            color = MaterialTheme.colors.onSurface,
            modifier = Modifier.align(Alignment.Bottom)
        )
    }
}

@Preview
@Composable
fun SelectableShippingProductPreview() {
    WooThemeWithBackground {
        Box(modifier = Modifier.padding(dimensionResource(R.dimen.major_100))) {
            SelectableShippingProduct(
                title = "Title",
                description = "23 x 23 x 52 cm",
                weight = "0.6kg",
                quantity = 1f,
                price = "$12.99",
                isSelected = true,
                onSelectionChange = {}
            )
        }
    }
}

@Preview
@Composable
fun ExpandableSelectableShippingProductPreview(@PreviewParameter(IsExpandedProvider::class) isExpanded: Boolean) {
    WooThemeWithBackground {
        Box(modifier = Modifier.padding(dimensionResource(R.dimen.major_100))) {
            ExpandableSelectableShippingProduct(
                title = "Title",
                description = "23 x 23 x 52 cm",
                weight = "0.6kg",
                quantity = 3f,
                price = "$12.99",
                isSelected = true,
                onSelectionChange = {},
                isExpanded = isExpanded,
                onExpand = {},
                singleWeight = "0.6kg",
                singlePrice = "$12.99",
                selectedIndexes = setOf(0, 1),
                onInnerSelectionChange = { _, _ -> }
            )
        }
    }
}

fun generateItems(number: Int): List<ShippableItemUI> {
    return List(number) { i ->
        val id = i + 1
        ShippableItemUI(
            itemId = id.toLong(),
            productId = id.toLong(),
            title = "Title $id",
            formattedSize = "23 x 23 x 52 cm",
            formattedWeight = "1.5kg",
            formattedPrice = "$12.99",
            quantity = (i % 2 + 1).toFloat()
        )
    }
}

class IsExpandedProvider : PreviewParameterProvider<Boolean> {
    override val values = sequenceOf(true, false)
}
