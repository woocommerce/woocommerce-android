package com.woocommerce.android.ui.orders.details

import android.content.res.Configuration
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.orders.Header
import java.math.BigDecimal

@Composable
fun ShippingLineSection(
    shippingLineDetails: List<OrderDetailViewModel.ShippingLineDetails>,
    formatCurrency: (amount: BigDecimal) -> String,
    modifier: Modifier = Modifier
) {
    if (shippingLineDetails.isNotEmpty()) {
        Column(modifier = modifier) {
            Header(text = stringResource(id = R.string.order_detail_shipping_header))
            Card(shape = RectangleShape) {
                Column(modifier = Modifier.padding(16.dp)) {
                    shippingLineDetails.forEachIndexed { i, shippingDetails ->
                        val itemModifier = if (i == 0) Modifier else Modifier.padding(top = 8.dp)
                        ShippingLineDetailsCard(
                            name = shippingDetails.name,
                            method = shippingDetails.shippingMethod?.title,
                            amount = formatCurrency(shippingDetails.amount),
                            modifier = itemModifier
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ShippingLineDetailsCard(
    name: String,
    method: String?,
    amount: String,
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
            .padding(dimensionResource(id = R.dimen.major_100))

    ) {
        Column(modifier = Modifier.weight(2f)) {
            Text(
                text = name,
                style = TextStyle(
                    fontWeight = FontWeight.Normal,
                    fontSize = 18.sp
                ),
                color = colorResource(id = R.color.color_on_surface),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (method != null) {
                Text(
                    text = method,
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
            text = amount,
            style = TextStyle(
                fontWeight = FontWeight.Normal,
                fontSize = 18.sp
            ),
            color = colorResource(id = R.color.color_on_surface),
        )
    }
}

@Preview
@Preview(name = "Dark mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ShippingLineDetailsPreview() {
    WooThemeWithBackground {
        ShippingLineDetailsCard(
            name = "UPS Shipping",
            method = "UPS",
            amount = "$10.00",
            modifier = Modifier.padding(16.dp)
        )
    }
}
