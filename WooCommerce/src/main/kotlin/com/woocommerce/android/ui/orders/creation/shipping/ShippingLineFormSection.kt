package com.woocommerce.android.ui.orders.creation.shipping

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.woocommerce.android.R
import com.woocommerce.android.model.ShippingMethod
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.orders.details.OrderDetailViewModel
import java.math.BigDecimal

@Composable
fun ShippingLineFormSection(
    shippingLineDetails: List<OrderDetailViewModel.ShippingLineDetails>,
    onAdd: () -> Unit,
    onEdit: (id: Long) -> Unit,
    formatCurrency: (amount: BigDecimal) -> String,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(shippingLineDetails.isNotEmpty()) {
        Card(shape = RectangleShape, modifier = modifier) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.padding(bottom = 16.dp)) {
                    Text(
                        text = "Shipping",
                        style = MaterialTheme.typography.h6,
                        modifier = modifier
                            .weight(2f, true)
                            .align(Alignment.CenterVertically)
                    )
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add shipping line",
                        modifier = Modifier
                            .align(Alignment.CenterVertically)
                            .clickable { onAdd() },
                        tint = MaterialTheme.colors.primary
                    )
                }

                shippingLineDetails.forEachIndexed { i, shippingDetails ->
                    val itemModifier = if (i == 0) Modifier else Modifier.padding(top = 8.dp)
                    ShippingLineEditCard(
                        shippingLine = shippingDetails,
                        onEdit = onEdit,
                        formatCurrency = formatCurrency,
                        modifier = itemModifier
                    )
                }
            }
        }
    }
}

@Composable
fun ShippingLineEditCard(
    shippingLine: OrderDetailViewModel.ShippingLineDetails,
    formatCurrency: (amount: BigDecimal) -> String,
    onEdit: (id: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .border(
                brush = SolidColor(MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                width = 1.dp,
                shape = RoundedCornerShape(dimensionResource(id = R.dimen.corner_radius_large))
            )
            .clickable { onEdit(shippingLine.id) }
            .padding(dimensionResource(id = R.dimen.major_100))

    ) {
        Column(
            modifier = Modifier
                .weight(2f)
                .align(Alignment.CenterVertically)
        ) {
            Text(
                text = shippingLine.name,
                style = TextStyle(
                    fontWeight = FontWeight.Normal,
                    fontSize = 18.sp
                ),
                color = colorResource(id = R.color.color_on_surface),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (shippingLine.shippingMethod != null) {
                Text(
                    text = shippingLine.shippingMethod.title,
                    style = TextStyle(
                        fontWeight = FontWeight.Normal,
                        fontSize = 14.sp,
                        color = Color.Gray
                    ),
                    color = colorResource(id = R.color.color_on_surface_medium),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = formatCurrency(shippingLine.amount),
            style = TextStyle(
                fontWeight = FontWeight.Normal,
                fontSize = 18.sp
            ),
            color = colorResource(id = R.color.color_on_surface),
            modifier = Modifier.align(Alignment.CenterVertically)
        )
        Icon(
            imageVector = Icons.Outlined.Edit,
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .padding(start = 16.dp)
        )
    }
}

@Preview
@Preview(name = "Dark mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ShippingLineDetailsPreview() {
    WooThemeWithBackground {
        OrderDetailViewModel.ShippingLineDetails(
            id = 1L,
            name = "UPS Shipping",
            shippingMethod = ShippingMethod(id = "ups", title = "UPS"),
            amount = BigDecimal.TEN,
        )
    }
}

@Preview
@Composable
fun ShippingLineFormSectionPreview() {
    val shippingDetails = List(3) { i ->
        OrderDetailViewModel.ShippingLineDetails(
            id = i * 1L,
            shippingMethod = null,
            amount = BigDecimal.TEN * i.toBigDecimal(),
            name = "Shipping $i"
        )
    }
    WooThemeWithBackground {
        ShippingLineFormSection(
            shippingLineDetails = shippingDetails,
            formatCurrency = { it.toString() },
            onAdd = { },
            onEdit = { }
        )
    }
}
