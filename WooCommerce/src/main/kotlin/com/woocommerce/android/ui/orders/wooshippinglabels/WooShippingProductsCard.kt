package com.woocommerce.android.ui.orders.wooshippinglabels

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
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
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.util.StringUtils

@Composable
fun ShippingProductsCard(
    shippableItems: ShippableItems,
    modifier: Modifier = Modifier,
    isExpanded: Boolean = false,
    onExpand: (Boolean) -> Unit = {}
) {
    Column(
        modifier.animateContentSize()
    ) {
        ShippingProductsCardHeader(
            shippableItems = shippableItems,
            isExpanded = isExpanded,
            modifier = Modifier
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
                shippableItems = shippableItems.shippableItems,
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
                shippableItems = ShippableItems(
                    shippableItems = generateItems(6),
                    totalWeight = "8.5kg",
                    totalPrice = "$92.78"
                ),
                isExpanded = isExpanded
            )
        }
    }
}

@Composable
private fun ShippingProductsCardHeader(
    shippableItems: ShippableItems,
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
        val items = StringUtils.getQuantityString(
            context = LocalContext.current,
            quantity = shippableItems.shippableItems.size,
            default = R.string.shipping_label_package_details_items_count_many,
            one = R.string.shipping_label_package_details_items_count_one,
        )
        Row {
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
                    shippableItems.totalWeight,
                    shippableItems.totalPrice
                ),
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .padding(end = dimensionResource(R.dimen.major_100)),
                style = MaterialTheme.typography.subtitle1,
                color = colorResource(id = R.color.color_on_surface_medium)
            )
            Icon(
                painter = painterResource(R.drawable.ic_arrow_down),
                tint = MaterialTheme.colors.primary,
                contentDescription =
                stringResource(id = R.string.shipping_label_package_details_items_expand_content_description),
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .size(dimensionResource(R.dimen.image_minor_100))
                    .rotate(rotationAnimation.value)
            )
        }
    }
}

@Preview
@Composable
private fun ShippingProductsCardHeaderPreview() {
    val shippableItems = ShippableItems(
        shippableItems = generateItems(4),
        totalWeight = "8.5kg",
        totalPrice = "$92.78"
    )
    val isExpanded = remember { mutableStateOf(false) }

    WooThemeWithBackground {
        Box(modifier = Modifier.padding(dimensionResource(R.dimen.major_100))) {
            ShippingProductsCardHeader(
                shippableItems = shippableItems,
                isExpanded = isExpanded.value,
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
    shippableItems: List<ShippableItem>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        shippableItems.forEach {
            ShippingProduct(
                title = it.title,
                description = it.description,
                weight = it.weight,
                price = it.price,
                quantity = it.quantity
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
    quantity: Int,
    modifier: Modifier = Modifier,
    imageUrl: String? = null
) {
    RoundedCornerBoxWithBorder(modifier.padding(dimensionResource(R.dimen.major_100))) {
        ShippingProductDetails(
            title = title,
            description = description,
            weight = weight,
            imageUrl = imageUrl,
            quantity = quantity
        )
        ShippingProductInfo(
            summary = price,
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
                quantity = 1
            )
        }
    }
}

@Composable
private fun ShippingProductDetails(
    title: String,
    description: String,
    weight: String,
    quantity: Int,
    modifier: Modifier = Modifier,
    imageUrl: String? = null
) {
    Row(modifier = modifier) {
        Box {
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
                    .size(dimensionResource(R.dimen.major_300))
                    .clip(RoundedCornerShape(3.dp))
            )
            val quantityPadding = dimensionResource(R.dimen.image_minor_40) / 2
            QuantityBadge(
                quantity = quantity,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .graphicsLayer {
                        translationX = quantityPadding.toPx()
                    }
            )
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
                ShippingProductInfo(description)
            }

            if (weight.isNotEmpty()) {
                ShippingProductInfo(summary = weight)
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
            quantity = 1
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
        style = MaterialTheme.typography.caption,
        color = colorResource(id = R.color.color_on_surface_medium),
        modifier = modifier
    )
}

@Composable
private fun QuantityBadge(
    quantity: Int,
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
            text = quantity.toString(),
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
            QuantityBadge(quantity = 1, modifier = Modifier.padding(dimensionResource(R.dimen.major_100)))
            QuantityBadge(quantity = 10, modifier = Modifier.padding(dimensionResource(R.dimen.major_100)))
            QuantityBadge(quantity = 45, modifier = Modifier.padding(dimensionResource(R.dimen.major_100)))
            QuantityBadge(quantity = 100, modifier = Modifier.padding(dimensionResource(R.dimen.major_100)))
        }
    }
}

@Composable
private fun RoundedCornerBoxWithBorder(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit = {},
) {
    Box(
        modifier = Modifier
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
    ) {
        content()
    }
}

fun generateItems(number: Int): List<ShippableItem> {
    return List(number) { i ->
        val id = i + 1
        ShippableItem(
            productId = id.toLong(),
            title = "Title $id",
            description = "23 x 23 x 52 cm",
            weight = "1.5kg",
            price = "$12.99",
            quantity = i % 2 + 1
        )
    }
}

class IsExpandedProvider : PreviewParameterProvider<Boolean> {
    override val values = sequenceOf(true, false)
}
